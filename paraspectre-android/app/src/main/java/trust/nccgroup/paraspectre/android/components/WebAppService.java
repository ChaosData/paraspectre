package trust.nccgroup.paraspectre.android.components;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.common.util.concurrent.SettableFuture;

import org.eclipse.jetty.server.Server;

import java.util.concurrent.ExecutionException;

import trust.nccgroup.paraspectre.android.Configuration;
import trust.nccgroup.paraspectre.android.Constants;
import trust.nccgroup.paraspectre.android.Setup;
import trust.nccgroup.paraspectre.core.Config;
import trust.nccgroup.paraspectre.webapp.WebAppConfig;

public class WebAppService extends Service {
  private static final String TAG = "PS/WebAppService";
  private Server server = null;

  public WebAppService() {

  }

  @Override
  public void onCreate() {

  }

  public class LocalBinder extends Binder {
    public WebAppService getService() {
      return WebAppService.this;
    }
  }

  private IBinder binder = new WebAppService.LocalBinder();

  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }


  @Override
  public void onDestroy() {
    if (server != null) {
      try {
        server.stop();
      } catch (Throwable t) {
        Log.e(TAG, "failed to stop webapp server", t);
      }
    }
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    return Service.START_STICKY;
  }

  public boolean startWebApp() {
    Log.e(TAG, "starting webapp service");

    Setup.web(getApplicationContext());
    final SettableFuture<Boolean> future = SettableFuture.create();


    Thread t = new Thread() {
      @Override
      public void run() {

        try {
          Config runtime_config = Configuration.getRuntimeConfig();
          if (runtime_config == null) {
            Log.e(TAG, "runtime_config == null; cannot start WebApp");
            future.set(false);
            return;
          }

          WebAppConfig c = new WebAppConfig();
          c.address = runtime_config.net.webapp.host;
          c.port = runtime_config.net.webapp.port;
          c.web_root = getFilesDir().getCanonicalPath() + "/web_root/";
          c.edit_root = getFilesDir().getCanonicalPath() + "/edit_root/";

          server = trust.nccgroup.paraspectre.webapp.WebApp.setup(c);
          //server = WebApp.setup(runtime_config.net.webapp.host, runtime_config.net.webapp.port, getApplicationContext());

          if (server == null) {
            Log.e(TAG, "failed to setup webapp");
            future.set(false);
            return;
          }

          Log.i(TAG, "API key: " + c.api_key);
          Intent i = new Intent();
          i.setAction(Constants.pkg + ".API_KEY");
          i.putExtra("api_key", c.api_key);
          LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);

          server.start();
        } catch (Exception e) {
          Log.e(TAG, "error starting webapp", e);
          future.set(false);
          return;
        }
        future.set(true);

        try {
          server.join();
        } catch (InterruptedException ie) {
          //pass
        }
      }
    };
    t.start();

    while (true) {
      try {
        boolean ret = future.get();
        Log.e(TAG, "webapp service started");
        return ret;
      } catch (InterruptedException ignored) {
        //pass
      } catch (ExecutionException e) {
        Log.e(TAG, "error executing webapp thread", e);
        return false;
      }
    }
  }

  public void stopWebApp() {
    Log.e(TAG, "stopping webapp service");

    try {
      server.stop();
    } catch (Exception e) {
      Log.e(TAG, "error stopping webapp", e);
    }

    Log.e(TAG, "webapp service stopped");

  }

}
