package com.sun.squawk.hal;

public class I2C extends AbstractI2C {
	static native int read(int desc, int ack);
	static native int start(int desc);
	static I2C owner;
	private int hz;
	
	public I2C(int sda, int scl) {
		super(sda, scl);
		owner = this;
	}

	public synchronized void setFrequency(int hz) {
		super.setFrequency(hz);
		this.hz = hz;
		owner = this;
	}

	private synchronized void acquire() {
		if (owner != this) {
			freq(d, this.hz);
			owner = this;
		}
	}
	
	public synchronized int read(int address, byte[] data, int offset, int len, boolean repeat) {
		acquire();
		return super.read(address, data, offset, len, repeat);
	}

	public synchronized int read(int ack) {
		return read(d, ack);
	}

	public synchronized int write(int address, byte[] data, int offset, int len, boolean repeat) {
		acquire();
		return super.write(address, data, offset, len, repeat);
	}

	public synchronized int write(int data) {
		return super.write(data);
	}

	public synchronized void start() {
		start(d);
	}

	public synchronized void stop() {
		super.stop();
	}
}
