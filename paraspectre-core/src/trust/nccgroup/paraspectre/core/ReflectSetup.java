package trust.nccgroup.paraspectre.core;

import trikita.log.Log;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ReflectSetup {

  private static final String TAG = "PS/ReflectSetup";

  public static final Null NULL = new Null();
  public static final VoidReturn VOID = new VoidReturn();


  @SuppressWarnings("unused") //used by agent
  public static Object prepJRuby(String home_path, ClassLoader jruby_cl) throws Throwable {
    Log.d(TAG, "prepJRuby");

    Object config = Proxy.RubyInstanceConfig.getConstructor().newInstance();
    Object OFF = Proxy.CompileMode_valueOf.invoke(null, "OFF");

    Proxy.setCompileMode.invoke(config, OFF);

    //Object RUBY2_0 = Proxy.CompatVersion_valueOf.invoke(null, "RUBY2_0");
    Object RUBY1_9 = Proxy.CompatVersion_valueOf.invoke(null, "RUBY1_9");
    Proxy.setCompatVersion.invoke(config, RUBY1_9);

    Proxy.setLoader.invoke(config, jruby_cl);


    @SuppressWarnings("unchecked")
    Map<String,String> env = new HashMap<>((Map<String,String>)Proxy.getEnvironment.invoke(config));
    env.put("HOME", home_path); //for pry internals

    Object ruby = null;
    try {
      ruby = Proxy.newInstance.invoke(null, config);
    } catch (Throwable t) {
      Log.e(TAG, "ruby.newInstance(config) failed", t);
      for (Throwable tt : t.getSuppressed()) {
        Log.e(TAG, "suppressed exception", tt);
      }
      Log.e(TAG, "caused by", t.getCause());
    }
    return ruby;
  }

  public static Object setupContainer(String script, ClassLoader dcl) throws Throwable {
    //Log.d(TAG, "setupContainer");
    Object container = Proxy.ScriptingContainer.getConstructor().newInstance();
    Proxy.setClassLoader.invoke(container, dcl);

    Proxy.runScriptlet.invoke(container, script);
    return container;
  }

  public static Object setupUnit(Object container, String hook_script) throws Throwable {
    return Proxy.parse.invoke(container, hook_script, new int[]{});
  }

  public static Object run(Object container, Object unit) throws Throwable {
    Proxy.put.invoke(container, "@null", NULL);
    Proxy.put.invoke(container, "@void", VOID);
    Object ret = Proxy.run.invoke(unit);
    return Proxy.rubyToJava.invoke(null, ret);
  }


  @SuppressWarnings("WeakerAccess")
  public static class Proxy {
    public static Class<?> RubyInstanceConfig = null;
    public static Method setCompileMode = null;
    public static Method setCompatVersion = null;
    public static Method setLoader = null;
    public static Method getEnvironment = null;

    public static Class<?> RubyInstanceConfig_CompileMode = null;
    public static Method CompileMode_valueOf = null;

    public static Class<?> CompatVersion = null;
    public static Method CompatVersion_valueOf = null;

    public static Class<?> Ruby = null;
    public static Method newInstance = null;


    public static Class<?> ScriptingContainer = null;
    public static Method setClassLoader = null;
    public static Method runScriptlet = null;
    public static Method parse = null;
    public static Method get = null;
    public static Method put = null;

    public static Class<?> JavaEmbedUtils_EvalUnit = null;
    public static Method run = null;

    public static Class<?> IRubyObject = null;
    public static Class<?> JavaEmbedUtils = null;
    public static Method rubyToJava = null;


    public static void init(ClassLoader dcl) {
      if (RubyInstanceConfig != null) {
        return;
      }

      try {
        RubyInstanceConfig = dcl.loadClass("org.jruby.RubyInstanceConfig");
        RubyInstanceConfig_CompileMode = dcl.loadClass("org.jruby.RubyInstanceConfig$CompileMode");

        setCompileMode = RubyInstanceConfig.getDeclaredMethod("setCompileMode", RubyInstanceConfig_CompileMode);

        CompatVersion = dcl.loadClass("org.jruby.CompatVersion");
        setCompatVersion = RubyInstanceConfig.getDeclaredMethod("setCompatVersion", CompatVersion);

        setLoader = RubyInstanceConfig.getDeclaredMethod("setLoader", ClassLoader.class);
        getEnvironment = RubyInstanceConfig.getDeclaredMethod("getEnvironment");

        CompileMode_valueOf = RubyInstanceConfig_CompileMode.getDeclaredMethod("valueOf", String.class);
        CompatVersion_valueOf = CompatVersion.getDeclaredMethod("valueOf", String.class);

        Ruby = dcl.loadClass("org.jruby.Ruby");
        newInstance = Ruby.getDeclaredMethod("newInstance", RubyInstanceConfig);


        ScriptingContainer = dcl.loadClass("org.jruby.embed.ScriptingContainer");
        setClassLoader = ScriptingContainer.getDeclaredMethod("setClassLoader", ClassLoader.class);
        runScriptlet = ScriptingContainer.getDeclaredMethod("runScriptlet", String.class);
        parse = ScriptingContainer.getDeclaredMethod("parse", String.class, int[].class);
        get = ScriptingContainer.getDeclaredMethod("get", String.class);
        put = ScriptingContainer.getDeclaredMethod("put", String.class, Object.class);

        JavaEmbedUtils_EvalUnit = dcl.loadClass("org.jruby.javasupport.JavaEmbedUtils$EvalUnit");
        run = JavaEmbedUtils_EvalUnit.getDeclaredMethod("run");

        JavaEmbedUtils = dcl.loadClass("org.jruby.javasupport.JavaEmbedUtils");
        IRubyObject = dcl.loadClass("org.jruby.runtime.builtin.IRubyObject");
        rubyToJava = JavaEmbedUtils.getDeclaredMethod("rubyToJava", IRubyObject);

      } catch (Throwable t) {
        Log.e(TAG, "failed to init reflection proxy", t);
      }
    }
  }

  private static class Null {

  }

  private static class VoidReturn {

  }

}
