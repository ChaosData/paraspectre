package trust.nccgroup.paraspectre.android;


import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.gson.Gson;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import trust.nccgroup.paraspectre.core.Config;

@Deprecated
public class WebApp {

  private static final String TAG = "PS/WebApp";


  private static final byte[] key = createKey();
  private static android.content.Context appctx = null;

  public static Server setup(String _bindaddr, int _bindport, android.content.Context _appctx) {

    appctx = _appctx;

    Server server = new Server(new InetSocketAddress(_bindaddr, _bindport));

    ServletContextHandler context = new ServletContextHandler(
        ServletContextHandler.NO_SESSIONS
    );

    context.setContextPath("/");
    server.setHandler(context);


    ServletHolder appServlet = context.addServlet(ConfigServlet.class, "/config");
    appServlet.setInitOrder(1);
    ServletHolder staticServlet = context.addServlet(DlServlet.class, "/");

    String api_key = Base64.encodeToString(key, Base64.NO_WRAP);
    Log.i(TAG, "API key: " + api_key);

    Intent i = new Intent();
    i.setAction(Constants.pkg + ".API_KEY");
    i.putExtra("api_key", api_key);
    LocalBroadcastManager.getInstance(appctx).sendBroadcast(i);

    return server;

  }

  //org.eclipse.jetty.servlet.DefaultServlet uses java.io.File.forPath for larger files or something
  //android is missing that method
  public static class DlServlet extends GenericServlet {

    private static final String TAG = "PS/WebApp/DlServlet";

    private static final Map<String, String> content_types = Maps.newHashMap(ImmutableMap.of(
        ".js", "text/javascript",
        ".css", "text/css",
        ".html", "text/html"
    ));

    private static SimpleDateFormat getLastModifiedFormat() {
      SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
      sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
      return sdf;
    }

    private static final SimpleDateFormat last_modified = getLastModifiedFormat();

    @Override
    public void service(ServletRequest _req, ServletResponse _res) throws ServletException, IOException {
      HttpServletRequest req = (HttpServletRequest)_req;
      HttpServletResponse res = (HttpServletResponse)_res;

      if (!req.getMethod().equals("GET")) {
        res.setStatus(404);
        return;
      }

      String uri = URLDecoder.decode(req.getRequestURI(), "UTF-8");
      if (uri.endsWith("/")) {
        uri += "index.html";
      }

      String basepath = appctx.getFilesDir().getCanonicalPath() + "/webapp/";
      File f = new File(basepath + uri);


      if (!f.getCanonicalPath().startsWith(basepath)) {
        res.setStatus(403);
        return;
      }

      if (!f.exists()) {
        res.setStatus(404);
        return;
      }


      long moddate = f.lastModified();
      String httpDate = last_modified.format(new Date(moddate));

      String ifmodifiedsince = req.getHeader("if-modified-since");

      String ct = content_types.get(uri.substring(uri.lastIndexOf('.')));
      if (ct != null) {
        res.setContentType(ct);
      } else {
        res.setContentType("application/binary");
      }
      res.setHeader("Content-Length", "" + f.length()); //setContentLength takes an int, not a long
      res.setHeader("Last-Modified", httpDate);

      if (ifmodifiedsince != null) {
        try {
          long ifmoddate = last_modified.parse(ifmodifiedsince).getTime();
          if (moddate <= ifmoddate) {
            res.setStatus(304);
            return;
          }

        } catch (Throwable t) {
          Log.e(TAG, "weird if-modified-since: " + ifmodifiedsince);
          return;
        }
      }

      res.setStatus(200);
      OutputStream out = res.getOutputStream();
      FileInputStream in = new FileInputStream(f);
      byte[] buffer = new byte[4096];
      int length;
      while ((length = in.read(buffer)) > 0){
        out.write(buffer, 0, length);
      }
      in.close();
      out.flush();
    }
  }

  public static class ConfigServlet extends GenericServlet {

    private static final String TAG = "PS/WebApp/ConfigServlet";

    private static boolean validateRequest(HttpServletRequest req) {
      String auth = req.getHeader("Authorization");
      if (auth == null) {
        return false;
      }
      return constEq(Base64.decode(auth, Base64.DEFAULT), key);
    }

    public static boolean constEq(byte[] a, byte[] b) {
      if (a.length != b.length) {
        return false;
      }
      int result = 0;
      for (int i = 0; i < a.length; i++) {
        result |= a[i] ^ b[i];
      }
      return result == 0;
    }

    private void doGet(HttpServletRequest req, HttpServletResponse res) throws Throwable {
      if (!validateRequest(req)) {
        res.setStatus(403);
        return;
      }

      String data = getConfig();
      if (data == null) {
        res.setStatus(500);
      } else {
        res.setContentType("application/json");
        res.getOutputStream().write(data.getBytes());
        res.setStatus(200);
      }

    }

    private static String getConfig() {
      try {
        File fd = appctx.getFilesDir();
        if (!fd.exists()) {
          boolean r = fd.mkdir();
          if (!r) {
            Log.e(TAG, "FAILED to create /files directory.");
          }
        }
        return Files.toString(new File(appctx.getFilesDir().getPath() + "/paraspectre.json"), Charsets.UTF_8);
      } catch (Throwable t) {
        Log.e(TAG, "getConfig failed", t);
        return null;
      }
    }

    private void doPut(HttpServletRequest req, HttpServletResponse res) throws Throwable {
      if (!validateRequest(req)) {
        res.setStatus(403);
        return;
      }

      String type = req.getContentType();
      if (type == null || !type.startsWith("application/json")) {
        res.setStatus(406);
        return;
      }

      String body = CharStreams.toString(req.getReader());

      try {
        Config c = new Gson().fromJson(body, Config.class);
        if (c == null) {
          Log.e(TAG, "json parsed as null");
          res.setStatus(500);
          return;
        }
      } catch (Throwable t) {
        Log.e(TAG, "json invalid", t);
        res.setStatus(500);
        return;
      }

      putConfig(body);
      res.setStatus(200);
    }

    private static synchronized void putConfig(String data) {
      try {
        File fd = appctx.getFilesDir();
        if (!fd.exists()) {
          boolean r = fd.mkdir();
          if (!r) {
            Log.e(TAG, "FAILED to create /files directory.");
          }
        }

        OutputStream o = new FileOutputStream(appctx.getFilesDir().getPath() + "/paraspectre.json");
        o.write(data.getBytes());
        o.flush();
        o.close();
      } catch (Throwable t) {
        Log.e(TAG, "updateConfig failed", t);
      }
    }

    @Override
    public void service(ServletRequest _req, ServletResponse _res) throws ServletException, IOException {
      HttpServletRequest req = (HttpServletRequest)_req;
      HttpServletResponse res = (HttpServletResponse)_res;

      try {
        if (req.getMethod().equals("GET")) {
          doGet(req, res);
        } else if (req.getMethod().equals("PUT")) {
          doPut(req, res);
        } else {
          res.setStatus(405);
        }
      } catch (Throwable t) {
        Log.e(TAG, "error handling request", t);
        res.setStatus(500);
      }

    }
  }

  private static byte[] createKey() {
    try {
      byte[] ret = new byte[16];
      RandomAccessFile raf = new RandomAccessFile("/dev/urandom", "r");
      raf.readFully(ret);
      return ret;
    } catch (Throwable t) {
      Log.e(TAG, "error reading /dev/urandom", t);
      return null;
    }
  }


}