package trust.nccgroup.parasect

class Reflector(private val obj: Any,
                private val pidToPath: java.lang.reflect.Method,
                private val getDaemon: java.lang.reflect.Method,
                private val getDruby: java.lang.reflect.Method,
                private val getPinger: java.lang.reflect.Method) {
  
  fun pidToPath(pid: Int): String? {
    return pidToPath.invoke(obj, pid) as String
  }
  
  fun getDaemon(): String {
    return getDaemon.invoke(obj) as String
  }
  
  fun getPinger(): String {
    return getPinger.invoke(obj) as String
  }
  
  fun getDruby(): String {
    return getDruby.invoke(obj) as String
  }
  
}