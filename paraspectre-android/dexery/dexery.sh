#!/bin/bash

[ -z "$ANDROID_HOME" ] && { echo "Need to set \$ANDROID_HOME"; exit 1; }

SCRIPT=$0
cd `dirname $SCRIPT`
SCRIPT=`basename $SCRIPT`

while [ -L "$SCRIPT" ]
do
  SCRIPT=`readlink $SCRIPT`
  cd `dirname $SCRIPT`
  SCRIPT=`basename $SCRIPT`
done
SCRIPTDIR=`pwd -P`

cd "$SCRIPTDIR"
mkdir -p "../app/src/main/assets/"
rm -rf \
  "../app/src/main/assets/runtime.dex.jar" \
  "../app/src/main/assets/jrubystuff.jar" \
  "./jrubystuff" \
  "./runtime"\
  "./dx.jar"\
  "./classes.jar" #\
#  "./jruby-complete.jar"

cd ".."
./gradlew :jdruby:assemble
cd "$SCRIPTDIR"

unzip "../jdruby/build/outputs/aar/jdruby-release.aar" classes.jar

if [ ! -f "./jruby-complete.jar" ]; then
  #curl -o "./jruby-complete.jar" 'https://s3.amazonaws.com/jruby.org/downloads/1.7.26/jruby-complete-1.7.26.jar'
  #curl -o "./jruby-complete.jar" 'https://s3.amazonaws.com/jruby.org/downloads/1.7.25/jruby-complete-1.7.25.jar'
  curl -o "./jruby-complete.jar" 'https://s3.amazonaws.com/jruby.org/downloads/1.7.27/jruby-complete-1.7.27.jar'
  zip -d ./jruby-complete.jar 'org/jruby/util/SunSignalFacade$1.class'
  zip -d ./jruby-complete.jar 'org/jruby/util/SunSignalFacade$JRubySignalHandler.class'
  zip -d ./jruby-complete.jar 'org/jruby/util/SunSignalFacade.class'
  zip -d ./jruby-complete.jar 'META-INF/jruby.home/lib/ruby/shared/json/ext.rb'

fi


#cp ~/.gradle/caches/modules-2/files-2.1/com.google.android.tools/dx/1.7/*/dx-1.7.jar ./dx.jar

SDK=$(ls $ANDROID_HOME/build-tools/ | sort -t '.' -k 1,1 -k 2,2 -k 3,3 -k 4,4 -g | tail -n 1 | sort -t '.' -k 1,1 -k 2,2 -k 3,3 -k 4,4 -g | tail -n 1)

cp $ANDROID_HOME/build-tools/$SDK/lib/dx.jar ./

java -Xmx2048M -jar $ANDROID_HOME/build-tools/$SDK/lib/dx.jar --dex --multi-dex --output=../app/src/main/assets/runtime.dex.jar jruby-complete.jar dx.jar classes.jar

unzip -d jrubystuff ../app/src/main/assets/runtime.dex.jar 'META-INF/jruby.home/*'
mv jrubystuff/META-INF/jruby.home jrubystuff/jruby.home
rm -rf jrubystuff/META-INF
#cp -r ../jrubygems jrubystuff/

mkdir jrubystuff/jrubygems
GEM_PATH=jrubystuff/jrubygems GEM_HOME=jrubystuff/jrubygems java -jar jruby-complete.jar -S gem install rb-readline pray-remote

zip -d "$SCRIPTDIR/../app/src/main/assets/runtime.dex.jar" META-INF
rm jrubystuff/jruby.home/lib/ruby/shared/readline.rb # kill it with fire
cp -r ../jdruby/src/main/ruby/* jrubystuff/
cd jrubystuff
zip -r ../../app/src/main/assets/jrubystuff.jar .
cd ..

for p in $(unzip -l ../app/src/main/assets/jrubystuff.jar| awk '/\.jar$/ {if(NR>1) print $NF}'); do
  zip -d ../app/src/main/assets/jrubystuff.jar "$p"
done
