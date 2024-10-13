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

class HistoryServlet extends GenericServlet {

  private static final String TAG = "PS/WebApp/HistoryServlet";
  private static final String GIT_NAME = "paraspectre-webapp";
  private static final String GIT_EMAIL = "jeff.dileo@paraspectre.local";

  private WebAppConfig config;

  //private Git git = null;

  HistoryServlet(WebAppConfig _config) {
    config = _config;

    //git = Util.getGit(config.edit_root);
  }

  private void doGet(HttpServletRequest req, HttpServletResponse res) {
    //note: responses for paths that were always directories will include
    //      commits for subpaths. this is how `git log -- <path>` normally
    //      works. any subsequent requests for a directory path at a returned
    //      commit via /editor/:path should "fail" w/ a 204

    String id = parseId(req);
    if (id == null) {
      Log.e(TAG, "null id");
      res.setStatus(400);
      return;
    }

    String file_path = config.edit_root + id;
    if (!Util.isPathSafe(file_path, config)) {
      Log.e(TAG, "unsafe path: " + file_path);
      res.setStatus(403);
      return;
    }

    synchronized (config) {
      try {

        List<String> commits = new ArrayList<>();
        for (RevCommit rc : config.git.log().addPath(id).call()) {
          commits.add(rc.toObjectId().getName());
        }
        res.setStatus(200);
        res.getOutputStream().write(new Gson().toJson(commits).getBytes());
      } catch (GitAPIException | IOException gae) {
        Log.e(TAG, "doGet failed", gae);
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
