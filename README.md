squawk
=======
An experimental project to customize squawk virtual machine for microcontrollers

Highlights
-----------
* Ported Squawk VM to mbed classic and esp8266
* Implemented aggressive dead code elimination; Static footprint is reduced to less than 128K (including romized classes)
* Optimized interpreter loop
* Provides easy way to add native methods
* Provides API to call HAL layer
* Has been tested on ESP8266, MICROBIT, LPC1768, NUCLEO_L476, and Linux/x86.

Build for mbed
---------------
* Install gcc-arm-none-eabi
* Install JDK
* git clone https://github.com/tomatsu/squawk
* cd squawk; ./build.sh
* cd project_gen;  make TARGET=LPC1768 MAIN_CLASS_NAME=Hello PROJECT=helloworld
* cd /tmp/lpc1768_helloworld; make

Build for ESP8266
------------------
* Install toolchain and SDK
* Install JDK
* git clone https://github.com/tomatsu/squawk
* cd squawk; ./build.sh
* cd project_gen;  make TARGET=ESP8266 MAIN_CLASS_NAME=Hello PROJECT=helloworld
* cd /tmp/esp8266_helloworld; make

Plans
-----
* Documentation
* Test
* CLDC8 support
