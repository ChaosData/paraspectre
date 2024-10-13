#!/bin/sh

adb -d reverse tcp:8080 tcp:8080 # burp

adb -d forward tcp:8088 tcp:8088 # phone-side webapp
adb -d reverse tcp:4442 tcp:4442 # client-side ping listener
# 4443: client-local ping proxy
adb -d forward tcp:4444 tcp:4444 # phone-side druby "forwarder" proxy
# 4445: phone-local druby "reverse" proxy
#adb -d reverse tcp:4446 tcp:4446 # client-side druby splitter proxy
# 4450+: client-local druby listeners
