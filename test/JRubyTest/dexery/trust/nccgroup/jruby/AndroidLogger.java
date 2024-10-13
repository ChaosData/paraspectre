package trust.nccgroup.jruby;

import java.io.PrintStream;
import org.jruby.util.log.Logger;
import java.lang.reflect.Method;


public class AndroidLogger implements Logger {

  private final String loggerName;
  private boolean debug = false;

  public AndroidLogger(String loggerName) {
    this.loggerName = loggerName;
  }

  public AndroidLogger(String loggerName, PrintStream stream) {
    this.loggerName = loggerName;
  }

  public String getName() {
    return loggerName;
  }

  public void warn(String message, Object... args) {
    Log.w("JRuby", format(message, args));
  }

  public void warn(Throwable throwable) {
    Log.w("JRuby", "error", throwable);
  }

  public void warn(String message, Throwable throwable) {
    Log.w("JRuby", message, throwable);
  }

  public void error(String message, Object... args) {
    Log.e("JRuby", format(message, args));
  }

  public void error(Throwable throwable) {
    Log.e("JRuby", "error", throwable);
  }

  public void error(String message, Throwable throwable) {
    Log.e("JRuby", message, throwable);
  }

  public void info(String message, Object... args) {
    Log.i("JRuby", format(message, args));
  }

  public void info(Throwable throwable) {
    Log.i("JRuby", "error", throwable);
  }

  public void info(String message, Throwable throwable) {
    Log.i("JRuby", message, throwable);
  }

  public void debug(String message, Object... args) {
    Log.d("JRuby", format(message, args));

  }

  public void debug(Throwable throwable) {
    Log.d("JRuby", "error", throwable);
  }

  public void debug(String message, Throwable throwable) {
    Log.d("JRuby", message, throwable);
  }

  public boolean isDebugEnabled() {
    return debug;
  }

  public void setDebugEnable(boolean debug) {
    this.debug = debug;
  }


  private String format(String message, Object... args) {
    final StringBuilder builder = new StringBuilder();
    if (message != null) {
      final String[] strings = message.split("\\{\\}");
      if (args.length == 0 || strings.length == args.length) {
        for (int i = 0; i < strings.length; i++) {
          builder.append(strings[i]);
          if (args.length > 0) {
            builder.append(args[i]);
          }
        }
      } else if (strings.length == 0 && args.length == 1) {
        builder.append(args[0]);
      } else {
        return "wrong number of placeholders / arguments";
      }
    }
    return builder.toString();
  }

  private static class Log {
    public static Class<?> _class = null;
    public static Method _i = null;
    public static Method _i_tmt = null;
    public static Method _d = null;
    public static Method _d_tmt = null;
    public static Method _w = null;
    public static Method _w_tmt = null;
    public static Method _e = null;
    public static Method _e_tmt = null;

    static {
      try {
        ClassLoader cl = Log.class.getClassLoader();
        _class = cl.loadClass("android.util.Log");
        _i = _class.getDeclaredMethod("i", String.class, String.class);
        _i_tmt = _class.getDeclaredMethod("i", String.class, String.class, Throwable.class);

        _d = _class.getDeclaredMethod("d", String.class, String.class);
        _d_tmt = _class.getDeclaredMethod("d", String.class, String.class, Throwable.class);

        _w = _class.getDeclaredMethod("w", String.class, String.class);
        _w_tmt = _class.getDeclaredMethod("w", String.class, String.class, Throwable.class);

        _e = _class.getDeclaredMethod("e", String.class, String.class);
        _e_tmt = _class.getDeclaredMethod("e", String.class, String.class, Throwable.class);
      } catch (Throwable t) { }
    }

    public static void i(String tag, String msg) {
      try {
        _i.invoke(null, tag, msg);
      } catch (Throwable t) { }
    }

    public static void i(String tag, String msg, Throwable tr) {
      try {
        _i_tmt.invoke(null, tag, msg, tr);
      } catch (Throwable t) { }
    }

    public static void d(String tag, String msg) {
      try {
        _d.invoke(null, tag, msg);
      } catch (Throwable t) { }
    }

    public static void d(String tag, String msg, Throwable tr) {
      try {
        _d_tmt.invoke(null, tag, msg, tr);
      } catch (Throwable t) { }
    }

    public static void w(String tag, String msg) {
      try {
        _w.invoke(null, tag, msg);
      } catch (Throwable t) { }
    }

    public static void w(String tag, String msg, Throwable tr) {
      try {
        _w_tmt.invoke(null, tag, msg, tr);
      } catch (Throwable t) { }
    }

    public static void e(String tag, String msg) {
      try {
        _e.invoke(null, tag, msg);
      } catch (Throwable t) { }
    }

    public static void e(String tag, String msg, Throwable tr) {
      try {
        _e_tmt.invoke(null, tag, msg, tr);
      } catch (Throwable t) { }
    }
  }

}
