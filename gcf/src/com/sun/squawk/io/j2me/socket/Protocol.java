package com.sun.squawk.io.j2me.socket;

import java.io.*;
import javax.microedition.io.*;
import com.sun.squawk.io.ConnectionBase;
import com.sun.squawk.io.Socket;

/**
 * Connection to the J2ME socket API.
 *
 * @version 1.0 1/16/2000
 */

public class Protocol extends ConnectionBase implements SocketConnection {

    /** Socket object used by native code */
    private Socket socket;

    /** Access mode */
    private int mode;

    /** Open count */
    int opens = 0;

    /** Connection open flag */
    private boolean copen = false;

    /** Input stream open flag */
    protected volatile boolean isopen = false;

    /** Output stream open flag */
    protected volatile boolean osopen = false;
    
    /** port number */
    private int port;
    
    /** remote host name used in open */
    private String remoteHostName;

    /**
     * Open the connection
     * @param name the target for the connection. It must be in this
     *        format: "//<name or IP number>:<port number>"
     * @param mode read/write mode of the connection (currently ignored).
     * @param timeouts A flag to indicate that the called wants timeout
     *        exceptions (currently ignored).
     * @return new connection
     * @throws IOException 
     */
    public Connection open(String protocol, String name, int mode, boolean timeouts)
		throws IOException {
		
        if (!name.startsWith("//")) {
            throw new IOException();
        }
        int i = name.indexOf(':');
        if (i < 0) {
            throw new IOException();
        }
        remoteHostName = name.substring(2, i);
        
        if (remoteHostName.length() == 0) {
            /*
             * If the open string is "socket://:nnnn" then we regard this as
             * "serversocket://:nnnn"
             */
            /* socket:// and socket://: are also valid serversocket urls */
			  com.sun.squawk.io.j2me.serversocket.Protocol con =
			  new com.sun.squawk.io.j2me.serversocket.Protocol();
			  //System.out.println("Found server socket. Trying name = " + name);
			  con.open("serversocket", name, mode, timeouts);
			  return con;
        }

        try {
            port = Integer.parseInt(name.substring(i+1));
        } catch (NumberFormatException e) {
            throw new IOException();
        }
        synchronized (this) {
            socket = new Socket(remoteHostName, port);
            opens++;
            copen = true;
            this.mode = mode;
        }
        return this;
	}
     
    /** default constructor used by GCF */
    public Protocol() {
    }
        
	/**
     * Open the connection
     * @param fd the accepted socket handle
     */
    public Protocol(Socket sock) {
    	synchronized (this) {
            this.socket = sock;
            opens++;
            copen = true;
            mode = Connector.READ_WRITE;
        }
	}

    /**
     * Ensure connection is open
     */
    void ensureOpen() throws IOException {
        if (!copen) {
            throw new IOException();
        }
    }

    /**
     * Returns an input stream for this socket.
     *
     * @return     an input stream for reading bytes from this socket.
     * @exception  IOException  if an I/O error occurs when creating the
     *                          input stream.
     */
    synchronized public InputStream openInputStream() throws IOException {
        ensureOpen();
        if ((mode&Connector.READ) == 0) {
            throw new IOException();
        }
        if (isopen) {
            throw new IOException();
        }
        isopen = true;
        InputStream in = socket.getInputStream();
        opens++;
        return in;
    }

    /**
     * Returns an output stream for this socket.
     *
     * @return     an output stream for writing bytes to this socket.
     * @exception  IOException  if an I/O error occurs when creating the
     *                          output stream.
     */
    synchronized public OutputStream openOutputStream() throws IOException {
        ensureOpen();
        if ((mode & Connector.WRITE) == 0) {
            throw new IOException();
        }
        if (osopen) {
            throw new IOException();
        }
        osopen = true;
        OutputStream os = socket.getOutputStream();
        opens++;
        return os;
    }

    /**
     * Close the connection.
     *
     * @exception  IOException  if an I/O error occurs when closing the
     *                          connection.
     */
    public synchronized void close() throws IOException {
        if (copen) {
            copen = false;
            realClose();
        }
    }

    /**
     * Close the connection.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    synchronized void realClose() throws IOException {
        if (--opens == 0) {
			socket.close();
        }
    }

    public void setSocketOption(byte option, int value) throws IllegalArgumentException, IOException {
        ensureOpen();
        
        socket.setSockOpt(option, value);
    }

    public int getSocketOption(byte option) throws IllegalArgumentException, IOException {
        ensureOpen();
        return socket.getSockOpt(option);
    }

    public String getLocalAddress() throws IOException {
        ensureOpen();
		int addr = socket.getLocalAddress();
		return (addr & 0xff) + "." + ((addr >> 8) & 0xff) + "." + ((addr >> 16) & 0xff) + "." + ((addr >> 24) & 0xff);		
    }

    public int getLocalPort() throws IOException {
        ensureOpen();
		return socket.getLocalPort();
    }

    public String getAddress() throws IOException {
        ensureOpen();
        return remoteHostName;
    }

    public int getPort() throws IOException {
        ensureOpen();
        return port;
    }
}

