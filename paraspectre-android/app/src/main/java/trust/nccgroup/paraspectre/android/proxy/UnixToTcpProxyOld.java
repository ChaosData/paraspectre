package trust.nccgroup.paraspectre.android.proxy;

import android.content.Context;
import android.net.LocalSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import trust.nccgroup.paraspectre.android.Constants;
import trust.nccgroup.paraspectre.core.Config;


@Deprecated
public class UnixToTcpProxyOld extends ThreadedUnixProxy {

  private Config config = null;
  private Context ctx = null;
  private ConcurrentMap<String, String> keymap = new ConcurrentHashMap<>();

  public UnixToTcpProxyOld(Config config, Context ctx) {
    this.config = config;
    this.ctx = ctx;
  }

  @Override
  protected String getTag() {
    return "PS/UnixToTcpProxy";
  }

  @Override
  protected String getPath() {
    return "/data/" + "data/" + Constants.pkg + "/files/parasect.druby.sock";
  }


  @Override
  protected void handleClient(final LocalSocket client_sock) {
    try {
      final Socket druby_sock = new Socket();
      druby_sock.connect(new InetSocketAddress(config.net.reverse.host, config.net.reverse.port), 2000);
      client_sock.setSoTimeout(2000);
      final InputStream druby_is = druby_sock.getInputStream();
      final OutputStream druby_os = druby_sock.getOutputStream();

      final InputStream client_is = client_sock.getInputStream();
      final OutputStream client_os = client_sock.getOutputStream();

      final Object lock = new Object();
      final boolean[] running = new boolean[]{true};

      Thread client_to_remote = new Thread() {
        @Override
        public void run() {
          try {
            while (true) {
              synchronized (lock) {
                if (!running[0]) {
                  break;
                }
              }
              try {
                byte[] buf = new byte[2048];
                Log.e(TAG, "read 1");
                int read = client_is.read(buf);
                if (read == -1) {
                  break;
                }
                druby_os.write(buf, 0, read);

              } catch (SocketTimeoutException ste) {
                //pass
              }
              catch (IOException ioe) {
                if (!ioe.getMessage().equals("Try again")) {
                  throw ioe;
                }
              }
            }
          } catch (IOException ignored) {}
          try {
            client_is.close();
          } catch (IOException ignored) {}
          try {
            client_os.close();
          } catch (IOException ignored) {}

          try {
            druby_is.close();
          } catch (IOException ignored) {}
          try {
            druby_os.close();
          } catch (IOException ignored) {}

          try {
            druby_sock.close();
          } catch (IOException ignored) {}
          try {
            client_sock.close();
          } catch (IOException ignored) {}
          synchronized (lock) {
            running[0] = false;
          }
          //Log.e(TAG, "ended client_to_remote");
        }
      };

      Thread remote_to_client = new Thread() {
        @Override
        public void run() {
          try {
            while (true) {
              synchronized (lock) {
                if (!running[0]) {
                  break;
                }
              }
              try {
                byte[] buf = new byte[2048];
                int read = druby_is.read(buf);
                if (read == -1) {
                  break;
                }
                client_os.write(buf, 0, read);
              } catch (SocketTimeoutException ste) {
                //pass
              }
              catch (IOException ioe) {
                if (!ioe.getMessage().equals("Try again")) {
                  throw ioe;
                }
              }
            }
          } catch (IOException ignored) { }
          try {
            client_is.close();
          } catch (IOException ignored) {}
          try {
            client_os.close();
          } catch (IOException ignored) {}

          try {
            druby_is.close();
          } catch (IOException ignored) {}
          try {
            druby_os.close();
          } catch (IOException ignored) {}

          try {
            druby_sock.close();
          } catch (IOException ignored) {}
          try {
            client_sock.close();
          } catch (IOException ignored) {}
          synchronized (lock) {
            running[0] = false;
          }
          //Log.e(TAG, "ended remote_to_client");
        }
      };

      client_to_remote.start();
      remote_to_client.start();


      try {
        client_to_remote.join();
      } catch (InterruptedException ignored) { }
      try {
        remote_to_client.join();
      } catch (InterruptedException ignored) { }


    } catch (IOException ioe) {
      Log.e(TAG, "error handling client_sock", ioe);
    } finally {
      try {
        client_sock.close();
      } catch (IOException ioe) {
        Log.e(TAG, "failed to close client_sock", ioe);
      }
    }

  }
}
