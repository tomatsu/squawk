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
* Run fullbuild-gcc-x86.sh for Linux/x86 executable.
