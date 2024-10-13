package trust.nccgroup.paraspectre.android;

import android.app.Application;
import android.os.StrictMode;
import android.util.Log;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

import java.lang.reflect.Field;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XCallback;
import trust.nccgroup.paraspectre.core.Config;
import trust.nccgroup.paraspectre.core.Hook;
import trust.nccgroup.paraspectre.core.config.Matcher;


public class XposedEntry implements IXposedHookZygoteInit, IXposedHookLoadPackage {

  private static final String TAG = "PS/XposedEntry";
  //public static final Config boot_config = Configuration.getFileConfig(new File("/data/data/trust.nccgroup.paraspectre.android/files/edit_root/paraspectre.json"));

  public static Config config = null;
  public static ClassLoader jruby_cl = null;
//  public static Object ruby = null;
  public static Object container = null;
  public static Object unit = null;
  public static final Object lock = new Object();
  public static boolean classLoaderFix = false;

  public static final Hook.Input dummy_input = new Hook.Input(null, null, null, null, null);

  @Override
  public void initZygote(StartupParam startupParam) throws Throwable {
    Log.i(TAG, "initZygote()");


//    //note: dir allowed by zygote.te
//    if (!new File("/data/dalvik-cache/paraspectre").exists()) {
//      return;
//    }
//    try {
//      jruby_cl = new PathClassLoader(
//        "/data/dalvik-cache/paraspectre/runtime.dex.jar",
//        "/vendor/lib:/system/lib",
//        this.getClass().getClassLoader()
//      );
//    } catch (Throwable t) {
//      Log.e(TAG, "error", t);
//    }

    PackageHook.threadedJRubyClassLoaderPrep();

    //note: This is a bit of magic to pre-initialize the asset config for all
    //      Zygote-forked classloaders once, at early boot.
    //      It also forces static init of Configuration.class pre-Zygote fork.
    Configuration.asset_config = Configuration.getAssetConfig();

  }

  @Override
  public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpp) throws Throwable {
    if (lpp.packageName.equals("android")) {
      grantPermsHook(lpp);
    }

    StrictMode.ThreadPolicy smtp = StrictMode.getThreadPolicy();
    StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX);

    try {
      Matcher matcher = Configuration.getMatcher(lpp.packageName);
      config = Configuration.getConfig(lpp.packageName, true);
      if (matcher != null) {
        if (matcher.disabled != null && matcher.disabled) {
          Log.e(TAG, lpp.packageName + " matcher disabled");
          return;
        }
        List<Matcher> lm = Lists.newArrayList(matcher);
        if (config.matchers != null) {
          for (Matcher m : config.matchers) {
            if (lpp.packageName.equals(m.pkg)) {
              lm.add(m);
            }
          }
        }

        config.matchers = lm;
      }

      if (config.matchers == null) {
        Log.e(TAG, "config.matchers == null");
        return;
      }

      for (Matcher m : config.matchers) {
        if (m.pkg.equals(lpp.packageName)) {
          if (m.disabled != null && m.disabled) {
            continue;
          }

          PackageHook.jRubyContainerInit();

          XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook(XCallback.PRIORITY_DEFAULT + 1) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
              Application app = (Application) param.thisObject;
              if (!NetGrantClient.hasInternet(app)) {
                Log.e(TAG, lpp.packageName + " does not have internet. granting... (after sub Application.onCreate finishes)");
                NetGrantClient ngc = new NetGrantClient(lpp.packageName);
                ngc.getInternet(app);
              }
            }
          });

          PackageHook ph = new PackageHook(lpp, m);
          ph.setupHooks();
        }
      }
    }
    catch (Throwable t) {
      Log.e(TAG, "error", t);
    }
    finally {
      StrictMode.setThreadPolicy(smtp);
    }
  }


  private void grantPermsHook(XC_LoadPackage.LoadPackageParam lpp) {
    try {
      final Class PackageManagerServiceClass = XposedHelpers.findClass("com.android.server.pm.PackageManagerService", lpp.classLoader);

      //private boolean grantSignaturePermission(String,android.content.pm.PackageParser$Package,
      //   com.android.server.pm.BasePermission, com.android.server.pm.PermissionsState) {

      XposedHelpers.findAndHookMethod(PackageManagerServiceClass,
        "grantSignaturePermission",
        "java.lang.String", "android.content.pm.PackageParser$Package", "com.android.server.pm.BasePermission", "com.android.server.pm.PermissionsState",
        new XC_MethodHook(XCallback.PRIORITY_DEFAULT + 1) {
          @Override
          protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
              Object pkg = param.args[1];
              Field packageNameField = pkg.getClass().getField("packageName");

              Object opackageName = packageNameField.get(pkg);
              if (opackageName instanceof String) {
                String packageName = (String) opackageName;
                if (packageName.equals(Constants.pkg)) {
                  param.setResult(true);
                }
              } else {
                Log.e(TAG, "not string?");
              }
            } catch (Throwable t) {
              Log.e(TAG, "???", t);
            }
          }
        });

      //private static void enforceDeclaredAsUsedAndRuntimeOrDevelopmentPermission(android.content.pm.PackageParser$Package,
      //            com.android.server.pm.BasePermission) {

      XposedHelpers.findAndHookMethod(PackageManagerServiceClass,
        "enforceDeclaredAsUsedAndRuntimeOrDevelopmentPermission",
        "android.content.pm.PackageParser$Package", "com.android.server.pm.BasePermission",
        new XC_MethodHook(XCallback.PRIORITY_DEFAULT + 1) {
          @Override
          protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
              Object bp = param.args[1];
              Field nameField = bp.getClass().getDeclaredField("name");
              Object oname = null;
              boolean wasAccessible = nameField.isAccessible();
              if (!wasAccessible) {
                nameField.setAccessible(true);
                oname = nameField.get(bp);
                nameField.setAccessible(false);
              }
              if (oname instanceof String) {
                String name = (String) oname;
                if (name.equals(android.Manifest.permission.INTERNET)) {
                  param.setResult(null);
                }
              } else {
                Log.e(TAG, "not string?");
              }
            } catch (Throwable t) {
              Log.e(TAG, "???", t);
            }
          }
        });

    } catch (Throwable t) {
      Log.e(TAG, "failed to setup permissions hook", t);
    }
  }
}
