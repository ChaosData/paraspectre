package trust.nccgroup.paraspectre.android.proxy;

import android.content.Context;
import android.util.Log;

import com.google.common.hash.Hashing;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentMap;

import trust.nccgroup.paraspectre.core.Config;

import static com.google.common.base.Charsets.UTF_8;


public class TcpForwardProxy {

  private static final String TAG = "PS/TcpForwardProxy";

  private Config config = null;
  private Context ctx = null;
  private ConcurrentMap<String, ProxyContext> keymap = null;
  private ServerSocket server_sock = null;


  public TcpForwardProxy(Config config, ConcurrentMap<String, ProxyContext> keymap) {
    this.config = config;
    this.keymap = keymap;
  }

  public enum State {
    RUNNING,
    NOT_RUNNING
  }

  private State state = State.NOT_RUNNING;

  public boolean setup() {
    try {
      server_sock = new ServerSocket();
      server_sock.setReuseAddress(true);
    } catch (IOException ioe) {
      Log.e(TAG, "failed to create socket_server", ioe);
      return false;
    }

    try {
      server_sock.bind(new InetSocketAddress(config.net.forwarder.host, config.net.forwarder.port), 50);
    } catch (IOException ioe) {
      Log.e(TAG, "failed to bind", ioe);
      return false;
    }

    synchronized (this) {
      state = State.RUNNING;
    }
    return true;
  }

  public void run() {


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
        synchronized (this) {
          if (server_sock != null) {
            Log.e(TAG, "failed to accept", ioe);
          }
        }
      }
    }
    try {
      if (server_sock != null) {
        server_sock.close();
      }
    } catch (IOException ioe) {
      Log.e(TAG, "failed to close server_sock", ioe);
    }
  }

  public void stop() {
    synchronized (this) {
      if (state == State.RUNNING) {
        try {
          server_sock.close();
        } catch (IOException ioe) {
          Log.e(TAG, "failed to close server_socket", ioe);
        }
        server_sock = null;
        state = State.NOT_RUNNING;
      }
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
    byte[] keybuf = new byte[32];

    while (read < keybuf.length) {
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

    String secret = new String(keybuf);

    String hash = Hashing.sha256().hashString(secret, UTF_8).toString();
    ProxyContext ctx = keymap.get(hash);
    if (ctx == null) {
      Log.e(TAG, "wrong key: " + secret);
      try {
        client_sock_r_is.close();
      } catch (IOException ignored) {}
      try {
        client_sock.close();
      } catch (IOException ignored) {}
      return;
    }
    //todo: deal w/ racy connections later, accept memory leak for now
    /*ctx.count -= 1;
    if (ctx.count == 0) {
      keymap.remove(hash);
    }*/

    //Log.e(TAG, "got client w/ valid key");

    Socket sock = null;
    try {
      sock = new Socket("127.0.0.1", ctx.port);
      sock.setSoTimeout(2000);
      client_sock.setSoTimeout(2000);
    } catch (IOException ioe) {
      Log.e(TAG, "error handling sockets", ioe);
      if (sock != null) {
        try {
          sock.close();
        } catch (IOException ignored) {}
      }
      try {
        client_sock_r_is.close();
      } catch (IOException ignored) {}
      try {
        client_sock.close();
      } catch (IOException ignored) {}
      return;
    }

    ShoGaNaiForwarder.ReflectSocket druby_sock_r
      = new ShoGaNaiForwarder.ReflectSocket(sock);

/*
    try {
      OutputStream druby_os = druby_sock_r.getOutputStream();
      druby_os.write(secret.getBytes());
      druby_os.flush();
    } catch (IOException ioe) {
      Log.e(TAG, "error forwarding secret to druby", ioe);
      try {
        sock.close();
      } catch (IOException ignored) {}
      try {
        client_sock.close();
      } catch (IOException ignored) {}
      return;
    }
*/

    ShoGaNaiForwarder forwarder
      = new ShoGaNaiForwarder(client_sock_r, client_sock_r_is, null, druby_sock_r, null, null);

    if (forwarder.isReady()) {
      forwarder.run();
    } else {
      try {
        sock.close();
      } catch (IOException ignored) {}
      try {
        client_sock.close();
      } catch (IOException ignored) {}
    }
  }
}
