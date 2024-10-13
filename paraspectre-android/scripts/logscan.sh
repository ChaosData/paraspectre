#!/bin/sh

if [ $# -gt 1 ]; then
  adb -d logcat | grep -E '(PS/|System\.out|System\.err|'"$1"')'
else
  adb -d logcat | grep -E '(PS/|System\.out|System\.err)'
fi
