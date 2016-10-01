package com.sun.squawk.hal;

public class Port {
	public final static int PORT_INPUT = 0;
	public final static int PORT_OUTPUT = 1;
	
	static native int init(int port, int mask, int direction);
	static native int read0(int desc);
	static native int write0(int desc, int value);
	static native int mode0(int desc, int value);
	static native int direction0(int desc, int direction);
	private int d;
	
	public Port(int port) {
		this(port, 0xffffffff, PORT_OUTPUT);
	}
	
	public Port(int port, int mask, int direction) {
		d = init(port, mask, direction);
	}

	public int read() {
		return read0(d);
	}

	public void write(int value) {
		write0(d, value);
	}

	public void setMode(int mode) {
		mode0(d, mode);
	}

	public void setDirection(int dir) {
		direction0(d, dir);
	}
}
