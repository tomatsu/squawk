package com.sun.squawk.io;

import com.sun.squawk.*;
import java.io.*;
import esp8266.Events;

public class DatagramSocket extends AbstractSocket {
	private static native int create0(int port);
	private static native int connect0(int handle, int addr, int port);
	private static native int close0(int handle);
	private static native int send0(int handle, byte[] buf, int off, int len);
	private static native int send1(int handle, int addr, int port, byte[] buf, int off, int len);
	private static native int receive0(int handle, byte[] buf, int off, int len);
	private int handle;
	
	public DatagramSocket() {
		this(0);
	}

	public DatagramSocket(int port) {
		this.handle = create0(port);
	}

	public int getLocalAddress() {
		throw new RuntimeException("TODO");
	}
	
	public int getLocalPort() {
		throw new RuntimeException("TODO");
	}
	
	public int getReceiveBufferSize() throws IOException {
		throw new RuntimeException("TODO");
	}
	
	public void close() throws IOException {
		close0(handle);
		this.handle = 0;
	}
	
	public void connect(int addr, int port) throws IOException {
		if (handle == 0) throw new IOException();
		connect0(handle, addr, port);
	}

	public int send(int addr, int port, byte[] buf, int off, int len) throws IOException {
		if (handle == 0) throw new IOException();
		int n;
		VMThread th = VMThread.currentThread();
		while (true) {
			n = send1(handle, addr, port, buf, off, len);
			if (n == 0) {
				VM.waitForInterrupt(Events.WRITE_READY_EVENT);
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

	public int write(int c) throws IOException {
		byte[] buf = new byte[]{(byte)c};
		return write(buf, 0, 1);
	}
	
	public int write(byte[] buf, int off, int len) throws IOException {
		if (handle == 0) throw new IOException();
		int n;
		VMThread th = VMThread.currentThread();
		while (true) {
			n = send0(handle, buf, off, len);
			if (n == 0) {
				VM.waitForInterrupt(Events.WRITE_READY_EVENT);
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

	public int read() throws IOException {
		byte[] buf = new byte[1];
		read(buf, 0, 1);
		return (int)buf[0];
	}

	public int read(byte[] buf, int off, int len) throws IOException {
		if (handle == 0) throw new IOException();
		int n;
		VMThread th = VMThread.currentThread();
		while (true) {
			n = receive0(handle, buf, off, len);
			if (n == 0) {
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
}
