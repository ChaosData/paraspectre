package trust.nccgroup.paraspectre.agent;


public class MySystemInterceptor {

  public static void loadLibrary(String s) throws SecurityException, UnsatisfiedLinkError, NullPointerException {
    System.out.println("loadLibrary2: " + s);
    if (s.equals("management")) {
      return;
    }
    try {
    } catch (Throwable t) {
      System.out.println("#####");
      t.printStackTrace();
    }
  }


}