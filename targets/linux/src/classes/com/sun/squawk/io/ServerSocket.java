package com.sun.squawk.io;

import java.io.*;
import com.sun.squawk.VM;

public class ServerSocket extends Socket {

    private static native int create(int port);
    private static native int accept0(int handle);

    public ServerSocket(int port) throws IOException {
	int pcb = create(port);
	if (pcb == 0) {
	    throw new IOException();
	}
	this.handle = pcb;
    }
    
    public Socket accept() throws IOException {
	int conn;
	while (true) {
	    conn = accept0(handle);
	    if (conn == -1) {
		throw new IOException();
	    } else if (conn == -2) {
		VM.waitForInterrupt(Events.ACCEPTED_EVENT);
	    } else {
		break;
	    }
	}
	return new Socket(conn);
    }
}
