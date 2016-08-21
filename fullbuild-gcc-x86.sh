#!/bin/sh

MAIN_CLASS_NAME=Hello
JARFILE="helloworld/classes.jar"

PROP=build-mbed.properties
CFLAGS="-flto -Os"
LDLAGS="-flto -Os"

case "$1" in
"clean" )
	./d.sh -override $PROP -q clean || exit 1
	rm -rf ${TOP}
	exit ;;
esac

skip_stage_1=true

if [ ! -d cldc/classes ]; then
	skip_stage_1=false
fi

# stage 1
if [ x${skip_stage_1} != "xtrue" ]; then
	echo "stage 1...."
	
	(cd builder; sh bld.sh) || exit 1
	./d.sh -override $PROP clean || exit 1
	./d.sh -override $PROP || exit 1
fi

# stage 2
echo "stage 2...."

./d.sh -override $PROP -fork shrink ${MAIN_CLASS_NAME} ${JARFILE} || exit 1

cflags=`for i in $CFLAGS; do echo -cflags:$i; done`
ldlags=`for i in $LDFLAGS; do echo -ldflags:$i; done`
./d.sh -override $PROP $cflags $ldflags  \
       rom -strip:a cldc $JARFILE || exit 1

strip squawk
