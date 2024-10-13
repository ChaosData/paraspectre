import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

class JTester {

  public static interface Tester {
    public String doStuff(String a);
  }

  public static class ATester implements Tester {

    @Override
    public String doStuff(String a) {
      System.out.println(a);
      return "";
    }
  }

  public String secretSauce(String a, int b) {
    try {
      throw new Exception();
    } catch (Throwable t) {
      StackTraceElement[] ste_arr = t.getStackTrace();
      for (StackTraceElement ste : ste_arr) {
        System.out.println(ste);
      }
    }

    return a + b;
  }

  public String secretSauce2(String a, int b) {
    try {
      throw new Exception();
    } catch (Throwable t) {
      StackTraceElement[] ste_arr = t.getStackTrace();
      for (StackTraceElement ste : ste_arr) {
        System.out.println(ste);
      }
    }
    return a + b;
  }

  public static void main(String[] argv) {
    System.out.println("->start");
    JTester j = new JTester();
    System.out.println(j.secretSauce("hell0", 55));
    System.out.println(j.secretSauce2("world", 66));

    ATester a = new ATester();
    a.doStuff("hi there");

    RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
    List<String> arguments = runtimeMxBean.getInputArguments();

    if (arguments.size() == 0) {
      System.out.println("1: Agent not loaded.");
    } else {
      for (String argument : arguments) {
        System.out.println(argument);
        if (argument.startsWith("-javaagent:")) {
          System.out.println("1: Agent loaded!");
        } else {
          System.out.println("1: Agent not loaded.");
        }
      }
    }

    try {
      Class.forName("trust.nccgroup.paraspectre.agent.PreMain");
      System.out.println("2: Agent loaded!");
    } catch (ClassNotFoundException e) {
      System.out.println("2: Agent not loaded.");
      e.printStackTrace();
    }

    System.out.println("->end");
  }

}
