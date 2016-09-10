package com.sun.squawk.hal;

public class SPI extends AbstractSPI {
    static SPI owner;

    public SPI(int mosi, int miso, int sclk, int ssel) {
		super(mosi, miso, sclk, ssel);
    }
	
    private void acquire() {
		if (owner != this) {
			format(d, bits, mode, false);
			freq(d, hz);
			owner = this;
		}
    }

    public int write(int value) {
		acquire();
		return AbstractSPI.write(d, value, false);
    }
}
