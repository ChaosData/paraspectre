package trust.nccgroup.paraspectre.android;

import android.util.Log;

public class Utils {

  private static final String TAG = "PS/Utils";

  public static String primitiveToHuman(char c) {
    switch (c) {
      case 'B': {
        return "byte";
      }
      case 'S': {
        return "short";
      }
      case 'I': {
        return "int";
      }
      case 'J': {
        return "long";
      }
      case 'F': {
        return "float";
      }
      case 'D': {
        return "double";
      }
      case 'C': {
        return "char";
      }
      case 'Z': {
        return "boolean";
      }
      case 'V': {
        return "void";
      }
      default: {
        return null;
      }
    }
  }

  public static String javaToHuman(Class<?> c) {
    String j = c.getName();
    try {
      if (j.startsWith("[")) {
        int pos = j.indexOf("L");
        if (pos != -1) {
          StringBuilder sb = new StringBuilder(j.length() - (pos+1) - 1 + (pos*2));
          sb.append(j.substring(pos+1, j.length()-1));
          for (int i = 0; i < pos; i++) {
            sb.append("[]");
          }
          return sb.toString();
        } else {
          StringBuilder sb = new StringBuilder((j.length()-1)*2 + 7);
          sb.append(primitiveToHuman(j.charAt(j.length()-1)));
          for (int i = 0; i < j.length()-1; i++) {
            sb.append("[]");
          }
          return sb.toString();
        }
      }
    } catch (Throwable t) {
      Log.e(TAG, "something strange happened", t);
    }
    return j;
  }

  private static final char[] hexArray = "0123456789ABCDEF".toCharArray();
  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int i = 0; i < bytes.length; i++) {
      int v = bytes[i] & 0xFF;
      hexChars[i * 2] = hexArray[v >>> 4];
      hexChars[i * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }

  public static byte[] hexToBytes(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
        + Character.digit(s.charAt(i+1), 16));
    }
    return data;
  }

}
