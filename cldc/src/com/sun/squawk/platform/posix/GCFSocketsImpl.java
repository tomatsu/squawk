/*
 * Copyright 2004-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */

package com.sun.squawk.platform.posix;

import com.sun.squawk.VM;
import com.sun.squawk.VMThread;
import com.sun.squawk.platform.callouts.Pointer;
import com.sun.squawk.platform.GCFSockets;
import com.sun.squawk.platform.callouts.IntStar;
import com.sun.squawk.platform.posix.callouts.Ioctl;
import com.sun.squawk.platform.posix.callouts.LibC;
import com.sun.squawk.platform.posix.callouts.Socket;
import com.sun.squawk.platform.posix.callouts.NetDB;
import com.sun.squawk.util.Assert;
import com.sun.squawk.platform.posix.callouts.LibC.*;
import java.io.IOException;

/**
 * POSIX implementation of GCFSockets that calls the BSD socket API.
 */
public class GCFSocketsImpl implements GCFSockets {
    
    final Pointer INADDR_ANY = new Pointer(Inet.htonl(Socket.INADDR_ANY), 0);
    
    /** Read errno, try to clean up fd, and create exception. */
    private static IOException newError(int fd, String msg)  {
        int err_code = LibC.errno(); // @TODO: NOT THREAD_SAFE!
        VM.print(msg);
        VM.print(": errno: ");
        VM.print(err_code);
        VM.println();
        Socket.shutdown(fd, 2);
        Socket.close(fd);
        return new IOException(" errno: " + err_code + " on fd: " + fd + " during " + msg);
    }
    
    private void set_blocking_flags(int fd, boolean is_blocking) throws IOException{
        int flags = LibC.fcntl(fd, LibC.F_GETFL, 0);
        if (flags >= 0) {
            if (is_blocking == true) {
                flags &= ~LibC.O_NONBLOCK;
            } else {
                flags |= LibC.O_NONBLOCK;
            }
            int res = LibC.fcntl(fd, LibC.F_SETFL, flags);
            if (res != -1) {
                return;
            }
        }
        throw newError(fd, "set_blocking_flags");
    }
    
    /**
     * @inheritDoc
     */
    public int open0(String hostname, int port, int mode) throws IOException {
        // init_sockets(); win32 only
        int fd = -1;

        fd = Socket.socket(Socket.AF_INET, Socket.SOCK_STREAM, 0);
//System.err.println("Socket.socket fd: " + fd);
        if (fd < 0) {
            throw newError(fd, "socket create");
        }

        set_blocking_flags(fd, /*is_blocking*/ false);

        NetDB.Struct_HostEnt phostent;
        // hostname is always NUL terminated. See socket/Protocol.java for detail.
        phostent = NetDB.gethostbyname(hostname);
        if (phostent == null) {
            throw newError(fd, "gethostbyname");
        }

        Socket.Struct_SockAddr destination_sin = new Socket.Struct_SockAddr();
        destination_sin.sin_family = Socket.AF_INET;
        destination_sin.sin_port = Inet.htons((short) port);
        destination_sin.sin_addr = new Pointer(phostent.h_length);
        Pointer.copyBytes(phostent.h_addr_list[0], 0, destination_sin.sin_addr, 0, phostent.h_length);

//System.err.println("   addr  " + Socket.inet_ntoa(destination_sin.sin_addr));
//System.err.println("connect: hostname: " + hostname + " port: " + port + " mode: " + mode);

        if (Socket.connect(fd, destination_sin) < 0) {
            int err_code = LibC.errno(); // @TODO: NOT THREAD_SAFE!
            if (err_code == LibC.EINPROGRESS || err_code == LibC.EWOULDBLOCK) {
                // When the socket is ready for connect, it becomes *writable*
                // (according to BSD socket spec of select())
                VMThread.getSystemEvents().waitForWriteEvent(fd);
            } else {
                throw newError(fd, "connect");
            }
        }

        destination_sin.sin_addr.free();
        return fd;
    }
    
    /**
     * Opens a server TCP connection to clients.
     * Creates, binds, and listens
     *
     * @param port local TCP port to listen on
     * @param backlog listen backlog.
     *
     * @return a native handle to the network connection.
     * @throws IOException 
     */
    public int openServer(int port, int backlog) throws IOException {
        int fd = -1;

        fd = Socket.socket(Socket.AF_INET, Socket.SOCK_STREAM, 0);
        if (fd < 0) {
            throw newError(fd, "socket create");
        }
        
        set_blocking_flags(fd, /*is_blocking*/ false);

        IntStar option_val = new IntStar(1);
        if (Socket.setSockOpt(fd, Socket.SOL_SOCKET, Socket.SO_REUSEADDR, option_val) < 0) {
            throw newError(fd, "setSockOpt");
        }
        
        Socket.Struct_SockAddr local_sin = new Socket.Struct_SockAddr();
        local_sin.sin_family = Socket.AF_INET;
        local_sin.sin_port = Inet.htons((short) port);
        local_sin.sin_addr = INADDR_ANY;
        if (Socket.bind(fd, local_sin) < 0) {
            throw newError(fd, "bind");
        }
        
       if (Socket.listen(fd, backlog) < 0) {
            throw newError(fd, "listen");
        }
               
        return fd;     
    }
    
