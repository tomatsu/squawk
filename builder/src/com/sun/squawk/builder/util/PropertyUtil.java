package com.sun.squawk.builder.util;
import java.io.*;
import java.util.*;

public class PropertyUtil {
	public static void main(String[] args) throws IOException {
		Properties p = new Properties();
		p.load(new FileInputStream(args[0]));
		for (Map.Entry<Object,Object> entry : p.entrySet()) {
			String key = (String)entry.getKey();
			String value = (String)entry.getValue();
			System.out.println(key + "=" + value);
		}
	}
}
