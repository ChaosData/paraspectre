mkdir -p ../app/src/main/assets/
rm ../app/src/main/assets/jruby-complete.dex.jar classes.dex
rm -rf jruby-complete
#cp ~/.gradle/caches/modules-2/files-2.1/org.jruby/jruby-complete/9.1.2.0/1ba9ec078d5c46583688110ddc7dec912d4c7763/jruby-complete-9.1.2.0.jar ./jruby-complete.jar
cp /Users/jtd/.gradle/caches/modules-2/files-2.1/org.jruby/jruby-complete/1.7.25/8eb234259ec88edc05eedab05655f458a84bfcab/jruby-complete-1.7.25.jar ./jruby-complete.jar
zip -r ./jruby-complete.jar ./trust
~/android/tools/dex2jar-2.0/d2j-jar2dex.sh -o classes.dex jruby-complete.jar
unzip -d jruby-complete jruby-complete.jar
cp classes.dex jruby-complete
rm jruby-complete/META-INF/MANIFEST.MF
cp META-INF/MANIFEST.MF jruby-complete/META-INF/MANIFEST.MF
cd jruby-complete
find . -name '*.class' -delete
zip -r ../../app/src/main/assets/jruby-complete.dex.jar .
