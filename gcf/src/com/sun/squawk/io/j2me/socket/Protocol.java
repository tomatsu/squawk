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
        InputStream in = new PrivateInputStream();
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
        OutputStream os = new PrivateOutputStream();
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

	/**
	 * Input stream for the connection
	 */
	class PrivateInputStream extends InputStream {

		/**
		 * End of file flag
		 */
		boolean eof = false;

		/**
		 * Constructor
		 * @param pointer to the connection object
		 *
		 * @exception  IOException  if an I/O error occurs.
		 */
		/* public */ PrivateInputStream() throws IOException {
		}

		/**
		 * Reads the next byte of data from the input stream.
		 * <p>
		 * Polling the native code is done here to allow for simple
		 * asynchronous native code to be written. Not all implementations
		 * work this way (they block in the native code) but the same
		 * Java code works for both.
		 *
		 * @return     the next byte of data, or <code>-1</code> if the end of the
		 *             stream is reached.
		 * @exception  IOException  if an I/O error occurs.
		 */
		synchronized public int read() throws IOException {
			int res;
			ensureOpen();
			if (eof) {
				return -1;
			}
			res = socket.read();
			if (res == -1) {
				eof = true;
			}
			return res;
		}

		/**
		 * Reads up to <code>len</code> bytes of data from the input stream into
		 * an array of bytes.
		 * <p>
		 * Polling the native code is done here to allow for simple
		 * asynchronous native code to be written. Not all implementations
		 * work this way (they block in the native code) but the same
		 * Java code works for both.
		 *
		 * @param      b     the buffer into which the data is read.
		 * @param      off   the start offset in array <code>b</code>
		 *                   at which the data is written.
		 * @param      len   the maximum number of bytes to read.
		 * @return     the total number of bytes read into the buffer, or
		 *             <code>-1</code> if there is no more data because the end of
		 *             the stream has been reached.
		 * @exception  IOException  if an I/O error occurs.
		 */
		synchronized public int read(byte b[], int off, int len)
            throws IOException {
			ensureOpen();
			if (eof) {
				return -1;
			}
			if (len == 0) {
				return 0;
			}
			// Check for array index out of bounds, and NullPointerException,
			// so that the native code doesn't need to do it
			int test = b[off];
			test = b[off + len - 1];
        
			int n = socket.read(b, off, len);
			if (n == -1) {
				eof = true;
			}

			return n;
		}

		/**
		 * Returns the number of bytes that can be read (or skipped over) from
		 * this input stream without blocking by the next caller of a method for
		 * this input stream.
		 *
		 * @return     the number of bytes that can be read from this input stream.
		 * @exception  IOException  if an I/O error occurs.
		 */
		synchronized public int available() throws IOException {
			ensureOpen();
			return socket.available();
		}

		/**
		 * Close the stream.
		 *
		 * @exception  IOException  if an I/O error occurs
		 */
		public synchronized void close() throws IOException {
			ensureOpen();
			realClose();
			isopen = false;
		}

	}

	/**
	 * Output stream for the connection
	 */
	class PrivateOutputStream extends OutputStream {

		/**
		 * Constructor
		 * @param pointer to the connection object
		 *
		 * @exception  IOException  if an I/O error occurs.
		 */
		/* public */ PrivateOutputStream() throws IOException {
		}

		/**
		 * Writes the specified byte to this output stream.
		 * <p>
		 * Polling the native code is done here to allow for simple
		 * asynchronous native code to be written. Not all implementations
		 * work this way (they block in the native code) but the same
		 * Java code works for both.
		 *
		 * @param      b   the <code>byte</code>.
		 * @exception  IOException  if an I/O error occurs. In particular,
		 *             an <code>IOException</code> may be thrown if the
		 *             output stream has been closed.
		 */
		synchronized public void write(int b) throws IOException {
			ensureOpen();
			while (true) {
				int res = socket.write(b);
				if (res != 0) {
					// IMPL_NOTE: should EOFException be thrown if write fails?
					return;
				}
			}
		}

		/**
		 * Writes <code>len</code> bytes from the specified byte array
		 * starting at offset <code>off</code> to this output stream.
		 * <p>
		 * Polling the native code is done here to allow for simple
		 * asynchronous native code to be written. Not all implementations
		 * work this way (they block in the native code) but the same
		 * Java code works for both.
		 *
		 * @param      b     the data.
		 * @param      off   the start offset in the data.
		 * @param      len   the number of bytes to write.
		 * @exception  IOException  if an I/O error occurs. In particular,
		 *             an <code>IOException</code> is thrown if the output
		 *             stream is closed.
		 */
		synchronized public void write(byte b[], int off, int len)
			throws IOException {
			ensureOpen();
			if (len == 0) {
				return;
			}

			// Check for array index out of bounds, and NullPointerException,
			// so that the native code doesn't need to do it
			int test = b[off] + b[off + len - 1];

			int n = 0;
			while (true) {
				n += socket.write(b, off + n, len - n);
				if (n == len) {
					break;
				}
			}
		}

		/**
		 * Close the stream.
		 *
		 * @exception  IOException  if an I/O error occurs
		 */
		public synchronized void close() throws IOException {
			ensureOpen();
			realClose();
			osopen = false;
		}
	}
}

