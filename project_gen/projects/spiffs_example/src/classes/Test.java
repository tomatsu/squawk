import spiffs.*;

import java.io.*;
	
public class Test {
	public static void main(String[] args) throws Exception {
		File root = new File("/");
		System.out.println("listFiles:");
		File[] files = root.listFiles();
		for (int i = 0; i < files.length; i++) {
			System.out.println(files[i]);
		}
		PrintStream out = new PrintStream(new FileOutputStream("/README.txt"));
		out.println("Hello World.");
		out.close();

		files = root.listFiles();
		for (int i = 0; i < files.length; i++) {
			System.out.println(files[i]);
		}

		FileInputStream in = new FileInputStream("/README.txt");
		int n;
		byte[] buf = new byte[16];
		while ((n = in.read(buf)) != -1) {
			System.out.write(buf, 0, n);
		}
		System.out.println();
		/*
		File readme = new File("/README.txt");
		FileOutputStream out = new FileOutputStream(readme);
		PrintStream ps = new PrintStream(out);
		ps.println("Hello");
		ps.close();
		System.out.println("created ");
		files = root.listFiles();
		for (int i = 0; i < files.length; i++) {
			System.out.println(files[i]);
		}
		*/
		while (true){
			System.out.println("hello");
			Thread.sleep(1000);
		}
	}
}
