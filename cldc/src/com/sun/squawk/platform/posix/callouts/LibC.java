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
import com.sun.squawk.GC;
import com.sun.squawk.platform.callouts.*;
import com.sun.squawk.util.Assert;
import java.io.IOException;

/**
 *
 * @author dw29446
 */
public class LibC {

    public final static int EPERM = 1;		/* Operation not permitted */

    public final static int ENOENT = 2;		/* No such file or directory */

    public final static int ESRCH = 3;		/* No such process */

    public final static int EINTR = 4;		/* Interrupted system call */

    public final static int EIO = 5;		/* Input/output error */

    public final static int ENXIO = 6;		/* Device not configured */

    public final static int E2BIG = 7;		/* Argument list too long */

    public final static int ENOEXEC = 8;		/* Exec format error */

    public final static int EBADF = 9;		/* Bad file descriptor */

    public final static int ECHILD = 10;		/* No child processes */

    public final static int EDEADLK = 11;		/* Resource deadlock avoided */
    /* 11 was EAGAIN */

    public final static int ENOMEM = 12;		/* Cannot allocate memory */

    public final static int EACCES = 13;		/* Permission denied */

    public final static int EFAULT = 14;		/* Bad address */

    public final static int EBUSY = 16;		/* Device busy */

    public final static int EEXIST = 17;		/* File exists */

    public final static int EXDEV = 18;		/* Cross-device link */

    public final static int ENODEV = 19;		/* Operation not supported by device */

    public final static int ENOTDIR = 20;		/* Not a directory */

    public final static int EISDIR = 21;		/* Is a directory */

    public final static int EINVAL = 22;		/* Invalid argument */

    public final static int ENFILE = 23;		/* Too many open files in system */

    public final static int EMFILE = 24;		/* Too many open files */

    public final static int ENOTTY = 25;		/* Inappropriate ioctl for device */

    public final static int ETXTBSY = 26;		/* Text file busy */

    public final static int EFBIG = 27;		/* File too large */

    public final static int ENOSPC = 28;		/* No space left on device */

    public final static int ESPIPE = 29;		/* Illegal seek */

    public final static int EROFS = 30;		/* Read-only file system */

    public final static int EMLINK = 31;		/* Too many links */

    public final static int EPIPE = 32;		/* Broken pipe */

    /* math software */
    public final static int EDOM = 33;		/* Numerical argument out of domain */

    public final static int ERANGE = 34;		/* Result too large */

    /* non-blocking and interrupt i/o */
    public final static int EAGAIN = 35;		/* Resource temporarily unavailable */

    public final static int EWOULDBLOCK = EAGAIN;		/* Operation would block */

    public final static int EINPROGRESS = 36;		/* Operation now in progress */

    public final static int EALREADY = 37;		/* Operation already in progress */

    /* ipc/network software -- argument errors */
    public final static int ENOTSOCK = 38;		/* Socket operation on non-socket */

    public final static int EDESTADDRREQ = 39;		/* Destination address required */

    public final static int EMSGSIZE = 40;		/* Message too long */

    public final static int EPROTOTYPE = 41;		/* Protocol wrong type for socket */

    public final static int ENOPROTOOPT = 42;		/* Protocol not available */

    public final static int EPROTONOSUPPORT = 43;		/* Protocol not supported */

    public final static int ENOTSUP = 45;		/* Operation not supported */

    public final static int EAFNOSUPPORT = 47;		/* Address family not supported by protocol family */

    public final static int EADDRINUSE = 48;		/* Address already in use */

    public final static int EADDRNOTAVAIL = 49;		/* Can't assign requested address */

    /* ipc/network software -- operational errors */
    public final static int ENETDOWN = 50;		/* Network is down */

    public final static int ENETUNREACH = 51;		/* Network is unreachable */

    public final static int ENETRESET = 52;		/* Network dropped connection on reset */

    public final static int ECONNABORTED = 53;		/* Software caused connection abort */

    public final static int ECONNRESET = 54;		/* Connection reset by peer */

    public final static int ENOBUFS = 55;		/* No buffer space available */

    public final static int EISCONN = 56;		/* Socket is already connected */

    public final static int ENOTCONN = 57;		/* Socket is not connected */

    public final static int ETIMEDOUT = 60;		/* Operation timed out */

    public final static int ECONNREFUSED = 61;		/* Connection refused */

    public final static int ELOOP = 62;	/* Too many levels of symbolic links */

    public final static int ENAMETOOLONG = 63;		/* File name too long */

    /* should be rearranged */
    public final static int EHOSTUNREACH = 65;		/* No route to host */

    public final static int ENOTEMPTY = 66;		/* Directory not empty */

