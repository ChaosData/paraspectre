package trust.nccgroup.paraspectre.android;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.util.Log;

class NetGrantClient {

  private String pkg = null;

  NetGrantClient(String pkg) {
    this.pkg = pkg;
  }

  static boolean hasInternet(Context ctx) {
    switch (ctx.checkPermission(Manifest.permission.INTERNET, android.os.Process.myPid(), Process.myUid())) {
      case PackageManager.PERMISSION_GRANTED: {
        return true;
      }
      case PackageManager.PERMISSION_DENIED: {
        return false;
      }
      default: {
        return false;
      }
    }
  }

  private static class ResponseHandler extends Handler {

    private final String TAG;
    Object lock = null;
    Context ctx = null;
    ServiceConnection conn = null;

    ResponseHandler(String pkg, Object lock, Context ctx, ServiceConnection conn) {
      this.TAG = "PS/NGCH/" + pkg;
      this.lock = lock;
      this.ctx = ctx;
      this.conn = conn;
    }

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case 1: {
          Log.e(TAG, "got response");
          /*synchronized (lock) {
            lock.notifyAll();
          }*/
          try {
            ctx.unbindService(conn);
          } catch (Throwable t) {
            Log.e(TAG, "failed to unbind", t);
          }
        }
      }
    }

  }

  private static class NetGrantConnection implements ServiceConnection {

    private final String TAG;
    Messenger mService = null;
    private Object lock = new Object();
    private String pkg = null;
    private Context ctx = null;

    NetGrantConnection(String pkg, Context ctx) {
      this.TAG = "PS/NGC/" + pkg;
      this.pkg = pkg;
      this.ctx = ctx;
    }

    @Override
    public void onServiceConnected(ComponentName className,
                                   IBinder service) {
      Log.e(TAG, "service connected");
      try {
        mService = new Messenger(service);
        Message msg = Message.obtain(null, 0x41, 0, 0);
        msg.replyTo = new Messenger(new ResponseHandler(pkg, lock, ctx, this));
        mService.send(msg);
      } catch (Exception e) {
        Log.e(TAG, "NetGrantService connection error", e);
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
      Log.e(TAG, "service disconnected");
    }

  }


  void getInternet(final Context ctx) {

    NetGrantConnection mConnection = new NetGrantConnection(pkg, ctx);

    Intent i = new Intent();
    i.setComponent(new ComponentName(Constants.pkg, Constants.pkg + ".components.NetGrantService"));

    //note: due to android shenanigans, this doesn't actually kick off until
    //      later in the process (read: after the core Application start), when
    //      context is more completely set up.
    ctx.bindService(i, mConnection, Context.BIND_AUTO_CREATE);
  }

}
