package com.sun.squawk.hal;

public class PwmOut {
    private static native int init(int pin);
    private static native int write0(int d, float value);
    private static native float read0(int d);
    private static native void period0(int d, float sec);
    private static native void period_ms0(int d, int msec);
    private static native void period_us0(int d, int us);
    private static native void pulsewidth0(int d, float sec);
    private static native void pulsewidth_ms0(int d, int ms);
    private static native void pulsewidth_us0(int d, int us);
	
    private int d;

    public PwmOut(int pin) {
		d = init(pin);
    }

    public void write(float value) {
		write0(d, value);
    }

    public float read() {
		return read0(d);
    }

    public void period(float sec) {
		period0(d, sec);
    }

    public void period_ms(int ms) {
		period_ms0(d, ms);
    }

    public void period_us(int us) {
		period_us0(d, us);
    }

    public void pulsewidth(float sec) {
		pulsewidth0(d, sec);
    }

    public void pulsewidth_ms(int ms) {
		pulsewidth_ms0(d, ms);
    }

    public void pulsewidth_us(int us) {
		pulsewidth_us0(d, us);
    }
}