    /* quotas & mush */
    public final static int EDQUOT = 69;		/* Disc quota exceeded */

    public final static int ENOLCK = 77;		/* No locks available */

    public final static int ENOSYS = 78;		/* Function not implemented */

    public final static int EOVERFLOW = 84;		/* Value too large to be stored in data type */

    public final static int ECANCELED = 89;		/* Operation canceled */

    public final static int EIDRM = 90;		/* Identifier removed */

    public final static int ENOMSG = 91;		/* No message of desired type */

    public final static int EILSEQ = 92;		/* Illegal byte sequence */

    public final static int EBADMSG = 94;		/* Bad message */

    public final static int EMULTIHOP = 95;		/* Reserved */

    public final static int ENODATA = 96;		/* No message available on STREAM */

    public final static int ENOLINK = 97;		/* Reserved */

    public final static int ENOSR = 98;		/* No STREAM resources */

    public final static int ENOSTR = 99;		/* Not a STREAM */

    public final static int EPROTO = 100;		/* Protocol error */

    public final static int ETIME = 101;		/* STREAM ioctl timeout */


    /* command values */
    public final static int	F_DUPFD		= 0;		/* duplicate file descriptor */
    public final static int	F_GETFD		= 1;		/* get file descriptor flags */
    public final static int	F_SETFD		= 2;		/* set file descriptor flags */
    public final static int	F_GETFL		= 3;	/* get file status flags */
    public final static int	F_SETFL		= 4;		/* set file status flags */

    /*
     * File status flags: these are used by open(2), fcntl(2).
     * They are also used (indirectly) in the kernel file structure f_flags,
     * which is a superset of the open/fcntl flags.  Open flags and f_flags
     * are inter-convertible using OFLAGS(fflags) and FFLAGS(oflags).
     * Open/fcntl flags begin with O_; kernel-internal flags begin with F.
     */
    /* open-only flags */
    public final static int	O_RDONLY	= 0x0000;		/* open for reading only */
    public final static int	O_WRONLY	= 0x0001;		/* open for writing only */
    public final static int	O_RDWR		= 0x0002;		/* open for reading and writing */
    public final static int	O_ACCMODE	= 0x0003;		/* mask for above modes */

    public final static int	O_NONBLOCK	= 0x0004;		/* no delay */
    public final static int	O_APPEND	= 0x0008;		/* set append mode */
    public final static int	O_SYNC		= 0x0080;		/* synchronous writes */
    public final static int	O_CREAT		= 0x0200;		/* create if nonexistant */
    public final static int	O_TRUNC		= 0x0400;		/* truncate to zero length */
    public final static int	O_EXCL		= 0x0800;		/* error if already exists */


    /* pure static class */
     LibC() {}
    
     private static final VarPointer errnoPtr = VarPointer.lookup("errno", 4);

    /**
     * Reads the C-level errno variable
     *
     * @return the error number
     */
    public static int errno() {
        return errnoPtr.getInt();
    }
    
    /**
     * Utility class that checks result. If result indicates an error (-1), then reads errno() and throws exception.
     * @param result
     * @return result
     * @throws java.io.IOException
     */
    public static int errCheckNeg(int result) throws IOException {
        if (result == -1) {
            throw new IOException("errno: " + errno());
        } else {
            return result;
        }
    }
    
    /**
     * Utility class that warns if the result indicates an error. 
     * If result indicates an error (-1), then reads errno() and prints a warning message.
     * @param result
     * @return result
     */
    public static int errWarnNeg(int result) {
        if (result == -1) {
            System.err.println("WARNING: errno: " + errno());
        }
        return result;
    }
    
    private static final FunctionPointer fcntlPtr = FunctionPointer.lookup("fcntl");
    private static final FunctionPointer openPtr  = FunctionPointer.lookup("open");
    private static final FunctionPointer closePtr = FunctionPointer.lookup("close");
    private static final FunctionPointer readPtr  = FunctionPointer.lookup("read");
    private static final FunctionPointer writePtr = FunctionPointer.lookup("write");
    private static final FunctionPointer lseekPtr = FunctionPointer.lookup("lseek");
    private static final FunctionPointer fsyncPtr = FunctionPointer.lookup("fsync");    

    /**
     * provides for control over descriptors.
     *
     * @param fd a descriptor to be operated on by cmd
     * @param cmd one of the cmd constants
     * @param arg 
     * @return a value that depends on the cmd.
     */
    public static int fcntl(int fd, int cmd, int arg) {
        return fcntlPtr.call3(fd, cmd, arg);
    }
    
    /**
     * open or create a file for reading or writing
     * 
     * @param namestr
     * @param oflag
     * @return If successful, returns a non-negative integer, termed a file descriptor.  Returns
     *         -1 on failure, and sets errno to indicate the error.
     */
    public static int open0(Address namestr, int oflag) {
        return openPtr.call2(namestr, oflag);
    }
    
