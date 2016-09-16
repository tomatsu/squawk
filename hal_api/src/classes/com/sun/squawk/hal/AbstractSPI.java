package com.sun.squawk.hal;

public abstract class AbstractSPI {
    static native int init(int mosi, int msio, int sclk, int ssel);
    static native void format(int d, int bits, int mode, boolean slave);
    static native void freq(int d, int hz);
    static native int write(int d, int value, boolean slave);

	int d;
    int bits;
    int mode;
    int hz;

    public AbstractSPI(int mosi, int msio, int sclk, int ssel) {
		this.d = init(mosi, msio, sclk, ssel);
		this.bits = 8;
		this.mode = 0;
		this.hz = 1000000;
    }


	public void setFormat(int bits, int mode, boolean slave) {
		format(d, bits, mode, slave);
	}
	
	public void setFrequency(int hz) {
		freq(d, hz);
	}
}
