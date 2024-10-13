package trust.nccgroup.paraspectre.webapp;

import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import trikita.log.Log;
import trust.nccgroup.paraspectre.core.Config;
import trust.nccgroup.paraspectre.core.config.Matcher;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

import static trust.nccgroup.paraspectre.webapp.Util.parseId;

class ValidatorServlet extends GenericServlet {

  private static final String TAG = "PS/WebApp/ValidatorServlet";

  private WebAppConfig config;

  private final Gson decoder = new Gson();

  private final Pattern hookpathre = Pattern.compile("^hooks\\/([a-zA-Z_.]+)\\.json$");
  private final Pattern metapathre = Pattern.compile("^hooks\\/[a-zA-Z_.]+-meta\\.json$");

  ValidatorServlet(WebAppConfig _config) {
    config = _config;
  }

  private void doPost(HttpServletRequest req, HttpServletResponse res) {
    String id = parseId(req);
    if (id == null) {
      Log.e(TAG, "null id");
      res.setStatus(400);
      return;
    }

    java.util.regex.Matcher hookpathmatch = hookpathre.matcher(id);
    java.util.regex.Matcher metapathmatch = metapathre.matcher(id);

    boolean config = "paraspectre.json".equals(id);
    boolean hook = hookpathmatch.matches();
    boolean meta = metapathmatch.matches();

    if (!config && !hook && !meta) {
      Log.e(TAG, "non config/hook/meta file path: " + id);
      res.setStatus(403);
      return;
    }

    String body = null;
    try {
      body = CharStreams.toString(req.getReader());
    } catch (IOException ioe) {
      Log.e(TAG, "no body supplied: " + id);
      res.setStatus(500);
      return;
    }

    if (config) {
      Config c = null;
      try {
        c = decoder.fromJson(body, Config.class);
      } catch (JsonSyntaxException jse) {
        Log.e(TAG, "invalid json for WebAppConfig");
        res.setStatus(500);
        return;
      }

      if (c == null) {
        Log.e(TAG, "null object");
        res.setStatus(500);
        return;
      }

      try {
        Validator.config.validate(c);
      } catch (Validator.ValidatorException ve) {
        Log.e(TAG, "invalid config contents for " + id, ve);
        res.setStatus(500);
        try {
          res.getOutputStream().write(ve.reason.getBytes());
        } catch (IOException ioe) {
          Log.e(TAG, "error", ioe);
        }
        return;
      }

    } else {
      Matcher matcher = null;
      try {
        matcher = decoder.fromJson(body, Matcher.class);
      } catch (JsonSyntaxException jse) {
        Log.e(TAG, "invalid json for Matcher");
        res.setStatus(500);
        return;
      }

      if (matcher == null) {
        Log.e(TAG, "null object");
        res.setStatus(500);
        return;
      }


      if (hook) {
        try {
          Validator.hook.validate(matcher);
        } catch (Validator.ValidatorException ve) {
          Log.e(TAG, "invalid hook contents for " + id, ve);
          res.setStatus(500);
          try {
            res.getOutputStream().write(ve.reason.getBytes());
          } catch (IOException ioe) {
            Log.e(TAG, "error", ioe);
          }
          return;
        }
      } else if (meta) {
        try {
          Validator.meta.validate(matcher);
        } catch (Validator.ValidatorException ve) {
          Log.e(TAG, "invalid meta contents for " + id);
          res.setStatus(500);
          try {
            res.getOutputStream().write(ve.reason.getBytes());
          } catch (IOException ioe) {
            Log.e(TAG, "error", ioe);
          }
          return;
        }
      } else {
        Log.e(TAG, "invalid state");
        res.setStatus(500);
      }
    }
  }



  @Override
  public void service(ServletRequest _req, ServletResponse _res) throws ServletException, IOException {
    HttpServletRequest req = (HttpServletRequest)_req;
    HttpServletResponse res = (HttpServletResponse)_res;

    if (!Util.validateRequest(req, config.api_key)) {
      Log.e(TAG, "invalid request");
      res.setStatus(403);
      return;
    }

    try {
      switch (req.getMethod()) {
        case "POST": {
          doPost(req, res);
          break;
        }
        default: {
          res.setStatus(405);
          break;
        }
      }
    } catch (Throwable t) {
      Log.e(TAG, "error handling request", t);
      res.setStatus(500);
    }

  }
}
