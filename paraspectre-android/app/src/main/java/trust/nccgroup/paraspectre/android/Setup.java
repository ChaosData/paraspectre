package trust.nccgroup.paraspectre.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import dalvik.system.PathClassLoader;

public class Setup {

  private static final String TAG = "PS/Setup";

  public static void web(Context ctx){
    //Log.d(TAG, "web");
    //byte[] buffer = new byte[4096];

    try{
      File files_dir = ctx.getFilesDir();
      if (!files_dir.exists() && !files_dir.mkdirs()) {
        Log.e(TAG, "failed to create files directory: " + files_dir);
      } else if (!files_dir.isDirectory()) {
        //todo: unify utils into core
        if (!files_dir.delete()) {
          Log.e(TAG, "failed to delete non-directory files directory " +
                     "(if this ever happens, i blame andy rubin)");
        }
        if (!files_dir.mkdirs()) {
          Log.e(TAG, "failed to recreate files directory");
        }
      }

    } catch(Throwable t){
      Log.e(TAG, "web setup failed", t);
    }
  }

  private static void publishAssets(String asset_file, String target_file) throws IOException {
    OutputStream o = new FileOutputStream(target_file);
    byte[] buffer = new byte[4096];
    int length;
    InputStream i = PackageHook.class.getResourceAsStream("/assets/" + asset_file);
    while ((length = i.read(buffer)) > 0) {
      o.write(buffer, 0, length);
    }
    i.close();
    o.flush();
    o.close();
  }

  //take assets from PS apk and copy them into /files dir of hooked app
  public static void publishAssets(String fname, Context hooked_ctx) {
    String target_file;

    try {
      target_file =  hooked_ctx.getFilesDir().getPath() + "/" + fname;

      File fd = new File(target_file);
      if (fd.exists()) {
        @SuppressLint("SetWorldReadable")
        boolean r = fd.setReadable(true, false);
        if (!r) {
          Log.e(TAG, "FAILED to set " + fname + " world readable.");
        }
        return;
      }

      fd = hooked_ctx.getFilesDir();
      if (!fd.exists()) {
        boolean r = fd.mkdir();
        if (!r) {
          Log.e(TAG, "FAILED to create /files directory.");
          return;
        }
      }

      publishAssets(fname, target_file);
    } catch (Throwable t) {
      Log.e(TAG, "publishAssets failed", t);
      return;
    }
    File fd = new File(target_file);

    @SuppressLint("SetWorldReadable")
    boolean r = fd.setReadable(true, false);

    if (!r) {
      Log.e(TAG, "FAILED to set " + target_file + " world readable.");
    }
  }


  public static ClassLoader loadRemoteJar(Context hooked_ctx) {
    File privateDirectory = hooked_ctx.getCodeCacheDir();
    if (!privateDirectory.exists() && !privateDirectory.mkdirs()) {
      Log.e(TAG, "failed to create code cache dir");
    }

    return new PathClassLoader(
        getApkName(hooked_ctx) + ":" + hooked_ctx.getFilesDir() + "/runtime.dex.jar",
        "/vendor/lib:/system/lib",
        hooked_ctx.getClassLoader()
    );
  }

  private static String getApkName(Context ctx) {
    String packageName = ctx.getPackageName();
    PackageManager pm = ctx.getPackageManager();
    try {
      ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
      return ai.publicSourceDir;
    } catch (Throwable t) {
      Log.e(TAG, "Could not get APK path, using default.", t);
    }

    for (int i = 1; i < 4; i++) {
      String path = "/data/app/" + Constants.pkg + "-" + i + "/base.apk";
      if (new File(path).exists()) {
        return path;
      }
    }

    return null;
  }
}
