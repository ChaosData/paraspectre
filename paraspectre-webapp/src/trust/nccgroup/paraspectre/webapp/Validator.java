package trust.nccgroup.paraspectre.webapp;

import trust.nccgroup.paraspectre.core.config.Matcher;
import trust.nccgroup.paraspectre.core.config.matcher.ClassMatcher;
import trust.nccgroup.paraspectre.core.config.matcher.clazz.ConstructorMatcher;
import trust.nccgroup.paraspectre.core.config.matcher.clazz.MethodMatcher;

import java.util.List;
import java.util.regex.Pattern;


public class Validator {
  @SuppressWarnings("unused")
  private static final String TAG = "PS/Validator";

  enum TYPE {
    hook,
    meta,
    config
  }

  private TYPE type;
  private Validator(TYPE type) {
    this.type = type;
  }

  private static final Pattern ipre = Pattern.compile("^([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})$");

  static final Validator hook = new Validator(TYPE.hook);
  public static final Validator meta = new Validator(TYPE.meta);
  public static final Validator config = new Validator(TYPE.config);

  static class ValidatorException extends Exception {
    public final String reason;
    public ValidatorException(String _reason) { reason = _reason; }
    public ValidatorException() { reason = ""; }
  }
  private static class InvalidConfigException extends ValidatorException {
    public InvalidConfigException() {}
    public InvalidConfigException(String _reason) { super(_reason); }
  }
  private static class InvalidNetException extends ValidatorException {
    public InvalidNetException() {}
    public InvalidNetException(String _reason) { super(_reason); }
  }
  private static class InvalidBindException extends ValidatorException {
    public InvalidBindException() {}
    public InvalidBindException(String _reason) { super(_reason); }
  }
  private static class InvalidMatcherException extends ValidatorException {
    public InvalidMatcherException() {}
    public InvalidMatcherException(String _reason) { super(_reason); }
  }
  private static class InvalidClassException extends ValidatorException {
    public InvalidClassException() {}
    public InvalidClassException(String _reason) { super(_reason); }
  }
  private static class InvalidMethodException extends ValidatorException {
    public InvalidMethodException() {}
    public InvalidMethodException(String _reason) { super(_reason); }
  }
  private static class InvalidConstructorException extends ValidatorException {
    public InvalidConstructorException() {}
    public InvalidConstructorException(String _reason) { super(_reason); }
  }


  private static boolean nullOrEmpty(String in) {
    return in == null || in.length() == 0;
  }

  private static boolean nullOrEmpty(List in) {
    return in == null || in.size() == 0;
  }

  void validate(trust.nccgroup.paraspectre.core.Config in) throws ValidatorException {
    if (in.net == null) {
      throw new InvalidConfigException("net is undeclared or null.");
    }

    validate(in.net);

    if (in.matchers != null) {
      for (Matcher m : in.matchers) {
        validate(m);
      }
    }
  }

  private void validate(trust.nccgroup.paraspectre.core.config.Net in) throws ValidatorException {
    if (in.forwarder == null) {
      throw new InvalidNetException("net.forwarder is undeclared or null.");
    }
    if (in.webapp == null) {
      throw new InvalidNetException("net.webapp is undeclared or null.");
    }
    if (in.pinger == null) {
      throw new InvalidNetException("net.pinger is undeclared or null.");
    }

    validate(in.forwarder, false);
    validate(in.webapp, true);
    validate(in.pinger, false);
  }

  private void validate(trust.nccgroup.paraspectre.core.config.net.Bind in, boolean ip) throws ValidatorException {
    if (nullOrEmpty(in.host)) {
      throw new InvalidBindException("net.*.host is undeclared, null, or empty.");
    }

    if (in.port == 0) {
      throw new InvalidBindException("net.*.port is undeclared or 0.");
    }

    if (ip) {
      java.util.regex.Matcher m = ipre.matcher(in.host);
      if (!m.matches()) {
        throw new InvalidBindException("net.*.host does not match an IPv4 address.");
      }

      for (int i=1; i<5; i++) {
        try {
          int oct = Integer.parseInt(m.group(i));
          if (oct > 255 || oct < 0) {
            throw new InvalidBindException("net.*.host IPv4 octet is not between 0 and 255.");
          }
        } catch (NumberFormatException nfe) {
          throw new InvalidBindException("net.*.host IPv4 octet is not an integer.");
        }
      }
    }

  }


  void validate(Matcher in) throws ValidatorException {
    switch (type) {
      case hook:
      case meta: {
        if (!nullOrEmpty(in.pkg)) {
          throw new InvalidMatcherException("pkg is somehow undeclared, null, or empty.");
        }
        break;
      }
      case config: {
        if (nullOrEmpty(in.pkg)) {
          throw new InvalidMatcherException("matchers.*.pkg is undeclared, null, or empty.");
        }
        break;
      }
    }

    if (in.classes != null) {
      for (ClassMatcher c : in.classes) {
        validate(c);
      }
    }

    switch (type) {
      case hook: {
//        if (in.eval == null && in.eval_file == null) {
//          throw new InvalidMatcherException();
//        }

        if (in.eval != null && in.eval_file != null) {
          throw new InvalidMatcherException("cannot have both top-level eval and eval_file.");
        }
        break;
      }
      case meta:
      case config: {
//        if (in.eval == null) {
//          throw new InvalidMatcherException();
//        }

        if (in.eval_file != null) {
          throw new InvalidMatcherException("top-level eval_file may not be used in this file.");
        }
        break;
      }
    }
  }


