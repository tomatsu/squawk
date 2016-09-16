package com.sun.squawk.hal;

public class I2CSlave extends I2C {
	static native int init(int desc);
	static native int address(int desc, int address);
	static native int receive0(int desc);
	
	public I2CSlave(int sda, int scl) {
		super(sda, scl);
		init(d);
		setFrequency(100000);
	}
	
	public void setAddress(int address) {
		address(d, address);
	}

	public int receive() {
		return receive0(d);
	}
}
