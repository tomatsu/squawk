package com.sun.squawk.hal;

public class AnalogOut {
    private static native int init(int pin);
    private static native void write0(int d, int value);
    private static native int read0(int d);
	private static native void write1(int d, float value);
	private static native float read1(int d);

    private int d;
    
    public AnalogOut(int pin) {
		d = init(pin);
    }

    public void write(int value) {
		write0(d, value);
    }

    public int read() {
		return read0(d);
    }

    public void writeFloat(float value) {
		write1(d, value);
    }

    public float readFloat() {
		return read1(d);
    }
}
