#!/bin/sh

if [ $# = 0 ]; then
    jarfile="helloworld/classes.jar"
else
    jarfile=$1
fi

#PROP=build-mbed-minimal.properties
PROP=build-mbed.properties

# The location of mbed tools
# See https://developer.mbed.org/handbook/mbed-tools
MBED=${MBED:-$HOME/mbed/.build/mbed}

# Setting
TARGET=NUCLEO_L476RG
VENDOR=STM
MCU=STM32L4
TOOLCHAIN=GCC_ARM
CFLAGS="-mthumb -mcpu=cortex-m4 -mfpu=fpv4-sp-d16 -mfloat-abi=softfp -Os -flto"
MBED_OBJECTDIR=$MBED/TARGET_$TARGET/TOOLCHAIN_$TOOLCHAIN
LINKERSCRIPT=`echo $MBED_OBJECTDIR/*ld`
LDFLAGS="-mthumb -mcpu=cortex-m4 -Os -flto --specs=nano.specs -u _printf_float -u _scanf_float -T$LINKERSCRIPT -fdata-sections -ffunction-sections"
LIBDIR=$MBED_OBJECTDIR
LDSUFFIXES="$MBED_OBJECTDIR/*.o -L$LIBDIR -lmbed -lc -lm -lnosys"

INCLUDE_DIRS="$MBED $MBED/TARGET_$TARGET $MBED/TARGET_$TARGET/TARGET_$VENDOR/TARGET_$MCU $MBED/TARGET_$TARGET/TARGET_$VENDOR/TARGET_$MCU/TARGET_$TARGET"

ioptions="`for i in $INCLUDE_DIRS; do echo -cflags:-I$i; done`"
cflags="`for i in $CFLAGS; do echo -cflags:$i; done` $ioptions"
ldflags="`for i in $LDFLAGS; do echo -ldflags:$i; done`"
ldsuffixes="`for i in $LDSUFFIXES; do echo -ldsuffixes:$i; done`"

(cd builder; sh bld.sh)
./d.sh -override $PROP clean
./d.sh -override $PROP
./d.sh -override $PROP \
       -comp:gcc-arm \
       $cflags \
       $ldflags \
       $ldsuffixes \
       rom -strip:a -verbose cldc $jarfile

arm-none-eabi-objcopy -O binary squawk squawk.bin
