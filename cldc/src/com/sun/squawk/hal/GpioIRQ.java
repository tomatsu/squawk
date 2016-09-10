package com.sun.squawk.hal;

import com.sun.squawk.VM;
import com.sun.squawk.*;
import java.io.*;
	
public class GpioIRQ {

	public static native int init(int pin, boolean fall);
   	public final static int GPIO_IRQ = 0;

	/*
	 * GpioIRQ.startIRQ(13, true, new IRQHandler() {
     *    public void signal() {
	 *      ...
	 *    }
	 * });
	 */
	public static void start(final GpioIRQHandler handler) {
		Thread th = new Thread(){
				public void run() {
					int r;
					try {
						while (true) {
							VM.waitForInterrupt(GPIO_IRQ);
							int e = VMThread.currentThread().event;
							int pin = e >> 2;
							boolean fall = ((e & 1) == 1);
							handler.signal(pin, fall);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
		th.start();
	}
}
