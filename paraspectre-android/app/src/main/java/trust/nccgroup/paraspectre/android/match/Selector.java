package trust.nccgroup.paraspectre.android.match;

import android.content.Context;
import android.util.Log;

import com.google.common.collect.Sets;
import com.google.gson.Gson;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import trust.nccgroup.paraspectre.android.Utils;
import trust.nccgroup.paraspectre.core.config.Matcher;
import trust.nccgroup.paraspectre.core.config.matcher.ClassMatcher;
import trust.nccgroup.paraspectre.core.config.matcher.clazz.ConstructorMatcher;
import trust.nccgroup.paraspectre.core.config.matcher.clazz.MethodMatcher;

public class Selector {
  private static final String TAG = "PS/Selector";

  private final Matcher matcher;
  private final Context ctx;
  private final ClassLoader appClassLoader;
  private final ClassLoader initClassLoader;

  private final List<Match> matches;

  public Selector(Matcher _matcher, Context _ctx,
                  ClassLoader _appClassLoader, ClassLoader _initClassLoader) {
    matcher = _matcher;
    ctx = _ctx;
    appClassLoader = _appClassLoader;
    initClassLoader = _initClassLoader;

    matches = new ArrayList<>();
  }

  @SuppressWarnings("unused")
  public void clear() {
    matches.clear();
  }

  public List<Match> match() {

    List<ClassMatcher.Full> full_matcher_classes = new ArrayList<>();
    for (ClassMatcher clz : matcher.classes) {
      try {
        if (clz == null || (clz.disabled != null && clz.disabled)) {
          continue;
        }
        full_matcher_classes.add(new ClassMatcher.Full(clz, appClassLoader, initClassLoader));
      } catch (ClassMatcher.LoadException cle) {
        Log.e(TAG, "failed to load classes defined in config. skipping matcher: " + new Gson().toJson(clz));
      }
    }

    ClassSearcher cs;
    try {
      cs = new ClassSearcher(full_matcher_classes, ctx, appClassLoader, initClassLoader);
    } catch (IOException ioe) {
      Log.e(TAG, "ioe", ioe);
      return matches;
    }

    int count = 0;
    for (Match.ClassToHook ch : cs) {
      count += 1;
      if (ch.ctf.constructors != null) {
        matchConstructors(ch.ctf, ch.cls);
      }

      if (ch.ctf.methods != null) {
        matchMethods(ch.ctf, ch.cls);
      }
    }

    return matches;
  }

//  private List<Match.ClassToHook> matchClasses(List<ClassMatcher.Full> fullclss) {
//    String packageCodePath = ctx.getPackageCodePath();
//    DexFile df;
//    try {
//      df = new DexFile(packageCodePath);
//    } catch (IOException ioe) {
//      Log.e(TAG, "failed to find dex file", ioe);
//      return new ArrayList<>();
//    }
//
//    List<Match.ClassToHook> toHook = new ArrayList<>();
//
//    List<ClassMatcher.Full> working_fct = new ArrayList<>(fullclss);
//    for (ClassMatcher.Full fct : fullclss) {
//      if (fct.cls != null) {
//        toHook.add(new Match.ClassToHook(fct, fct.cls));
//        working_fct.remove(fct);
//      }
//    }
//
//    if (working_fct.size() != 0) {
//
//      for (Enumeration<String> iter = df.entries(); iter.hasMoreElements(); ) {
//
//        String clClassStr = iter.nextElement();
//
//        Class<?> clClass = MagicClassFinder.getClass(clClassStr, appClassLoader, initClassLoader);
//        if (clClass == null) {
//          Log.e(TAG, "null class for " + clClassStr);
//          continue;
//        }
//
//        for (ClassMatcher.Full fct : working_fct) {
//
//          if (fct.superCls != null) {
//            if (fct.superCls.isAssignableFrom(clClass)) {
//              if (fct.superCls == clClass) { //we want subclasses, not the actual parent
//                continue;
//              }
//            } else {
//              continue;
//            }
//          }
//
//          if (fct.ifClss != null) {
//            boolean tohook = false;
//            for (Class<?> ifClass : fct.ifClss) {
//              if (ifClass.isAssignableFrom(clClass)) {
//                if (ifClass == clClass) {
//                  break;
//                }
//                tohook = true;
//              } else {
//                tohook = false;
//                break;
//              }
//            }
//            if (!tohook) {
//              continue;
//            }
//          }
//
//          if ((fct.modifiers & clClass.getModifiers()) != fct.modifiers) {
//            continue;
//          }
//
//          toHook.add(new Match.ClassToHook(fct, clClass));
//        }
//      }
//    }
//
//    return toHook;
//  }

