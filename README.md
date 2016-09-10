squawk4mbed
==========
An experimental project to customize squawk virtual machine for mbed devices

Plan
----
* Port the VM to mbed devices
* Reduce static footprint of minimal application down to less than 128k
* For devices with larger (>=256K) flash memory, implement suite loading mechanism
* CLDC8 support
* Extend API to make it useful for real world applications

Build
-----
* Install gcc-arm-none-eabi
* Install JDK
* Install yotta
* git clone https://github.com/tomatsu/squawk4mbed
* Run build-microbit.sh for micro:bit.
* Run build-linux-x86.sh for Linux/x86 executable.

Current Status
--------------
* Has been tested on MICROBIT, NUCLEO_L476, and Linux/x86.
* Image size for simple app is around 100KB.  Unused code are eliminated to reduce memory footprint.
* Interpreter loop is optimized using GCC's 'Labels as Values'.
* Currently, the API is based on cldc1.1. It has experimental/undocumented/non-standard APIs to utilize HAL layer.
* An issue is that build takes long time.  The build system is divided into two steps:
   1) set up build environment, which does not need to be rebuilt when application code is changed,
   2) build for specific application code. 
  We want to minimize the time taken by the step 2).
* Possibly, there should be a simple way to add native methods in the step 2) above.
