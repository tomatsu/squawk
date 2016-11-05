package com.sun.squawk.io;

import com.sun.squawk.*;
import esp8266.*;
import java.io.*;

public class SSLSocket extends Socket {
	
	private static native void init();
	private static native int createSSLContext(int handle);
	private static native int read0(int context, byte[] buf, int offset, int size);
	private static native int write0(int context, byte[] buf, int offset, int size);
	private static native int close0(int context);
	
	private int context;

	static {
		init();
	}
	
	public SSLSocket(String host, int port) throws IOException {
		super(host, port);
		this.context = createSSLContext(handle);
	}

//	public SSLSession getSession() {
//	}

	public int read(byte[] buf, int offset, int len) throws IOException {
		int n;
		VMThread th = VMThread.currentThread();
		while (true) {
			n = read0(context, buf, offset, len);
			if (n == 0) {
				VM.waitForInterrupt(Events.READ_READY_EVENT);
				int e = th.event;
				switch (e) {
				case -1:
					return -1;
				case 0:
					break;
				default:
					throw new IOException();
				}
			} else {
				break;
			}
		} 
		return n;
	}
	
	public int write(byte[] buf, int offset, int len) throws IOException {
		int n;
		VMThread th = VMThread.currentThread();
		while (true) {
			n = write0(context, buf, offset, len);
			if (n == 0) {
				VM.waitForInterrupt(Events.WRITE_READY_EVENT);
				int e = th.event;
				switch (e) {
				case -1:
					return -1;
				case 0:
					break;
				default:
					throw new IOException();
				}
			} else {
				break;
			}
		}
		return n;
	}

	public void close() throws IOException {
		close0(context);
	}
}
