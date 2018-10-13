package com.sun.squawk.io;
import java.io.*;

abstract class AbstractSocket {
    public abstract int read() throws IOException;
    public abstract int read(byte[] buf, int off, int len) throws IOException;
    public abstract int write(int c) throws IOException;
    public abstract int write(byte[] buf, int off, int len) throws IOException;
    public abstract void close() throws IOException;
    protected int count = 0;
	
    public InputStream getInputStream() {
	count++;
	return new PrivateInputStream();
    }
	
    public OutputStream getOutputStream() {
	count++;
	return new PrivateOutputStream();
    }
	
    class PrivateInputStream extends InputStream {
	public int read() throws IOException {
	    return AbstractSocket.this.read();
	}

	public int read(byte b[], int off, int len) throws IOException {
	    return AbstractSocket.this.read(b, off, len);
	}
		
	public int available() throws IOException {
	    return 0;
	}
		
	public void close() throws IOException {
	    if (--count < 1) {
		AbstractSocket.this.close();
	    }
	}
    }

    class PrivateOutputStream extends OutputStream {
	public void write(int c) throws IOException {
	    AbstractSocket.this.write(c);
	}
		
	public void write(byte[] b, int off, int len) throws IOException {
	    AbstractSocket.this.write(b, off, len);
	}

	public void close() throws IOException {
	    if (--count < 1) {
		AbstractSocket.this.close();
	    }
	}
    }
}
