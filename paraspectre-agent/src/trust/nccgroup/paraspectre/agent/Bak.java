package trust.nccgroup.paraspectre.agent;
/*
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import groovy.lang.Binding;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.commons.lang3.ClassUtils;
import trust.nccgroup.paraspectre.core.Shell;
import trust.nccgroup.paraspectre.core.net.ReverseTcp;

import javax.management.ObjectName;
import javax.management.loading.ClassLoaderRepository;
import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
*/

public class Bak {


//  public class PreMain {

//    private static trust.nccgroup.paraspectre.agent.PreMain.NetConfig netconf = null;
//
//    public static class JsonConfig {
//      trust.nccgroup.paraspectre.agent.PreMain.NetConfig net;
//      List<trust.nccgroup.paraspectre.agent.PreMain.ClassTarget> targets;
//    }
//
//    public static class NetConfig {
//      String host;
//      int port;
//    }
//
//    public static class ClassTarget {
//      String name;
//      List<String> extending;
//      List<String> extendedby;
//      List<trust.nccgroup.paraspectre.agent.PreMain.MethodTarget> methods;
//    }
//
//    public static class MethodTarget {
//      String name;
//      String returns;
//      List<String> params;
//    }
//
//
//    public static class Interceptor {
//
//      public static interface Morpher {
//        Object invoke(Object[] args);
//      }
//
//      private static Object real_intercept(trust.nccgroup.paraspectre.agent.PreMain.Interceptor.Morpher _morpher, Method _method, Object _this, Object[] _args) throws Exception {
//        Binding vars = new Binding();
//        vars.setVariable("orig", _morpher);
//        vars.setVariable("method", _method);
//        vars.setVariable("thiz", _this);
//        vars.setVariable("args", _args);
//
//        try {
//          Shell.start(ReverseTcp.setupConnection(netconf.host, netconf.port), vars);
//        } catch (Throwable t) {
//          //t.printStackTrace();
//        }
//
//        try {
//          Object ret = vars.getVariable("ret");
//          return ret;
//        } catch (Throwable t) {}
//
//        try {
//          return _morpher.invoke(_args);
//        } catch (Exception e) {
//          throw e;
//        }
//      }
//
//      @RuntimeType
//      public static Object intercept(@Morph trust.nccgroup.paraspectre.agent.PreMain.Interceptor.Morpher _morpher, @Origin Method _method, @This Object _this, @AllArguments Object[] _args) throws Exception {
//        return real_intercept(_morpher, _method, _this, _args);
//      }
//
//      @RuntimeType
//      public static Object intercept(@Morph trust.nccgroup.paraspectre.agent.PreMain.Interceptor.Morpher _morpher, @Origin Method _method, @AllArguments Object[] _args) throws Exception {
//        return real_intercept(_morpher, _method, null, _args);
//      }
//
//    }
//
//    public static <T> T getAs(Object o, Class<T> type) {
//      if (o != null) {
//        try {
//          return type.cast(o);
//        } catch (Throwable t) {
//          return null;
//        }
//      }
//
//      return null;
//    }
//
///*
//
//      ClassLoader loader = Thread.currentThread().getContextClassLoader();
//      try {
//        for (final ClassPath.ClassInfo info : ClassPath.from(loader).getTopLevelClasses()) {
//          String name = info.getName();
//        }
//        ...
// */
//
//    public static class RuntimeMXBeanInterceptor {
//
//      public static interface Morpher {
//        Object invoke(Object[] args);
//      }
//
//      @RuntimeType
//      public static Object intercept(@Morph trust.nccgroup.paraspectre.agent.PreMain.RuntimeMXBeanInterceptor.Morpher _morpher, @Origin Method _method, @This Object _this, @AllArguments Object[] _args) throws Exception {
//        System.out.println("HOOKED!");
//        Object o = null;
//        try {
//          o = _morpher.invoke(_args);
//        } catch (Exception e) {
//          throw e;
//        }
//
//        if (o instanceof List) {
//          List<Object> lo = (List)o;
//
//          List<String> ls = lo.stream()
//              .filter((e) -> {
//                return (e instanceof String) && !((String)e).startsWith("-javaagent:");
//              })
//              .map((e) -> {
//                return (String)e;
//              })
//              .collect(Collectors.toList());
//          return ls;
//        }
//        return o;
//      }
//
//    }
//
//    public static class MyInterceptor {
//      public static List<String> intercept(@This Object instance,
//                                           @SuperCall Callable<String> zuper) throws Exception{
//        return new ArrayList<>();
//      }
//    }
//
//
//    public static class RuntimeMXBeanMod implements RuntimeMXBean {
//
//      private RuntimeMXBean _inner;
//
//      public RuntimeMXBeanMod(RuntimeMXBean r) {
//        _inner = r;
//      }
//
//      @Override
//      public String getName() {
//        return _inner.getName();
//      }
//
//      @Override
//      public String getVmName() {
//        return _inner.getVmName();
//      }
//
//      @Override
//      public String getVmVendor() {
//        return _inner.getVmVendor();
//      }
//
//      @Override
//      public String getVmVersion() {
//        return _inner.getVmVersion();
//      }
//
//      @Override
//      public String getSpecName() {
//        return _inner.getSpecName();
//      }
//
//      @Override
//      public String getSpecVendor() {
//        return _inner.getSpecVendor();
//      }
//
//      @Override
//      public String getSpecVersion() {
//        return _inner.getSpecVersion();
//      }
//
//      @Override
//      public String getManagementSpecVersion() {
//        return _inner.getManagementSpecVersion();
//      }
//
//      @Override
//      public String getClassPath() {
//        return _inner.getClassPath();
//      }
//
//      @Override
//      public String getLibraryPath() {
//        return _inner.getLibraryPath();
//      }
//
//      @Override
//      public boolean isBootClassPathSupported() {
//        return _inner.isBootClassPathSupported();
//      }
//
//      @Override
//      public String getBootClassPath() {
//        return _inner.getBootClassPath();
//      }
//
//      @Override
//      public List<String> getInputArguments() {
//        return _inner.getInputArguments().stream()
//            .filter((e) -> {
//              return (e instanceof String) && !((String)e).startsWith("-javaagent:");
//            })
//            .map((e) -> {
//              return (String)e;
//            })
//            .collect(Collectors.toList());
//      }
//
//      @Override
//      public long getUptime() {
//        return _inner.getUptime();
//      }
//
//      @Override
//      public long getStartTime() {
//        return _inner.getStartTime();
//      }
//
//      @Override
//      public Map<String, String> getSystemProperties() {
//        return _inner.getSystemProperties();
//      }
//
//      @Override
//      public ObjectName getObjectName() {
//        return _inner.getObjectName();
//      }
//
//    }
//
//
//    public static class MyInterceptor2 {
//
//      public static RuntimeMXBean getRuntimeMXBean(@SuperCall Callable<RuntimeMXBean> zuper) {
//        try {
//          return new trust.nccgroup.paraspectre.agent.PreMain.RuntimeMXBeanMod(zuper.call());
//        } catch (Throwable t) {
//          return null;
//        }
//      }
//    }
//
//
//    public static class MyInterceptorr {
//
//      public static List<String> intercept(@SuperCall Callable<List<String>> zuper) throws Exception {
//        System.out.println("Intercepted!");
//        return zuper.call();
//      }
//    }
//
//    //static Class<?> jvmClass = null;
//
//
//    //NOTE: for some reason, it doesn't look like the sun.management.RuntimeImpl class
//    //      implementing java.lang.management.RuntimeMXBean can be instrumented
//    public static void hide(Instrumentation inst) {
//
//
//      System.out.println("<hiding!>");
//
//
//
//
//      //RuntimeMXBean rmxb = ManagementFactory.getRuntimeMXBean();
///*
//    List<String> replace = rmxb.getInputArguments().stream()
//        .filter((e) -> {
//          return (e instanceof String) && !((String)e).startsWith("-javaagent:");
//        })
//        .map((e) -> {
//          return (String)e;
//        })
//        .collect(Collectors.toList());
//*/
//      try {
//
//        ClassLoader cl = null;
//
//        ClassLoaderRepository clr = ManagementFactory.getPlatformMBeanServer().getClassLoaderRepository();
//        System.out.println(clr.getClass());
//
//        Field real_clr_f = clr.getClass().getDeclaredField("clr");
//        real_clr_f.setAccessible(true);
//        Object /*com.sun.jmx.mbeanserver.ClassLoaderRepositorySupport*/ real_clr = real_clr_f.get(clr);
//
//        Field loaders_f = real_clr.getClass().getDeclaredField("loaders");
//        loaders_f.setAccessible(true);
//        Object[] loaders = (Object[])loaders_f.get(real_clr);
//
//        for (Object loader : loaders) {
//          Field name_f = loader.getClass().getDeclaredField("name");
//          name_f.setAccessible(true);
//          Object name = name_f.get(loader);
//          System.out.println(name);
//
//          Field real_loader_f = loader.getClass().getDeclaredField("loader");
//          real_loader_f.setAccessible(true);
//          Object real_loader = real_loader_f.get(loader);
//          System.out.println(real_loader);
//
//          if (real_loader instanceof ClassLoader) {
//            cl = (ClassLoader)real_loader;
//          }
//
//          System.out.println("=====");
//        }
//
//      /*
//      for (Field f : clr.getClass().getDeclaredFields()) {
//        f.setAccessible(true);
//        System.out.println(f);
//        Object o = f.get(clr);
//        System.out.println(o.getClass());
//        for (Field ff : o.getClass().getDeclaredFields()) {
//          ff.setAccessible(true);
//          System.out.println(ff);
//        }
//      }*/
//
//        Class c = Class.forName("java.lang.management.ManagementFactory", true, cl);
//
////      new ByteBuddy()
////          .redefine(c, ClassFileLocator.ForClassLoader.of(cl))
////          .method(ElementMatchers.named("getRuntimeMXBean"))
////          .intercept(FixedValue.value(new RuntimeMXBeanMod(null)))
////          .make()
////          .load(cl, ClassReloadingStrategy.fromInstalledAgent());
//
//        new ByteBuddy()
//            .rebase(c, ClassFileLocator.ForClassLoader.of())
//            .method(ElementMatchers.named("getRuntimeMXBean"))
//            .intercept(FixedValue.value(new trust.nccgroup.paraspectre.agent.PreMain.RuntimeMXBeanMod(null)))
//            .make()
//            .load(c.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
//
//
//        File temp = Files.createTempDirectory("tmp").toFile();
//        ClassInjector.UsingInstrumentation.of(temp, ClassInjector.UsingInstrumentation.Target.BOOTSTRAP, inst).inject(
//            Collections.singletonMap(
//                new TypeDescription.ForLoadedType(trust.nccgroup.paraspectre.agent.PreMain.MyInterceptorr.class),
//                ClassFileLocator.ForClassLoader.read(trust.nccgroup.paraspectre.agent.PreMain.MyInterceptorr.class).resolve()
//            )
//        );
//        new AgentBuilder.Default()
//            .enableBootstrapInjection(inst, temp)
//            .type(ElementMatchers.nameEndsWith(".RuntimeImpl"))
//            .transform(new AgentBuilder.Transformer() {
//              @Override
//              public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
//                System.out.print(">>>>>>>");
//                return builder.method(ElementMatchers.named("getInputArguments")).intercept(MethodDelegation.to(trust.nccgroup.paraspectre.agent.PreMain.MyInterceptorr.class));
//              }
//            }).installOn(inst);
//
//
////      Class c = Class.forName("java.lang.management.ManagementFactory", true, inst.getClass().getClassLoader());
////
////      /*
////      new AgentBuilder.Default()
////          .type(ElementMatchers.named("java.lang.management.ManagementFactory"))
////          .transform((builder, type, loader) -> {
////              System.out.println(type);
////              return builder.method(ElementMatchers.named("getRuntimeMXBean"))
////                  .intercept(MethodDelegation.to(MyInterceptor2.class));
////          })
////          .installOn(inst);
////*/
////
////      ClassLoader loader =
////      AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
////        public ClassLoader More ...run() {
////          return cls.getClassLoader();
////        }
////      });
////
////      new ByteBuddy()
////          .redefine(c, new ClassFileLocator.AgentBased(inst, java.lang.management.ManagementFactory.class.getClassLoader()))
////          .method(ElementMatchers.named("getRuntimeMXBean"))
////          .intercept(FixedValue.value(new RuntimeMXBeanMod(null)))
////          .make()
////          .load(c.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
//
///*
//      final ByteBuddy byteBuddy = new ByteBuddy().with(Implementation.Context.Default.Factory.INSTANCE);//   .Disabled.Factory.INSTANCE);
//      new AgentBuilder.Default()
//          .with(byteBuddy)
//          .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
//          .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
//          .type(new AgentBuilder.RawMatcher() {
//            @Override
//            public boolean matches(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, Class<?> classBeingRedefined, ProtectionDomain protectionDomain) {
//              if (classBeingRedefined.getName().equals("java.lang.management.ManagementFactory")) {
//                System.out.println("found");
//                return true;
//              } else {
//                return false;
//              }
//            }
//          })
//          //.type(ElementMatchers.named("java.lang.management.ManagementFactory"))
//          .transform((builder, type, loader) -> {
//            System.out.println(type);
//            return builder.method(ElementMatchers.named("getRuntimeMXBean"))
//                .intercept(MethodDelegation.to(MyInterceptor2.class));
//          })
//          .installOn(inst);
//*/
//
///*
//      new ByteBuddy()
//          .redefine(c)
//          .method(ElementMatchers.named("getRuntimeMXBean"))
//          .intercept(MethodDelegation.to(MyInterceptor2.class))
//          .make()
//          .load(PreMain.class.getClassLoader(), new ClassReloadingStrategy(inst, ClassReloadingStrategy.Strategy.RETRANSFORMATION));
//*/
//      /*Field[] fs = rmxb.getClass().getDeclaredFields();
//      for (Field f : fs) {
//        System.out.println(f);
//      }*/
//
//
//        //Field f = rmxb.getClass().getDeclaredField("jvm");
//        //boolean a = f.isAccessible();
//        //f.setAccessible(true);
//        //Object jvm = f.get(rmxb);
//        //System.out.println(jvm.getClass());
////      ClassReloadingStrategy classReloadingStrategy =
////          ClassReloadingStrategy.fromInstalledAgent();
//
////      Class jvmClass = Class.forName("sun.management.VMManagementImpl", true, inst.getClass().getClassLoader());
////      Class runtimeImplClass = Class.forName("sun.management.RuntimeImpl", true, inst.getClass().getClassLoader());
//
//
//      /*inst.addTransformer(new ClassFileTransformer() {
//        @Override
//        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
//
//          if (className.equals("sun/management/VMManagementImpl")) {
//            jvmClass = classBeingRedefined;
//          }
//
//          return classfileBuffer;
//        }
//      });*/
//
///*
//      new ByteBuddy()
//          .redefine(jvmClass)
//          .method(ElementMatchers.named("getVmArguments"))
//          .intercept(FixedValue.value(new ArrayList<String>()))
//          .make()
//          .load(jvmClass.getClassLoader(), classReloadingStrategy);
//*/
//
///*
//      new ByteBuddy()
//          .redefine(runtimeImplClass)
//          .method(ElementMatchers.named("getInputArguments"))
//          .intercept(FixedValue.value(new ArrayList<String>()))
//          .make()
//          .load(runtimeImplClass.getClassLoader(), new ClassReloadingStrategy(inst, ClassReloadingStrategy.Strategy.RETRANSFORMATION));
//*/
//        //f.set(rmxb, jvm2);
//        //f.setAccessible(a);
//
///*
//      new AgentBuilder.Default()
//          .type(ElementMatchers.named("sun.management.RuntimeImpl"))
//          .transform((builder, type, loader) ->
//              builder.method(ElementMatchers.named("getVmArguments"))
//                  .intercept(MethodDelegation.to(MyInterceptor.class)))
//          .installOn(inst);
//
//      new AgentBuilder.Default()
//          .type(ElementMatchers.named("sun.management.VMManagementImpl"))
//          .transform((builder, type, loader) ->
//              builder.method(ElementMatchers.named("getInputArguments"))
//                  .intercept(MethodDelegation.to(MyInterceptor.class)))
//          .installOn(inst);
//*/
//
//      } catch (Throwable t) {
//        t.printStackTrace();
//      }
//
//      //System.out.println(rmxb.getClass());
///*
//    new AgentBuilder.Default()
//        .type(ElementMatchers.named(rmxb.getClass().getName()))
//        //.type(ElementMatchers.isSubTypeOf(java.lang.management.RuntimeMXBean.class))
//        .transform(new AgentBuilder.Transformer() {
//          @Override
//          public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
//            try {
//              System.out.println(typeDescription.getName());
//            } catch (Throwable t) {
//              t.printStackTrace();
//            }
//            return builder
//                .method(
//                    ElementMatchers.named("getInputArguments")
//                    .and(ElementMatchers.returns(List.class)
//                    .and(ElementMatchers.takesArguments(0)))
//                )
//                .intercept(
//                    MethodDelegation.to(RuntimeMXBeanInterceptor.class)
//                        .appendParameterBinder(Morph.Binder.install(RuntimeMXBeanInterceptor.Morpher.class))
//                );
//          }
//        })
//        .installOn(inst);
//        */
//      System.out.println("</hiding>");
//    }
//
//    public static void premain(String arg, Instrumentation inst) throws Throwable {
//      //ByteBuddyAgent.install();
//      net.bytebuddy.agent.Installer.premain(arg, inst);
//
//      hide(inst);
//
//      String config_path = arg;
//      trust.nccgroup.paraspectre.agent.PreMain.JsonConfig jc = new Gson().fromJson(new String(Files.readAllBytes(Paths.get(config_path))), trust.nccgroup.paraspectre.agent.PreMain.JsonConfig.class);
//
//      if (jc == null) {
//        return;
//      }
//
//      netconf = jc.net;
//
//      for (trust.nccgroup.paraspectre.agent.PreMain.ClassTarget ct : jc.targets) {
//
//        ElementMatcher.Junction ctmatcher = ElementMatchers.any();
//
//        if (ct.name != null) {
//          ctmatcher = ctmatcher.and(ElementMatchers.named(ct.name));
//        } else {
//          if (ct.extendedby != null && ct.extendedby.size() > 0) {
//            for (String zuper : ct.extendedby) {
//              ctmatcher = ctmatcher.and(ElementMatchers.isSuperTypeOf(ClassUtils.getClass(zuper,true)));
//            }
//          }
//
//          if (ct.extending != null && ct.extending.size() > 0) {
//            for (String base : ct.extending) {
//              ctmatcher = ctmatcher.and(ElementMatchers.isSubTypeOf(ClassUtils.getClass(base, true)));
//            }
//          }
//
//        }
//
//        ElementMatcher.Junction mtsmatcher = ElementMatchers.none();
//
//        for (trust.nccgroup.paraspectre.agent.PreMain.MethodTarget mt : ct.methods) {
//          ElementMatcher.Junction mtmatcher = ElementMatchers.any();
//
//          if (mt.name != null) {
//            mtmatcher = mtmatcher.and(ElementMatchers.named(mt.name));
//          }
//
//          if (mt.returns != null) {
//            if (mt.returns.equals("-")) {
//              mtmatcher = mtmatcher.and(ElementMatchers.returns((Class)null));
//            } else {
//              mtmatcher = mtmatcher.and(ElementMatchers.returns(ClassUtils.getClass(mt.returns, true)));
//            }
//          }
//
//          if (mt.params != null) {
//            List<Class<?>> param_types = new ArrayList<>();
//            for (String param : mt.params) {
//              param_types.add(ClassUtils.getClass(param, true));
//            }
//
//            mtmatcher = mtmatcher.and(ElementMatchers.takesArguments(Iterables.toArray(param_types, Class.class)));
//          }
//
//          mtsmatcher = mtsmatcher.or(mtmatcher);
//        }
//
//        final ElementMatcher.Junction _mtsmatcher = mtsmatcher;
//
//
//        new AgentBuilder.Default()
//            .type(ctmatcher)
//            .transform(new AgentBuilder.Transformer() {
//              @Override
//              public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
//
//                return builder
//                    .method(_mtsmatcher)
//                    .intercept(
//                        MethodDelegation.to(trust.nccgroup.paraspectre.agent.PreMain.Interceptor.class)
//                            .appendParameterBinder(Morph.Binder.install(trust.nccgroup.paraspectre.agent.PreMain.Interceptor.Morpher.class))
//                    );
//
//              }
//            })
//            .installOn(inst);
//
//      }
//    }
//
//  }


}
