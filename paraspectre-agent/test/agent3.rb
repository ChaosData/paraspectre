#require 'java'
#java_import 'trikita.log.Log'

ENV['GEM_PATH']='/tmp/jrubygems'
ENV['GEM_HOME']='/tmp/jrubygems'
require 'socket'
require 'pry-remote'

#Pry.config.quiet = true
Pry.config.color = true

s = TCPSocket.new '127.0.0.1', 4454


class Test
  def initialize(sock)
    @sock = sock
    @count = 1
  end

  def tty?
    true
  end

  def send(data)
    if data == nil
      return
    elsif data == :tty?
      return
      #data = "[#{@count}] pry(main)>\n"
      #@count += 1
    end
    @sock.send(data.to_s.gsub("\n", "\r\n"), 0)
    #@sock.send(name.to_s, 0)
    puts "send:"
    puts "data: " + data.to_s
    puts "data.class: " + data.class.to_s
  end

  def print(thing1)
    if thing1 == nil
      return
    end
    @sock.send(thing1.to_s.gsub("\n", "\r\n"), 0)
    #@sock.send(thing1.to_s, 0)
    puts "print:"
    puts "thing1: " + thing1.to_s
  end

  #def recv(size)
  #  puts "recv:"
  #  ret = @sock.recv(size) #.gsub("\n", "\r\n")
  #  puts "ret: " + ret.to_s
  #  ret
  #end

  #def readline(sep=$/)
  #  sio = StringIO.new(@sock.recv(2048))
  #  sio.readline(sep)
  #end

  def method_missing(method, *args, &block)
    puts "method_missing:"
    puts "method: " + method.to_s
    puts "args: " + args.to_s
    puts "block: " + block.to_s
    begin
      ret = @sock.__send__(method, *args, &block)
    rescue =>e
      puts e
      puts e.backtrace
      raise e
    end
    puts "ret: " + ret.to_s
    ret
  end
end

t = Test.new(s)

Pry.config.input = t
Pry.config.output = t

def rem
  binding.pry
  #binding.remote_pry('127.0.0.1', 6666)
rescue => e
  puts e
  puts e.backtrace
end

x = rem()
puts x
