#!/bin/sh

LANG=C
PROP=build-mbed.properties
MAIN_CLASS_NAME=com.sun.squawk.Dine
TARGET=bbc-microbit-classic-gcc-nosd

CFLAGS="-DNRF51 -DDEFAULT_RAM_SIZE=13*1024"
cflags="`for i in $CFLAGS; do echo -cflags:$i; done`"

(cd builder; sh bld.sh) || exit 1
./d.sh -override $PROP clean || exit 1
./d.sh -override $PROP -DMAIN_CLASS_NAME=\"${MAIN_CLASS_NAME}\" || exit 1
./d.sh -override $PROP -fork shrink ${MAIN_CLASS_NAME} app/classes || exit 1
./d.sh -override $PROP \
       -DMAIN_CLASS_NAME=\"${MAIN_CLASS_NAME}\" \
       -comp:yotta \
       $cflags \
       rom -strip:a cldc app/classes.jar || exit 1

# set up yotta

TOP=build
SOURCE=$TOP/source
rm -rf $TOP
mkdir -p $SOURCE
cp module.json .yotta.json $TOP
cp squawk.suite.c cflags.cmake $SOURCE

tar cf - -C vmcore/src \
	`(cd vmcore/src; echo rts/gcc-arm vm/fp vm/*.h vm/squawk.c vm/util vm/*.c.inc)` \
	| (cd $SOURCE; tar xf -)

# build 

cd build
yotta target ${TARGET}
yotta up
patch -p 0 < ../compiler_abstraction.patch
yotta build 2>&1 > log
