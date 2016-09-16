package com.sun.squawk.hal;

public interface GpioIRQHandler {
		void signal(int pin, boolean fall);
}
