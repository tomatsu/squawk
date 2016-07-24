#!/bin/sh

if [ $# = 0 ]; then
    jarfile="helloworld/classes.jar"
else
    jarfile=$1
fi

#PROP=build-mbed-minimal.properties
PROP=build-mbed.properties

CFLAGS="-flto -Os"
LDLAGS="-flto -Os"

cflags=`for i in $CFLAGS; do echo -cflags:$i; done`
ldlags=`for i in $LDFLAGS; do echo -ldflags:$i; done`

(cd builder; sh bld.sh)
./d.sh -override $PROP clean
./d.sh -override $PROP
./d.sh -override $PROP $cflags $ldflags  \
       rom -strip:a cldc $jarfile

strip squawk
