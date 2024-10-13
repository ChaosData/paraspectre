package trust.nccgroup.paraspectre.android.proxy;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentMap;

import trust.nccgroup.paraspectre.core.Config;

@Deprecated
public class TcpReverseProxy {

  private static final String TAG = "PS/TcpReverseProxy";

  private Config config = null;
  private ConcurrentMap<String, ProxyContext> keymap = null;

  public TcpReverseProxy(Config config, ConcurrentMap<String, ProxyContext> keymap) {
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
      server_sock.bind(new InetSocketAddress(config.net.reverse.host, config.net.reverse.port), 50);
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


    Socket sock = null;
    try {
      sock = new Socket(config.net.remote.host, config.net.remote.port);
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
        client_sock.close();
      } catch (IOException ignored) {}
      return;
    }

    ShoGaNaiForwarder.ReflectSocket druby_sock_r
      = new ShoGaNaiForwarder.ReflectSocket(sock);

    ShoGaNaiForwarder forwarder
      = new ShoGaNaiForwarder(client_sock_r, null, null, druby_sock_r, null, null);

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
