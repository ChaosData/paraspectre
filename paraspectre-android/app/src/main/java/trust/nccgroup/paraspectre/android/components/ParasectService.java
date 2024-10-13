package trust.nccgroup.paraspectre.android.components;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import trust.nccgroup.paraspectre.android.Configuration;
import trust.nccgroup.paraspectre.android.proxy.PingerProxyTcp;
import trust.nccgroup.paraspectre.android.proxy.ProxyContext;
import trust.nccgroup.paraspectre.android.proxy.TcpForwardProxy;
import trust.nccgroup.paraspectre.core.Config;


public class ParasectService extends Service {
  private static final String TAG = "PS/ParasectService";

  private final ParasectService self = this;

  private PingerProxyTcp pingerProxy;
  private TcpForwardProxy forwardProxy;

  private boolean running = false;

  public ParasectService() {

  }

  public class LocalBinder extends Binder {
    public ParasectService getService() {
      return ParasectService.this;
    }
  }

  private IBinder binder = new LocalBinder();

  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }


  @Override
  public void onCreate() {

  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    return Service.START_STICKY;
  }

  @Override
  public void onDestroy() {
    stopParasect();
  }

  public boolean startParasect() {

    Log.e(TAG, "starting parasect proxy");

    try {
      Config runtime_config = Configuration.getRuntimeConfig();
      if (runtime_config == null) {
        Log.e(TAG, "runtime_config == null; cannot start parasect proxy");
        return false;
      }

      final ConcurrentMap<String, ProxyContext> keymap = new ConcurrentHashMap<>();

      pingerProxy = new PingerProxyTcp(runtime_config, self, keymap);
      forwardProxy = new TcpForwardProxy(runtime_config, keymap);
      //drubyProxy = new UnixToTcpProxy(runtime_config, self);

      new Thread() {
        @Override
        public void run() {
          pingerProxy.run();
        }
      }.start();

      if (!forwardProxy.setup()) {
        pingerProxy.stop();
        return false;
      }

      new Thread() {
        @Override
        public void run() {
          forwardProxy.run();
        }
      }.start();

    } catch (Throwable t) {
      Log.e(TAG, "failed to start parasect proxy", t);
      return false;
    }

    Log.e(TAG, "parasect proxy started");
    running = true;
    return true;
  }

  public boolean isRunning() {
    return running;
  }

  public void stopParasect() {
    Log.e(TAG, "stopping parasect proxy");
    if (pingerProxy != null) {
      pingerProxy.stop();
    }

    if (forwardProxy != null) {
      forwardProxy.stop();
    }
    Log.e(TAG, "parasect proxy stopped");
    running = false;
  }

}
