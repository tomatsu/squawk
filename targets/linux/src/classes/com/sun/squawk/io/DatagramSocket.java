package com.sun.squawk.io;

import com.sun.squawk.*;
import java.io.*;

public class DatagramSocket extends AbstractSocket {
    private static native int create0(int port);
    private static native int connect0(int handle, int addr, int port);
    private static native int close0(int handle);
    private static native int send0(int handle, byte[] buf, int off, int len);
    private static native int send1(int handle, int addr, int port, byte[] buf, int off, int len);
    private static native int receive0(int handle, byte[] buf, int off, int len);
    private int handle;
	
    public DatagramSocket() throws IOException {
	this(0);
    }

    public DatagramSocket(int port) throws IOException {
	int s = create0(port);
	if (s == 0) {
	    throw new IOException();
	}
	this.handle = s;
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
	if (close0(handle) == 0) {
	    this.handle = 0;
	} else {
	    throw new IOException();
	}
    }
	
    public void connect(int addr, int port) throws IOException {
	if (handle == 0) {
	    throw new IOException();
	}
	if (connect0(handle, addr, port) == -1) {
	    throw new IOException();
	}
    }

    public int send(int addr, int port, byte[] buf, int off, int len) throws IOException {
	if (handle == 0) {
	    throw new IOException();
	}
	int n;
	while (true) {
	    n = send1(handle, addr, port, buf, off, len);
	    if (n == -1) {
		throw new IOException();
	    } else if (n == -2) {
		VM.waitForInterrupt(Events.WRITE_READY_EVENT);
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
	if (handle == 0) {
	    throw new IOException();
	}
	int n;
	while (true) {
	    n = send0(handle, buf, off, len);
	    if (n == -1) {
		throw new IOException();
	    } else if (n == -2) {
		VM.waitForInterrupt(Events.WRITE_READY_EVENT);
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
	if (handle == 0) {
	    throw new IOException();
	}
	int n;
	while (true) {
	    n = receive0(handle, buf, off, len);
	    if (n == -1) {
		throw new IOException();
	    } else if (n == -2) {
		VM.waitForInterrupt(Events.READ_READY_EVENT);
	    } else {
		System.out.println("n="+n);
		break;
	    }
	}
	return n;
    }
}
