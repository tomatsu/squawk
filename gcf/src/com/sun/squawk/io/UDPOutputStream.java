package com.sun.squawk.io;

import java.io.OutputStream;
import java.io.IOException;
import javax.microedition.io.*;

public class UDPOutputStream extends OutputStream {
    private DatagramConnection conn = null;
    private Datagram dg = null;
    private byte[] buffer;
    private int idx = 0;

    public UDPOutputStream(DatagramConnection conn) throws IOException {
		this(conn, 128);
	}
	
    public UDPOutputStream(DatagramConnection conn, int size) throws IOException {
		this.conn = conn;
		this.buffer = new byte[size];
		this.dg = conn.newDatagram(buffer, size);
    }

    public void close() throws IOException {
		conn.close();
		conn = null;
		idx = 0;
    }

    public void flush() throws IOException {
		if (conn == null) {
			throw new IOException();
		}
		if (idx == 0) {  // no data in buffer
			return;
		}
		dg.setLength(idx);
		conn.send(dg);
		idx = 0;
    }

    public void write(int value) throws IOException {
		if (conn == null) {
			throw new IOException();
		}
		buffer[idx++] = (byte) (value & 0x0ff);
		if (idx >= buffer.length) {
			flush();
		}
    }

    public void write(byte[] data) throws IOException {
		write(data, 0, data.length);
    }

    public void write(byte[] data, int off, int len) throws IOException {
		if (conn == null) {
			throw new IOException();
		}

		while (true) {
			if (buffer.length - idx <= len) {
				System.arraycopy(data, off, buffer, idx, buffer.length - idx);
				flush();
				len -= (buffer.length - idx);
			} else {
				System.arraycopy(data, off, buffer, idx, len);
				idx += len;
				break;
			}
		}
    }
}
