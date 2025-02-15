package org.ruboto;

import android.util.Log;

import com.android.dx.Version;
import com.headius.android.dex.DexClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import dalvik.system.DexClassLoader;

@SuppressWarnings("unused") //used via reflection
public class DalvikProxyClassFactory extends org.jruby.javasupport.proxy.JavaProxyClassFactory {
  private static final String TAG = "DalvikProxyClassFactory";

  private static final String DEX_IN_JAR_NAME = "classes.dex";
  private static final Attributes.Name CREATED_BY = new Attributes.Name("Created-By");

  public Class invokeDefineClass(ClassLoader loader, String className, byte[] data) {
    String cachePath = System.getProperty("jruby.class.cache.path");
    if (cachePath != null) {
      byte[] dalvikByteCode = new DexClient().classesToDex(
        new String[] { className.replace('.', '/') + ".class" }, new byte[][] { data });
      String jarFileName = cachePath + "/" + className.replace('.', '/') + ".jar";
      createJar(jarFileName, dalvikByteCode);
      try {
        return new DexClassLoader(jarFileName, cachePath, null, loader)
          .loadClass(className);
      } catch (ClassNotFoundException e1) {
        Log.e(TAG, "error loading class: " + className, e1);
        e1.printStackTrace();
      }
    }
    return null;
  }

  private static boolean createJar(String fileName, byte[] dexArray) {
    File parentFile = new File(fileName).getParentFile();
    if (!parentFile.exists() && !parentFile.mkdirs()) {
      Log.e(TAG, "failed to create directory " + parentFile + " for " + fileName);
    }
    try {
      TreeMap<String, byte[]> outputResources = new TreeMap<>();
      Manifest manifest = makeManifest();
      OutputStream out = (fileName.equals("-") || fileName.startsWith("-.")) ? System.out
        : new FileOutputStream(fileName);
      JarOutputStream jarOut = new JarOutputStream(out, manifest);
      outputResources.put(DEX_IN_JAR_NAME, dexArray);
      try {
        for (Map.Entry<String, byte[]> e : outputResources.entrySet()) {
          String name = e.getKey();
          byte[] contents = e.getValue();
          JarEntry entry = new JarEntry(name);
          entry.setSize(contents.length);
          jarOut.putNextEntry(entry);
          jarOut.write(contents);
          jarOut.closeEntry();
        }
      } finally {
        jarOut.finish();
        jarOut.flush();
        if (out != null) {
          out.flush();
          if (out != System.out) {
            out.close();
          }
        }
        jarOut.close();
      }
    } catch (Exception ex) {
      Log.e(TAG, "error writing jar: " + fileName, ex);
      return false;
    }
    return true;
  }

  private static Manifest makeManifest() throws IOException {
    Manifest manifest = new Manifest();
    Attributes attribs = manifest.getMainAttributes();
    attribs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
    attribs.put(CREATED_BY, "dx " + Version.VERSION);
    attribs.putValue("Dex-Location", DEX_IN_JAR_NAME);
    return manifest;
  }

}
