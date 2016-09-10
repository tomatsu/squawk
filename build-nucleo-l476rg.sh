#!/bin/sh

# common settings
#MAIN_CLASS_NAME=Hello
#JARFILE=helloworld/classes.jar
MAIN_CLASS_NAME=Blinky
JARFILE=nucleo_blinky/classes.jar
export EXTRA_BUILDER_VMFLAGS=-Xbootclasspath/a:tools/asm-5.1.jar

# Setting
MBED=${MBED:-../mbed/.build/mbed}
TARGET=NUCLEO_L476RG
VENDOR=STM
MCU=STM32L4
TOOLCHAIN=GCC_ARM
CFLAGS="-mthumb -mcpu=cortex-m4 -mfpu=fpv4-sp-d16 -mfloat-abi=softfp -Os -DMAIN_CLASS_NAME=${MAIN_CLASS_NAME} "
MBED_OBJECTDIR=$MBED/TARGET_$TARGET/TOOLCHAIN_$TOOLCHAIN
LINKERSCRIPT=`echo $MBED_OBJECTDIR/*ld`
LDFLAGS="-mthumb -mcpu=cortex-m4 -Os --specs=nano.specs -u _printf_float -u _scanf_float -T$LINKERSCRIPT -fdata-sections -ffunction-sections"
LIBDIR=$MBED_OBJECTDIR
LDSUFFIXES="$MBED_OBJECTDIR/*.o -L$LIBDIR -lmbed -lc -lm -lnosys"

INCLUDE_DIRS="$MBED $MBED/TARGET_$TARGET $MBED/TARGET_$TARGET/TARGET_$VENDOR/TARGET_$MCU $MBED/TARGET_$TARGET/TARGET_$VENDOR/TARGET_$MCU/TARGET_$TARGET"
DEVICE_HAS="ANALOGIN ANALOGOUT CAN I2C I2CSLAVE INTERRUPTIN PORTIN PORTINOUT PORTOUT PWMOUT RTC SERIAL SERIAL_ASYNCH SERIAL_FC SLEEP SPI SPISLAVE STDIO_MESSAGES"
device_options="`for i in $DEVICE_HAS; do echo -DDEVICE_$i=1; done`"
ioptions="`for i in $INCLUDE_DIRS; do echo -cflags:-I$i; done`"
cflags="`for i in $CFLAGS; do echo -cflags:$i; done` $ioptions $device_options"
ldflags="`for i in $LDFLAGS; do echo -ldflags:$i; done`"
ldsuffixes="`for i in $LDSUFFIXES; do echo -ldsuffixes:$i; done`"
PROP=build-mbed.properties
TOP=build
SOURCE=$TOP/source

case "$1" in
"clean" )
	./d.sh -override $PROP -q clean || exit 1
	rm -rf ${TOP}	
	exit ;;
"setup" )
	stage_1_only=true
	;;
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
	./d.sh -override $PROP || exit 1

	(cd builder; sh nbld.sh) || exit 1
	./d.sh -override $PROP -q clean || exit 1
	./d.sh -override $PROP || exit 1
fi

if [ "x${stage_1_only}" = "xtrue" ]; then
	exit;
fi
	
# stage 2
echo "stage 2...."

TMP=/tmp/d

rm -rf $TMP
mkdir -p $TMP/classes
(cwd=`pwd`; cd $TMP/classes; jar xf $cwd/cldc/classes.target.jar; jar xf $cwd/${JARFILE}; jar cfM $TMP/merged.jar .)

rm -rf $TMP/classes2
mkdir -p $TMP/classes2
java -Xbootclasspath/a:tools/asm-5.1.jar -cp build.jar com.sun.squawk.builder.asm.Shrink $TMP/merged.jar $TMP/classes2 ${MAIN_CLASS_NAME} $TMP/merged.jar

rm -rf $TMP/j2meclasses
mkdir $TMP/j2meclasses
tools/linux-x86/preverify -d $TMP/j2meclasses $TMP/classes2
(cd $TMP/j2meclasses; jar cfM $TMP/j2meclasses.jar .)

java -jar build.jar \
       -override $PROP \
       -comp:gcc-arm \
       $cflags \
       $ldflags \
       $ldsuffixes \
       rom -d:${TOP} -strip:a $TMP/j2meclasses.jar || exit 1

#./d.sh -override $PROP map -cp:cldc/j2meclasses:$JARFILE squawk.suite

arm-none-eabi-objcopy -O binary ${TOP}/squawk ${TOP}/squawk.bin
