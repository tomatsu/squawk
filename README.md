squawk4mbed
==========
An experimental project to customize squawk virtual machine for mbed devices

Plan
----
* Port the VM with HAL and run on mbed devices
* Reduce static footprint of minimal application down to less than 128k
* For devices with larger (>256K) flash memory, implement suite loading mechanism
* Extend API to make it useful for real world applications

Build
-----
* Install gcc-arm-none-eabi
* Install JDK1.6.x
* Download mbed SDK
* Build tools for your device - Follow the instruction described in https://developer.mbed.org/handbook/mbed-tools
* git clone https://github.com/tomatsu/squawk4mbed
* Run fullbuild-gcc-x86.sh for Linux/x86 executable.
* Edit fullbuild-gcc-arm.sh and run it for your mbed device