  private void validate(ClassMatcher in) throws ValidatorException {
    if (nullOrEmpty(in.name) && nullOrEmpty(in.extending)
        && nullOrEmpty(in.implementing)) {
      throw new InvalidClassException("class requires a name, extending, or implementing field.");
    }

    if ("".equals(in.name)) {
      throw new InvalidClassException("class name may not be blank.");
    }

    if ("".equals(in.extending)) {
      throw new InvalidClassException("class extending may not be blank.");
    }

    if (in.constructors != null) {
      for (ConstructorMatcher c : in.constructors) {
        validate(c);
      }
    }

    if (in.methods != null) {
      for (MethodMatcher m : in.methods) {
        validate(m);
      }
    }

    if (in.modifiers != null) {
      if (in.modifiers.isEmpty()) {
        throw new InvalidClassException("class modifiers may not be empty.");
      }
      for (String s : in.modifiers) {
        if (nullOrEmpty(s)) {
          throw new InvalidMethodException("class modifiers elements may not be null or blank.");
        }
      }
    }

    switch (type) {
      case hook: {
//        if (in.eval == null && in.eval_file == null) {
//          throw new InvalidClassException();
//        }

        if (in.eval != null && in.eval_file != null) {
          throw new InvalidClassException("cannot have both class-level eval and eval_file.");
        }
      }
      case meta:
      case config: {
//        if (in.eval == null) {
//          throw new InvalidClassException();
//        }

        if (in.eval_file != null) {
          throw new InvalidClassException("class-level eval_file may not be used in this file.");
        }
      }
    }
  }

  private void validate(MethodMatcher in) throws ValidatorException {
    if (in.name == null && in.aliases != null) {
      throw new InvalidMethodException("cannot use method alias without a name.");
    }

    if (in.name == null && in.returns == null && in.params == null && in.throwing == null && in.modifiers == null) {
      throw new InvalidMethodException("method requires a name, returns, params, throwing, or modifiers field.");
    }

    if ("".equals(in.name)) {
      throw new InvalidMethodException("method name may not be blank.");
    }

    if ("".equals(in.returns)) {
      throw new InvalidMethodException("method returns may not be blank.");
    }

    if (in.params != null) {
      for (String s : in.params) {
        if (nullOrEmpty(s)) {
          throw new InvalidMethodException("method params elements may not be null or blank.");
        }
      }
    }

    if (in.throwing != null) {
      for (String s : in.throwing) {
        if (nullOrEmpty(s)) {
          throw new InvalidMethodException("method throwing elements may not be null or blank.");
        }
      }
    }

    if (in.modifiers != null) {
      if (in.modifiers.isEmpty()) {
        throw new InvalidClassException("method modifiers may not be empty.");
      }
      for (String s : in.modifiers) {
        if (nullOrEmpty(s)) {
          throw new InvalidMethodException("method modifiers elements may not be null or blank.");
        }
      }
    }


    switch (type) {
      case hook: {
        if (in.eval != null && in.eval_file != null) {
          throw new InvalidConstructorException("cannot have both method-level eval and eval_file.");
        }
        break;
      }
      case meta:
      case config: {
        if (in.eval_file != null) {
          throw new InvalidConstructorException("method-level eval_file may not be used in this file.");
        }
        break;
      }
    }
  }

  private void validate(ConstructorMatcher in) throws ValidatorException { //TODO: make sure this doesn't pull all constructors in by default
    //TODO: add additional checks
    if (in.params != null) {
      for (String s : in.params) {
        if (nullOrEmpty(s)) {
          throw new InvalidConstructorException("constructor params elements may not be null or blank.");
        }
      }
    }

    if (in.throwing != null) {
      for (String s : in.throwing) {
        if (nullOrEmpty(s)) {
          throw new InvalidConstructorException("constructor throwing elements may not be null or blank.");
        }
      }
    }

    if (in.modifiers != null) {
      if (in.modifiers.isEmpty()) {
        throw new InvalidClassException("constructor modifiers may not be empty.");
      }
      for (String s : in.modifiers) {
        if (nullOrEmpty(s)) {
          throw new InvalidMethodException("constructor modifiers elements may not be null or blank.");
        }
      }
    }

    switch (type) {
      case hook: {
        if (in.eval != null && in.eval_file != null) {
          throw new InvalidConstructorException("cannot have both constructor-level eval and eval_file.");
        }
        break;
      }
      case meta:
      case config: {
        if (in.eval_file != null) {
          throw new InvalidConstructorException("constructor-level eval_file may not be used in this file.");
        }
        break;
      }
    }
  }

}
