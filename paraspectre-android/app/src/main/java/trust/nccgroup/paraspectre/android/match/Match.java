package trust.nccgroup.paraspectre.android.match;

import java.lang.reflect.Member;

import trust.nccgroup.paraspectre.core.config.matcher.ClassMatcher;

public class Match {
  public Member m;
  public String eval;

  Match(Member _m, String _eval) {
    m = _m;
    eval = _eval;
  }

  public static class ClassToHook {
    ClassMatcher.Full ctf;
    Class<?> cls;

    ClassToHook(ClassMatcher.Full _ctf, Class<?> _cls) {
      ctf = _ctf;
      cls = _cls;
    }
  }

}
