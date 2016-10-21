package com.sun.squawk.hal;

public abstract class AbstractI2C {
	static native int init(int sda, int scl);	
	static native int freq(int desc, int hz);
	static native int read(int desc, int address, byte[] data, int offset, int len, boolean repeat);
	static native int write0(int desc, int address, byte[] data, int offset, int len, boolean repeat);
	static native int write1(int desc, int data);
	static native int stop(int desc);
	
	protected int d;
	
	public AbstractI2C(int sda, int scl) {
		this.d = init(sda, scl);
	}

	public void setFrequency(int hz) {
		freq(d, hz);
	}
	
	public int read(int address, byte[] data, int offset, int len, boolean repeat) {
		return read(d, address, data, offset, len, repeat);
	}

	public int write(int address, byte[] data, int offset, int len, boolean repeat) {
		return write0(d, address, data, offset, len, repeat);
	}

	public int write(int data) {
		return write1(d, data);
	}

	public void stop() {
		stop(d);
	}
}
