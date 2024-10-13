package trust.nccgroup.paraspectre.webapp;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import trikita.log.Log;

import javax.servlet.http.HttpServletRequest;

class Util {

  private static final String TAG = "PS/WebApp/Util";

  static byte[] createKey() {
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

  private static boolean constEq(byte[] a, byte[] b) {
    if (a.length != b.length) {
      return false;
    }
    int result = 0;
    for (int i = 0; i < a.length; i++) {
      result |= a[i] ^ b[i];
    }
    return result == 0;
  }

  static boolean validateRequest(HttpServletRequest req, String key) {
    String auth = req.getHeader("Authorization");
    if (auth == null) {
      return false;
    }
    try {
      return constEq(auth.getBytes(), key.getBytes());
    } catch (IllegalArgumentException iae) {
      return false;
    }
  }

  static boolean isPathSafe(String path, WebAppConfig config) {
    try {
      String root = new File(config.edit_root).getCanonicalPath();
      String target = new File(path).getCanonicalPath();
      if (!target.startsWith(root)
          || target.toLowerCase().startsWith(root.toLowerCase() + ".git")) {
        return false;
      }
    } catch (IOException e) {
      return false;
    }
    return true;
  }



  static Git getGit(String path) {
    File f = new File(path);
    if (!f.exists() && !f.mkdirs()) {
      Log.e(TAG, "failed to create git directory: " + f);
      return null;
    }

    Git g;
    File fg = new File(path, ".git");
    if (fg.exists()) {
      //FileRepositoryBuilder builder = new FileRepositoryBuilder();
      try {
        g = Git.init().setDirectory(new File(path)).call();
//        return new Git(builder.setGitDir(new File(path))
//            .readEnvironment() // scan environment GIT_* variables
//            .findGitDir() // scan up the file system tree
//            .build());
//      } catch (IOException e) {
//        Log.e(TAG, "failed to load git repo", e);
//        return null;
      } catch (GitAPIException e) {
        Log.e(TAG, "failed to load git repo", e);
        return null;
      }
    } else {
      Repository repository;
      try {
        repository = FileRepositoryBuilder.create(new File(path, ".git"));
        repository.create();
        g = new Git(repository);
      } catch (IOException ioe) {
        Log.e(TAG, "failed to create git repo", ioe);
        return null;
      }
    }

    //note: this is to put the repo in a consistent state
    File gi = new File(path,".gitignore");
    if (!gi.exists()) {
      try {
        if (!gi.createNewFile()) {
          Log.e(TAG, "failed to create .gitignore");
        }
      } catch (IOException ioe) {
        //pass
        Log.e(TAG, "error creating .gitignore");
      }
    }

    try {
      g.add().setUpdate(false).addFilepattern(".gitignore").call();
      //note: to add any existing stuff in addition to .gitignore
      g.add().setUpdate(false).addFilepattern(".").call();
      g.add().setUpdate(true).addFilepattern(".").call();
      g.commit()
        //.setAllowEmpty(false)
        .setAuthor(WebAppConfig.GIT_NAME, WebAppConfig.GIT_EMAIL)
        .setMessage("adding .gitignore and/or any other existing files")
        .call();
    } catch (GitAPIException gae) {
        Log.e(TAG, "problem when initing repo", gae);
        return null;
    }
    return g;
  }

  static String parseId(HttpServletRequest req) {
    //String path = req.getPathInfo(); //decodes the entire non-context URI, which is stupid

    String path = req.getRequestURI().substring(req.getServletPath().length());
    String[] segments = path.split("/");
    if (segments.length == 0) {
      return "";
    } else if (segments.length != 2) {
      return null;
    }
    try {
      return URLDecoder.decode(segments[1], "UTF-8").replace("*", "\\*"); //lol git globs
    } catch (UnsupportedEncodingException e) {
      return null;
    }
  }

  public static void delete(File fd) {
    if (!fd.exists()) {
      return;
    }

    if (!fd.isDirectory()) {
      if (!fd.delete()) {
        Log.e(TAG, "failed to delete file: " + fd);
      }
      return;
    }

    File[] fds = fd.listFiles();
    if (fds != null) {
      for (File ifd : fds) {
        delete(ifd);
      }
    } else {
      Log.e(TAG, "failed to list files under: " + fd);
    }
    if (!fd.delete()) {
      Log.e(TAG, "failed to delete directory: " + fd);
    }
  }

  static List<String> getResourceDirListing(String path) {
    List<String> ret = new ArrayList<>();
    try {
      JarFile jar;
      Enumeration<URL> en = WebApp.class.getClassLoader().getResources(path);
      URL url;
      if (en.hasMoreElements()) {
        url = en.nextElement();

        JarURLConnection urlcon = (JarURLConnection) (url.openConnection());
        jar = urlcon.getJarFile();
      } else {
        //note: getResources() doesn't work on Android
        String jar_path;
        if (new File("/data/app/trust.nccgroup.paraspectre.android-1/base.apk").exists()) {
          jar_path = "/data/app/trust.nccgroup.paraspectre.android-1/base.apk";
        } else {
          jar_path = "/data/app/trust.nccgroup.paraspectre.android-2/base.apk";
        }

        //note: also, for some reason, opening an app's own APK file via URL
        //      results in the internal handler for the APK (used for class
        //      resource loading) being closed permanently, therefore breaking
        //      future getResource() calls that would otherwise work.
        jar = new JarFile(jar_path);
      }

      Enumeration<JarEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        String entry = entries.nextElement().getName();
        if (entry.startsWith(path) && !entry.equals(path)) {
          ret.add(entry);
        }
      }
    } catch (IOException ioe) {
      Log.e(TAG, "failed to load resource dir listing", ioe);
    }

    return ret;
  }

}
