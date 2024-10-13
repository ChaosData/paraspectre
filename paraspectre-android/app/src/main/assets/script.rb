require 'java'

def android
  Java::Android
end

java_import 'android.util.Log'

ENV['HOME']='/data/local/tmp'

$LOAD_PATH.clear
$LOAD_PATH << 'file:%s/jrubystuff.jar!/jruby.home/lib/ruby/shared'
$LOAD_PATH << 'file:%s/jrubystuff.jar!/jruby.home/lib/ruby/1.9'
#$LOAD_PATH << 'file:%s/jrubystuff.jar!/jruby.home/lib/ruby/2.0'

require 'rubygems'
Gem.use_paths(nil, Gem.path << 'file:%s/jrubystuff.jar!/jrubygems')

require 'securerandom'
require 'socket'
require 'pry-remote'

def stack_trace
  begin
    java.net.URL.new("!!")
  rescue NativeException => e
    e.backtrace
  end
end

Pry.config.quiet = true
Pry.config.color = true

