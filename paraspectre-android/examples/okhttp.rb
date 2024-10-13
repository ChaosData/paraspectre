# okhttp3.OkHttpClient$Builder::build
this.proxy(java.net.Proxy.new(java.net.Proxy::Type.valueOf('HTTP'),java.net.InetSocketAddress.new('127.0.0.1', 8080)))

trustAllCerts = Class.new() {
  include javax.net.ssl.X509TrustManager
  def checkClientTrusted(chain,authType)
  end
  def checkServerTrusted(chain,authType)
  end
  def getAcceptedIssuers()
    [].to_java(java.security.cert.X509Certificate)
  end
}.new

sslContext = javax.net.ssl.SSLContext.getInstance('SSL')
sslContext.init(nil, [trustAllCerts], java.security.SecureRandom.new)
sslSocketFactory = sslContext.getSocketFactory()

this.sslSocketFactory(sslSocketFactory, trustAllCerts)
verifier = Class.new() {
  include javax.net.ssl.HostnameVerifier
  def verify(hostname,session)
    true
  end
}.new
this.hostnameVerifier(verifier)
