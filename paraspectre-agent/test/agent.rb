require 'java'
#java_import 'trikita.log.Log'

ENV['GEM_PATH']='/tmp/jrubygems'
ENV['GEM_HOME']='/tmp/jrubygems'
require 'socket'
require 'pry-remote'

#Pry.config.quiet = true
Pry.config.color = true

def rem
  binding.remote_pry('127.0.0.1', 6666)
rescue => e
  puts e.backtrace
end

x = rem()
puts x