    /**
     * Accept client connections on server socket fd.
     * Blocks until a client connects.
     *
     * @param fd open server socket. See {@link #openServer}.
     *
     * @return a native handle to the network connection.
     * @throws IOException 
     */
    public int accept(int fd) throws IOException {
        VMThread.getSystemEvents().waitForReadEvent(fd);

        Socket.Struct_SockAddr remote_sin = new Socket.Struct_SockAddr();
        int newSocket = Socket.accept(fd, remote_sin);
        if (newSocket < 0) {
            throw newError(fd, "accept");
        }
        
        set_blocking_flags(newSocket, /*is_blocking*/ false);
        // we could read info about client from remote_sin, but don't need to.
        
        return newSocket;     
    }
    
    /**
     * @inheritDoc
     */
    public int readBuf(int fd, byte b[], int offset, int length) throws IOException {
        int result = LibC.read(fd, b, offset, length); // We rely on open0() for setting the socket to non-blocking

        if (result == 0) {
            // If remote side has shut down the connection gracefully, and all
            // data has been received, recv() will complete immediately with
            // zero bytes received.
            //
            // This is true for Win32/CE and Linux
            result = -1;
        } else if (result < 0) {
            int err_code = LibC.errno();
            if (err_code == LibC.EWOULDBLOCK) {
                VMThread.getSystemEvents().waitForReadEvent(fd);
                result = LibC.read(fd, b, offset, length); // We rely on open0() for setting the socket to non-blocking
            }
            LibC.errCheckNeg(result);
        }

        return result;
    }

    public int readByte(int fd) throws IOException {
        int result = -1;
        byte[] b = new byte[1];
        int n = readBuf(fd, b, 0, 1);
 
        if (n == 1) {
            result = b[0]; // do not sign-extend

            Assert.that(0 <= result && result <= 255, "no sign extension");
        } else if (n == 0) {
            // If remote side has shut down the connection gracefully, and all
            // data has been received, recv() will complete immediately with
            // zero bytes received.
            //
            // This is true for Win32/CE and Linux
            result = -1;
        }

        return result;
    }
    
    /**
     * @inheritDoc
     */
    public int writeBuf(int fd, byte buffer[], int off, int len) throws IOException {
        int result = 0;
        result = LibC.write(fd, buffer, off, len);// We rely on open0() for setting the socket to non-blocking

        if (result < 0) {
            int err_code = LibC.errno();
            if (err_code == LibC.EWOULDBLOCK) {
                VMThread.getSystemEvents().waitForWriteEvent(fd);
                result = LibC.write(fd, buffer, off, len); // We rely on open0() for setting the socket to non-blocking
            } 
            LibC.errCheckNeg(result);
        }

        return result;
    }

    /**
     * @inheritDoc
     */
    public int writeByte(int fd, int b) throws IOException {
        byte[] buf = new byte[1];
        int result = writeBuf(fd, buf, 0, 1);
        return result;
    }

    public int available0(int fd) throws IOException {
        Pointer buf = new Pointer(4);
        int err = Ioctl.ioctl(fd, Ioctl.FIONREAD, buf.address().toUWord().toPrimitive());
        int result = buf.getInt(0);
        buf.free();
        LibC.errCheckNeg(err);
//        System.err.println("available0(" + fd + ") = " + result);
        return result; 

    }

    public void close0(int fd) throws IOException {
        // NOTE: this would block the VM. A real implementation should
        // make this a async native method.
        Socket.shutdown(fd, 2);
        Socket.close(fd);
    }
    
    /**
     * set a socket option
     * 
     * @param socket socket descriptor
     * @param option_name 
     * @param option_value new value
     * @throws IOException on error
     */
    public void setSockOpt(int socket, int option_name, int option_value) throws IOException {
        IntStar value = new IntStar(option_value);
        int err = Socket.setSockOpt(socket, Socket.SOL_SOCKET, option_name, value);
        value.freeMemory();
        LibC.errCheckNeg(err);
    }
  
    /**
     * get a socket option
     * 
     * @param socket socket descriptor
     * @param option_name 
     * @throws IOException on error
     */
    public int getSockOpt(int socket, int option_name) throws IOException {
        IntStar value = new IntStar(0);
        int err = Socket.getSockOpt(socket, Socket.SOL_SOCKET, option_name, value);
        int result = value.get();
        value.freeMemory();
        LibC.errCheckNeg(err);
        return result;
    }

}
