package trust.nccgroup.jrubytest;

import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.jruby.embed.ScriptingContainer;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;

import org.joor.Reflect;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

public class MainActivity extends AppCompatActivity {

  public Reflect testme() {
    return null;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

  }

  @Override
  protected void onStart() {
    super.onStart();

    StrictMode.ThreadPolicy tp = StrictMode.getThreadPolicy();
    StrictMode.ThreadPolicy ntp = new StrictMode.ThreadPolicy.Builder(tp).permitNetwork().build();
    StrictMode.setThreadPolicy(ntp);

    String script = null;
    try {
      script = getScript();
    } catch (Throwable t) {
      Log.e("NCC", "failed to get script.rb asset.", t);
      return;
    }

    //onStartCompiled(script);
    //onStartProvided(script);
    try {
      onStartVanillaReflect(script, App.dcl);
    } catch (Throwable t) {
      Log.e("NCC", "onStartVanillaReflect failed", t);
    }
    StrictMode.setThreadPolicy(tp);
  }


  private void onStartCompiled(String script) {
    Log.e("NCC", "onStartCompiled");

    ScriptingContainer container = new ScriptingContainer();

    container.put("@message", "What's up?");
    container.put("message2", "#YOLO");


    JavaEmbedUtils.EvalUnit unit = container.parse(script);

    IRubyObject ret = unit.run();

    Log.e("NCC", "@msg = " + container.get("@msg"));
    Log.e("NCC", "returned: " + JavaEmbedUtils.rubyToJava(ret));
  }

  private void onStartProvided(String script) {
    Log.e("NCC", "onStartProvided");

    Object container = Reflect.on("org.jruby.embed.ScriptingContainer", App.dcl).create().get();
    Reflect.on(container).call("setClassLoader", (ClassLoader)App.dcl);

    Reflect.on(container).call("put", "@msg", "foo");

    Object unit = Reflect.on(container).call("parse", script, new int[]{}).get();

    Object ret = Reflect.on(unit).call("run").get();

    Object getMsg = Reflect.on(container).call("get", "@msg").get();
    Log.e("NCC", "@msg = " + getMsg);

    Object convertedret = Reflect.on("org.jruby.javasupport.JavaEmbedUtils", App.dcl)
      .call("rubyToJava", ret).get();
    Log.e("NCC", "returned: " + convertedret);
  }

  private void onStartVanillaReflect(String script, ClassLoader dcl) throws Throwable {
    Log.e("NCC", "onStartVanillaReflect");

    Proxy.init(dcl);
    Object container = Proxy.ScriptingContainer.getConstructor().newInstance();
    Proxy.setClassLoader.invoke(container, dcl);

    Proxy.runScriptlet.invoke(container, script);

    Object[] ctx = new Object[]{
      null, //method
      null, //thiz
      null  //args
    };

    Proxy.put.invoke(container, "@msg", "foo4");
    Proxy.put.invoke(container, "@__ctx__", ctx);

    Object unit = Proxy.parse.invoke(container, "" +
      "method = @__ctx__[0]\n" +
      "thiz = @__ctx__[1]\n" +
      "args = @__ctx__[2]\n" +
      "binding.remote_pry '127.0.0.1', 4444\n",

      new int[]{}
    );

    Proxy.put.invoke(container, "@msg", "foo5");

//    for (int i = 0; i < 2; i++) {
//      ctx[2] = new Object[]{"arg" + i, i};
//      Proxy.put.invoke(container, "@msg", Proxy.get.invoke(container, "@msg"));
//
//      Object ret = Proxy.run.invoke(unit);
//      Object convertedret = Proxy.rubyToJava.invoke(null, ret);
//      Log.e("NCC", "returned: " + convertedret);
//    }

    Object[] ctx1 = new Object[]{
      "gg", //method
      "zz", //thiz
      new Object[]{"hello", 5}  //args
    };

    Object[] ctx2 = new Object[]{
      "__", //method
      "{}", //thiz
      new Object[]{"goodbye", 6}  //args
    };



    Object getMsg = Proxy.get.invoke(container, "@msg");
    Log.e("NCC", "@msg = " + getMsg);

  }

  private String getScript() throws Throwable {
    String script_template = null;
    StringBuilder templatesb = new StringBuilder();

    InputStream i = getApplicationContext().getAssets().open("script.rb");
    BufferedReader r = new BufferedReader(new InputStreamReader(i, "UTF-8"));
    String line;

    while ((line = r.readLine()) != null) {
      templatesb.append(line);
      templatesb.append('\n');
    }
    r.close();
    script_template = templatesb.toString();

    StringBuilder scriptsb = new StringBuilder();
    Formatter formatter = new Formatter(scriptsb);

    String path = getApplicationContext().getFilesDir().getPath();
    //String ip = getIPAddress();

    formatter.format(script_template, path, path, path, path/*, ip*/);
    return scriptsb.toString();
  }

  private static String getIPAddress() {
    try {
      List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
      for (NetworkInterface intf : interfaces) {
        if (!intf.getDisplayName().equals("wlan0")) {
          continue;
        }
        List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
        for (InetAddress addr : addrs) {
          if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
            return addr.getHostAddress();
          }
        }
      }
    } catch (Throwable t) {
      Log.e("NCC", "getIPAddress error", t);
    }
    return "";
  }

  public static class Proxy {
    public static Class<?> ScriptingContainer = null;
    public static Method setClassLoader = null;
    public static Method runScriptlet = null;
    public static Method parse = null;
    public static Method get = null;
    public static Method put = null;

    public static Class<?> JavaEmbedUtils_EvalUnit = null;
    public static Method run = null;

    public static Class<?> IRubyObject = null;
    public static Class<?> JavaEmbedUtils = null;
    public static Method rubyToJava = null;


    public static void init(ClassLoader dcl) {
      try {
        ScriptingContainer = dcl.loadClass("org.jruby.embed.ScriptingContainer");
        setClassLoader = ScriptingContainer.getDeclaredMethod("setClassLoader", ClassLoader.class);
        runScriptlet = ScriptingContainer.getDeclaredMethod("runScriptlet", String.class);
        parse = ScriptingContainer.getDeclaredMethod("parse", String.class, int[].class);
        get = ScriptingContainer.getDeclaredMethod("get", String.class);
        put = ScriptingContainer.getDeclaredMethod("put", String.class, Object.class);

        JavaEmbedUtils_EvalUnit = dcl.loadClass("org.jruby.javasupport.JavaEmbedUtils$EvalUnit");
        run = JavaEmbedUtils_EvalUnit.getDeclaredMethod("run");

        JavaEmbedUtils = dcl.loadClass("org.jruby.javasupport.JavaEmbedUtils");
        IRubyObject = dcl.loadClass("org.jruby.runtime.builtin.IRubyObject");
        rubyToJava = JavaEmbedUtils.getDeclaredMethod("rubyToJava", IRubyObject);
      } catch (Throwable t) {
        Log.e("NCC", "failed to init Proxy", t);
      }
    }
  }
}
