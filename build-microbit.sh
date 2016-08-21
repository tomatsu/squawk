#!/bin/sh

# common settings
MAIN_CLASS_NAME=Hello
JARFILE=helloworld/classes.jar

TARGET=bbc-microbit-classic-gcc-nosd
CFLAGS="-DNRF51 -DDEFAULT_RAM_SIZE=13*1024 -DMAIN_CLASS_NAME=${MAIN_CLASS_NAME}"
PROP=build-mbed.properties
TOP=build
SOURCE=$TOP/source

case "$1" in
"clean" )
	./d.sh -override $PROP -q clean || exit 1
	rm -rf ${TOP}
	exit ;;
esac

skip_stage_1=true

if [ ! -d ${TOP} ]; then
	skip_stage_1=false
fi

# stage 1
if [ x${skip_stage_1} != "xtrue" ]; then
	echo "stage 1...."

	(cd builder; sh bld.sh) || exit 1
	./d.sh -override $PROP -q clean || exit 1
	./d.sh -override $PROP -q || exit 1

# set up yotta
	rm -rf ${TOP}
	mkdir -p $SOURCE
	cp module.json .yotta.json $TOP
	cp cflags.cmake $SOURCE

	(cd ${TOP}; yotta target ${TARGET} && \
		 yotta up && \
		 patch -p 0 < ../compiler_abstraction.patch)
fi

# stage 2
echo "stage 2...."

cflags="`for i in $CFLAGS; do echo -cflags:$i; done`"

./d.sh -override $PROP -q -fork shrink ${MAIN_CLASS_NAME} ${JARFILE} || exit 1
./d.sh -override $PROP -q \
       -comp:yotta \
       $cflags \
       rom -strip:a cldc ${JARFILE} || exit 1

# copy files to yotta build directory

cp squawk.suite.c $SOURCE

tar cf - -C vmcore/src \
	`(cd vmcore/src; echo rts/gcc-arm vm/fp vm/*.h vm/squawk.c vm/util vm/*.c.inc)` \
	| (cd $SOURCE; tar xf -)

# build
cd ${TOP}

yotta build
