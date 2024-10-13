package trust.nccgroup.jdruby;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


//note: need to handle IO.select
public class UNIXSocket {

  private static final String TAG = "PS/UNIXSocket";

  private String path;
  private LocalSocket socket;
  private InputStream in;
  private OutputStream out;

  private boolean initialized = false;

  public UNIXSocket(String path) {
    this.path = path;
    socket = new LocalSocket();
    try {
      socket.bind(new LocalSocketAddress(this.path, LocalSocketAddress.Namespace.FILESYSTEM));
      in = socket.getInputStream();
      out = socket.getOutputStream();
    } catch (IOException ioe) {
      Log.e(TAG, "failed to initialize", ioe);
      close();
      return;
    }
    initialized = true;
  }

  public UNIXSocket(LocalSocket socket) {
    this.socket = socket;
    path = this.socket.getLocalSocketAddress().getName();

    try {
      in = this.socket.getInputStream();
      out = this.socket.getOutputStream();
    } catch (IOException ioe) {
      Log.e(TAG, "failed to initialize", ioe);
      close();
      return;
    }
    initialized = true;
  }

  public static UNIXSocket open(String path) {
    Log.e(TAG, "open");
    return new UNIXSocket(path);
  }

  public void close() {
    Log.e(TAG, "close");
    this.initialized = false;
    if (in != null) {
      try {
        in.close();
      } catch (IOException ignored) {}
    }

    if (out != null) {
      try {
        out.close();
      } catch (IOException ignored) {}
    }

    if (socket != null) {
      try {
        socket.close();
      } catch (IOException ignored) {}
    }
  }

  public void fcntl(Object o, Object o2) {
    Log.e(TAG, "fcntl");
  }

  public void setsockopt(Object o, Object o2) {
    Log.e(TAG, "setsockopt");
  }

  public String path() {
    return path;
  }

  public byte[] read(int count) {
    if (initialized && in != null) {
      int read = 0;
      byte[] buf = new byte[count];
      try {
        while (read < count) {
          int tmp_read = in.read(buf, read, count-read);
          if (tmp_read == -1) {
            close();
            return new byte[]{};
          }
        }
        return buf;
      } catch (IOException ioe) {
        close();
      }
    }
    return new byte[]{};
  }

  public void write(byte[] data) {
    Log.e(TAG, "write");
    if (initialized && out != null) {
      try {
        out.write(data);
      } catch (IOException e) {
        close();
      }
    }
  }

  public Object[] addr() {
    Log.e(TAG, "addr");
    return new Object[]{"AF_UNIX", path};
  }

  public Object[] peeraddr() {
    Log.e(TAG, "peeraddr");
    return new Object[]{"AF_UNIX", path};
  }

}
