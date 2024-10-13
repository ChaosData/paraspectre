package trust.nccgroup.parasect


class stub {
  @Suppress("UNUSED")
  fun pidToPath(@Suppress("UNUSED_PARAMETER") pid: Int): String {
    return "/tmp/test2.sock"
  }
  
  @Suppress("UNUSED")
  fun getDaemon(): String {
    return "127.0.0.1:4444"
  }
  
  @Suppress("UNUSED")
  fun getPinger(): String {
    return "127.0.0.1:4446"
  }
  
  @Suppress("UNUSED")
  fun getDruby(): String {
    return "127.0.0.1:4448"
  }
  
}

fun main(args: Array<String>) {
  
  val s = Server.start(Reflector(stub(),
        stub::class.java.getMethod("pidToPath", Int::class.java),
        stub::class.java.getMethod("getDaemon"),
        stub::class.java.getMethod("getDruby"),
        stub::class.java.getMethod("getPinger")
      ),
      "/tmp/parasect.ping.sock",
      "/tmp/parasect.out.sock"
  )
  
  if (s != null) {
    println("running")
  }

//  if (s != null) {
//    println("stopping")
//    s.stop()
//  }
}

