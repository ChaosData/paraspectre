package trust.nccgroup.paraspectre.agent;

import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.commons.lang3.ClassUtils;
import trikita.log.Log;
import trust.nccgroup.paraspectre.agent.BluePill.BluePill;
import trust.nccgroup.paraspectre.core.Config;
import trust.nccgroup.paraspectre.core.Hook;
import trust.nccgroup.paraspectre.core.ReflectSetup;
import trust.nccgroup.paraspectre.core.config.Net;
import trust.nccgroup.paraspectre.core.config.matcher.Clazz;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PreMain {

  private static final String TAG = "PS/Agent";

  //TODO: constructor hooking

  //TODO: dynamic unhooking +
  //      - use Interceptor instances
  //      - instances will have:
  //        - enabled (boolean)
  //        - method (Method)
  //        - evalString (String)
  //      - keep track of them
  //        - complicated
  //      - on updates, compare and update / disable / add new as appropriate
  //        - for updates add a file change listener
  //          (http://stackoverflow.com/questions/16251273/can-i-watch-for-single-file-change-with-watchservice-not-the-whole-directory)
  //        - use cp as the update mechanism instead of modifying directly (would work, but error prone)

  private static Net netconf = null;
  private static Object ruby = null;
  private static Object container = null;
  private static String script = getScript();

  private static Map<Long, Object[]> containerUnits = new HashMap<>();

  private static Set<Long> active = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());


  private static String getScript() {
    try {
      StringBuilder sb = new StringBuilder();

      InputStream i = PreMain.class.getResourceAsStream("/script.rb");
      BufferedReader r = new BufferedReader(new InputStreamReader(i, "UTF-8"));
      String line;

      while ((line = r.readLine()) != null) {
        sb.append(line);
        sb.append('\n');
      }
      r.close();
      return sb.toString();
    } catch (Throwable t) {
      Log.e(TAG, "error", t);
    }
    return "";
  }

  public static class Interceptor {

    public static interface Morpher {
      Object invoke(Object[] args);
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Eval { }


    private static
    Object real_intercept(Morpher _morpher, Method _method, Object _this,
                          Object[] _args, Object _eval)
      throws Throwable {

      long tid = Thread.currentThread().getId();

      if (!active.contains(tid)) {
        active.add(tid);

        try {
          Hook hook = new Hook(script, (String) _eval, containerUnits,
            PreMain.class.getClassLoader(), TAG,
            netconf.pinger, netconf.daemon);
          _method.setAccessible(true);
          Object[] ret_pair = hook.run(tid, _method, _this, _args);
          if ((boolean)ret_pair[0]) {
            Object ret = ret_pair[1];
            if (ret != null) {
              if (ret == ReflectSetup.NULL) {
                return null;
              } else {
                return ret;
              }
            }
          }
        } catch (Throwable t) {
          throw t;
        } finally {
          active.remove(tid);
        }
      } else {
        Log.i(TAG, _method.toString() + " called from same thread");
      }
      return _morpher.invoke(_args);
    }

    @RuntimeType
    public static
    Object intercept(@Morph Morpher _morpher, @Origin Method _method,
                     @This Object _this, @AllArguments Object[] _args,
                     @Eval Object _eval)
      throws Throwable {
      return real_intercept(_morpher, _method, _this, _args, _eval);
    }

    @RuntimeType
    public static
    Object intercept(@Morph Morpher _morpher, @Origin Method _method,
                     @AllArguments Object[] _args, @Eval Object _eval)
      throws Throwable {
      return real_intercept(_morpher, _method, null, _args, _eval);
    }

  }

  public static
  void premain(String arg, Instrumentation inst)throws Throwable {
    net.bytebuddy.agent.Installer.premain(arg, inst);
    BluePill.configure(inst);

    Config jc = null;

    try {
      jc = new Gson().fromJson(
        new String(Files.readAllBytes(Paths.get(arg))),
        Config.class
      );
    } catch (Throwable t) {
      t.printStackTrace();
    }

    if (jc == null) {
      System.out.println("<<JSON config not found>>");
    }

    netconf = jc.net;


    if (jc.matchers.size() == 0) {
      return;
    }

    ReflectSetup.Proxy.init(PreMain.class.getClassLoader());
    ruby = ReflectSetup.prepJRuby("/tmp/jrubygems", PreMain.class.getClassLoader());

    for (Clazz ct : jc.matchers.get(0).classes) {
      if (ct.eval == null) {
        ct.eval = "";
      }

      ElementMatcher.Junction ctmatcher = ElementMatchers.any();

      if (ct.name != null) {
        ctmatcher = ctmatcher.and(ElementMatchers.named(ct.name));
      } else {
        /*
        if (ct.extendedby != null && ct.extendedby.size() > 0) {
          for (String zuper : ct.extendedby) {
            ctmatcher = ctmatcher.and(
              ElementMatchers.isSuperTypeOf(ClassUtils.getClass(zuper,true))
            );
          }
        }
        */

        if (ct.extending != null) {
          ctmatcher = ctmatcher.and(
            ElementMatchers.isSubTypeOf(ClassUtils.getClass(ct.extending, true))
          );
        }
        if (ct.implementing != null && ct.implementing.size() > 0) {
          for (String iface : ct.implementing) {
            ctmatcher = ctmatcher.and(
              ElementMatchers.isSubTypeOf(ClassUtils.getClass(iface, true))
                .and(
                  ElementMatchers.not(ElementMatchers.isInterface())
                )
            );
          }
        }

      }

      for (trust.nccgroup.paraspectre.core.config.matcher.clazz.Method mt : ct.methods) {
        if (mt.eval == null) {
          mt.eval = "";
        }


        ElementMatcher.Junction mtmatcher = ElementMatchers.any();

        if (mt.name != null) {
          mtmatcher = mtmatcher.and(ElementMatchers.named(mt.name));
        }

        if (mt.returns != null) {
          if (mt.returns.equals("-")) {
            mtmatcher = mtmatcher.and(ElementMatchers.returns((Class)null));
          } else {
            mtmatcher = mtmatcher.and(
              ElementMatchers.returns(ClassUtils.getClass(mt.returns, true))
            );
          }
        }

        if (mt.params != null) {
          List<Class<?>> param_types = new ArrayList<>();
          for (String param : mt.params) {
            param_types.add(ClassUtils.getClass(param, true));
          }

          mtmatcher = mtmatcher.and(
            ElementMatchers.takesArguments(
              Iterables.toArray(param_types, Class.class)
            )
          );
        }

        final ElementMatcher.Junction _mtmatcher = mtmatcher;

        String evalString = ct.eval + (ct.eval.isEmpty() || ct.eval.endsWith(";") ? "":"; ")
          + mt.eval + (mt.eval.isEmpty() || mt.eval.endsWith(";") ? "":"; ");


        new AgentBuilder.Default()
          .type(ctmatcher)
          .transform(new AgentBuilder.Transformer() {
            @Override
            public DynamicType.Builder<?>
            transform(DynamicType.Builder<?> builder,
                      TypeDescription typeDescription,
                      ClassLoader classLoader) {
              return builder
                .method(_mtmatcher)
                .intercept(
                  MethodDelegation
                    /*
                      .withDefaultConfiguration() //1.6+
                      .withBinders(
                          Morph.Binder.install(Interceptor.Morpher.class),
                          TargetMethodAnnotationDrivenBinder.ParameterBinder
                              .ForFixedValue.OfConstant.of(
                              Interceptor.Eval.class,
                              evalString
                          )
                      )
                    */
                    .to(Interceptor.class)
                    ///*
                    .appendParameterBinder(Morph.Binder.install(Interceptor.Morpher.class)) // < 1.6
                    .appendParameterBinder(TargetMethodAnnotationDrivenBinder.ParameterBinder
                        .ForFixedValue.OfConstant.of(
                            Interceptor.Eval.class,
                            evalString
                        ))
                    //*/
                );
            }
          })
          .installOn(inst);

      }


    }
  }

}
