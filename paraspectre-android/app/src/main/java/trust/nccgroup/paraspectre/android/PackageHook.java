package trust.nccgroup.paraspectre.android;


import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import trust.nccgroup.paraspectre.android.generated.RubyCode;
import trust.nccgroup.paraspectre.android.match.Match;
import trust.nccgroup.paraspectre.android.match.Selector;
import trust.nccgroup.paraspectre.core.ContainerUnitHolder;
import trust.nccgroup.paraspectre.core.Hook;
import trust.nccgroup.paraspectre.core.ReflectSetup;
import trust.nccgroup.paraspectre.core.config.Matcher;

public class PackageHook {

  private static String TAG = "PS/Zygote";

  private XC_LoadPackage.LoadPackageParam lpp = null;
  private String pkg = null;

  private Matcher matcher = null;

  //private List<String> context_classes = null;
  private String script = null;

  private ClassLoader jruby_cl = null;
  //Object ruby = null;

  private static final Method findClass;

  static {
    Method fc = null;
    try {
      fc = BaseDexClassLoader.class.getDeclaredMethod("findClass", String.class);
    } catch (NoSuchMethodException nsme) {
      Log.e(TAG, "error", nsme);
    }
    findClass = fc;
    if (findClass == null) {
      Log.e(TAG, "could not get method findClass");
    } else {
      findClass.setAccessible(true);
    }
  }

  //Map<Long, Object[]> containerUnits = new HashMap<>();
  static final ConcurrentMap<Long, ContainerUnitHolder> container_cache = new ConcurrentHashMap<>();

  //Map<Member,XC_MethodHook.Unhook> hooked_methods = new HashMap<>();;
  //private ClassLoader dcl = null;

  static Set<Long> active = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());

  @SuppressLint("StaticFieldLeak")
  static Context appctx = null;

  PackageHook(XC_LoadPackage.LoadPackageParam _lpp, Matcher _matcher) {
    lpp = _lpp;
    pkg = lpp.packageName;
    matcher = _matcher;
    TAG = "PS/" + lpp.packageName;
  }

  private static Class<?> findClass(ClassLoader cl, String name) {
    try {
      return (Class<?>)findClass.invoke(cl, name);
    } catch (Throwable ignored) {
      return null;
    }
  }

