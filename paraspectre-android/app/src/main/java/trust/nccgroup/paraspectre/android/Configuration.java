package trust.nccgroup.paraspectre.android;

import android.util.Log;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import trust.nccgroup.paraspectre.core.Config;
import trust.nccgroup.paraspectre.core.config.Matcher;

public class Configuration {

  private static final String TAG = "PS/Configuration";
  public static Config asset_config = null;

  private static final Set<String> problem_pkgs = new HashSet<>(Arrays.asList(new String[] {
      "android",
      "com.android.providers.settings",
      "com.android.server.telecom"
  }));

  public static Config getAssetConfig() {
    Config config = null;
    InputStream is = null;
    try {
      //note: caching is evil
      is = Configuration.class.getResource("/assets/paraspectre.json").openStream();
      String config_str = CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
      config = new Gson().fromJson(config_str, Config.class);
    } catch (Throwable t) {
      Log.e(TAG, "failed to get config", t);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException ioe) {
          Log.e(TAG, "failed to close", ioe);
        }
      }
    }
    return config;
  }

  //note: This cannot be run from initZygote due to it being too early in boot to access the file
  public static Config getRuntimeConfig() {
    File config_file = new File("/data" + "/data/" + Constants.pkg + "/files/edit_root/paraspectre.json");
    try {
      if (!config_file.exists()) {
        Log.e(TAG, "getRuntimeConfig: " + config_file.getPath() + " does not exist");
        return null;
      }

      String config_str = Files.toString(config_file, Charsets.UTF_8);
      try {
        Config c = new Gson().fromJson(config_str, Config.class);
        if (c != null) {
          return c;
        } else {
          Log.e(TAG, "getRuntimeConfig failed: null config");
        }
      } catch (Throwable t) {
        Log.e(TAG, "getRuntimeConfig failed: invalid config");
      }
    } catch (Throwable t) {
      Log.w(TAG, "getRuntimeConfig: cannot access " + config_file.getPath(), t);
    }
    return null;
  }


  public static Config getFileConfig(File config_file, String pkg) {
    try {
      if (!config_file.exists()) {
        return null;
      }

      String config_str = Files.toString(config_file, Charsets.UTF_8);
      try {
        Config c = new Gson().fromJson(config_str, Config.class);
        if (c != null) {
          return c;
        } else {
          Log.e(TAG, "getFileConfig failed: null config");
        }
      } catch (Throwable t) {
        Log.e(TAG, "getFileConfig failed: invalid config");
      }
    } catch (Throwable t) {
      if (problem_pkgs.contains(pkg)) {
        Log.w(TAG, "pkg:" + pkg + " cannot access " + config_file.getPath());
      } else {
        Log.e(TAG, "getFileConfig failed for pkg: " + pkg, t);
      }
    }
    return null;
  }

  public static Matcher getFileMatcher(File matcher_file, String pkg) {
    try {
      if (!matcher_file.exists()) {
        return null;
      }

      String config_str = Files.toString(matcher_file, Charsets.UTF_8);
      try {
        Matcher m  = new Gson().fromJson(config_str, Matcher.class);
        if (m != null) {
          return m;
        } else {
          Log.e(TAG, "getFileMatcher failed: null matcher");
        }
      } catch (Throwable t) {
        Log.e(TAG, "getFileMatcher failed: invalid matcher");
      }
    } catch (Throwable t) {
      if (problem_pkgs.contains(pkg)) {
        Log.w(TAG, "pkg:" + pkg + " cannot access " + matcher_file.getPath());
      } else {
        Log.e(TAG, "getFileMatcher failed for pkg: " + pkg, t);
      }
    }
    return null;
  }


  /*
  public static Config get(String pkg) {
    return get(pkg, false, false);
  }

  public static Config get(String pkg, boolean ps_file_fallback) {
    return get(pkg, ps_file_fallback, false);
  }
  */

  public static Matcher getMatcher(String pkg) {
    Matcher m = null;
    File matcher_file = new File("/data" + "/data/" + Constants.pkg + "/files/edit_root/hooks/" + pkg + "-meta.json");
    if (matcher_file.exists()) {
      m = getFileMatcher(matcher_file, pkg);
      if (m != null) {
        m.pkg = pkg;
      }
    }
    return m;
  }

  public static Config getConfig(String pkg, boolean asset_fallback) {
    Config c;
    File config_file = new File("/data" + "/data/" + Constants.pkg + "/files/edit_root/paraspectre.json");
    c = getFileConfig(config_file, pkg);

    if (c == null && asset_fallback) {
      c = asset_config;
    }

    return c;
  }

}
