package com.sun.squawk.hal;

import com.sun.squawk.VM;
import com.sun.squawk.*;
import java.io.*;
	
public class TimerIRQ {

	public static native int init(int delay, boolean repeat);
	public static native int cancel(int d);
	
   	public final static int TIMER_IRQ = 1;
	static TimerIRQ[] irq = new TimerIRQ[32];

	private int d;
	private boolean cancelled;
	private boolean repeat;
	private TimerIRQHandler handler;

	static {
		start();
	}
	
	public TimerIRQ(int delay, boolean repeat, TimerIRQHandler handler) {
		this.d = init(delay, repeat);
		if (d < 0) {
			throw new RuntimeException();
		}
		this.handler = handler;
		this.repeat = repeat;
		irq[d] = this;
	}

	public void cancel() {
		cancelled = true;
		handler = null;
		cancel(d);
	}

	private void signal() {
		TimerIRQHandler handler = this.handler;
		if (!cancelled) {
			handler.signal();
		}
		if (!repeat) {
			irq[d] = null;
		}
	}

	static void start() {
		Thread th = new Thread(){
				public void run() {
					VMThread th = VMThread.currentThread();
					try {
						while (true) {
							VM.waitForInterrupt(TIMER_IRQ);
							int i = 0;
							int e = th.event;
							while (e != 0) {
								if ((e & 1) != 0) {
									TimerIRQ req = irq[i];
									req.signal();
								}
								e >>= 1;
								i++;
							}
						}
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			};
		th.start();
	}
}
