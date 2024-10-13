#!/bin/sh

mkdir /tmp/jrubygems
wget https://s3.amazonaws.com/jruby.org/downloads/1.7.26/jruby-complete-1.7.26.jar
GEM_PATH=/tmp/jrubygems GEM_HOME=/tmp/jrubygems java -jar ./jruby-complete-1.7.26.jar -S gem install -i /tmp/jrubygems rb-readline pry pry-remote
