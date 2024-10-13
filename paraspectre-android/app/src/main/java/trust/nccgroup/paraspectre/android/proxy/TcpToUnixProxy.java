package trust.nccgroup.paraspectre.android.proxy;

import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentMap;

import trust.nccgroup.paraspectre.android.Utils;
import trust.nccgroup.paraspectre.core.Config;


public class TcpToUnixProxy {

  private static final String TAG = "PS/TcpToUnixProxy";

  private Config config = null;
  private Context ctx = null;
  private ConcurrentMap<String, String> keymap = null;

  public TcpToUnixProxy(Config config, ConcurrentMap<String, String> keymap) {
    this.config = config;
    this.keymap = keymap;
  }

  public enum State {
    RUNNING,
    NOT_RUNNING
  }

  protected State state = State.NOT_RUNNING;

  public void run() {

    ServerSocket server_sock;
    try {
      server_sock = new ServerSocket();
    } catch (IOException ioe) {
      Log.e(TAG, "failed to create socket_server", ioe);
      return;
    }

    try {
      server_sock.bind(new InetSocketAddress(config.net.forwarder.host, config.net.forwarder.port), 50);
    } catch (IOException ioe) {
      Log.e(TAG, "failed to bind", ioe);
      return;
    }

    synchronized (this) {
      state = State.RUNNING;
    }

    while (true) {
      synchronized (this) {
        if (state != State.RUNNING) {
          break;
        }
      }
      try {
        final Socket client_sock = server_sock.accept();
        new Thread() {
          @Override
          public void run() {
            handleClient(client_sock);
          }
        }.start();
      } catch (IOException ioe) {
        Log.e(TAG, "failed to accept", ioe);
      }
    }
    try {
      server_sock.close();
    } catch (IOException ioe) {
      Log.e(TAG, "failed to close server_sock", ioe);
    }
  }

  public void stop() {
    synchronized (this) {
      state = State.NOT_RUNNING;
    }
  }


  protected void handleClient(final Socket client_sock) {

    ShoGaNaiForwarder.ReflectSocket client_sock_r
      = new ShoGaNaiForwarder.ReflectSocket(client_sock);

    InputStream client_sock_r_is;
    try {
      client_sock_r_is = client_sock_r.getInputStream();
    } catch (IOException ioe) {
      Log.e(TAG, "failed to get client_sock InputStream", ioe);
      try {
        client_sock.close();
      } catch (IOException ignored) {}
      return;
    }

    int read = 0;
    byte[] keybuf = new byte[16];

    while (read < 16) {
      try {
        int tmp_read = client_sock_r_is.read(keybuf, read, keybuf.length - read);
        if (tmp_read == -1) {
          try {
            client_sock_r_is.close();
          } catch (IOException ignored) {}
          try {
            client_sock.close();
          } catch (IOException ignored) {}
          return;
        }
        read += tmp_read;
      } catch (IOException ioe) {
        Log.e(TAG, "read failed", ioe);
        try {
          client_sock_r_is.close();
        } catch (IOException ignored) {}
        try {
          client_sock.close();
        } catch (IOException ignored) {}
        return;
      }
    }

    String asciikey = Utils.bytesToHex(keybuf);

    String socket_path = keymap.remove(asciikey);
    if (socket_path == null) {
      Log.e(TAG, "wrong key: " + asciikey);
      try {
        client_sock_r_is.close();
      } catch (IOException ignored) {}
      try {
        client_sock.close();
      } catch (IOException ignored) {}
      return;
    }

    LocalSocket unix_sock = new LocalSocket();
    try {
      unix_sock.connect(new LocalSocketAddress(socket_path, LocalSocketAddress.Namespace.FILESYSTEM));
      unix_sock.setSoTimeout(2000);
      client_sock.setSoTimeout(2000);
    } catch (IOException ioe) {
      Log.e(TAG, "error handling sockets", ioe);
      try {
        unix_sock.close();
      } catch (IOException ignored) {}
      try {
        client_sock_r_is.close();
      } catch (IOException ignored) {}
      try {
        client_sock.close();
      } catch (IOException ignored) {}
      return;
    }

    ShoGaNaiForwarder.ReflectSocket druby_sock_r
      = new ShoGaNaiForwarder.ReflectSocket(unix_sock);

    ShoGaNaiForwarder forwarder
      = new ShoGaNaiForwarder(client_sock_r, client_sock_r_is, null, druby_sock_r, null, null);

    if (forwarder.isReady()) {
      forwarder.run();
    } else {
      try {
        unix_sock.close();
      } catch (IOException ignored) {}
      try {
        client_sock.close();
      } catch (IOException ignored) {}
    }
  }
}
