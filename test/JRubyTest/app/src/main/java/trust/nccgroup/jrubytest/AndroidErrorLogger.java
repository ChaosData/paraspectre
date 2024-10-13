package trust.nccgroup.jrubytest;

import android.util.Log;

import java.io.PrintStream;

import org.jruby.util.log.Logger;


public class AndroidErrorLogger implements Logger {

  private final String loggerName;
  private boolean debug = false;

  public AndroidErrorLogger(String loggerName) {
    this.loggerName = loggerName;
  }

  public AndroidErrorLogger(String loggerName, PrintStream stream) {
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


}

