package com.sun.squawk.hal;

public class SPISlave extends AbstractSPI {
	static native int read0(int d);
	static native int receive0(int d);

	public SPISlave(int mosi, int miso, int sclk, int ssel) {
		super(mosi, miso, sclk, ssel);
	}

	public int receive() {
		return receive0(d);
	}
	
	public int read() {
		return read0(d);
	}

	public void reply(int value) {
		super.write(d, value, true);
	}
}
