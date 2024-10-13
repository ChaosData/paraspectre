require 'java'

#def android
#  Java::Android
#end

#java_import 'android.util.Log'

#ENV['HOME']='/tmp/jrubygems'

#$LOAD_PATH.clear
#$LOAD_PATH << 'file:/Users/jtd/projects/research/workspace/paraspectre/paraspectre-android/app/src/main/assets/jrubystuff.jar!/jruby.home/lib/ruby/shared'
#$LOAD_PATH << 'file:/Users/jtd/projects/research/workspace/paraspectre/paraspectre-android/app/src/main/assets/jrubystuff.jar!/jruby.home/lib/ruby/1.9'
#$LOAD_PATH << 'file:/Users/jtd/projects/research/workspace/paraspectre/paraspectre-android/app/src/main/assets/jrubystuff.jar!/jruby.home/lib/ruby/2.0'

require 'rubygems'
#Gem.use_paths(nil, Gem.path << 'file:/Users/jtd/projects/research/workspace/paraspectre/paraspectre-android/app/src/main/assets/jrubystuff.jar!/jrubygems')

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

def rem
  binding.remote_pry('127.0.0.1', 6666)
rescue => e
  puts e.backtrace
end

x = rem()
puts x
