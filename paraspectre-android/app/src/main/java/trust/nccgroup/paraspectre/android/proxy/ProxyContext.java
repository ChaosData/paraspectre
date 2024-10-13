package trust.nccgroup.paraspectre.android.proxy;

public class ProxyContext {

  public static final int SECRET_LENGTH = 32;

  public String pkg;
  public int port;
  public String secret;
  public int count = 2;

  public ProxyContext(String _pkg, int _port, String _secret) {
    pkg = _pkg;
    port = _port;
    secret = _secret;
  }
}
