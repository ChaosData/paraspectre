#!/bin/sh

cd dexery
./dexery.sh
if [ $? -ne 0 ]
then
  exit 1
fi
cd ..
./gradlew assembleDebug
