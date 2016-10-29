package com.sun.squawk.io;

import com.sun.squawk.*;
import java.io.IOException;
import esp8266.Events;
   
public class NetUtil {
	private static native int resolve(String host);
	
	public static int gethostbyname(String host) throws IOException {
		System.out.println("gethostbyname " + host);
		int n = resolve(host);
		if (n != 0) {
			return n;
		}
		VM.waitForInterrupt(Events.RESOLVED_EVENT);
		System.out.println("gethostbyname done");
		return VMThread.currentThread().event;
	}

	public static String formatIPAddr(int addr) {
		return (addr & 0xff) + "." + ((addr >> 8) & 0xff) + "." + ((addr >> 16) & 0xff) + "." + ((addr >> 24) & 0xff);
	}

	public static String formatByteArray(byte[] array) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		int len = array.length;
		if (len > 0) {
			sb.append(Integer.toHexString(array[0] & 0xff));
		}
		for (int i = 1; i < len; i++) {
			sb.append(':');
			sb.append(Integer.toHexString(array[i] & 0xff));
		}
		sb.append(']');
		return sb.toString();
	}
}
