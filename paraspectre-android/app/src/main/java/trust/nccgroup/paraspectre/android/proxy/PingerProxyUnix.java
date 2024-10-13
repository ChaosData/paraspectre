package trust.nccgroup.paraspectre.android.proxy;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.LocalSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentMap;

import trust.nccgroup.paraspectre.android.Constants;
import trust.nccgroup.paraspectre.android.Utils;
import trust.nccgroup.paraspectre.core.Config;

@Deprecated
public class PingerProxyUnix extends ThreadedUnixProxy {

  private Config config = null;
  private Context ctx = null;
  private ConcurrentMap<String, String> keymap = null;

  public PingerProxyUnix(Config config, Context ctx, ConcurrentMap<String, String> keymap) {
    this.config = config;
    this.ctx = ctx;
    this.keymap = keymap;
  }

  @Override
  protected String getTag() {
    return "PS/PingerProxyUnix";
  }

  @Override
  protected String getPath() {
    return "/data/" + "data/" + Constants.pkg + "/files/ping.sock";
  }

  private String uidToPath(int uid) {
    PackageManager pm = ctx.getPackageManager();
    String[] pkgs = pm.getPackagesForUid(uid);
    if (pkgs == null || pkgs.length == 0) {
      return null;
    }
    return "/data/" + "data/" + pkgs[0] + "/files/paraspectre.sock"; //FIXME: add real checking
    //have client send own pkg and compare to validate it
  }


  @Override
  protected void handleClient(LocalSocket client_sock) {
    String asciikey = null;
    try {
      int read = 0;
      byte[] keybuf = new byte[32];
      byte[] portbuf = new byte[4];

      InputStream client_is = client_sock.getInputStream();
      while (read < keybuf.length) {
        int tmp_read = client_is.read(keybuf, read, keybuf.length - read);
        if (tmp_read == -1) {
          try {
            client_is.close();
          } catch (IOException ignored) {}
          return;
        }
        read += tmp_read;
      }

      asciikey = Utils.bytesToHex(keybuf);

      int uid = client_sock.getPeerCredentials().getUid();
      String socket_path = uidToPath(uid);

      if (socket_path == null) {
        Log.e(TAG, "socket_path: null");
        return;
      }

      keymap.put(asciikey, socket_path);

      client_is.close();
    } catch (IOException ioe) {
      Log.e(TAG, "error handling client_sock", ioe);
    } finally {
      try {
        client_sock.close();
      } catch (IOException ioe) {
        Log.e(TAG, "failed to close client_sock", ioe);
      }
    }

    if (asciikey == null) {
      return;
    }

    Socket pinger_sock = null;
    try {
      pinger_sock = new Socket(config.net.pinger.host, config.net.pinger.port);

      OutputStream pinger_os = pinger_sock.getOutputStream();
      pinger_os.write(Utils.hexToBytes(asciikey));
      pinger_os.flush();
      pinger_os.close();
    } catch (IOException ioe) {
      Log.e(TAG, "error handling pinger_sock", ioe);
    } finally {
      try {
        if (pinger_sock != null) {
          pinger_sock.close();
        }
      } catch (IOException ioe) {
        Log.e(TAG, "failed to close pinger_sock", ioe);
      }
    }

  }
}
