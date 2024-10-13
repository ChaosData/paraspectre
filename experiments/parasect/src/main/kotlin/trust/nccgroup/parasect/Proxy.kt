package trust.nccgroup.parasect

import com.google.common.collect.Maps
import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.epoll.*
import io.netty.channel.unix.DomainSocketAddress
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentMap

class Proxy {
  
  
  enum class Type {
    UnixToTcp,
    TcpToUnix,
    Pinger;
  }
  
  enum class State {
    AUTH,
    PROXY;
  }
  
  
  companion object {
    val keymap: ConcurrentMap<String, String> = Maps.newConcurrentMap<String,String>()
  
    val hexArray = "0123456789ABCDEF".toCharArray()
  
    fun setup(type: Type, reflector: Reflector, bossGroup: EpollEventLoopGroup, workerGroup: EpollEventLoopGroup): ServerBootstrap {
  
      val bt = ServerBootstrap()
      bt.group(bossGroup, workerGroup)
          .option(ChannelOption.SO_BACKLOG, 100)
          .childHandler(object : ChannelInitializer<Channel>() {
            @Throws(Exception::class)
            public override fun initChannel(ch: Channel) {
              val p = ch.pipeline()
              p.addLast(InboundHandler(type, reflector))
            }
          })
      
      when (type) {
        Type.TcpToUnix -> {
          bt.channel(EpollServerSocketChannel::class.java)
          bt.option(ChannelOption.SO_REUSEADDR, true)
        }
        Type.UnixToTcp -> {
          bt.channel(EpollServerDomainSocketChannel::class.java)
        }
        Type.Pinger -> {
          bt.channel(EpollServerDomainSocketChannel::class.java)
        }
      }
      return bt
    }
  
    fun bytesToHex(bytes: ByteArray): String {
      val hexChars = CharArray(bytes.size * 2)
      for (j in bytes.indices) {
        val v = bytes[j].toInt() and 0xFF
        hexChars[j * 2] = hexArray[v.ushr(4)]
        hexChars[j * 2 + 1] = hexArray[v and 0x0F]
      }
      return String(hexChars)
    }
  
  }
  
  
  class InboundHandler(private val type: Type,
                       private val reflector: Reflector) : ChannelInboundHandlerAdapter() {
    
    val buf = ByteArray(16)
    var bufc = 0
    
    private var state: State = State.AUTH
    
    private var inboundChannel: Channel? = null
    private var outboundChannel: Channel? = null
    
    override fun channelActive(ctx: ChannelHandlerContext) {
      if (outboundChannel != null) {
        return
      }
      inboundChannel = ctx.channel()
    }
    
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
      val input: ByteBuf = msg as ByteBuf
      
      when (state) {
        State.AUTH -> {
          var socket_path: String? = null
          var asciikey: String? = null
  
          if (type == Type.TcpToUnix) {
            val l = input.readableBytes()
            val tocopy = if (l + bufc > buf.size) {
              buf.size - bufc
            } else {
              l
            }
            
            input.readBytes(buf, bufc, tocopy)
            bufc += tocopy
            
            
            if (bufc < 16) {
              return
            }
            
            asciikey = bytesToHex(buf)
            
            socket_path = keymap.remove(asciikey)
            if (socket_path == null) {
              inboundChannel!!.close()
              return
            }
          } else if (type == Type.Pinger) {
            if (input.readableBytes() < 16) {
              return
            }
  
            val buf: ByteArray = ByteArray(16)
  
            input.readBytes(buf)
  
            asciikey = bytesToHex(buf)
  
            val pc = (inboundChannel as EpollDomainSocketChannel).peerCredentials()
            socket_path = reflector.pidToPath(pc.uid())
            
            if (socket_path == null) {
              inboundChannel!!.close()
              return
            }
  
            keymap.put(asciikey, socket_path)
          }
          val b = Bootstrap()
          b.group(inboundChannel!!.eventLoop())
              .handler(OutboundHandler(inboundChannel!!))
              .option(ChannelOption.AUTO_READ, false)
    
          val f = if (type == Type.TcpToUnix) {
            b.channel(EpollDomainSocketChannel::class.java)
            b.connect(DomainSocketAddress(socket_path))
          } else if (type == Type.UnixToTcp) {
            val druby = reflector.getDruby().split(":")
            b.channel(EpollSocketChannel::class.java)
            b.connect(InetSocketAddress(druby[0], druby[1].toInt()))
          } else {
            val pinger = reflector.getPinger().split(":")
            b.channel(EpollSocketChannel::class.java)
            b.connect(InetSocketAddress(pinger[0], pinger[1].toInt()))
          }
          
          outboundChannel = f.channel()
    
          f.addListener({ future ->
            if (future.isSuccess) {
              if (outboundChannel!!.isActive) {
                val tosend: Any = if (type == Type.Pinger) {
                  Unpooled.wrappedBuffer(asciikey!!.toByteArray())
                } else {
                  input
                }
                
                outboundChannel!!.writeAndFlush(tosend).addListener({ future ->
                  if (type == Type.Pinger) {
                    outboundChannel!!.close()
                    inboundChannel!!.close()
                  } else {
                    if (future.isSuccess) {
                      ctx.channel().read()
                    } else {
                      outboundChannel!!.close()
                      inboundChannel!!.close()
                    }
                  }
                })
              }
              if (type == Type.Pinger) {
                outboundChannel!!.close()
                inboundChannel!!.close()
              } else {
                inboundChannel!!.read()
              }
            } else {
              outboundChannel!!.close()
              inboundChannel!!.close()
            }
          })
          
          state = State.PROXY
        }
        State.PROXY -> {
          if (type == Type.Pinger) {
            outboundChannel!!.close()
            inboundChannel!!.close()
          } else {
            if (outboundChannel!!.isActive) {
              outboundChannel!!.writeAndFlush(input).addListener({ future ->
                if (future.isSuccess) {
                  ctx.channel().read()
                } else {
                  outboundChannel!!.close()
                }
              })
            }
          }
        }
      }

      
    }
  
    override fun channelInactive(ctx: ChannelHandlerContext) {
      if (outboundChannel != null) {
        closeOnFlush(outboundChannel as Channel)
      }
    }
    
    companion object {
      
      /**
       * Closes the specified channel after all queued write requests are flushed.
       */
      internal fun closeOnFlush(ch: Channel) {
        if (ch.isActive) {
          ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(io.netty.channel.ChannelFutureListener.CLOSE)
        }
      }
    }
  }
  
  class OutboundHandler(private val inboundChannel: Channel) : ChannelInboundHandlerAdapter() {
    
    override fun channelActive(ctx: ChannelHandlerContext) {
      ctx.read()
    }
    
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
      inboundChannel.writeAndFlush(msg).addListener({ future ->
        if (future.isSuccess) {
          ctx.channel().read()
        } else {
          inboundChannel.close()
        }
      })
    }
    
    override fun channelInactive(ctx: ChannelHandlerContext) {
      InboundHandler.closeOnFlush(inboundChannel)
    }
    
  }
}
