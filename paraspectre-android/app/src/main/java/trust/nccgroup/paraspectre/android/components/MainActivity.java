package trust.nccgroup.paraspectre.android.components;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import trust.nccgroup.paraspectre.android.Constants;
import trust.nccgroup.paraspectre.android.R;
import trust.nccgroup.paraspectre.android.Setup;

import eu.chainfire.libsuperuser.Shell;

//TODO: fancy splash screen
public class MainActivity extends Activity {

  private static final String TAG = "PS/MainActivity";

  enum SUState {
    NoSU,
    Failure,
    Success
  }

  private ParasectService parasectService;
  private WebAppService webAppService;


  @Override
  protected void onStart() {
    super.onStart();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    startService(new Intent(this, ConfigService.class));
    startService(new Intent(this, NetGrantService.class));


    ServiceConnection parasectServiceConnection = new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        parasectService = ((ParasectService.LocalBinder) iBinder).getService();

        ToggleButton parasectServiceToggle = (ToggleButton) findViewById(R.id.proxyToggle);
        if (parasectServiceToggle != null) {
          parasectServiceToggle.setChecked(parasectService.isRunning());

          parasectServiceToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
              if (isChecked) {
                buttonView.setChecked(parasectService.startParasect());
              } else {
                parasectService.stopParasect();
              }
            }
          });
        }

      }

      @Override
      public void onServiceDisconnected(ComponentName componentName) {
        Log.e(TAG, "Parasect service disconnected");
      }
    };
    bindService(
      new Intent(this, ParasectService.class),
      parasectServiceConnection, BIND_AUTO_CREATE
    );


    final ToggleButton serverToggle = (ToggleButton)findViewById(R.id.serverToggle);
    final TextView apiKeyLabel = (TextView)findViewById(R.id.apiKeyLabel);
    final TextView apiKeyView = (TextView)findViewById(R.id.apiKeyView);

    LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        String s = intent.getStringExtra("api_key");
        if ((apiKeyLabel != null) && (apiKeyView != null) && (s != null)) {
          apiKeyView.setText(String.format(" %s", s));

          apiKeyLabel.setVisibility(View.VISIBLE);
          apiKeyView.setVisibility(View.VISIBLE);
        }
      }
    }, new IntentFilter(Constants.pkg + ".API_KEY"));


    ServiceConnection webAppServiceConnection = new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        webAppService = ((WebAppService.LocalBinder) iBinder).getService();


        if (serverToggle != null) {
          serverToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

              if (!isChecked) {
                webAppService.stopWebApp();

                if (apiKeyLabel != null && apiKeyView != null) {
                  apiKeyLabel.setVisibility(View.INVISIBLE);
                  apiKeyView.setVisibility(View.INVISIBLE);

                  apiKeyView.setText("");
                }
              } else {
                buttonView.setChecked(webAppService.startWebApp());
              }
            }
          });
        }
      }

      @Override
      public void onServiceDisconnected(ComponentName componentName) {
        Log.e(TAG, "WebApp service disconnected");
      }
    };

    bindService(
      new Intent(this, WebAppService.class),
      webAppServiceConnection, BIND_AUTO_CREATE
    );




    SharedPreferences prefs = getPreferences(MODE_PRIVATE);
    if (!prefs.contains("optimized")) {
      prefs.edit().putBoolean("optimized", true).apply();
    }

    boolean optimized = prefs.getBoolean("optimized", true);
    CheckBox is_optimized = (CheckBox)findViewById(R.id.is_optimized);
    is_optimized.setChecked(optimized);
    /*if (optimized) {
      setupOptimization();
    } else {
      teardownOptimization();
    }*/
    is_optimized.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
          setupOptimization();
        } else {
          teardownOptimization();
        }
      }
    });
  }


  void setupOptimization() {
    File psdir = new File("/data/dalvik-cache/paraspectre");
    if (psdir.exists()) {
      Log.e(TAG, "/data/dalvik-cache/paraspectre exists, can't mkdir it");
      return;
    }

    Setup.publishAssets("runtime.dex.jar", this);
    Setup.publishAssets("jrubystuff.jar", this);

    new AsyncTask<Void, Void, SUState>() {

      @Override
      protected SUState doInBackground(Void... params) {

        if (!Shell.SU.available()) {
          return SUState.NoSU;
        }

        String dir;
        try {
          dir = MainActivity.this.getApplicationContext().getFilesDir().getCanonicalPath();
        } catch (IOException ioe) {
          Log.e(TAG, "failed to get own file directory, couldn't run su commands", ioe);
          return SUState.Failure;
        }


        String[] cmds = {
          "mkdir /data/dalvik-cache/paraspectre",
          "chmod 755 /data/dalvik-cache/paraspectre",
          "cp " + dir + "/runtime.dex.jar /data/dalvik-cache/paraspectre/",
          "chmod 644 /data/dalvik-cache/paraspectre/runtime.dex.jar",
          "cp " + dir + "/jrubystuff.jar /data/dalvik-cache/paraspectre/",
          "chmod 644 /data/dalvik-cache/paraspectre/jrubystuff.jar"
        };

        long completed = 0;
        for (String cmd : cmds) {
          if (Shell.SU.run(cmd) == null) {
            break;
          }
          completed += 1;
        }

        if (completed == 0) {
          Log.e(TAG, "failed to run any SU commands");
        } else if (completed < cmds.length) {
          Log.e(TAG, "failed to run all SU commands, only " + completed + " of " + cmds.length);
        } else if (completed == cmds.length) {
          SharedPreferences prefs = getPreferences(MODE_PRIVATE);
          prefs.edit().putBoolean("optimized", true).apply();
          return SUState.Success;
        }
        return SUState.Failure;
      }

      @Override
      protected void onPostExecute(SUState state) {

        CheckBox cb = ((CheckBox) MainActivity.this.findViewById(R.id.is_optimized));

        switch (state) {
          case NoSU: {
            Toast.makeText(MainActivity.this, "failed to aquire SU", Toast.LENGTH_LONG).show();
            cb.setChecked(false);
            break;
          }
          case Failure: {
            cb.setChecked(false);
            break;
          }
          case Success: {
            cb.setChecked(true);
            break;
          }
        }
      }

    }.execute();

  }

  void teardownOptimization() {
    File psdir = new File("/data/dalvik-cache/paraspectre");
    if (!psdir.exists()) {
      Log.e(TAG, "/data/dalvik-cache/paraspectre doesn't exist, can't rm it");
      return;
    }

    try {
      String dir = MainActivity.this.getApplicationContext().getFilesDir().getCanonicalPath();
      File runtime_dex_jar = new File(dir + "/runtime.dex.jar");
      boolean worked = runtime_dex_jar.delete();
      if (!worked) {
        Log.e(TAG, "Failed to delete runtime.dex.jar");
      }

      File jrubystuff_jar = new File(dir + "/jrubystuff.jar");
      worked = jrubystuff_jar.delete();
      if (!worked) {
        Log.e(TAG, "Failed to delete jrubystuff.jar");
      }

    } catch (IOException ioe) {
      Log.e(TAG, "error cleaning assets", ioe);
      return;
    }

    new AsyncTask<Void, Void, SUState>() {
      @Override
      protected SUState doInBackground(Void... params) {
        if (!Shell.SU.available()) {
          return SUState.NoSU;
        }

        if (Shell.SU.run("rm -rf /data/dalvik-cache/paraspectre") == null) {
          Log.e(TAG, "failed to run teardownOptimization");
          return SUState.Failure;
        }

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        prefs.edit().putBoolean("optimized", false).apply();

        return SUState.Success;
      }

      @Override
      protected void onPostExecute(SUState state) {

        CheckBox cb = ((CheckBox) MainActivity.this.findViewById(R.id.is_optimized));

        switch (state) {
          case NoSU: {
            Toast.makeText(MainActivity.this, "failed to aquire SU", Toast.LENGTH_LONG).show();
            cb.setChecked(true);
            break;
          }
          case Failure: {
            cb.setChecked(true);
            break;
          }
          case Success: {
            cb.setChecked(false);
            break;
          }
        }

      }

    }.execute();
  }


}
