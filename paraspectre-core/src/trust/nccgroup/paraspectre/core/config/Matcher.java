package trust.nccgroup.paraspectre.core.config;

import trust.nccgroup.paraspectre.core.config.matcher.ClassMatcher;

import java.util.List;

public class Matcher {

  public String pkg;
  public List<ClassMatcher> classes;
  public String eval;
  public String eval_file;

  public Boolean disabled;

}
