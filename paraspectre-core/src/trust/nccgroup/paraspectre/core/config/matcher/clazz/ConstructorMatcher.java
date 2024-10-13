package trust.nccgroup.paraspectre.core.config.matcher.clazz;

import java.lang.reflect.Modifier;
import java.util.List;

public class ConstructorMatcher {

  public List<String> params;
  public List<String> throwing;

  @SuppressWarnings({"WeakerAccess"})
  public List<String> modifiers;

  public String eval;
  public String eval_file;

  public Boolean disabled;

  public static class Full extends ConstructorMatcher {
    public Integer imodifiers;

    public Full(ConstructorMatcher c) {
      this.params = c.params;
      this.throwing = c.throwing;

      this.eval = c.eval;
      this.eval_file = c.eval_file;

      this.disabled = c.disabled;

      if (c.modifiers != null) {
        imodifiers = 0;
        for (String modstr : c.modifiers) {
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
