package trust.nccgroup.paraspectre.android.components;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import org.joor.Reflect;

public class NetGrantService extends Service {

  private static final String TAG = "PS/NetGrantService";

  //private final IBinder mBinder = new APIBinder();
  final Messenger mMessenger = new Messenger(new NGHandler());

  public NetGrantService() { }

  @Override
  public void onCreate() {

  }

  @Override
  public void onDestroy() {

  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.e(TAG, "starting up");

    return Service.START_STICKY_COMPATIBILITY;
  }

  private void grantInternet(int uid) {
    Log.e(TAG, "grantInternet(" + uid + ")");
    PackageManager mypm = this.getPackageManager();
    //int uid = Binder.getCallingUid(); //doesn't work here
    int userId = uid / 100000; //per user range for multi-user
    String[] pkgs = mypm.getPackagesForUid(uid);

    if (pkgs == null) {
      Log.e(TAG, "failed to find packages for uid: " + uid);
      return;
    }

    Log.e(TAG, "pkgs count: " + pkgs.length);

    for (String pkg : pkgs) {
      Log.e(TAG, "pkg: " + pkg);

      Object pmstub = Reflect
        .on("android.os.ServiceManager")
        .call("getService", "package")
        .get();
      Object pm = Reflect
        .on("android.content.pm.IPackageManager$Stub")
        .call("asInterface", pmstub)
        .get();
      try {
        Reflect.on(pm)
          .call("grantRuntimePermission", pkg, android.Manifest.permission.INTERNET, userId)
          .get();
      } catch (Throwable t) {
        Log.e(TAG, "failed to grant internet permission to: " + pkg, t);
      }
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    Log.e(TAG, "onBind");
    return mMessenger.getBinder();
  }


  //note: currently needs strong ref to outer class to perform permission grant
  //todo: move that code into the handler and verify it still works
  private class NGHandler extends Handler {
    private final static String TAG = "PS/NGS/Handler";
    private int uid = 0;

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case 0x41:
          try {
            NetGrantService.this.grantInternet(uid);
          } catch (Throwable t) {
            Log.e(TAG, "an error occurred", t);
          }
          break;
        default:
          super.handleMessage(msg);
      }
      try {
        msg.replyTo.send(Message.obtain(null, 0x1));
      } catch (RemoteException re) {
        Log.e(TAG, "failed to send reply", re);
      }
    }

    @Override
    public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
      uid = Binder.getCallingUid();
      return super.sendMessageAtTime(msg, uptimeMillis);
    }
  }
}
