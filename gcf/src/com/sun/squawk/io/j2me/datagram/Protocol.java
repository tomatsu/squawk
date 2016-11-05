package com.sun.squawk.io.j2me.datagram;

import java.io.*;
import javax.microedition.io.*;
import com.sun.squawk.io.*;

/**
 * This implements the "datagram://" protocol for J2SE in a not very
 * efficient way.
 *
 * @version 1.1 11/19/99
 */
public class Protocol extends ConnectionBase implements DatagramConnection, UDPDatagramConnection {

    DatagramSocket endpoint;
	String address;
    protected boolean open;

    public String getLocalAddress() throws IOException {
        if (!open) {
            throw new IOException("Connection closed");
        }
		return NetUtil.formatIPAddr(endpoint.getLocalAddress());
    }

    public int getLocalPort() throws IOException {
        if (!open) {
            throw new IOException("Connection closed");
        }
        return endpoint.getLocalPort();
    } 
	
    /**
     * Open a connection to a target. <p>
     *
     * The name string for this protocol should be:
     * "[address:][port]"
     *
     * @param name the target of the connection
     * @param writeable a flag that is true if the caller intends to
     *        write to the connection.
     * @param timeouts A flag to indicate that the called wants timeout exceptions
     */
    public Connection open(String protocol, String name, int mode, boolean timeout) throws IOException, ConnectionNotFoundException {
		if (!name.startsWith("//")) {
            throw new IllegalArgumentException();
        }
		int idx = name.indexOf(':', 2);
		boolean server;
		if (idx < 0) {
			throw new IllegalArgumentException();
		} else if (idx == 2) { // no host
			server = true;
		} else {
			server = false;
		}
		String port = name.substring(idx + 1);
		if ("".equals(port) || !server) {
			endpoint = new DatagramSocket();
		} else if (server) {
			endpoint = new DatagramSocket(Integer.parseInt(port));
		}
		address = protocol + ":" + name;
        open = true;
		return this;
    }

    public int getNominalLength() throws IOException {
        if (!open) {
            throw new IOException("Connection closed");
        }
        return getMaximumLength();
	}
	
    /**
     * Get the maximum length a datagram can be.
     *
     * @return    address      The length
     */
    public int getMaximumLength() throws IOException {
		return endpoint.getReceiveBufferSize();
    }
	
    /**
     * Send a datagram
     *
     * @param     dgram        A datagram
     * @exception IOException  If an I/O error occurs
     */
    public void send(Datagram dgram) throws IOException {
        DatagramObject dh = (DatagramObject)dgram;
        endpoint.send(dh.remoteaddr, dh.remoteport, dh.data, dh.offset, dh.len);
    }

    /**
     * Receive a datagram
     *
     * @param     dgram        A datagram
     * @exception IOException  If an I/O error occurs
     */
    public void receive(Datagram dgram) throws IOException {
        DatagramObject dh = (DatagramObject)dgram;
        dh.len = endpoint.read(dh.data, dh.offset, dh.data.length - dh.offset);
        dh.pointer = 0;
    }

    /**
     * Close the connection to the target.
     *
     * @exception IOException  If an I/O error occurs
     */
    public void close() throws IOException {
        if (open) {
            open = false;
        }
        endpoint.close();
    }

    /**
     * Get a new datagram object
     *
     * @return                 A new datagram
     */
    public Datagram newDatagram(int size)  throws IllegalArgumentException, IOException {
        return newDatagram(new byte[size], size);
    }

    /**
     * Get a new datagram object
     *
     * @param     addr         The address to which the datagram must go
     * @return                 A new datagram
     */
    public Datagram newDatagram(int size, String addr) throws IOException,
															  IllegalArgumentException {
        return newDatagram(new byte[size], size, addr);
    }

    /**
     * Get a new datagram object
     *
     * @return                 A new datagram
     */
    public Datagram newDatagram(byte[] buf, int size)  throws IOException, IllegalArgumentException {
		return newDatagram(buf, size, address);
    }

    /**
     * Get a new datagram object
     *
     * @param     addr         The address to which the datagram must go
     * @return                 A new datagram
     */
    public Datagram newDatagram(byte[] buf, int size, String addr) throws IOException, IllegalArgumentException {
		if (size < 0 || buf == null) {
			throw new IllegalArgumentException();
        }
        return new DatagramObject(buf, size, addr);
    }
}
