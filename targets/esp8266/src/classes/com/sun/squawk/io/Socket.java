package com.sun.squawk.io;

import com.sun.squawk.*;
import java.io.IOException;
import static com.sun.squawk.io.NetUtil.*;
import esp8266.Events;

public class Socket extends AbstractSocket {
	private static native int getlocaladdr(int handle);
	private static native int getlocalport(int handle);
	private static native int available0(int handle);
	private static native int connect0(int addr, int port);
	private static native int read0(int handle, byte[] buf, int offset, int size);
	private static native int read1(int handle);
	private static native int write0(int handle, byte[] buf, int offset, int size);
	private static native int write1(int handle, byte value);
	private static native int close0(int handle);

	int handle;
	
	public Socket() {
	}

	Socket(int handle) {
		this.handle = handle;
	}
		
	public Socket(String host, int port) throws IOException {
		connect(gethostbyname(host), port);
	}
	
	public Socket(int addr, int port) throws IOException {
		connect(addr, port);
	}

	public void connect(int addr, int port) throws IOException {
		int n = connect0(addr, port);
		if (n == 0) {
			throw new IOException();
		}
		VM.waitForInterrupt(Events.CONNECTED_EVENT);
		if (VMThread.currentThread().event != 0) {
			throw new IOException();
		}
		this.handle = n;
	}

	public void setSockOpt(int option, int value) {
		throw new RuntimeException("TODO");
	}

	public int getSockOpt(int option) {
		throw new RuntimeException("TODO");
	}

	public int getLocalAddress() {
		return getlocaladdr(handle);
	}
	
	public int getLocalPort() {
		return getlocalport(handle);
	}

	public int read() throws IOException {
		int n;
		VMThread th = VMThread.currentThread();
		while (true) {
			n = read1(handle);
			if (n == -2) {
				VM.waitForInterrupt(Events.READ_READY_EVENT);
				int e = th.event;
				switch (e) {
				case -1: // EOF
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
	
	public int read(byte[] buf, int off, int len) throws IOException {
		int n;
		VMThread th = VMThread.currentThread();
		while (true) {
			n = read0(handle, buf, off, len);
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

	public int available() {
		return available0(handle);
	}

	public int write(int b) throws IOException {
		int n;
		while (true) {
			n = write1(handle, (byte)b);
			if (n == 0) {
				VM.waitForInterrupt(Events.WRITE_READY_EVENT);
			} else {
				break;
			}
		}
		return n;
	}
	
	public int write(byte[] buf, int off, int len) throws IOException {
		int n;
		VMThread th = VMThread.currentThread();
		while (true) {
			n = write0(handle, buf, off, len);
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
		close0(handle);
	}
}
