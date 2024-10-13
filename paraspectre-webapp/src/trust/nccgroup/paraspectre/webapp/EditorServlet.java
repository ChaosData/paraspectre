package trust.nccgroup.paraspectre.webapp;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.gson.Gson;
import org.eclipse.jgit.api.Git;
//import org.eclipse.jgit.api.errors.EmtpyCommitException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import trikita.log.Log;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

class EditorServlet extends GenericServlet {

  private static final String TAG = "PS/WebApp/EditorServlet";

  private WebAppConfig config;

  //private Git git = null;

  EditorServlet(WebAppConfig _config) {
    config = _config;
  }

  private class FileListing {
    String path;
    String type;

    FileListing(String _path, String _type) {
      path = _path;
      type = _type;
    }

    @Override
    public String toString() {
      return type + ": " + path;
    }
  }

  private void doGet(HttpServletRequest req, HttpServletResponse res) {

    String id = Util.parseId(req);
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

    String commit_string = req.getParameter("commit");
    if (commit_string == null) {
      File f = new File(file_path);
      if (!f.exists()) {
        res.setStatus(404);
        return;
      }

      if (f.isFile()) {
        try {
          String data = Files.toString(new File(file_path), Charsets.UTF_8);
          res.setContentType("application/binary");
          int pos = id.lastIndexOf('/');
          res.setHeader("Content-Disposition",
              "attachment; filename=\""
                  + URLEncoder.encode(pos == -1 ? id : id.substring(pos+1), "UTF-8")
                  + "\""
          );
          res.getOutputStream().write(data.getBytes());
          res.setStatus(200);
        } catch (Throwable t) {
          Log.e(TAG, "error reading file", t);
          res.setStatus(500);
        }
      } else if (f.isDirectory()) {
        List<FileListing> lfl = new ArrayList<>();
        synchronized (config) {
          try {
            ObjectId head = config.git.getRepository().resolve("HEAD");
            RevWalk walk = new RevWalk(config.git.getRepository());
            RevCommit head_commit = walk.parseCommit(head);
            RevTree tree = head_commit.getTree();

            TreeWalk treeWalk = new TreeWalk(config.git.getRepository());
            treeWalk.addTree(tree);
            treeWalk.setRecursive(false);

            if (id.equals("")) {
              while (treeWalk.next()) {
                if (treeWalk.isSubtree()) {
                  lfl.add(new FileListing(treeWalk.getPathString(), "dir"));
                  treeWalk.enterSubtree();
                } else {
                  lfl.add(new FileListing(treeWalk.getPathString(), "file"));
                }
              }
            } else {
              while (treeWalk.next()) {
                if (treeWalk.getPathString().equals(id)) {
                  treeWalk.enterSubtree();
                  continue;
                } else if (!treeWalk.getPathString().startsWith(id + "/")) {
                  continue;
                }

                if (treeWalk.isSubtree()) {
                  lfl.add(new FileListing(treeWalk.getPathString(), "dir"));
                  treeWalk.enterSubtree();
                } else {
                  lfl.add(new FileListing(treeWalk.getPathString(), "file"));
                }
              }
            }

            res.setContentType("application/json");
            res.getOutputStream().write(new Gson().toJson(lfl).getBytes());
          } catch (IOException ioe) {
            Log.e(TAG, "doGet failed", ioe);
            res.setStatus(500);
          }
        }

        res.setStatus(200);
      } else {
        res.setStatus(500);
      }
    } else {
      synchronized (config) {
        try {
          ObjectId commitId = config.git.getRepository().resolve(commit_string);
          {
            RevWalk revWalk = new RevWalk(config.git.getRepository());
            RevCommit commit = revWalk.parseCommit(commitId);
            RevTree tree = commit.getTree();


            {
              TreeWalk treeWalk = new TreeWalk(config.git.getRepository());
              treeWalk.addTree(tree);
              treeWalk.setRecursive(true);
              treeWalk.setFilter(PathFilter.create(id));
              if (!treeWalk.next()) {
                res.setStatus(404);
                return;
              }

              if (!id.equals(treeWalk.getPathString())) {
                //it's a directory
                //not currently supporting snapshots in time listing of directories
                res.setStatus(204);
                return;
              }

              FileMode type = treeWalk.getFileMode(0);

              if (!type.equals(FileMode.REGULAR_FILE)
                  && !type.equals(FileMode.EXECUTABLE_FILE)) {
                //not handling symlinks and other weird git internals stuff
                res.setStatus(204);
                return;
              }

              ObjectId objectId = treeWalk.getObjectId(0);
              ObjectLoader loader = config.git.getRepository().open(objectId);

              res.setStatus(200);
              loader.copyTo(res.getOutputStream());
            }

            revWalk.dispose();
          }
        } catch (IOException ioe) {
          Log.e(TAG, "doGet failed", ioe);
          res.setStatus(500);
        }
      }
    }
  }

