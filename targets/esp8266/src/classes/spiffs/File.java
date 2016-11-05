package spiffs;

import java.util.List;

public class File {
	private String path;

	public File(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public File[] listFiles() {
		List<String> files = FileSystem.getInstance().list(path);
		if (files == null) {
			return null;
		}
		int n = files.size();
		File[] array = new File[n];
		int i = 0;
		for (String name : files) {
			array[i++] = new File(name);
		}
		return array;
	}

	public boolean exists() {
		return FileSystem.getInstance().exists(path);
	}

    public boolean mkdir() {
		throw new UnsupportedOperationException();
	}
	
    public boolean mkdirs() {
		throw new UnsupportedOperationException();
	}

	public String toString() {
		return path;
	}
}
