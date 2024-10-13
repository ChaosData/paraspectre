package trust.nccgroup.paraspectre.android.proxy;

import android.content.Context;
import android.net.LocalSocket;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import trust.nccgroup.paraspectre.android.Constants;
import trust.nccgroup.paraspectre.core.Config;


@Deprecated
public class UnixToTcpProxy extends ThreadedUnixProxy {

  private Config config = null;
  private Context ctx = null;

  public UnixToTcpProxy(Config config, Context ctx) {
    this.config = config;
    this.ctx = ctx;
  }

  @Override
  protected String getTag() {
    return "PS/UnixToTcpProxy";
  }

  @Override
  protected String getPath() {
    return "/data/" + "data/" + Constants.pkg + "/files/druby.sock";
  }

  @Override
  protected void handleClient(final LocalSocket client_sock) {
    Socket druby_sock = null;
    try {
      druby_sock = new Socket();
      druby_sock.connect(new InetSocketAddress(config.net.reverse.host, config.net.reverse.port), 2000);
      client_sock.setSoTimeout(2000);
    } catch (IOException ioe) {
      Log.e(TAG, "error handling sockets", ioe);
      try {
        druby_sock.close();
      } catch (IOException ignored) {}
      try {
        client_sock.close();
      } catch (IOException ignored) {}
      return;
    }

    ShoGaNaiForwarder.ReflectSocket druby_sock_r
      = new ShoGaNaiForwarder.ReflectSocket(druby_sock);

    ShoGaNaiForwarder.ReflectSocket client_sock_r
      = new ShoGaNaiForwarder.ReflectSocket(client_sock);

    ShoGaNaiForwarder forwarder = new ShoGaNaiForwarder(druby_sock_r, client_sock_r);
    if (forwarder.isReady()) {
      forwarder.run();
    } else {
      try {
        druby_sock.close();
      } catch (IOException ignored) {}
      try {
        client_sock.close();
      } catch (IOException ignored) {}
    }
  }
}
