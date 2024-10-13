package trust.nccgroup.paraspectre.android.components;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class ConfigService extends Service {

  private static final String TAG = "PS/ConfigService";

  public ConfigService() {
  }

  @Override
  public void onCreate() {

  }

  @Override
  public void onDestroy() {

  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.e(TAG, "starting up");
    setupConfig();

    return Service.START_STICKY_COMPATIBILITY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    throw new UnsupportedOperationException("Nope.");
  }


  /*
  //note: this can cause racy behavior w/ the config object
  private void onConfigUpdated() {
    try {
      InputStream is = new FileInputStream(getApplicationContext().getFilesDir() + "/edit_root/paraspectre.json");
      String config_json_str = CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
      _Hook.config = new Gson().fromJson(config_json_str, Config.class);
    } catch (Throwable t) {
      Log.e(TAG, "failed to get config", t);
    }
  }
  */

  private void setupConfig() {
    try {
      File files_dir = getApplicationContext().getFilesDir();
      if (!files_dir.exists() && files_dir.mkdirs()) {
        Log.e(TAG, "failed to create files directory");
      }

      @SuppressLint("SetWorldReadable")
      boolean r = files_dir.setReadable(true, false);

      if (!r) {
        Log.e(TAG, "FAILED to set files directory world readable.");
      }
      r = files_dir.setExecutable(true, false);
      if (!r) {
        Log.e(TAG, "FAILED to set files directory world executable.");
      }

      File config_file = new File(getApplicationContext().getFilesDir() + "/edit_root/paraspectre.json");
      if (!config_file.exists()) {
        if (!config_file.getParentFile().mkdirs()) {
          Log.e(TAG, "failed to create edit_root directory");
        }
        OutputStream o = new FileOutputStream(config_file);
        byte[] buffer = new byte[4096];
        int length;
        InputStream i = getApplicationContext().getAssets().open("paraspectre.json");
        while ((length = i.read(buffer)) > 0) {
          o.write(buffer, 0, length);
        }
        i.close();
        o.flush();
        o.close();

        @SuppressLint("SetWorldReadable")
        boolean rr = config_file.setReadable(true, false);
        if (!rr) {
          Log.e(TAG, "failed to set paraspectre.json world readable");
        }
      }
    } catch (Throwable t) {
      Log.e(TAG, "setup failed", t);
    }

  }

}
