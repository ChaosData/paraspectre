package trust.nccgroup.paraspectre.webapp;

import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import org.eclipse.jgit.api.Git;
//import org.eclipse.jgit.api.errors.EmtpyCommitException;
import org.eclipse.jgit.api.errors.GitAPIException;
import trikita.log.Log;
import trust.nccgroup.paraspectre.core.config.Matcher;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.regex.Pattern;

import static trust.nccgroup.paraspectre.webapp.Util.parseId;

class AssemblerServlet extends GenericServlet {

  private static final String TAG = "PS/WebApp/AssemblerServlet";

  private WebAppConfig config;

  //private Git git = null;

  private final Pattern hookpathre = Pattern.compile("^hooks\\/([a-zA-Z_.]+)\\.json$");
  private final Gson decoder = new Gson();
  private final Gson encoder = new GsonBuilder().setPrettyPrinting().create();

  AssemblerServlet(WebAppConfig _config) {
    config = _config;

    //git = Util.getGit(config.edit_root);
    Assembler.setConfig(config);
  }

  private void doPost(HttpServletRequest req, HttpServletResponse res) {
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


    java.util.regex.Matcher m = hookpathre.matcher(id);
    if (!m.matches()) {
      Log.e(TAG, "non hook file path: " + id);
      res.setStatus(403);
      return;
    }

    File f = new File(file_path);
    if (!f.exists()) {
      Log.e(TAG, "path not found: " + file_path);
      res.setStatus(404);
      return;
    }

    String body = "";
    try {
      body = CharStreams.toString(req.getReader());
    } catch (IOException ignored) { }

    Matcher matcher = null;

    try {
      if ("".equals(body)) {
        matcher = decoder.fromJson(new FileReader(f), Matcher.class);
      } else {
        matcher = decoder.fromJson(body, Matcher.class);
      }
    } catch (FileNotFoundException fnfe) {
      Log.e(TAG, "file not found?: " + file_path);
      res.setStatus(500);
      return;
    } catch (JsonIOException jioe) {
      Log.e(TAG, "error reading: " + file_path);
      res.setStatus(500);
      return;
    } catch (JsonSyntaxException jse) {
      Log.e(TAG, "invalid json for Matcher: " + file_path, jse);
      res.setStatus(500);
      return;
    }
    if (matcher == null) {
      Log.e(TAG, "null object for: " + file_path);
      res.setStatus(500);
      return;
    }

    try {
      Validator.hook.validate(matcher);
    } catch (Validator.ValidatorException ve) {
      Log.e(TAG, "invalid hook contents: " + file_path, ve);
      res.setStatus(500);
      try {
        res.getOutputStream().write(ve.reason.getBytes());
      } catch (IOException ioe) {
        Log.e(TAG, "error", ioe);
      }
      return;
    }

    trust.nccgroup.paraspectre.core.config.Matcher meta = null;
    try {
      meta = Assembler.assemble(matcher);
    } catch (Assembler.AssemblerException ae) {
      Log.e(TAG, "failed to assemble hook: " + file_path, ae);
      res.setStatus(500);
      try {
        res.getOutputStream().write(ae.reason.getBytes());
      } catch (IOException ioe) {
        Log.e(TAG, "error", ioe);
      }
      return;
    }

    String meta_text = encoder.toJson(meta);
    String pkg = m.group(1);
    String meta_path = "hooks/" + pkg + "-meta.json";
    String meta_fullpath = config.edit_root + meta_path;

    try {
      File mf = new File(meta_fullpath);
      if (!mf.exists()) {
        if (!mf.createNewFile()) {
          throw new IOException("failed to create file: " + meta_fullpath);
        }
      }
      mf.setReadable(true, false);
      FileWriter mfw = new FileWriter(mf, false);
      mfw.write(meta_text);
      mfw.flush();
      mfw.close();
    } catch (IOException ioe) {
      Log.e(TAG, "failed to write meta hook: " + meta_fullpath, ioe);
      res.setStatus(500);
      return;
    }

    synchronized (config) {
      try {
        config.git.add().setUpdate(false).addFilepattern(meta_path).call();
        config.git.add().setUpdate(true).addFilepattern(meta_path).call();
        config.git.commit()
            //.setAllowEmpty(false)
            .setAuthor(WebAppConfig.GIT_NAME, WebAppConfig.GIT_EMAIL)
            .setMessage("assembled " + meta_path)
            .call();
      /*} catch (EmtpyCommitException e) {
        try {
          config.git.add().setUpdate(false).addFilepattern(meta_path).call();
          config.git.commit()
              .setAllowEmpty(false)
              .setAuthor(WebAppConfig.GIT_NAME, WebAppConfig.GIT_EMAIL)
              .setMessage(("assembled ") + meta_path)
              .call();
        } catch (EmtpyCommitException ee) {
          res.setStatus(200);
          return;
        } catch (GitAPIException gae) {
          Log.e(TAG, "doPost failed", gae);
          res.setStatus(500);
        }*/
      } catch (GitAPIException gae) {
        Log.e(TAG, "doPost failed", gae);
        res.setStatus(500);
      }
      res.setStatus(200);
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
