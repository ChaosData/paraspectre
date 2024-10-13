package trust.nccgroup.paraspectre.core;

//import net.bytebuddy.asm.Advice;

import java.util.ArrayList;
import java.util.List;

public class MySystemInterceptor {

  public static void loadLibrary(String s) throws SecurityException, UnsatisfiedLinkError, NullPointerException {
    System.out.println("loadLibrary1: " + s);
    if (s.equals("management")) {
      return;
    }
    try {
    } catch (Throwable t) {
      System.out.println("#####");
      t.printStackTrace();
    }
  }

  public static List<String> getInputArguments() {
    System.out.println("#YOLO");
    return new ArrayList<String>();
  }

/*
  @Advice.OnMethodEnter
  static void foo(@Advice.BoxedArguments Object[] boxed) {
    System.out.println("foo");
  }

  @Advice.OnMethodExit
  static void bar(@Advice.BoxedReturn(readOnly = false) Object value) {
    System.out.println("bar");
  }
*/


}