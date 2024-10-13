package trust.nccgroup.parasect

import io.netty.channel.Channel
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.unix.DomainSocketAddress
import java.io.File
import java.net.InetSocketAddress

/**
 * Created by jtd on 1/16/17.
 */
class Server(private val tcpunix: Channel?,
             private val unixtcp: Channel?,
             private val unixreg: Channel?,
             private val bossGroup: EpollEventLoopGroup,
             private val workerGroup: EpollEventLoopGroup) {
  
  fun stop() {
    if (tcpunix != null) {
      tcpunix.close().sync()
    }
  
    if (unixtcp != null) {
      unixtcp.close().sync()
    }
  
    if (unixreg != null) {
      unixreg.close().sync()
    }
    bossGroup.shutdownGracefully()
    workerGroup.shutdownGracefully()
  }
  
  companion object {
    fun start(reflector: Reflector, ping_path: String, out_path: String): Server? {
      val bossGroup = EpollEventLoopGroup(1)
      val workerGroup = EpollEventLoopGroup()
    
      var tuc: Channel? = null
      var utc: Channel? = null
      var urc: Channel? = null
    
      try {
        val daemon = reflector.getDaemon().split(":")
        val btu = Proxy.setup(Proxy.Type.TcpToUnix, reflector, bossGroup, workerGroup)
        val tu = btu.bind(InetSocketAddress(daemon[0], daemon[1].toInt())).sync()
      
        val bur = Proxy.setup(Proxy.Type.Pinger, reflector, bossGroup, workerGroup)
        File(ping_path).delete()
        val ur = bur.bind(DomainSocketAddress(ping_path)).sync()
      
        val but = Proxy.setup(Proxy.Type.UnixToTcp, reflector, bossGroup, workerGroup)
        File(out_path).delete()
        val ut = but.bind(DomainSocketAddress(out_path)).sync()
      
        tuc = tu.channel()
        utc = ut.channel()
        urc = ur.channel()
        
        return Server(tuc, utc, urc, bossGroup, workerGroup)
        
      } catch (t: Throwable) {
        t.printStackTrace()
        
        if (tuc != null) {
          tuc.closeFuture().sync()
        }
  
        if (utc != null) {
          utc.closeFuture().sync()
        }
  
        if (urc != null) {
          urc.closeFuture().sync()
        }
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
      }
      return null;
    }
  }
}