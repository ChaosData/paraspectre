package trust.nccgroup.paraspectre.webapp;


import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import trikita.log.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

@SuppressWarnings("WeakerAccess") //used by paraspectre-android
public class WebApp {

  private static final String TAG = "PS/WebApp";

  public static Server setup(WebAppConfig _config) {

    File web_root = new File(_config.web_root);
    if (!web_root.exists() && !web_root.mkdirs()) {
      Log.e(TAG, "failed to create web_root directory: " + web_root);
      return null;
    } else {
      if (_config.delete_existing_web_root) {
        Util.delete(web_root);
        if (!web_root.mkdirs()) {
          Log.e(TAG, "failed to recreate web_root directory: " + web_root);
          return null;
        }
      }
    }

    File hooks_dir = new File(_config.edit_root + "hooks");
    if (!hooks_dir.exists() && !hooks_dir.mkdirs()) {
      Log.e(TAG, "failed to create edit_root/hooks directory: " + hooks_dir);
    } else if (!hooks_dir.isDirectory()) {
      Util.delete(hooks_dir);
      if (!hooks_dir.mkdirs()) {
        Log.e(TAG, "failed to recreate edit_root/hooks directory: " + hooks_dir);
        return null;
      }

    }

    //not going to work in Android 7/N+
    if (!hooks_dir.setReadable(true, false)) {
      Log.e(TAG, "failed to make edit_root/hooks directory world readable: " + hooks_dir);
      return null;
    }
    if (!hooks_dir.setExecutable(true, false)) {
      Log.e(TAG, "failed to make edit_root/hooks directory world executable: " + hooks_dir);
      return null;
    }

    File hooks_dir_gitignore = new File(_config.edit_root + "hooks/.gitignore");
    if (!hooks_dir_gitignore.exists()) {
      try {
        if (!hooks_dir_gitignore.createNewFile()) {
          Log.e(TAG, "failed to create: " + hooks_dir_gitignore);
          return null;
        }
      } catch (IOException ioe) {
        Log.e(TAG, "error creating file", ioe);
        return null;
      }
    } else if (!hooks_dir_gitignore.isFile()) {
      Util.delete(hooks_dir_gitignore);
      try {
        if (!hooks_dir_gitignore.createNewFile()) {
          Log.e(TAG, "failed to recreate: " + hooks_dir_gitignore);
          return null;
        }
      } catch (IOException ioe) {
        Log.e(TAG, "error", ioe);
        return null;
      }
    }

    _config.git = Util.getGit(_config.edit_root);

    if (_config.delete_existing_web_root) {
      List<String> web_root_filepaths = Util.getResourceDirListing("web_root/");
      /*List<String> web_root_filepaths = Lists.newArrayList("web_root/ace.js", "web_root/index.html",
          "web_root/mode-plain_text.js", "web_root/theme-monokai.js",
          "web_root/ext-searchbox.js", "web_root/mode-json.js",
          "web_root/mode-ruby.js", "web_root/worker-json.js");
      */

      for (String path : web_root_filepaths) {
        try {
          String inner = path.substring(path.indexOf("/"));
          File f = new File(web_root.getCanonicalPath() + inner);
          if (!f.getParentFile().exists() && !f.getParentFile().mkdirs()) {
            Log.e(TAG, "failed to create directory: " + f.getParentFile());
            continue;
          }
          if (!f.createNewFile()) {
            Log.e(TAG, "failed to create file: " + f);
            continue;
          }
          FileOutputStream fos = new FileOutputStream(f);

          //InputStream is = WebApp.class.getResource("/" + path).openStream();
          //String data = CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));

          fos.write(
            //ByteStreams.toByteArray(WebApp.class.getClassLoader().getResource(path).openStream())
            ByteStreams.toByteArray(WebApp.class.getResource("/" + path).openStream())
            //data.getBytes()

          );
          fos.flush();
          fos.close();
        } catch (FileNotFoundException fnfe) {
          Log.e(TAG, "failed to find target file: " + path, fnfe);
          return null;
        } catch (IOException ioe) {
          Log.e(TAG, "failed to load resource file: " + path, ioe);
          return null;
        }
      }
    }

    File edit_root = new File(_config.edit_root);
    if (!edit_root.exists() && !edit_root.mkdir()) {
      Log.e(TAG, "failed to create directory: " + edit_root);
      return null;
    } else if (!edit_root.isDirectory()) {
      Util.delete(edit_root);
      if (!edit_root.mkdirs()) {
        Log.e(TAG, "failed to recreate directory: " + edit_root);
      }
    }

    //not going to work in Android 7/N+
    if (!edit_root.setReadable(true, false)) {
      Log.e(TAG, "failed to make directory world readable: " + edit_root);
      return null;
    }
    if (!edit_root.setExecutable(true, false)) {
      Log.e(TAG, "failed to make directory world executable: " + edit_root);
      return null;
    }


    System.setProperty("org.eclipse.jetty.util.log.class", "trust.nccgroup.paraspectre.webapp.NullLogger");

    Server server = new Server(new InetSocketAddress(_config.address , _config.port));

    ServletContextHandler context = new ServletContextHandler(
        ServletContextHandler.NO_SESSIONS
    );

    context.setContextPath("/");
    server.setHandler(context);

    ServletHolder templateServlet = new ServletHolder(new TemplateServlet(_config));
    context.addServlet(templateServlet, "/template");

    ServletHolder editorServlet = new ServletHolder(new EditorServlet(_config));
    context.addServlet(editorServlet, "/editor/*");

    ServletHolder historyServlet = new ServletHolder(new HistoryServlet(_config));
    context.addServlet(historyServlet, "/history/*");

    ServletHolder validatorServlet = new ServletHolder(new ValidatorServlet(_config));
    context.addServlet(validatorServlet, "/validate/*");

    ServletHolder assmblerServlet = new ServletHolder(new AssemblerServlet(_config));
    context.addServlet(assmblerServlet, "/assemble/*");

    ServletHolder staticServlet = new ServletHolder(new StaticServlet(_config));
    context.addServlet(staticServlet, "/");

    byte[] key = Util.createKey();
    if (key == null) {
      Log.e(TAG, "failed to create key");
      return null;
    }
    _config.api_key = BaseEncoding.base64().encode(key).replace("=", "");

    return server;
  }




}
