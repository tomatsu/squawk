package spiffs;

import java.io.*;

public class FileOutputStream extends OutputStream {
	private static native int open(String path);
	private static native int write1(int handle, int ch);
	private static native int write0(int handle, byte[] buf, int offset, int size);
	private static native int flush0(int handle);
	private static native int close0(int handle);
	private int handle;

	static {
		FileSystem.getInstance();
	}
	
	public FileOutputStream(File file) {
		this(file.getPath());
	}
	
	public FileOutputStream(String path) {
		this.handle = open(path);
	}
	
	public void write(int ch) throws IOException {
		int n = write1(handle, ch);
		if (n < -1) {
			throw new IOException();
		}
	}
	
	public void write(byte[] buf, int offset, int size) throws IOException {
		int n = write0(handle, buf, offset, size);
		if (n < -1) {
			throw new IOException();
		}
	}


	public void flush() throws IOException {
		flush0(handle);
	}
	
    public void close() throws IOException {
		flush();
		if (close0(handle) < 0) {
			throw new IOException();
		}
	}
}
