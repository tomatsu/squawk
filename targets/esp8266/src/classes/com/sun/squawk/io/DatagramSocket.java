package com.sun.squawk.io;

import com.sun.squawk.*;
import java.io.*;
import esp8266.Events;

public class DatagramSocket {
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
	}
	
	public void connect(int addr, int port) throws IOException {
		connect0(handle, addr, port);
	}

	public int send(int addr, int port, byte[] buf, int off, int len) throws IOException {
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
	
	public int send(byte[] buf, int off, int len) throws IOException {
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

	public int receive(byte[] buf, int off, int len) throws IOException {
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

	public InputStream getInputStream() {
		return new PrivateInputStream();
	}
	
	public OutputStream getOutputStream(int remoteaddr, int remoteport) {
		return new PrivateOutputStream(remoteaddr, remoteport);
	}
	
	static byte[] inbuf = new byte[1];
	static byte[] outbuf = new byte[1];
	
	class PrivateInputStream extends InputStream {
		public int read() throws IOException {
			// assume single thread
			int n = receive(inbuf, 0, 1);
			if (n == 1) {
				return inbuf[0];
			} else {
				return n;
			}
		}

		public int read(byte b[], int off, int len) throws IOException {
			return receive(b, off, len);
		}
		
		public int available() throws IOException {
			return 0;
		}
		
		public void close() throws IOException {
		}
	}

	class PrivateOutputStream extends OutputStream {
		int remoteaddr;
		int remoteport;

		PrivateOutputStream(int remoteaddr, int remoteport) {
			this.remoteaddr = remoteaddr;
			this.remoteport = remoteport;
		}
		
		public void write(int i) throws IOException {
			// assume single thread
			outbuf[0] = (byte)i;
			send(outbuf, 0, 1);
		}
		
		public void write(byte[] b, int off, int len) throws IOException {
			send(remoteaddr, remoteport, b, off, len);
		}

		public void close() throws IOException {
		}
	}
}
