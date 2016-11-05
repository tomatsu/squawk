package spiffs;

import java.io.*;
import java.util.*;

public class FileSystem {
	static native boolean mount0();
	static native boolean unmount0();
	static native boolean format0();
	static native boolean rename0(String from, String to);
	static native boolean delete0(String path);
	static native boolean exists0(String path);
	static native int opendir(String path);
	static native int readdir(int handle, byte[] name);
	static native int closedir(int handle);
	static native int getLastError0();

	private static FileSystem instance;
	static {
		instance = new FileSystem();
		if (!instance.mount()) {
			System.out.println("Could not mount filesystem");
		}
	}

	private FileSystem() {
	}
	
	public static FileSystem getInstance() {
		return instance;
	}
	
	public boolean mount() {
		if (!mount0()) {
			format0();
			return mount0();
		}
		return true;
	}

	public void format() {
		unmount();
		format0();
		mount0();
	}

	public boolean unmount() {
		return unmount0();
	}

	public boolean rename(String from, String to) {
		return rename0(from, to);
	}

	public boolean delete(String path) {
		return delete0(path);
	}

	public List<String> list(String path) {
		int dir = opendir(path);
		if (dir == 0) {
			return null;
		}
		byte[] name = new byte[32];  // assume SPIFFS_OBJ_NAME_LEN=32
		int n;
		List<String> files = new ArrayList<String>();
		while ((n = readdir(dir, name)) != 0) {
			files.add(new String(name, 0, n));
		}
		closedir(dir);
		return files;
	}
	
	public boolean exists(String path) {
		return exists0(path);
	}
	
	public int getLastError() {
		return getLastError0();
	}
}