  private void matchConstructors(ClassMatcher.Full ctf, Class<?> cls) {
    Constructor[] cons = cls.getDeclaredConstructors();

    for (ConstructorMatcher cm : ctf.constructors) {
      if (cm == null) {
        continue;
      } else if (cm.disabled != null && cm.disabled) {
        continue;
      }

      ConstructorMatcher.Full cmf = new ConstructorMatcher.Full(cm);

      HashSet<Constructor> matching = Sets.newHashSet(cons);

      if (cmf.params != null) {
        HashSet<Constructor> ccs = Sets.newHashSet(matching);
        for (Constructor cc : ccs) {
          Class<?>[] pts = cc.getParameterTypes();
          if (cmf.params.size() != pts.length) {
            matching.remove(cc);
            continue;
          }

          List<String> ctor_pts = new ArrayList<>();
          for (Class<?> t : pts) {
            ctor_pts.add(Utils.javaToHuman(t));
          }

          if (!ctor_pts.equals(cmf.params)) {
            matching.remove(cc);
          }
        }
      }

      if (cmf.throwing != null) {
        HashSet<Constructor> ccs = Sets.newHashSet(matching);
        for (Constructor cc : ccs) {
          Class<?>[] ets = cc.getExceptionTypes();
          if (cmf.throwing.size() != ets.length) {
            matching.remove(cc);
            continue;
          }

          List<String> ctor_ets = new ArrayList<>();
          for (Class<?> t : ets) {
            ctor_ets.add(Utils.javaToHuman(t));
          }

          if (!ctor_ets.equals(cmf.throwing)) {
            matching.remove(cc);
          }
        }
      }

      if (cmf.imodifiers != null) {
        for (Constructor cc : Sets.newHashSet(matching)) {
          if ((cmf.imodifiers & cc.getModifiers()) != cmf.imodifiers) {
            matching.remove(cc);
          }
        }
      }

      for (Constructor cc : matching) {
        if (!Modifier.isAbstract(cc.getModifiers())) {
          String evalString =
            matcher.eval
              + (matcher.eval.isEmpty() || matcher.eval.endsWith(";") ? "" : "; ")
              + ctf.eval
              + (ctf.eval.isEmpty() || ctf.eval.endsWith(";") ? "" : "; ")
              + cmf.eval
              + (cmf.eval.isEmpty() || cmf.eval.endsWith(";") ? "" : "; ");
          matches.add(new Match(cc, evalString));
        }
      }
    }
  }

  private static List<Method> getDeclaredMethods(Class c) {
    List<Method> ms = new ArrayList<>();
    Collections.addAll(ms, c.getDeclaredMethods());
    return ms;
  }

  private static List<Method> getRecursiveDeclaredMethods(Class c) {
    List<Method> ms = new ArrayList<>();

    do {
      Collections.addAll(ms, c.getDeclaredMethods());
      if (c != Object.class) {
        c = c.getSuperclass();
      }
    } while (c != null && c != Object.class);

    return ms;
  }

  private void matchMethods(ClassMatcher.Full ctf, Class<?> cls) {
    List<Method> methods = getDeclaredMethods(cls);
    List<Method> recursive_methods = null;

    for (MethodMatcher mm : ctf.methods) {
      if (mm == null) {
        continue;
      } else if (mm.disabled != null && mm.disabled) {
        continue;
      }

      MethodMatcher.Full mmf = new MethodMatcher.Full(mm);

      if (mmf.recursive != null && mmf.recursive) {
        if (recursive_methods == null) {
          recursive_methods = getRecursiveDeclaredMethods(cls);
        }
      }

      HashSet<Method> matching = (mmf.recursive != null && mmf.recursive) ?
        Sets.newHashSet(recursive_methods) : Sets.newHashSet(methods);

      Set<String> aliases;
      if (mmf.aliases != null) {
        aliases = new HashSet<>(mmf.aliases);
      } else {
        aliases = new HashSet<>();
      }
      if (mmf.name != null) {
        aliases.add(mmf.name);
      }

      //if (mt.name != null || mt.names != null) {
      if (aliases.size() > 0) {
        HashSet<Method> ms = Sets.newHashSet(matching);
        for (Method m : ms) {
          if (!aliases.contains(m.getName())) {
            matching.remove(m);
          }

          //if (!m.getName().equals(mt.name)) {
          //  matching.remove(m);
          //}
        }
      }

      if (mmf.returns != null) {
        HashSet<Method> ms = Sets.newHashSet(matching);
        for (Method m : ms) {
          if (!Utils.javaToHuman(m.getReturnType()).equals(mmf.returns)) {
            matching.remove(m);
          }
        }
      }

      if (mmf.params != null) {
        HashSet<Method> ms = Sets.newHashSet(matching);
        for (Method m : ms) {
          Class<?>[] pts = m.getParameterTypes();
          if (mmf.params.size() != pts.length) {
            matching.remove(m);
            continue;
          }

          List<String> mpts = new ArrayList<>();
          for (Class<?> t : pts) {
            mpts.add(Utils.javaToHuman(t));
          }

          if (!mpts.equals(mmf.params)) {
            matching.remove(m);
          }
        }
      }

      if (mmf.throwing != null) {
        HashSet<Method> ms = Sets.newHashSet(matching);
        for (Method m : ms) {
          Class<?>[] ets = m.getExceptionTypes();
          if (mmf.throwing.size() != ets.length) {
            matching.remove(m);
            continue;
          }

          List<String> mets = new ArrayList<>();
          for (Class<?> t : ets) {
            mets.add(Utils.javaToHuman(t));
          }

          if (!mets.equals(mmf.throwing)) {
            matching.remove(m);
          }
        }
      }

      if (mmf.imodifiers != null) {
        for (Method m : Sets.newHashSet(matching)) {
          if ((mmf.imodifiers & m.getModifiers()) != mmf.imodifiers) {
            matching.remove(m);
          }
        }
      }

      for (Method m : matching) {
        if (!Modifier.isAbstract(m.getModifiers())) {
          String evalString =
            matcher.eval
              + (matcher.eval.isEmpty() || matcher.eval.endsWith(";") ? "" : "; ")
              + ctf.eval
              + (ctf.eval.isEmpty() || ctf.eval.endsWith(";") ? "" : "; ")
              + mmf.eval
              + (mmf.eval.isEmpty() || mmf.eval.endsWith(";") ? "" : "; ");

          matches.add(new Match(m, evalString));
        }
      }
    }

  }

}
