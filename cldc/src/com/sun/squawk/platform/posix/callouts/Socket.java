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

package com.sun.squawk.platform.posix.callouts;

import com.sun.squawk.Address;
import com.sun.squawk.platform.callouts.*;

/**
 *
 * This is the Java interface to the BSD sockets API
 */
public class Socket  extends LibC {
    //            /* Supported address families. */
    //#define AF_UNSPEC	0
    //#define AF_UNIX		1	/* Unix domain sockets 		*/
    //#define AF_INET		2	/* Internet IP Protocol 	*/
    //#define AF_AX25		3	/* Amateur Radio AX.25 		*/
    //#define AF_IPX		4	/* Novell IPX 			*/
    //#define AF_APPLETALK	5	/* Appletalk DDP 		*/
    //#define	AF_NETROM	6	/* Amateur radio NetROM 	*/
    //#define AF_BRIDGE	7	/* Multiprotocol bridge 	*/
    //#define AF_AAL5		8	/* Reserved for Werner's ATM 	*/
    //#define AF_X25		9	/* Reserved for X.25 project 	*/
    //#define AF_INET6	10	/* IP version 6			*/
    //#define AF_MAX		12	/* For now.. */
    public final static int AF_INET = 2;
    
    /* Socket types. */
    //#define SOCK_RDM	4		/* reliably-delivered message	*/
    //#define SOCK_SEQPACKET	5		/* sequential packet socket	*/
    //#define SOCK_PACKET	10		/* linux specific way of	*/
    public final static int SOCK_STREAM = 1; /* stream (connection) socket	*/
    public final static int SOCK_DGRAM = 2;  /* datagram (conn.less) socket	*/
    public final static int SOCK_RAW = 3;    /* raw socket			*/
    
    /* Definitions of bits in internet address integers. */
    public final static int INADDR_ANY = 0x00000000;
    
    /* Socket options */
    /*
     * Level number for (get/set)sockopt() to apply to socket itself.
     */
    public final static int SOL_SOCKET = 0xffff;		/* options for socket level */
    /*
     * Option flags per-socket.
     */
    public final static int SO_DEBUG = 0x0001;		/* turn on debugging info recording */
    public final static int SO_ACCEPTCONN = 0x0002;		/* socket has had listen() */
    public final static int SO_REUSEADDR = 0x0004;		/* allow local address reuse */
    public final static int SO_KEEPALIVE = 0x0008;		/* keep connections alive */
    public final static int SO_DONTROUTE = 0x0010;		/* just use interface addresses */
    public final static int SO_BROADCAST = 0x0020;		/* permit sending of broadcast msgs */
//#if !defined(_POSIX_C_SOURCE) || defined(_DARWIN_C_SOURCE)
//#define	SO_USELOOPBACK	0x0040		/* bypass hardware when possible */
//#define SO_LINGER	0x0080          /* linger on close if data present (in ticks) */
//#else
//#define SO_LINGER	0x1080          /* linger on close if data present (in seconds) */
//#endif	/* (!_POSIX_C_SOURCE || _DARWIN_C_SOURCE) */
    public final static int SO_OOBINLINE = 0x0100;		/* leave received OOB data in line */
//#if !defined(_POSIX_C_SOURCE) || defined(_DARWIN_C_SOURCE)
//#define	SO_REUSEPORT	0x0200		/* allow local address & port reuse */
//#define	SO_TIMESTAMP	0x0400		/* timestamp received dgram traffic */
//#ifndef __APPLE__
//#define	SO_ACCEPTFILTER	0x1000		/* there is an accept filter */
//#else
//#define SO_DONTTRUNC	0x2000		/* APPLE: Retain unread data */
//					/*  (ATOMIC proto) */
//#define SO_WANTMORE		0x4000		/* APPLE: Give hint when more data ready */
//#define SO_WANTOOBFLAG	0x8000		/* APPLE: Want OOB in MSG_FLAG on receive */
//#endif
//#endif	/* (!_POSIX_C_SOURCE || _DARWIN_C_SOURCE) */ 
        
