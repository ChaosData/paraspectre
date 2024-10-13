require 'java'

def android
  Java::Android
end

java_import 'android.util.Log'

ENV['HOME']='/data/dalvik-cache/paraspectre'

$LOAD_PATH.clear
$LOAD_PATH << 'file:/data/dalvik-cache/paraspectre/jrubystuff.jar!/jruby.home/lib/ruby/shared'
$LOAD_PATH << 'file:/data/dalvik-cache/paraspectre/jrubystuff.jar!/jruby.home/lib/ruby/1.9'

require 'rubygems'
Gem.use_paths(nil, Gem.path << 'file:/data/dalvik-cache/paraspectre/jrubystuff.jar!/jrubygems')

require 'securerandom'
require 'socket'
require 'pray-remote'

def stack_trace
  begin
    java.net.URL.new('!!')
  rescue NativeException => e
    e.backtrace[12..-1]
  end
end

def get_field(obj, name)
  ret = obj.getClass.getDeclaredField(name)
  ret.accessible = true
  ret.get(obj)
end

Pry.config.quiet = true
Pry.config.color = true

ENV['HOME']='/data/local/tmp'
