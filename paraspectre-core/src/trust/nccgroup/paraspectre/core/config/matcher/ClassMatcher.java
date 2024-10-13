package trust.nccgroup.paraspectre.core.config.matcher;

import trikita.log.Log;
import trust.nccgroup.paraspectre.core.config.matcher.clazz.ConstructorMatcher;
import trust.nccgroup.paraspectre.core.config.matcher.clazz.MethodMatcher;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class ClassMatcher {

  public String name;
  public String extending;
  public List<String> implementing;

  @SuppressWarnings({"WeakerAccess"})
  public List<String> modifiers;

  public List<ConstructorMatcher> constructors;
  public List<MethodMatcher> methods;
  public String eval;
  public String eval_file;

  public Boolean disabled;

  public static class LoadException extends Exception {

  }

  public static class Full extends ClassMatcher {
    private static final String TAG = "PS/ClassMatcher.Full";

    public Class<?> cls = null;
    public Class<?> superCls = null;
    public List<Class<?>> ifClss = null;
    public Integer imodifiers = null;

    static Class<?> getClass(String cls, ClassLoader cl, ClassLoader fallback_cl) {
      Class<?> c = null;
      try {
        c = cl.loadClass(cls);
      } catch (Throwable ignored) { }
      if (c == null && fallback_cl != null) {
        try {
          c = fallback_cl.loadClass(cls);
        } catch (Throwable ignored) { }
      }
      if (c == null) {
        if (fallback_cl != cl) {
          Log.e(TAG, "error loading class " + cls);
        }
      }
      return c;
    }


    public Full(ClassMatcher clz, ClassLoader cl, ClassLoader fallback_cl) throws LoadException {
      this.name = clz.name;
      this.extending = clz.extending;
      this.implementing = clz.implementing;
      this.constructors = clz.constructors;
      this.methods = clz.methods;
      this.eval = clz.eval;
      this.eval_file = clz.eval_file;

      if (clz.name != null) {
        this.cls = getClass(clz.name, cl, fallback_cl);
        if (this.cls == null) {
          throw new LoadException();
        }
      }
      if (clz.extending != null) {
        this.superCls = getClass(clz.extending, cl, fallback_cl);
        if (this.superCls == null) {
          throw new LoadException();
        }
      }
      if (clz.implementing != null) {
        this.ifClss = new ArrayList<>();
        for (String ifClsStr : clz.implementing) {
          Class<?> ifcls = getClass(ifClsStr, cl, fallback_cl);
          if (ifcls == null) {
            this.ifClss = null;
            throw new LoadException();
          }
          this.ifClss.add(ifcls);
        }
      }

      if (clz.modifiers != null) {
        imodifiers = 0;
        for (String modstr : clz.modifiers) {
          switch (modstr.toUpperCase()) {
            case "ABSTRACT":
              imodifiers |= Modifier.ABSTRACT; break;
            case "FINAL":
              imodifiers |= Modifier.FINAL; break;
            case "INTERFACE":
              imodifiers |= Modifier.INTERFACE; break;
            case "NATIVE":
              imodifiers |= Modifier.NATIVE; break;
            case "PRIVATE":
              imodifiers |= Modifier.PRIVATE; break;
            case "PROTECTED":
              imodifiers |= Modifier.PROTECTED; break;
            case "PUBLIC":
              imodifiers |= Modifier.PUBLIC; break;
            case "STATIC":
              imodifiers |= Modifier.STATIC; break;
            case "STRICT":
              imodifiers |= Modifier.STRICT; break;
            case "SYNCHRONIZED":
              imodifiers |= Modifier.SYNCHRONIZED; break;
            case "TRANSIENT":
              imodifiers |= Modifier.TRANSIENT; break;
            case "VOLATILE":
              imodifiers |= Modifier.VOLATILE; break;
          }
        }
      }

      //note: this should be done after eval_file to eval translation
      if (this.eval == null) {
        this.eval = "";
      }
    }
  }
}