    /* Actual function pointers for calling out to libs */
    private static final FunctionPointer socketPtr = FunctionPointer.lookup("socket");
    private static final FunctionPointer connectPtr  = FunctionPointer.lookup("connect");
    private static final FunctionPointer bindPtr = FunctionPointer.lookup("bind");
    private static final FunctionPointer listenPtr = FunctionPointer.lookup("listen");
    private static final FunctionPointer acceptPtr = FunctionPointer.lookup("accept");
    private static final FunctionPointer shutdownPtr = FunctionPointer.lookup("shutdown");
    private static final FunctionPointer inet_ntoaPtr  = FunctionPointer.lookup("inet_ntoa");
    private static final FunctionPointer inet_atonPtr = FunctionPointer.lookup("inet_aton");
    private static final FunctionPointer setSockOptPtr = FunctionPointer.lookup("setsockopt");
    private static final FunctionPointer getSockOptPtr = FunctionPointer.lookup("getsockopt");
    
    /* pure static class */
    private Socket() {}

    /**
     * socket() creates an endpoint for communication and returns a descriptor.
     * 
     * @param domain specifies a communications domain within which communication
     *               will take place; this selects the protocol family which should
     *               be used. The currently understood formats are:
     *               AF_UNIX, AF_INET, AF_ISO, AF_NS, AF_IMPLINK
     * @param type   specifies the semantics of communication.  Currently defined types are:
     *               SOCK_STREAM, SOCK_DGRAM, SOCK_RAW, SOCK_SEQPACKET, SOCK_RDM
     * @param protocol The protocol number to use is particular to the
     *               communication domain in which communication is to take place; see
     *                protocols(5).
     * @return  A -1 is returned if an error occurs, otherwise the return value is a
     *          descriptor referencing the socket.
     */
     public static int socket(int domain, int type, int protocol) {
         return socketPtr.call3(domain, type, protocol);
     }    
     
    /**
     * initiate a connection on a socket.
     * 
     * @param socket socket descriptor
     * @param address ptr to a SockAddr_In buffer
     * @param size 
     * @return  A -1 is returned if an error occurs, otherwise zero is returned
     */
     private static int connect0(int socket, Address address, int size) {
         return connectPtr.call3(socket, address, size);
     }
     
    /**
     * initiate a connection on a socket.
     * 
     * @param socket socket descriptor
     * @param address ptr to a SockAddr_In buffer
     * @return  A -1 is returned if an error occurs, otherwise the return value is a
     *          descriptor referencing the socket.
     */
    public static int connect(int socket, Struct_SockAddr address) {
        address.allocateMemory();
        address.write();
System.err.println("Socket.connect(" + socket + ", " + address);
System.err.println("   mem " + address.getMemory());      
        int result = connect0(socket, address.getMemory().address(), address.getMemory().size().toInt());
        address.freeMemory();
        return result;
    }
    
    /**
     * bind a socket to a port
     * 
     * @param socket socket descriptor
     * @param address ptr to a SockAddr_In buffer
     * @param size 
     * @return  A -1 is returned if an error occurs, otherwise zero is returned
     */
     private static int bind0(int socket, Address address, int size) {
         return bindPtr.call3(socket, address, size);
     }
     
    /**
     * bind a socket to a port
     * 
     * @param socket socket descriptor
     * @param myaddress ptr to a SockAddr_In buffer
     * @return  A -1 is returned if an error occurs, otherwise the return value is a
     *          descriptor referencing the socket.
     */
    public static int bind(int socket, Struct_SockAddr myaddress) {
        myaddress.allocateMemory();
        myaddress.write();
//System.err.println("Socket.connect(" + socket + ", " + address);
//System.err.println("   mem " + address.getMemory());      
        int result = bind0(socket, myaddress.getMemory().address(), myaddress.getMemory().size().toInt());
        myaddress.freeMemory();
        return result;
    }
    
    /**
     * accept a connection from a client
     * 
     * @param socket socket descriptor
     * @param remoteAddress ptr to a SockAddr_In buffer
     * @param sizeStar 
     * @return  A -1 is returned if an error occurs, otherwise zero is returned
     */
     private static int accept0(int socket, Address remoteAddress, Address size) {
// System.err.println("Socket.accept0(" + socket + ", " + remoteAddress.toUWord().toPrimitive() + ", " + size.toUWord().toPrimitive() + ")");
         return acceptPtr.call3(socket, remoteAddress, size);
     }
     