  private void doDelete(HttpServletRequest req, HttpServletResponse res) {
    String id = Util.parseId(req);
    if (id == null || id.equals("")) {
      res.setStatus(400);
      return;
    }

    String file_path = config.edit_root + id;
    if (!Util.isPathSafe(file_path, config)) {
      res.setStatus(403);
      return;
    }

    File f = new File(file_path);
    if (!f.exists()) {
      res.setStatus(404);
      return;
    }

    synchronized (config) {
      boolean cached = req.getParameter("cached") != null
          && req.getParameter("cached").equals("true");


      try {
        config.git.rm().setCached(cached).addFilepattern(id).call();
        config.git.commit()
            //.setAllowEmpty(false)
            .setAuthor(WebAppConfig.GIT_NAME, WebAppConfig.GIT_EMAIL)
            .setMessage("deleted " + (cached ? "(cached) " : "") + id)
            .call();
      //} catch (EmtpyCommitException e) {
        //pass
      } catch (GitAPIException gae) {
        Log.e(TAG, "goDelete failed", gae);
        res.setStatus(500);
      }
    }
  }

  private void doPut(HttpServletRequest req, HttpServletResponse res) {
    String id = Util.parseId(req);
    if (id == null || id.equals("")) {
      res.setStatus(400);
      return;
    }

    String file_path = config.edit_root + id;
    if (!Util.isPathSafe(file_path, config)) {
      res.setStatus(403);
      return;
    }

    File f = new File(file_path);
    boolean existed = f.exists();

    if (existed && f.isDirectory()) {
      res.setStatus(403);
      return;
    }

    try {
      String body = CharStreams.toString(req.getReader());
      if (!f.exists()) {
        File parent = f.getParentFile();
        Log.e(TAG, "parent: " + parent);
        Log.e(TAG, "file_path: " + file_path);
        if (!parent.exists() && !parent.mkdirs()) {
          Log.e(TAG, "doPut failed: could not create directory path for " + file_path);
          res.setStatus(500);
          return;
        }
      }
      OutputStream o = new FileOutputStream(file_path);
      o.write(body.getBytes());
      o.flush();
      o.close();
    } catch (Throwable t) {
      Log.e(TAG, "doPut failed", t);
      res.setStatus(500);
      return;
    }

    synchronized (config) {
      try {
        config.git.add().setUpdate(false).addFilepattern(id).call();
        config.git.add().setUpdate(true).addFilepattern(id).call();
        config.git.commit()
            //.setAllowEmpty(false)
            .setAuthor(WebAppConfig.GIT_NAME, WebAppConfig.GIT_EMAIL)
            .setMessage("updated " + id)
            .call();
      /*} catch (EmtpyCommitException e) {
        try {
          config.git.add().setUpdate(false).addFilepattern(id).call();
          config.git.commit()
              //.setAllowEmpty(false)
              .setAuthor(WebAppConfig.GIT_NAME, WebAppConfig.GIT_EMAIL)
              .setMessage((existed ? "updated (re-adding) " : "adding ") + id)
              .call();
        } catch (EmtpyCommitException ee) {
          res.setStatus(200);
          return;
        } catch (GitAPIException gae) {
          Log.e(TAG, "doPut failed", gae);
          res.setStatus(500);
        }*/
      } catch (GitAPIException gae) {
        Log.e(TAG, "doPut failed", gae);
        res.setStatus(500);
      }
      res.setStatus(200);
    }
  }

    /*
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
            WebAppConfig c = new Gson().fromJson(body, WebAppConfig.class);
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

    private synchronized void putConfig(String data) {
        try {
            OutputStream o = new FileOutputStream(config.web_root + "/config.json");
            o.write(data.getBytes());
            o.flush();
            o.close();
        } catch (Throwable t) {
            Log.e(TAG, "updateConfig failed", t);
        }
    }
    */

  @Override
  public void service(ServletRequest _req, ServletResponse _res) throws ServletException, IOException {
    HttpServletRequest req = (HttpServletRequest)_req;
    HttpServletResponse res = (HttpServletResponse)_res;

    if (!Util.validateRequest(req, config.api_key)) {
      //Log.e(TAG, "invalid request");
      res.setStatus(403);
      return;
    }

    try {
      switch (req.getMethod()) {
        case "GET": {
          doGet(req, res);
          break;
        }
        case "PUT": {
          doPut(req, res);
          break;
        }
        case "DELETE": {
          doDelete(req, res);
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