    /**
     * open or create a file for reading or writing
     * 
     * @param name String
     * @param oflag
     * @return If successful, returns a non-negative integer, termed a file descriptor.  Returns
     *         -1 on failure, and sets errno to indicate the error.
     */
    public static int open(String name, int oflag) {
        Pointer name0 = Pointer.createStringBuffer(name);
        int result = open0(name0.address(), oflag);
        name0.free();
        return result;
    }
    
    /**
     * delete a descriptor
     * 
     * @param fd a descriptor to be operated on by cmd
     * @return Upon successful completion, a value of 0 is returned.  Otherwise, a value of -1 is returned
     *         and the global integer variable errno is set to indicate the error.
     */
    public static int close(int fd) {
        return closePtr.call1(fd);
    }
    
    /**
     * Flush output on a descriptor
     * 
     * @param fd a descriptor to be flushed
     * @return Upon successful completion, a value of 0 is returned.  Otherwise, a value of -1 is returned
     *         and the global integer variable errno is set to indicate the error.
     */
    public static int fsync(int fd) {
        return fsyncPtr.call1(fd);
    }

    /**
     * read input
     * 
     * @param fd file descriptor
     * @param buf data buffer to read into
     * @param nbyte number of bytes to read
     * @return the number of bytes actually read is returned.  Upon reading end-of-file, zero
     *         is returned.  If error, a -1 is returned and the global variable errno is set to indicate
     *         the error
     */
    public static int read0(int fd, Address buf, int nbyte) {
        return readPtr.call3(fd, buf, nbyte);
    }
    
    /**
     * Get a pointer to the interior of a Java array. 
     * Check that the range requested is within the array bounds.
     * @param array
     * @param offset
     * @param len
     * @return
     */
    private static Address getPtrToArray(byte[] array, int offset, int len) {
        Assert.that(GC.setGCEnabled(false) == false);
        int alen = array.length;
        if (offset < 0 || (offset + len) > alen) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return Address.fromObject(array).add(offset);
    }
    
    /**
     * read input
     * 
     * @param fd file descriptor
     * @param buf data buffer to read into
     * @param offset offset into data buffer where read data should begin going to.
     * @param nbyte number of bytes to read
     * @return the number of bytes actually read is returned.  Upon reading end-of-file, zero
     *         is returned.  If error, a -1 is returned and the global variable errno is set to indicate
     *         the error
     */
    public static int read(int fd, byte[] buf, int offset, int nbyte) {
        boolean oldGCState = GC.setGCEnabled(false);
        try {
            /*------------------- DISABLE GC: ---------------------------*/
            Address ptr = getPtrToArray(buf, offset, nbyte);
            int result = read0(fd, ptr, nbyte);
            return result;
        } finally {
            GC.setGCEnabled(oldGCState);
            /*------------------- ENDABLE GC: ---------------------------*/
        }
    }
    
    /**
     * write output
     * 
     * @param fd file descriptor
     * @param buf data buffer to write
     * @param nbyte number of bytes to read
     * @return the number of bytes which were written is returned.  If error,
     *         -1 is returned and the global variable errno is set to indicate the error.
     */
    public static int write0(int fd, Address buf, int nbyte) {
        return writePtr.call3(fd, buf, nbyte);
    }
  
    /**
     * write output
     * 
     * @param fd file descriptor
     * @param buf data buffer to write
     * @param offset in data buffer to start writing from
     * @param nbyte number of bytes to read
     * @return the number of bytes which were written is returned.  If error,
     *         -1 is returned and the global variable errno is set to indicate the error.
     */
    public static int write(int fd, byte[] buf, int offset, int nbyte) {
        boolean oldGCState = GC.setGCEnabled(false);
        try {
            /*------------------- DISABLE GC: ---------------------------*/
            Address ptr = getPtrToArray(buf, offset, nbyte);
            int result = write0(fd, ptr, nbyte);
            return result;
        } finally {
            GC.setGCEnabled(oldGCState);
        /*------------------- ENDABLE GC: ---------------------------*/
        }
    }

    /**
     * reposition read/write file offset
     * 
     * @param fd file descriptor
     * @param offset the offset to seek to
     * @param whence the kind of offset (SEEK_SET, SEEK_CUR, or SEEK_END)
     * @return the resulting offset location as measured in
     *         bytes from the beginning of the file.  If error, -1 is returned and errno is set
     *         to indicate the error.
     */
    public static int lseek(int fd, long offset, int whence) {
        return lseekPtr.call4(fd, (int)(offset >>> 32), (int)offset, whence);
    }
    

}
