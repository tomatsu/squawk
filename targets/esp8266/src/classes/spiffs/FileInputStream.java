package spiffs;

import java.io.*;

public class FileInputStream extends InputStream {
	private static native int open(String path);
	private static native int read1(int handle);
	private static native int read0(int handle, byte[] buf, int offset, int size);
	private static native int skip0(int handle, int n);
	private static native int available0(int handle);
	private static native int close0(int handle);
	private int handle;

	static {
		FileSystem.getInstance();
	}
	
	public FileInputStream(File file) {
		this(file.getPath());
	}
	
	public FileInputStream(String path) {
		this.handle = open(path);
	}
	
	public int read() throws IOException {
		int n = read1(handle);
		if (n < -1) {
			throw new IOException();
		}
		return n;
	}
	
	public int read(byte[] buf, int offset, int size) throws IOException {
		int n = read0(handle, buf, offset, size);
		if (n < -1) {
			throw new IOException();
		}
		return n;
	}

    public long skip(long n) throws IOException {
		int r = skip0(handle, (int)n);
		if (r < 0) {
			throw new IOException();
		}
		return (long)r;
	}
	
    public int available() throws IOException {
		int a = available0(handle);
		if (a < 0) {
			throw new IOException();
		}
		return a;
	}
	
    public void close() throws IOException {
		if (close0(handle) < 0) {
			throw new IOException();
		}
	}
}
