package trust.nccgroup.udstest;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.Network;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = "NCC/UDStest/MA";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    File files_dir = getFilesDir();
    if (!files_dir.exists()) {
      files_dir.mkdirs();
    }

    String socket_path = null;
    try {
      socket_path = files_dir.getCanonicalPath() + "/uds.socket";
    } catch (IOException ioe) {
      Log.e(TAG, "failed to get socket_path", ioe);
      return;
    }

    File socket_file = new File(socket_path);
    if (!socket_file.exists()) {
      try {
        socket_file.createNewFile();
        socket_file.setReadable(true, false);
        socket_file.setWritable(true, false);
      } catch (IOException ioe) {
        Log.e(TAG, "failed to create socket_file", ioe);
        return;
      }
    }

    FileDescriptor socket_fd = null;
//    try {
//      socket_fd = new FileInputStream(socket_file).getFD();
//    } catch (IOException ioe) {
//      Log.e(TAG, "failed to get socket_fd", ioe);
//      return;
//    }

    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
    StrictMode.setThreadPolicy(policy);

    LocalSocketAddress lsa = new LocalSocketAddress(socket_path, LocalSocketAddress.Namespace.FILESYSTEM);

    {
      LocalSocket ls = new LocalSocket();
      try {
        ls.bind(lsa);
        socket_fd = ls.getFileDescriptor();
      } catch (IOException ioe) {
        Log.e(TAG, "failed to bind", ioe);
        return;
      }
    }

    LocalServerSocket socket_server = null;
    try {
      socket_server = new LocalServerSocket(socket_fd);
    } catch (IOException ioe) {
      Log.e(TAG, "failed to create socket_server", ioe);
      return;
    }

    LocalSocket ls = null;
    try {
      ls = socket_server.accept();
    } catch (IOException ioe) {
      Log.e(TAG, "failed to accept", ioe);
      return;
    }

    try {
      InputStream is = ls.getInputStream();
      OutputStream os = ls.getOutputStream();

      String data = CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
      Log.e(TAG, "got: " + data);

      os.write("thanks\n".getBytes());
      os.flush();
    } catch (IOException ioe) {
      Log.e(TAG, "error handling socket", ioe);
    } finally {
      try {
        ls.close();
      } catch (IOException ioe) {
        Log.e(TAG, "failed to close ls", ioe);
      }
    }

  }

}
