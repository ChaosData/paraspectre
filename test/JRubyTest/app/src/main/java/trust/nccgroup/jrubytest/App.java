package trust.nccgroup.jrubytest;

import android.app.Application;
//import android.support.multidex.MultiDexApplication;
import android.support.multidex.MultiDex;
import android.util.Log;

import org.joor.Reflect;
import org.jruby.CompatVersion;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.embed.ScriptingContainer;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Map;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class App extends Application {

  public static ClassLoader dcl = null;
  public static Object ruby = null;

  @Override
  public void onCreate() {
    super.onCreate();

    MultiDex.install(this);

    // Used to enable JRuby to generate proxy classes
    //System.setProperty("jruby.class.cache.path", this.getDir("dex", 0).getAbsolutePath());
    //System.setProperty("java.io.tmpdir", this.getCacheDir().getAbsolutePath());

    //System.setProperty("jruby.debug.loadService", "true");
    //System.setProperty("jruby.debug.loadService.timing", "true");
    System.setProperty("jruby.logger.class", "trust.nccgroup.jrubytest.AndroidErrorLogger");


    migrateDependencies("jruby-complete.dex.jar");
    migrateDependencies("jrubystuff.jar");

    //prepCompiled();

    setupRemoteJar();
    //prepProvided();

    Proxy.init(dcl);
    try {
      ruby = prepVanillaReflect();
    } catch (Throwable t) {
      Log.e("NCC", "prepVanillaReflect failed", t);
    }

  }

  private void migrateDependencies(String fname) {
    try {
      File fd = new File(getApplicationContext().getFilesDir().getPath() + "/" + fname);
      if (fd.exists()) {
        boolean r = fd.setReadable(true, false);
        if (!r) {
          Log.e("NCC", "FAILED to set " + fname + " world readable.");
        }
        return;
      }

      fd = getApplicationContext().getFilesDir();
      if (!fd.exists()) {
        boolean r = fd.mkdir();
        if (!r) {
          Log.e("NCC", "FAILED to create /files directory.");
        }
      }

      OutputStream o = new FileOutputStream(getApplicationContext().getFilesDir().getPath() + "/" + fname);
      byte[] buffer = new byte[4096];
      int length;
      InputStream i = getApplicationContext().getAssets().open(fname);
      while ((length = i.read(buffer)) > 0) {
        o.write(buffer, 0, length);
      }
      i.close();
      o.flush();
      o.close();
    } catch (Throwable t) {
      Log.e("NCC", "migrateDependencies failed", t);
    }
    File fd = new File(getApplicationContext().getFilesDir().getPath() + "/" + fname);

    boolean r = fd.setReadable(true, false);
    if (!r) {
      Log.e("NCC", "FAILED to set " + fname + " world readable.");
    }
  }

  protected void prepCompiled() {
    Log.e("NCC", "prepCompiled");

    RubyInstanceConfig config = new RubyInstanceConfig();
    config.setCompatVersion(CompatVersion.RUBY2_0);
    config.setCompileMode(RubyInstanceConfig.CompileMode.OFF);

    Map<String, Object> env = config.getEnvironment();
    //Log.e("NCC", "RUBY: " + env.get("RUBY"));
    env.put("HOME", getFilesDir().getAbsolutePath()); //for pry internals

//    ClassLoader loader = config.getLoader();
//    Log.e("NCC", "loader: " + loader);
//    Log.e("NCC", "RubyInstanceConfig.class.getClassLoader(): " + RubyInstanceConfig.class.getClassLoader());
//    Log.e("NCC", "Thread.currentThread().getContextClassLoader(): " + Thread.currentThread().getContextClassLoader());

    Ruby ruby = Ruby.newInstance(config);
    ruby.useAsGlobalRuntime();
  }

  //@Override
  protected void prepProvided() {
    Log.e("NCC", "prepProvided");

    Object config = Reflect.on("org.jruby.RubyInstanceConfig", App.dcl)
      .create().get();

    Object OFF = Reflect.on("org.jruby.RubyInstanceConfig$CompileMode", App.dcl)
      .call("valueOf", "OFF").get();
    Reflect.on(config).call("setCompileMode", OFF);

    Object RUBY2_0 = Reflect.on("org.jruby.CompatVersion", App.dcl)
      .call("valueOf", "RUBY2_0").get();

    Reflect.on(config).call("setCompatVersion", RUBY2_0);

//    Log.e("NCC", "loader: " + Reflect.on(config).call("getLoader").get());
//    Log.e("NCC", "config.getClass().getClassLoader(): " + config.getClass().getClassLoader());
//    Log.e("NCC", "Thread.currentThread().getContextClassLoader(): " + Thread.currentThread().getContextClassLoader());

    Reflect.on(config).call("setLoader", (ClassLoader)App.dcl);

//    Log.e("NCC", "loader: " + Reflect.on(config).call("getLoader").get());
//    Log.e("NCC", "config.getClass().getClassLoader(): " + config.getClass().getClassLoader());
//    Log.e("NCC", "Thread.currentThread().getContextClassLoader(): " + Thread.currentThread().getContextClassLoader());


    Map<String,Object> env = Reflect.on(config).call("getEnvironment").get();
    //Log.e("NCC", "RUBY: " + env.get("RUBY"));
    env.put("HOME", getFilesDir().getAbsolutePath()); //for pry internals

    Object ruby = null;
    try {
      Method newInstance = App.dcl.loadClass("org.jruby.Ruby").getDeclaredMethod("newInstance", config.getClass());
      ruby = newInstance.invoke(null, config);
    } catch (Throwable t) {
      Log.e("NCC", "newInstance fail", t);
      for (Throwable tt : t.getSuppressed()) {
        Log.e("NCC", "suppressed newInstance fail", tt);
      }
      Log.e("NCC", "newInstance fail (cause)", t.getCause());
    }

    //Reflect.on(ruby).call("useAsGlobalRuntime");

  }

  protected Object prepVanillaReflect() throws Throwable {
    Log.e("NCC", "prepVanillaReflect");

    Object config = Proxy.RubyInstanceConfig.getConstructor().newInstance();
    Object OFF = Proxy.CompileMode_valueOf.invoke(null, "OFF");

    Proxy.setCompileMode.invoke(config, OFF);

    Object RUBY2_0 = Proxy.CompatVersion_valueOf.invoke(null, "RUBY2_0");
    Proxy.setCompatVersion.invoke(config, RUBY2_0);

    Proxy.setLoader.invoke(config, App.dcl);


    Map<String,Object> env = (Map<String,Object>)Proxy.getEnvironment.invoke(config);
    env.put("HOME", getFilesDir().getAbsolutePath()); //for pry internals

    Object ruby = null;
    try {
      ruby = Proxy.newInstance.invoke(null, config);
    } catch (Throwable t) {
      Log.e("NCC", "newInstance fail", t);
      for (Throwable tt : t.getSuppressed()) {
        Log.e("NCC", "suppressed newInstance fail", tt);
      }
      Log.e("NCC", "newInstance fail (cause)", t.getCause());
    }

    //Reflect.on(ruby).call("useAsGlobalRuntime");
    return ruby;
  }


  private void setupRemoteJar() {

    File privateDirectory = getApplicationContext().getCodeCacheDir();
    //File privateDirectory = new File("/data/data/trust.nccgroup.jrubytest/code_cache/");
    if (!privateDirectory.exists()) {
      privateDirectory.mkdir();
    }

    /*
    DexClassLoader dexClassLoader = new DexClassLoader(
      //getApplicationContext().getFilesDir().getPath() + "jruby-complete.dex.jar",
      "/data/data/trust.nccgroup.jrubytest/files/jruby-complete.dex.jar",
      privateDirectory.getAbsolutePath(),
      null,
      getClassLoader()
    );*/

    ClassLoader dexClassLoader = new /*DexClassLoader(*/PathClassLoader(
      "/data/app/trust.nccgroup.jrubytest-1/base.apk:/data/data/trust.nccgroup.jrubytest/files/jruby-complete.dex.jar",
      "/vendor/lib:/system/lib",
      //null,
      getClassLoader()
    );

    dcl = dexClassLoader;

  }

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

    public static void init(ClassLoader dcl) {
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
      } catch (Throwable t) {
        Log.e("NCC", "failed to init Proxy", t);
      }
    }
  }

}
