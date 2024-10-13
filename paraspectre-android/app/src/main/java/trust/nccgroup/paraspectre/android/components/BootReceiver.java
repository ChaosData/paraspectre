package trust.nccgroup.paraspectre.android.components;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;

public class BootReceiver extends BroadcastReceiver {

  private static final String TAG = "PS/BootReceiver";

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.e(TAG, "received intent");
    String action = intent.getAction();
    if (!action.equals(Intent.ACTION_BOOT_COMPLETED)) {
      Log.e(TAG, "received invalid intent action: " + action);
      return;
    }

    File fd = context.getFilesDir();
    if (!fd.exists() && !fd.mkdirs()) {
      Log.e(TAG, "failed to create files directory");
    }

    context.startService(new Intent(context, ConfigService.class));
    context.startService(new Intent(context, NetGrantService.class));
  }
}
