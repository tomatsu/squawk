squawk4mbed
==========
An experimental project to customize squawk virtual machine for mbed devices

Highlights
-----------
* Ported Squawk VM to mbed devices
* Implemented aggressive dead code elimination; Static footprint is reduced to less than 128K (including romized classes)
* Optimized interpreter loop
* Provides easy way to add native methods
* Provides API to call HAL layer
* Has been tested on MICROBIT, LPC1768, NUCLEO_L476, and Linux/x86

Build
-----
* Install gcc-arm-none-eabi
* Install JDK
* git clone https://github.com/tomatsu/squawk4mbed
* ./build-linux-x86.sh
* cd example_gen; TARGET=LPC1768 MAIN_CLASS_NAME=Hello PROJECT=helloworld ./generate-example.sh
* cd /tmp/lpc1768_helloworld; make

Plans
-----
* Documentation
* Test
* CLDC8 support
* Network API
* Interactive use-case
* Suite loading mechanism
