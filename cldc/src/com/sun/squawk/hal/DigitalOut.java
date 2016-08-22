package com.sun.squawk.hal;

public class DigitalOut {
	private static native int init0(int pin);
	private static native void write0(int d, int value);
	private static native int read0(int d);
	private static native boolean isConnected0(int d);

	private int d;
	
	public DigitalOut(int pin) {
		d = init0(pin);
	}

	public void write(int value) {
		write0(d, value);
	}

	public int read() {
		return read0(d);
	}

	public boolean isConnected() {
		return isConnected0(d);
	}
}
