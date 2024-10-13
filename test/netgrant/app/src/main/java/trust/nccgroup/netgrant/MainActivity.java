package trust.nccgroup.netgrant;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;
import android.os.UserHandle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.joor.Reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    //String pkg = "trust.nccgroup.uihookdemo";
    //int uid = 10099;
    String pkg = "trust.nccgroup.nointernet";
    int uid = 10103;
    int userId = uid / 100000; //per user range for multi-user

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
      Log.e("NCC", "???", t);
    }

  }
}
