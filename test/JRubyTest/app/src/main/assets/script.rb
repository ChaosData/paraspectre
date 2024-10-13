require 'java'
java_import 'android.util.Log'

puts $LOAD_PATH
$LOAD_PATH.clear
$LOAD_PATH << 'file:%s/jrubystuff.jar!/jruby.home/lib/ruby/shared'
$LOAD_PATH << 'file:%s/jrubystuff.jar!/jruby.home/lib/ruby/1.9'
$LOAD_PATH << 'file:%s/jrubystuff.jar!/jruby.home/lib/ruby/2.0'

require 'rubygems'
Gem.use_paths(nil, Gem.path << 'file:%s/jrubystuff.jar!/jrubygems')

require 'pry-remote'
#binding.remote_pry '127.0.0.1', 4444