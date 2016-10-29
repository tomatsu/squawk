package com.sun.squawk.io;

import com.sun.squawk.*;
import java.io.IOException;
import esp8266.Events;

public class ServerSocket extends Socket {
	private static native int create(int port);
	private static native int accept0(int handle);
	
	private int handle;
	
	public ServerSocket(int port) throws IOException {
		int pcb = create(port);
		if (pcb == 0) {
			throw new IOException();
		}
		this.handle = pcb;
	}

	public Socket accept() throws IOException {
		int conn = accept0(handle);
		if (conn == 0) {
			throw new IOException();
		}
		VM.waitForInterrupt(Events.ACCEPTED_EVENT);
		return new Socket(conn);
	}
}
