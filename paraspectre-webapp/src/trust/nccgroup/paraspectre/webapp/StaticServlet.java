package trust.nccgroup.paraspectre.webapp;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import trikita.log.Log;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

//org.eclipse.jetty.servlet.DefaultServlet uses java.io.File.forPath for larger files or something
//android is missing that method
class StaticServlet extends GenericServlet {

  private static final String TAG = "PS/WebApp/StaticServlet";

  private static final Map<String, String> content_types = Maps.newHashMap(ImmutableMap.of(
      ".js", "text/javascript",
      ".css", "text/css",
      ".html", "text/html"
  ));

  private WebAppConfig config;

  StaticServlet(WebAppConfig _config) {
    config = _config;
  }

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

    File f = new File(config.web_root + uri);

    if (!f.getCanonicalPath().startsWith(new File(config.web_root).getCanonicalPath())) {
      System.out.println(f.getCanonicalFile());
      res.setStatus(403);
      return;
    }

    if (!f.exists()) {
      //res.setStatus(404);
      f = new File(config.web_root + "/index.html");
      //return;
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
    res.setHeader("X-Frame-Options", "DENY");
    res.setHeader("X-Content-Type-Options", "nosniff");

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
