package com.sun.squawk.io;

import java.io.*;
import javax.microedition.io.*;

public class UDPInputStream extends InputStream {
	private DatagramConnection conn;
	private Datagram dg;
	private byte[] ddata;
	private int packSize = 0;
	private int idx = 0;

	public UDPInputStream(DatagramConnection conn) throws IOException {
		this(conn, 128);
	}
	
	public UDPInputStream(DatagramConnection conn, int bufferSize) throws IOException {
		this.conn = conn;
		this.ddata = new byte[bufferSize];
		this.dg = conn.newDatagram(ddata, bufferSize);
	}

	public void close() throws IOException {
		conn.close();
		this.conn = null;
		this.ddata = null;
		this.packSize = 0;
		this.idx = 0;
	}

	public int available() throws IOException {
		return packSize - idx;
	}
    
	public int read() throws IOException {
		if (ddata == null) {
			throw new IOException();
		}
		if (idx == packSize) {
			receive();
		}
		return ddata[idx++] & 0xff;
	}

	public int read(byte[] buff) throws IOException {
		return read(buff, 0, buff.length);
	}

	public int read(byte[] buff, int off, int len) throws IOException {
		if (ddata == null) {
			throw new IOException();
		}
		if (idx == packSize) {
			receive();
		}
		int avail = available();
		System.arraycopy(ddata, idx, buff, off, avail);
		idx += avail;
		return avail;
	}

	public long skip(long len) throws IOException {
		if (idx == packSize) {
			receive();
		}
		int remain = (int)len;
		while (available() < remain) {
			remain -= available();
			receive();
		}
		idx += remain;
		return len;
	}

	private void receive() throws IOException {
		if (ddata == null) throw new IOException();
		dg.reset();
		conn.receive(dg);
		idx = 0;
		packSize = dg.getLength();
	}

	public void mark(int readlimit) {}

	public void reset() throws IOException {
		throw new IOException();
	}

	public boolean markSupported() {
		return false;
	}
}
