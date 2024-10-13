package trust.nccgroup.paraspectre.webapp;

import org.eclipse.jetty.server.Server;
import trikita.log.Log;

public class Main {

  private static final String TAG = "PS/Main";

  public static void main(String[] argv) {
    Main m = new Main();
    m.real_main(argv);
  }

  private void real_main(String[] argv) {
    if (argv.length < 2) {
      System.err.println("usage: java path/to/xxx-all.jar <path/to/web_root> <path/to/edit_root> [delete_web]");
      return;
    }

    WebAppConfig c = new WebAppConfig();
    c.address = "127.0.0.1";
    c.port = 8088;
    c.web_root = argv[0] + "/";
    c.edit_root = argv[1] + "/";
    if (argv.length == 3) {
      c.delete_existing_web_root = Boolean.parseBoolean(argv[2]);
    }
    Server s = WebApp.setup(c);

    Log.i(TAG, "API key: " + c.api_key);

    if (s == null) {
      return;
    }

    try {
      s.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
