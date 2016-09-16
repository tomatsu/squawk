#!/bin/sh

rm -rf classes output
mkdir -p classes output
javac -target 1.3 -source 1.2 -d classes `find src -name '*.java' -print`
../tools/linux-x86/preverify -d output -classpath ../cldc/classes.target.jar classes
(cd output; jar cfM ../classes.jar .)

export MAIN_CLASS_NAME=Hello
export JARFILE=classes.jar
export BUILD_TOP=build

#sh ../hoge.sh
#sh ../hoge-lpc1768.sh
