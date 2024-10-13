package trust.nccgroup.paraspectre.android.proxy;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;


public abstract class ThreadedUnixProxy {

  abstract protected String getTag();
  abstract protected String getPath();
  abstract protected void handleClient(LocalSocket client_sock);

  public enum State {
    RUNNING,
    NOT_RUNNING
  }

  protected State state = State.NOT_RUNNING;

  protected final String TAG = getTag();
  protected final String PATH = getPath();

  public void run() {
    File socket_file = new File(PATH);
    if (socket_file.exists() && !socket_file.delete()) {
      Log.e(TAG, "failed to wipe unix domain socket");
    }
    try {
      if (!socket_file.createNewFile()) {
        Log.e(TAG, "failed to create socket_file");
      }
    } catch (IOException ioe) {
      Log.e(TAG, "error creating socket_file", ioe);
      return;
    }

    LocalSocket ls = new LocalSocket();
    try {
      ls.bind(new LocalSocketAddress(PATH, LocalSocketAddress.Namespace.FILESYSTEM));
    } catch (IOException ioe) {
      Log.e(TAG, "failed to bind: " + PATH, ioe);
    }

    FileDescriptor sock_fd = ls.getFileDescriptor();

    if (sock_fd == null) {
      Log.e(TAG, "failed to bind on " + PATH);
      return;
    }
    Log.e(TAG, "bound on " + PATH);

    LocalServerSocket server_sock;
    try {
      server_sock = new LocalServerSocket(sock_fd);
    } catch (IOException ioe) {
      Log.e(TAG, "failed to create socket_server", ioe);
      return;
    }

    if (!socket_file.setReadable(true, false)) {
      Log.e(TAG, "failed to set socket_file " + PATH + "world readable");
    }
    if (!socket_file.setWritable(true, false)) {
      Log.e(TAG, "failed to set socket_file " + PATH + "world writeable");
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
        final LocalSocket client_sock = server_sock.accept();
        Log.e(TAG, "client connected to: " + PATH);
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

}
