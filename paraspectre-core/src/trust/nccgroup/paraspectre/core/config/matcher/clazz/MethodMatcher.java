package trust.nccgroup.paraspectre.core.config.matcher.clazz;

import java.lang.reflect.Modifier;
import java.util.List;

public class MethodMatcher {

  public String name;
  public List<String> aliases; //for obfuscated names
  public String returns;
  public List<String> params;
  public List<String> throwing;

  @SuppressWarnings({"WeakerAccess"})
  public List<String> modifiers;

  public Boolean recursive;
  public String eval;
  public String eval_file;

  public Boolean disabled;

  public static class Full extends MethodMatcher {
    public Integer imodifiers = null;

    public Full(MethodMatcher m) {
      this.name = m.name;
      this.aliases = m.aliases;
      this.returns = m.returns;
      this.params = m.params;
      this.throwing = m.throwing;

      this.recursive = m.recursive;
      this.eval = m.eval;
      this.eval_file = m.eval_file;

      this.disabled = m.disabled;

      if (m.modifiers != null) {
        imodifiers = 0;
        for (String modstr : m.modifiers) {
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