//  private static List<String> getContextClasses(String TAG, Context ctx) {
//    List<String> classes = new ArrayList<>();
//    try {
//      String packageCodePath = ctx.getPackageCodePath();
//      DexFile df = new DexFile(packageCodePath);
//      for (Enumeration<String> iter = df.entries(); iter.hasMoreElements();) {
//        String className = iter.nextElement();
//        classes.add(className);
//      }
//    } catch (IOException e) {
//      //No original dex files found for dex location
//    } catch (Throwable t) {
//      Log.e(TAG, "error", t);
//    }
//    return classes;
//  }


  @SuppressWarnings("unused")
  private List<String> getSystemFrameworkClasses() {

    File systemFrameworkDir = new File("/system/framework");
    File[] systemFrameworkFiles = systemFrameworkDir.listFiles();

    List<String> classes = new ArrayList<>();

    for (File file : systemFrameworkFiles) {
      if (!file.getAbsolutePath().endsWith(".jar")) {
        continue;
      }

      try {
        DexFile df = new DexFile(file);

        for (Enumeration<String> iter = df.entries(); iter.hasMoreElements();) {
          String className = iter.nextElement();
          classes.add(className);
        }
      } catch (IOException e) {
        //No original dex files found for dex location
      } catch (Throwable t) {
        Log.e(TAG, "error", t);
      }
    }
    return classes;
  }


  public static Class<?> getClass(String cls, ClassLoader cl, ClassLoader fallback_cl) {
    Class<?> c = null;
    try {
      c = findClass(cl, cls);
    } catch (Throwable ignored) { Log.e(TAG, "findClass1 error", ignored); }
    if (c == null && fallback_cl != null) {
      try {
        c = findClass(fallback_cl, cls);
      } catch (Throwable ignored) { Log.e(TAG, "findClass2 error", ignored); }
    }
    if (c == null) {
      if (fallback_cl != cl) {
        Log.e(TAG, "error loading class " + cls);
      }
    }
    return c;
  }



  void setupHooks() throws Throwable {
    try {
      //note: regardless of where this is run from, this hook happens after a ruby hook for the same method
      XposedHelpers.findAndHookMethod("android.app.Application",
        lpp.classLoader, "onCreate", new XC_MethodHook() {
          @Override
          protected void beforeHookedMethod(MethodHookParam param)
            throws Throwable {
            Application thiz = param.thisObject instanceof Application ? (Application) param.thisObject : null;
            if (thiz != null) {
              appctx = thiz.getApplicationContext();
            }
          }
        });
    } catch (Throwable t) {
      Log.e(TAG, "failed to hook android.app.Application::onCreate", t);
    }

    setupOnLoadedHook();
  }


  static void threadedJRubyClassLoaderPrep() {
    new Thread() {
      public void run() {
        synchronized (XposedEntry.lock) {
          //note: dir allowed by zygote.te
          if (!new File("/data/dalvik-cache/paraspectre").exists()) {
            return;
          }
          try {
            XposedEntry.jruby_cl = new PathClassLoader(
              "/data/dalvik-cache/paraspectre/runtime.dex.jar",
              "/vendor/lib:/system/lib",
              this.getClass().getClassLoader()
            );
            ReflectSetup.Proxy.init(XposedEntry.jruby_cl);
          } catch (Throwable t) {
            Log.e(TAG, "error", t);
          }


        }
      }
    }.start();
  }

  //note: even in a separate thread, this slows down zygote too much and
  //      boot services fail to start up properly, deadlocking startup.
  //      it needs to be run in each hooked app.
  static void jRubyContainerInit() {
    new Thread() {
      public void run() {
        if (XposedEntry.jruby_cl != null) {
          synchronized (container_cache) {
            String script = RubyCode.INIT_RB;
            try {
              ReflectSetup.Proxy.init(XposedEntry.jruby_cl);
              XposedEntry.container = ReflectSetup.setupContainer(script, XposedEntry.jruby_cl);
              XposedEntry.unit = ReflectSetup.setupUnit(XposedEntry.container, RubyCode.HOOK_RB /*getHookScriptTemplate()*/);

              ReflectSetup.Proxy.put.invoke(XposedEntry.container, "@eval", "return;");
              ReflectSetup.Proxy.put.invoke(XposedEntry.container, "@this", null);
              ReflectSetup.Proxy.put.invoke(XposedEntry.container, "@method", null);
              ReflectSetup.Proxy.put.invoke(XposedEntry.container, "@constructor", null);
              ReflectSetup.Proxy.put.invoke(XposedEntry.container, "@args", new Object[]{});

              try {
                Method unixPing = PackageHook.class.getMethod("unixPing", byte[].class, byte[].class);
                ReflectSetup.Proxy.put.invoke(XposedEntry.container, "@unixPing", unixPing);
              } catch (Throwable t) {
                Log.e(TAG, "failed to load unixPing", t);
              }


              ReflectSetup.run(XposedEntry.container, XposedEntry.unit);

            } catch (Throwable t) {
              Log.e(TAG, "failed to setup container", t);
            }
          }
        }
      }
    }.start();
  }

  private void setupOnLoadedHook() throws Throwable {
    try {
      XposedHelpers.findAndHookMethod("android.app.Application",
          lpp.classLoader, "attach", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param)
                throws Throwable {
              Context context = (Context) param.args[0];

              System.setProperty("jruby.logger.class", "trust.nccgroup.jdruby.AndroidLogger");
              System.setProperty("jruby.rewrite.java.trace", "false");
              System.setProperty("jruby.compile.mode", "OFF");
              System.setProperty("jruby.interfaces.useProxy", "true");
              System.setProperty("jruby.bytecode.version", "1.6");

              System.setProperty("jruby.ji.proxyClassFactory", "org.ruboto.DalvikProxyClassFactory");
              System.setProperty("jruby.ji.upper.case.package.name.allowed", "true");
              System.setProperty("jruby.class.cache.path", context.getDir("dex", 0).getAbsolutePath());
              System.setProperty("java.io.tmpdir", context.getCacheDir().getAbsolutePath());

              System.setProperty("jruby.management.enabled", "false");
              System.setProperty("jruby.objectspace.enabled", "false");
              System.setProperty("jruby.thread.pooling", "true");
              System.setProperty("jruby.native.enabled", "false");
              System.setProperty("jruby.ir.passes", "LocalOptimizationPass,DeadCodeElimination");
              System.setProperty("jruby.backtrace.style", "normal"); // normal raw full mri

              script = RubyCode.INIT_RB; //getScript(context);

              ClassLoader localClassLoader = context.getClassLoader();

              synchronized (XposedEntry.lock) {
                if (XposedEntry.jruby_cl == null) {
                  Setup.publishAssets("runtime.dex.jar", context);
                  //Setup.publishAssets("jrubystuff.jar", context);
                  jruby_cl = Setup.loadRemoteJar(context);
                } else {
                  if (!XposedEntry.classLoaderFix) {
                    try {
                      Field parent = ClassLoader.class.getDeclaredField("parent");
                      parent.setAccessible(true);
                      parent.set(XposedEntry.jruby_cl, localClassLoader);
                      XposedEntry.classLoaderFix = true;
                    } catch (Throwable t) {
                      Log.e(TAG, "error", t);
                    }
                  }
                  jruby_cl = XposedEntry.jruby_cl;
                }
              }

              //context_classes = getContextClasses(TAG, context);

              ReflectSetup.Proxy.init(jruby_cl);


              Selector selector = new Selector(matcher, context, localClassLoader, lpp.classLoader);

              for (Match match : selector.match()) {
                setupIntercept(match);
              }
            }
          });
    } catch (Throwable t) {
      Log.e(TAG, "(outer) stack trace", t);
    }

  }

  @SuppressWarnings({"WeakerAccess", "unused"})
  public static void unixPing(final byte[] secret, final byte[] port) { //called from Ruby

    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Thread.sleep(1500);
        } catch (InterruptedException ie) {
          //pass
        }

        try {
          LocalSocket ls = new LocalSocket();
          ls.connect(new LocalSocketAddress("/data/" + "data/trust.nccgroup.paraspectre.android/files/ping.sock", LocalSocketAddress.Namespace.FILESYSTEM));
          OutputStream lsos = ls.getOutputStream();
          lsos.write(secret);
          lsos.write(port);
          lsos.flush();
        } catch (IOException ioe) {
          Log.e(TAG, "failed to ping", ioe);
        }
      }
    }).start();

  }

  private void setupIntercept(final Match match) {

    Member m = match.m;
    String eval = match.eval;

    if (m instanceof Method) {
      Method mm = (Method)m;
      Log.e(TAG, "hooking: " + mm.toGenericString());
      mm.setAccessible(true);
    } else if (m instanceof Constructor) {
      Constructor cc = (Constructor)m;
      cc.setAccessible(true);
      Log.e(TAG, "hooking constructor: " + cc.toString());
    }

    Hook.Scripts scripts = new Hook.Scripts(script, RubyCode.HOOK_RB, eval);

    Hook hook = new Hook(TAG, jruby_cl, scripts, container_cache);
    XposedBridge.hookMethod(m, new MemberHook(hook, pkg));

  }

}
