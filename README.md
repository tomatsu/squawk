squawk
=======
An experimental project to customize squawk virtual machine for microcontrollers.  Users can write embedded application programs in Java (with CLDC API) that can run on small embedded system without RTOS.

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
* Install mbed cli (tested with 1.7.5)
* git clone https://github.com/tomatsu/squawk
* cd squawk; make
* cd project_gen/targets/mbed;  make TARGET=LPC1768 PROJECT=helloworld
* cd /tmp/lpc1768_helloworld; make

Build for ESP8266
------------------
* Install toolchain and SDK
* Install JDK
* git clone https://github.com/tomatsu/squawk
* cd squawk; make
* Set environment variable PATH and ESP_SDK
  For example, export ESP_SDK=$HOME/esp-open-sdk/sdk; export PATH=$HOME/esp-open-sdk/xtensa-lx106-elf/bin:$PATH
* cd project_gen/targets/esp8266;  make PROJECT=helloworld
* cd /tmp/esp8266_helloworld; make

Plans
-----
* Documentation
* Test
* CLDC8 support
