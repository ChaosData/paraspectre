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
  end

  def tty?
    puts "tty? called"
    true
  end

  def send(data)
    if data == nil
      return
    elsif data == :tty?
      return
    end
    @sock.send(data.to_s, 0)
    puts "send:"
    puts "data: " + data.to_s
  end

  def print(thing1)
    if thing1 == nil
      return
    end
    @sock.send(thing1.to_s, 0)
    puts "print:"
    puts "thing1: " + thing1.to_s
  end

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

  def respond_to_missing?(method_name, include_private = false)
    puts "responding"
    method_name.to_s.start_with?('user_') || super
  end

end

t = Test.new(s)

Pry.config.input = t
Pry.config.output = t

def rem
  binding.pry
rescue => e
  puts e
  puts e.backtrace
end

x = rem()
puts x
