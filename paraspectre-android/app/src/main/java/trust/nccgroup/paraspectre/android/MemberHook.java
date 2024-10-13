package trust.nccgroup.paraspectre.android;

import android.os.Process;
import android.os.StrictMode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import trust.nccgroup.paraspectre.core.ContainerUnitHolder;
import trust.nccgroup.paraspectre.core.Hook;
import trust.nccgroup.paraspectre.core.ReflectSetup;


class MemberHook extends XC_MethodHook {

  private Hook hook = null;
  private String pkg = null;

  MemberHook(Hook _hook, String _pkg) {
    hook = _hook;
    pkg = _pkg;
  }

  @Override
  protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
    long tid = Thread.currentThread().getId();

    if (!PackageHook.active.contains(tid)) {
      StrictMode.ThreadPolicy tp = StrictMode.getThreadPolicy();
      PackageHook.active.add(tid);

      //StrictMode.ThreadPolicy ntp = new StrictMode.ThreadPolicy.Builder(tp).permitNetwork().build();
      StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX);

      Method m = null;
      Constructor c = null;

      if (param.method instanceof Method) {
        m = (Method)param.method;
      } else if (param.method instanceof Constructor) {
        c = (Constructor)param.method;
      }

      Hook.Input input = new Hook.Input(param.thisObject, c, m, param.args, null);
      input.vars.put("uid", Process.myUid());
      input.vars.put("tid", tid);
      input.vars.put("pkg", pkg);
      input.vars.put("appctx", PackageHook.appctx);
      input.vars.put("appcl", PackageHook.appctx.getClass().getClassLoader());


      try {
        synchronized (PackageHook.container_cache) {
          if (XposedEntry.container != null) {
            ContainerUnitHolder cuh = new ContainerUnitHolder(XposedEntry.container, XposedEntry.unit);
            XposedEntry.container = null;
            XposedEntry.unit = null;
            PackageHook.container_cache.put(tid, cuh);
          }
        }

        Object[] ret_pair = hook.run(tid, input);

        if ((boolean)ret_pair[0]) {
          Object ret = ret_pair[1];
          if (ret != null) {
            if (ret == ReflectSetup.NULL) {
              param.setResult(null);
            } else if (ret == ReflectSetup.VOID) {
              param.setResult(null);
            } else {
              param.setResult(ret);
            }
          }
        }
      } catch (Throwable t) {
        param.setThrowable(t);
      }

      PackageHook.active.remove(tid);
      StrictMode.setThreadPolicy(tp);
    }/* else {
      Log.i(hook.TAG, param.method.toString() + " called from same thread");
    }*/
    super.beforeHookedMethod(param);
  }

//  @Override
//  protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//    super.afterHookedMethod(param);
//  }

}
