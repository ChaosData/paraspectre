package trust.nccgroup.paraspectre.webapp;

import com.google.common.io.CharStreams;
import trikita.log.Log;
import trust.nccgroup.paraspectre.core.config.Matcher;
import trust.nccgroup.paraspectre.core.config.matcher.ClassMatcher;
import trust.nccgroup.paraspectre.core.config.matcher.clazz.ConstructorMatcher;
import trust.nccgroup.paraspectre.core.config.matcher.clazz.MethodMatcher;

import java.io.*;
import java.util.ArrayList;

class Assembler {

  private static final String TAG = "PS/Assembler";

  public static WebAppConfig config = null;

  public static void setConfig(WebAppConfig c) {
    config = c;
  }

  static class AssemblerException extends Exception {
    public final String reason;
    public AssemblerException(String _reason) { reason = _reason; }
    public AssemblerException() { reason = ""; }
  }

  private static class UnsafePathException extends AssemblerException {
    public UnsafePathException() {}
    public UnsafePathException(String _reason) { super(_reason); }
  }
  private static class EvalFileNotFoundException extends AssemblerException {
    public EvalFileNotFoundException() {}
    public EvalFileNotFoundException(String _reason) { super(_reason); }
  }
  private static class MiscException extends AssemblerException {
    public MiscException() {}
    public MiscException(String _reason) { super(_reason); }
  }
  private static class EvalFileReadException extends AssemblerException {
    public EvalFileReadException() {}
    public EvalFileReadException(String _reason) { super(_reason); }
  }


  private static String readFile(String path) throws AssemblerException {
    if (path == null) {
      return "";
    }

    String file_path = config.edit_root + path;
    if (!Util.isPathSafe(file_path, config)) {
      Log.e(TAG, "unsafe path: " + file_path);
      throw new UnsafePathException("invalid path: \"" + path + "\"");
    }

    InputStream fis;
    try {
      fis = new FileInputStream(file_path);
    } catch (FileNotFoundException fnfe) {
      throw new EvalFileNotFoundException("file not found: \"" + path + "\"");
    }

    Reader r;
    try {
      r = new InputStreamReader(fis, "UTF-8");
    } catch (UnsupportedEncodingException uee) {
      throw new MiscException("data not utf-8?");
    }

    String text;
    try {
      text = CharStreams.toString(r);
    } catch (IOException ioe) {
      throw new EvalFileReadException("error reading eval file.");
    }
    return text;
  }

  private static MethodMatcher assemble(MethodMatcher in) throws AssemblerException {
    MethodMatcher out = new MethodMatcher();
    out.disabled = in.disabled;
    out.name = in.name;
    out.aliases = in.aliases;
    out.returns = in.returns;
    out.params = in.params;
    out.throwing = in.throwing;

    if (in.eval != null) {
      out.eval = in.eval;
    } else {
      out.eval = readFile(in.eval_file);
    }
    out.eval_file = null;
    return out;
  }

  private static ConstructorMatcher assemble(ConstructorMatcher in) throws AssemblerException {
    ConstructorMatcher out = new ConstructorMatcher();
    out.disabled = in.disabled;
    out.params = in.params;
    out.throwing = in.throwing;

    if (in.eval != null) {
      out.eval = in.eval;
    } else {
      out.eval = readFile(in.eval_file);
    }
    out.eval_file = null;
    return out;
  }

  private static ClassMatcher assemble(ClassMatcher in) throws AssemblerException {
    ClassMatcher out = new ClassMatcher();
    out.disabled = in.disabled;
    out.name = in.name;
    out.extending = in.extending;
    out.implementing = in.implementing;
    if (in.constructors != null) {
      out.constructors = new ArrayList<>();
      for (ConstructorMatcher c : in.constructors) {
          out.constructors.add(assemble(c));
        }
    } else {
      out.constructors = null;
    }
    if (in.methods != null) {
      out.methods = new ArrayList<>();
      for (MethodMatcher m : in.methods) {
        out.methods.add(assemble(m));
      }
    } else {
      out.methods = null;
    }

    if (in.eval != null) {
      out.eval = in.eval;
    } else {
      out.eval = readFile(in.eval_file);
    }
    out.eval_file = null;
    return out;
  }

  static Matcher assemble(Matcher in) throws AssemblerException {
    Matcher out = new Matcher();
    out.disabled = in.disabled;
    out.pkg = in.pkg;
    out.classes = new ArrayList<>();
    if (in.classes != null) {
      for (ClassMatcher c : in.classes) {
        out.classes.add(assemble(c));
      }
    } else {
      out.classes = null;
    }

    if (in.eval != null) {
      out.eval = in.eval;
    } else {
      out.eval = readFile(in.eval_file);
    }
    out.eval_file = null;
    return out;
  }

}
