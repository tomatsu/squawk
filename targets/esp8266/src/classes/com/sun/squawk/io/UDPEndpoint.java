package com.sun.squawk.io;

import com.sun.squawk.*;
import java.io.IOException;
import esp8266.Events;

public class UDPEndpoint {
	private static native int create0();
	private static native int connect0(int handle, int addr, int port);
	private static native int close0(int handle);
	private static native int send0(int handle, byte[] buf, int off, int len);
	private static native int send1(int handle, int addr, int port, byte[] buf, int off, int len);
	private static native int receive0(int handle, byte[] buf, int off, int len);
	private int handle;
	
	public UDPEndpoint() {
		this.handle = create0();
	}

	public void close() throws IOException {
		close0(handle);
	}
	
	public void connect(int addr, int port) throws IOException {
		connect0(handle, addr, port);
	}

	public int send(int addr, int port, byte[] buf, int off, int len) throws IOException {
		int n;
		while (true) {
			n = send1(handle, addr, port, buf, off, len);
			if (n == 0) {
				VM.waitForInterrupt(Events.WRITE_READY_EVENT);
			} else {
				break;
			}
		}
		return n;
	}
	
	public int send(byte[] buf, int off, int len) throws IOException {
		int n;
		while (true) {
			n = send0(handle, buf, off, len);
			if (n == 0) {
				VM.waitForInterrupt(Events.WRITE_READY_EVENT);
			} else {
				break;
			}
		}
		return n;
	}

	public int receive(byte[] buf, int off, int len) throws IOException {
		int n;
		while (true) {
			n = receive0(handle, buf, off, len);
			if (n == 0) {
				VM.waitForInterrupt(Events.READ_READY_EVENT);
			} else {
				break;
			}
		}
		return n;
	}
}
