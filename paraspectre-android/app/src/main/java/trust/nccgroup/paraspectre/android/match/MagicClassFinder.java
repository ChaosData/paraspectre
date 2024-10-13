package trust.nccgroup.paraspectre.android.match;

import android.util.Log;

import java.lang.reflect.Method;

import dalvik.system.BaseDexClassLoader;

public class MagicClassFinder {

  private static final String TAG = "PS/MagicClassFinder";

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

  public static Class<?> findClass(ClassLoader cl, String name) {
    try {
      return (Class<?>)findClass.invoke(cl, name);
    } catch (Throwable ignored) {
      return null;
    }
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


}