    /**
     * accept a connection from a client
     * 
     * @param socket socket descriptor
     * @param remoteAddress ptr to a SockAddr_In buffer that will contain the address of the remote client
     * @return  A -1 is returned if an error occurs, otherwise the return value is a
     *          descriptor referencing the socket.
     */
    public static int accept(int socket, Struct_SockAddr remoteAddress) {
        remoteAddress.allocateMemory();
        IntStar addr_len = new IntStar(remoteAddress.getMemory().size().toInt());
        remoteAddress.write();
//System.err.println("Socket.accept(" + socket);
        int result = accept0(socket, remoteAddress.getMemory().address(), addr_len.getMemory().address());
        int errno = LibC.errno();
        remoteAddress.read();
//System.err.println("   errno " + errno);
//System.err.println("   remote address " + remoteAddress);
        remoteAddress.freeMemory();
        return result;
    }
    
    
    /**
     * listen for connections on socket
     * 
     * @param socket socket descriptor
     * @param backlog
     * @return  A -1 is returned if an error occurs, otherwise zero is returned
     */
     private static int listen0(int socket, int backlog) {
         return listenPtr.call2(socket, backlog);
     }
     
    /**
     * listen for connections on socket
     * 
     * @param socket socket descriptor
     * @param backlog
     * @return  A -1 is returned if an error occurs, otherwise the return value is a
     *          descriptor referencing the socket.
     */
    public static int listen(int socket, int backlog) {
//System.err.println("Socket.listen(" + socket + ", " + socket);
        int result = listen0(socket, backlog);
        return result;
    }
    
    /**
     * initiate a connection on a socket.
     * 
     * @param socket socket descriptor
     * @param how  If how is SHUT_RD, further receives will be disallowed.  If how
     *             is SHUT_WR, further sends will be disallowed.  If how is SHUT_RDWR, further sends and
     *             receives will be disallowed.
     * @return  A -1 is returned if an error occurs, otherwise zero is returned
     */
     public static int shutdown(int socket, int how) {
         return shutdownPtr.call2(socket, how);
     }
     
    /** C STRUCTURE sockaddr_in  /
     struct sockaddr_in {
        u_char  sin_len;     1
        u_char  sin_family;  1
        u_short sin_port;    2
        struct  in_addr sin_addr; 4
        char    sin_zero[8];     8
     }; 
     * 
     struct sockaddr {
	__uint8_t	sa_len;		/* total length 
	sa_family_t	sa_family;	/* [XSI] address family 
	char		sa_data[14];	/* [XSI] addr value (actually larger) 
};
     */
    public final static class Struct_SockAddr extends Structure {
        public static final byte SIZEOF_SockAddr = 16;
        public static final byte SIZEOF_in_addr_t = 4;
        
        /** u_char */
        public int sin_len;
        
        /** u_char */
        public int sin_family;
        
        /** u_short */
        public int sin_port;
        
        /** in_addr is an opaque type, that is typically a 4-byte int. APIs often use ptr to 
         * in_addr. So don't cache value in field, just use ptr to underlying value.*/
        public Pointer sin_addr;
        
        /* public long  sin_zero; // why bother in proxy? */
        
        
        public Struct_SockAddr() {
            sin_len = SIZEOF_SockAddr; // default....
        }
        
        public void read() {
            Pointer p = getMemory();
            sin_len = p.getByte(0) & 0xFF;
            sin_family = p.getByte(1) & 0xFF;
            sin_port = p.getShort(2) & 0xFFFF;
            sin_addr = p.getPointer(4, 4);
        }

        public void write() {
            Pointer p = getMemory();
            clear();
            p.setByte(0, (byte)sin_len);
            p.setByte(1, (byte)sin_family);
            p.setShort(2, (short)sin_port);
            if (sin_addr != null && !sin_addr.address().isZero()) {
                Pointer.copyBytes(sin_addr, 0, p, 4, SIZEOF_in_addr_t); // odd way to more easily port if sin_addr is ever > 4 bytes.
            }
        }

