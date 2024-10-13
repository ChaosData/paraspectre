package trust.nccgroup.paraspectre.android.match;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import dalvik.system.DexFile;
import trust.nccgroup.paraspectre.core.config.matcher.ClassMatcher;

class ClassSearcher implements Iterable<Match.ClassToHook> {
  private static final String TAG = "PS/ClassSearcher";

  private List<ClassMatcher.Full> fullclss;

  private List<Match.ClassToHook> toHook = new ArrayList<>();
  private List<ClassMatcher.Full> working_fct;
  private Enumeration<String> iter;

  private final ClassLoader appClassLoader;
  private final ClassLoader initClassLoader;

  ClassSearcher(List<ClassMatcher.Full> _fullclss, Context ctx,
                ClassLoader _appClassLoader,
                ClassLoader _initClassLoader) throws IOException {
    String packageCodePath = ctx.getPackageCodePath();
    DexFile df;
    try {
      df = new DexFile(packageCodePath);
    } catch (IOException ioe) {
      Log.e(TAG, "failed to find dex file", ioe);
      throw ioe;
    }

    fullclss = _fullclss;
    working_fct = new ArrayList<>(fullclss);

    for (ClassMatcher.Full fct : fullclss) {
      if (fct.cls != null) {
        toHook.add(new Match.ClassToHook(fct, fct.cls));
        working_fct.remove(fct);
      }
    }

    iter = df.entries();
    appClassLoader = _appClassLoader;
    initClassLoader = _initClassLoader;
  }

  @Override
  public Iterator<Match.ClassToHook> iterator() {
    return new Iterator<Match.ClassToHook>() {
      @Override
      public boolean hasNext() {
        if (!toHook.isEmpty()) {
          return true;
        }

        if (working_fct.size() == 0) {
          return false;
        }

        while (iter.hasMoreElements()) {
          String clClassStr = iter.nextElement();

          Class<?> clClass = MagicClassFinder.getClass(clClassStr, appClassLoader, initClassLoader);
          if (clClass == null) {
            Log.e(TAG, "null class for " + clClassStr);
            continue;
          }

          for (ClassMatcher.Full fct : working_fct) {

            if (fct.superCls != null) {
              if (fct.superCls.isAssignableFrom(clClass)) {
                if (fct.superCls == clClass) { //we want subclasses, not the actual parent
                  continue;
                }
              } else {
                continue;
              }
            }

            if (fct.ifClss != null) {
              boolean tohook = false;
              for (Class<?> ifClass : fct.ifClss) {
                if (ifClass.isAssignableFrom(clClass)) {
                  if (ifClass == clClass) {
                    break;
                  }
                  tohook = true;
                } else {
                  tohook = false;
                  break;
                }
              }
              if (!tohook) {
                continue;
              }
            }

            if ((fct.imodifiers & clClass.getModifiers()) != fct.imodifiers) {
              continue;
            }

            toHook.add(new Match.ClassToHook(fct, clClass));
          }

        }

        return hasNext();
      }

      @Override
      public Match.ClassToHook next() {
        if (!toHook.isEmpty()) {
          return toHook.remove(toHook.size()-1);
        }
        return null;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }
}
