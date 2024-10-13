require 'java'
java_import 'trikita.log.Log'

ENV['GEM_PATH']='/tmp/jrubygems'
ENV['GEM_HOME']='/tmp/jrubygems'
require 'socket'
require 'pry-remote'

Pry.config.quiet = true
Pry.config.color = true
