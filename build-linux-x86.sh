#!/bin/sh

MAIN_CLASS_NAME=Hello
JARFILE="helloworld/classes.jar"

PROP=build-mbed.properties
OUT=build
SQUAWK_TOP=.

CFLAGS="-m32 -Os"
LDFLAGS="-m32 -lm"

case "$1" in
"clean" )
	./d.sh -override $PROP -q clean || exit 1
	rm -rf ${OUT}
	exit ;;
"setup" )
	stage_1_only=true
	;;
esac

skip_stage_1=true

if [ ! -f cldc/classes.target.jar ]; then
	skip_stage_1=false
fi

# stage 1
if [ x${skip_stage_1} != "xtrue" ]; then
	echo "stage 1...."
	
	(cd builder; sh bld.sh) || exit 1
	./d.sh -override $PROP clean || exit 1
	./d.sh -override $PROP || exit 1
	if [ -d cldc/weaved ]; then
		(cd cldc/weaved; jar cfM ../classes.target.jar .)
	fi
fi

if [ "x${stage_1_only}" = "xtrue" ]; then
	rm -f vmcore/src/vm/vm2c.c.inc.spp
    java -jar build.jar -override $PROP runvm2c	-o:vmcore/src/vm/vm2c.c.inc.spp \
		 -cp: -sp:.:cldc/preprocessed-vm2c -root:com.sun.squawk.VM -root:com.sun.squawk.MethodHeader -root:com.sun.squawk.CheneyCollector \
		 $(find cldc/preprocessed-vm2c -name '*\.java' -print)
    java -jar build.jar -override $PROP spp `find vmcore/src -name '*spp'`
	exit;
fi

if [ ! -f ${JARFILE} ]; then
	(cd helloworld; sh build.sh)
fi

# stage 2
echo "stage 2...."

TMP=/tmp/d

rm -rf $TMP/classes
mkdir -p $TMP/classes

(cwd=`pwd`; cd $TMP/classes; jar xf $cwd/cldc/classes.target.jar; jar xf $cwd/${JARFILE})
java -Xbootclasspath/a:${SQUAWK_TOP}/tools/asm-5.1.jar -cp ${SQUAWK_TOP}/build.jar com.sun.squawk.builder.glue.NativeGlueGen -d:${OUT} $TMP/classes
tar cf - -C ${OUT}/cldc/classes .  | (cd $TMP/classes; tar xf -)
(cd $TMP/classes; jar cfM $TMP/merged.jar .)

rm -rf $TMP/classes2
mkdir -p $TMP/classes2
java -Xbootclasspath/a:tools/asm-5.1.jar -cp build.jar com.sun.squawk.builder.asm.Shrink $TMP/merged.jar $TMP/classes2 ${MAIN_CLASS_NAME} $TMP/merged.jar

rm -rf $TMP/j2meclasses
mkdir $TMP/j2meclasses
echo tools/linux-x86/preverify -classpath cldc/classes.target.jar -d $TMP/j2meclasses $TMP/classes2
tools/linux-x86/preverify -classpath cldc/classes.target.jar -d $TMP/j2meclasses $TMP/classes2

(cd $TMP/j2meclasses; jar cfM ../j2meclasses.jar .)

cflags=`for i in $CFLAGS; do echo -cflags:$i; done`
ldflags=`for i in $LDFLAGS; do echo -ldflags:$i; done`

java -jar build.jar -override $PROP $cflags $ldflags \
     rom -d:${OUT} -cp:${OUT}/cldc/classes -strip:a $TMP/j2meclasses.jar || exit 1

strip ${OUT}/squawk
