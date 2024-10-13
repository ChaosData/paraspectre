package trust.nccgroup.paraspectre.webapp;

import com.google.gson.Gson;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import trikita.log.Log;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static trust.nccgroup.paraspectre.webapp.Util.parseId;
import trust.nccgroup.paraspectre.core.generated.JsonTemplate;

class TemplateServlet extends GenericServlet {

  private static final String TAG = "PS/WebApp/TemplateServlet";
  private WebAppConfig config;

  TemplateServlet(WebAppConfig _config) {
    config = _config;
  }

  private void doGet(HttpServletRequest req, HttpServletResponse res) {
    try {
      OutputStream os = res.getOutputStream();
      res.setStatus(200);
      os.write(JsonTemplate.HOOK_TEMPLATE_JSON.getBytes());
    } catch (IOException ioe) {
      Log.e(TAG, "error", ioe);
      res.setStatus(500);
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
        case "GET": {
          doGet(req, res);
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
