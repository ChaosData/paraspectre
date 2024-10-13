package trust.nccgroup.nointernet;

import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    URL u = null;
    try {
      u = new URL("https://c.yber.ninja");
    } catch (MalformedURLException ignored) {}

    StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX);

    byte[] b = new byte[20];
    try {
      u.openConnection().getInputStream().read(b);
      Log.e("NCC", new String(b));
    } catch (Throwable t) {
      Log.e("NCC", "error", t);
    }

  }
}
