package com.sun.squawk.hal;

public class DigitalIn {
	private static native int init(int pin);
	private static native int read(int d);
	private static native void mode(int d, int mode);
	private static native boolean isConnected(int d);

	private int d;

	public DigitalIn(int pin) {
		d = init(pin);
	}
	
	public int read() {
		return read(d);
	}

	public void setMode(int mode) {
		mode(d, mode);
	}

	public boolean isConnected() {
		return isConnected(d);
	}
}
