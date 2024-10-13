package trust.nccgroup.paraspectre.android.proxy;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.LocalSocket;
import android.util.Log;

import com.google.common.hash.Hashing;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentMap;

import trust.nccgroup.paraspectre.android.Configuration;
import trust.nccgroup.paraspectre.android.Constants;
import trust.nccgroup.paraspectre.core.Config;
import trust.nccgroup.paraspectre.core.config.Matcher;

import static com.google.common.base.Charsets.UTF_8;

public class PingerProxyTcp extends ThreadedUnixProxy {

  private Config config = null;
  private Context ctx = null;

  private ConcurrentMap<String, ProxyContext> keymap = null;

  public PingerProxyTcp(Config config, Context ctx, ConcurrentMap<String, ProxyContext> keymap) {
    this.config = config;
    this.ctx = ctx;
    this.keymap = keymap;
  }

  @Override
  protected String getTag() {
    return "PS/PingerProxyTcp";
  }

  @Override
  protected String getPath() {
    return "/data/" + "data/" + Constants.pkg + "/files/ping.sock";
  }

  private String pkgForUid(int uid) {
    PackageManager pm = ctx.getPackageManager();
    String[] pkgs = pm.getPackagesForUid(uid);
    if (pkgs == null || pkgs.length == 0) {
      return null;
    }
    return pkgs[0];
  }


  @Override
  protected void handleClient(LocalSocket client_sock) {
    String secret = null;
    String pkg_name = null;
    int port = 0;
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
          } catch (IOException ignored) { }
          return;
        }
        read += tmp_read;
      }
      secret = new String(keybuf);
      //Log.e(TAG, "got secret:" + secret);

      read = 0;
      while (read < portbuf.length) {
        int tmp_read = client_is.read(portbuf, read, portbuf.length - read);
        if (tmp_read == -1) {
          try {
            client_is.close();
          } catch (IOException ignored) {}
          return;
        }
        read += tmp_read;
      }

      ByteBuffer bb = ByteBuffer.allocate(4);
      bb.put(portbuf);
      port = bb.getInt(0);
      //Log.e(TAG, "got port:" + port);

      int uid = client_sock.getPeerCredentials().getUid();
      pkg_name = pkgForUid(uid);

      if (pkg_name == null) {
        Log.e(TAG, "no pkg for " + uid);
        return;
      }

      Matcher m = Configuration.getMatcher(pkg_name);
      if (m == null) {
        Config c = Configuration.getConfig(pkg_name, true);
        if (c == null) {
          Log.e(TAG, "no configs found at all?");
          return;
        } else if (c.matchers == null) {
          Log.e(TAG, "no configs found for " + pkg_name);
          return;
        }
        boolean found = false;
        for (Matcher mm : c.matchers) {
          if (pkg_name.equals(mm.pkg)) {
            found = true;
            break;
          }
        }
        if (!found) {
          Log.e(TAG, "no matchers found for " + pkg_name);
          return;
        }
      }

    } catch (IOException ioe) {
      Log.e(TAG, "error handling client_sock", ioe);
      return;
    } finally {
      try {
        client_sock.close();
      } catch (IOException ioe) {
        Log.e(TAG, "failed to close client_sock", ioe);
      }
    }

//    if (secret == null || pkg_name == null) {
//      return;
//    }

    keymap.put(Hashing.sha256().hashString(secret, UTF_8).toString(), new ProxyContext(pkg_name, port, secret));

//    try {
//      Thread.sleep(1000);
//    } catch (InterruptedException ie) {
//      Log.e(TAG, "sleep interrupted", ie);
//    }

    //Log.e(TAG, "connecting to ping listener");

    Socket pinger_sock = null;
    try {
      pinger_sock = new Socket(config.net.pinger.host, config.net.pinger.port);

      OutputStream pinger_os = pinger_sock.getOutputStream();
      pinger_os.write(secret.getBytes());
      ByteBuffer bb = ByteBuffer.allocate(4);
      bb.putInt(0, pkg_name.length());
      byte[] pkg_len = new byte[4];
      bb.get(pkg_len, 0, pkg_len.length);
      pinger_os.write(pkg_len);
      pinger_os.write(pkg_name.getBytes());
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