        public int size() {
            return SIZEOF_SockAddr;
        }
        
        public String toString() {
            return "Struct_SockAddr{len: " + sin_len + ", family: " + sin_family + ", port: " + sin_port + ", sin_addr: " + sin_addr + "}";
        }
                 
    } /* SockAddr */
    
    /**
     * Interprets the specified character string as an Internet address, placing the
     * address into the structure provided.  It returns 1 if the string was successfully interpreted, or 0 if
     * the string is invalid
     * 
     * @param strAddr      const char *
     * @param structAddr   struct in_addr *
     * @return true if success
     */
    private static boolean inet_aton0(Address strAddr, Address structAddr) {
        int result = inet_atonPtr.call2(strAddr, structAddr);
        return (result == 0) ? false : true;
    }

    /**
     * Interprets the specified character string as an Internet address, placing the
     * address into the structure provided.  It returns 1 if the string was successfully interpreted, or 0 if
     * the string is invalid
     * 
     * @param str 
     * @param in_addr 
     * @return true if success
     */
    public static boolean inet_aton(String str, Pointer in_addr) {
        Pointer name0 = Pointer.createStringBuffer(str);
        boolean result = inet_aton0(name0.address(), in_addr.address());
        name0.free();
        return result;
    }

    /**
     * Takes an Internet address and returns an ASCII string representing the address
     * in `.' notation
     * 
     * @param structAddr 
     * @return address of char*
     */
    private static Address inet_ntoa0(Address structAddr) {
        return Address.fromPrimitive(inet_ntoaPtr.call1(structAddr));
    }
    
    /**
     * Takes an Internet address and returns string representing the address
     * in `.' notation
     * 
     * @param structAddr 
     * @return String
     */
    public static String inet_ntoa(Pointer structAddr) {
        Address result =  inet_ntoa0(structAddr.address());
        return Pointer.NativeUnsafeGetString(result);
    }
    
    /**
     * set a socket option
     * 
     * @param socket socket descriptor
     * @param remoteAddress ptr to a SockAddr_In buffer
     * @param size 
     * @return  A -1 is returned if an error occurs, otherwise zero is returned
     */
     private static int setSockOpt0(int socket, int level, int option_name, Address option_value, int option_len) {
         return setSockOptPtr.call5(socket, level, option_name, option_value, option_len);
     }
     
    /**
     * set a socket option
     * 
     * @param socket socket descriptor
     * @param level 
     * @param option_name 
     * @param option_value 
     * @return  A -1 is returned if an error occurs, otherwise the return value is a
     *          descriptor referencing the socket.
     */
    public static int setSockOpt(int socket, int level, int option_name, Structure option_value) {
        option_value.allocateMemory();
        option_value.write();
//System.err.println("Socket.setSockOpt(" + socket);
        int result = setSockOpt0(socket, level, option_name, option_value.getMemory().address(), option_value.getMemory().size().toInt());
//System.err.println("   address " + option_value);
        option_value.freeMemory();
        return result;
    }
    
    /**
     * get a socket option
     * 
     * @return  A -1 is returned if an error occurs, otherwise zero is returned
     */
     private static int getSockOpt0(int socket, int level, int option_name, Address option_value, Address option_len) {
         return getSockOptPtr.call5(socket, level, option_name, option_value, option_len);
     }
     
    /**
     * get a socket option
     * 
     * @param socket socket descriptor
     * @param level 
     * @param option_name 
     * @param option_value 
     * @return  A -1 is returned if an error occurs, otherwise the return value is a
     *          descriptor referencing the socket.
     */
    public static int getSockOpt(int socket, int level, int option_name, Structure option_value) {
        IntStar opt_len = new IntStar(option_value.getMemory().size().toInt());
        option_value.allocateMemory();
        
System.err.println("Socket.getSockOpt(" + socket);
        int result = getSockOpt0(socket, level, option_name, option_value.getMemory().address(), opt_len.getMemory().address());
System.err.println("   address " + option_value);
System.err.println("   real opt_len " + opt_len.get());
        option_value.read();
        option_value.freeMemory();
        opt_len.freeMemory();
        return result;
    }
}
