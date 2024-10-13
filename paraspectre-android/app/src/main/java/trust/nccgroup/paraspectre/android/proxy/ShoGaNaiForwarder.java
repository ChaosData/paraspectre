package trust.nccgroup.paraspectre.android.proxy;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;

//note: currently no way to shut it down from the outside
//note: originally named しょうがないForwarder, proguard cannot handle japanese ;__;
public class ShoGaNaiForwarder implements Runnable {

  private static final String TAG = "PS/しょうがないForwarder";

  public static class ReflectSocket {

    private Object socket;
    private InputStream is = null;
    private OutputStream os = null;

    private Method getInputStream;
    private Method getOutputStream;
    private Method close;

    public ReflectSocket(Object socket) {
      this.socket = socket;

      try {
        getInputStream = socket.getClass().getMethod("getInputStream");
        getOutputStream = socket.getClass().getMethod("getOutputStream");
        close = socket.getClass().getMethod("close");
      } catch (NoSuchMethodException nsme) {
        Log.e(TAG, "ReflectSocket failed", nsme);
      }



    }

    public InputStream getInputStream() throws IOException {
      if (is != null) {
        return is;
      }
      try {
        is = (InputStream)getInputStream.invoke(socket);
        return is;
      } catch (IllegalAccessException iae) {
        //pass
      } catch (InvocationTargetException ite) {
        Throwable t = ite.getTargetException();
        if (t instanceof IOException) {
          throw (IOException)t;
        } else {
          Log.e(TAG, "getInputStream failed", t);
        }
      }
      return null;
    }

    public OutputStream getOutputStream() throws IOException {
      if (os != null) {
        return os;
      }
      try {
        os = (OutputStream)getOutputStream.invoke(socket);
        return os;
      } catch (IllegalAccessException iae) {
        //pass
      } catch (InvocationTargetException ite) {
        Throwable t = ite.getTargetException();
        if (t instanceof IOException) {
          throw (IOException)t;
        } else {
          Log.e(TAG, "getOutputStream failed", t);
        }
      }
      return null;
    }

    public void close() throws IOException {
      try {
        close.invoke(socket);
      } catch (IllegalAccessException iae) {
        //pass
      } catch (InvocationTargetException ite) {
        Throwable t = ite.getTargetException();
        if (t instanceof IOException) {
          throw (IOException)t;
        } else {
          Log.e(TAG, "close failed", t);
        }
      }
    }

  }

  private static class SingleForwarder {

    private InputStream is;
    private OutputStream os;
    private ShoGaNaiForwarder runner;

    public SingleForwarder(InputStream is, OutputStream os,
                           ShoGaNaiForwarder runner) {
      this.is = is;
      this.os = os;
      this.runner = runner;
    }

    public void forward() {
      try {
        while (true) {
          synchronized (runner.lock) {
            if (!runner.running) {
              break;
            }
          }
          try {
            byte[] buf = new byte[2048];
            int read = is.read(buf);
            if (read == -1) {
              break;
            }
            os.write(buf, 0, read);
          } catch (SocketTimeoutException ste) {
            //pass
          } catch (IOException ioe) {
            if (!ioe.getMessage().equals("Try again")) { //LocalSocket hijinks
              throw ioe;
            }
          }
        }
      } catch (IOException ignored) {}
      synchronized (runner.lock) {
        if (runner.running) {
          runner.running = false;
          try {
            runner.peer1_is.close();
          } catch (IOException ignored) {}
          try {
            runner.peer1_os.close();
          } catch (IOException ignored) {}

          try {
            runner.peerA_is.close();
          } catch (IOException ignored) {}
          try {
            runner.peerA_os.close();
          } catch (IOException ignored) {}

          try {
            runner.peer1_sock.close();
          } catch (IOException ignored) {}
          try {
            runner.peerA_sock.close();
          } catch (IOException ignored) {}
        }
      }
    }

  }

  public ReflectSocket peer1_sock;
  public ReflectSocket peerA_sock;

  public InputStream peer1_is;
  public OutputStream peer1_os;

  public InputStream peerA_is;
  public OutputStream peerA_os;

  private final Object lock = new Object();
  private boolean running = false;

  public ShoGaNaiForwarder(ReflectSocket peer1_sock, ReflectSocket peerA_sock) {
    this(peer1_sock, null, null, peerA_sock, null, null);
  }

  public ShoGaNaiForwarder(
    ReflectSocket _peer1_sock, InputStream _peer1_is, OutputStream _peer1_os,
    ReflectSocket _peerA_sock, InputStream _peerA_is, OutputStream _peerA_os) {

    peer1_sock = _peer1_sock;
    peerA_sock = _peerA_sock;

    peer1_is = _peer1_is;
    peer1_os = _peer1_os;

    peerA_is = _peerA_is;
    peerA_os = _peerA_os;

    try {
      if (peer1_is == null) {
        peer1_is = peer1_sock.getInputStream();
        if (peer1_is == null) {
          throw new IOException();
        }
      }
      if (peer1_os == null) {
        peer1_os = peer1_sock.getOutputStream();
        if (peer1_os == null) {
          throw new IOException();
        }
      }
    } catch (IOException ioe) {
      Log.e(TAG, "1", ioe);
      if (peer1_is != null) {
        try {
          peer1_is.close();
        } catch (IOException _ioe) { Log.e(TAG, "2", _ioe); }
        peer1_is = null;
      }
      return;
    }

    try {
      if (peerA_is == null) {
        peerA_is = peerA_sock.getInputStream();
        if (peerA_is == null) {
          throw new IOException();
        }
      }
      if (peerA_os == null) {
        peerA_os = peerA_sock.getOutputStream();
        if (peerA_os == null) {
          throw new IOException();
        }
      }
    } catch (IOException ioe) {
      Log.e(TAG, "3", ioe);
      if (peerA_is != null) {
        try {
          peerA_is.close();
        } catch (IOException _ioe) { Log.e(TAG, "4", _ioe); }
        peerA_is = null;
      }
      try {
        peer1_is.close();
      } catch (IOException _ioe) { Log.e(TAG, "5", _ioe); }
      peer1_is = null;
      try {
        peer1_os.close();
      } catch (IOException _ioe) { Log.e(TAG, "6", _ioe); }
      peer1_os = null;
    }
  }

  public boolean isReady() {
    return peer1_sock != null && peer1_is != null && peer1_os != null
      && peerA_sock != null && peerA_is != null && peerA_os != null;
  }

  @Override
  public void run() {
    if (peer1_is == null) {
      return;
    }
    running = true;

    final SingleForwarder sfa = new SingleForwarder(peer1_is, peerA_os, this);
    final SingleForwarder sfb = new SingleForwarder(peerA_is, peer1_os, this);

    Thread ta = new Thread() {
      @Override
      public void run() {
        sfa.forward();
      }
    };

    Thread tb = new Thread() {
      @Override
      public void run() {
        sfb.forward();
      }
    };

    ta.start();
    tb.start();

    try {
      ta.join();
    } catch (InterruptedException ignored) { }
    try {
      tb.join();
    } catch (InterruptedException ignored) { }

    running = false;
  }
}
