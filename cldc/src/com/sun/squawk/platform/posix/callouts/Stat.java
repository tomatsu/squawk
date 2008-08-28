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
 * This is the Java interface to the POSIX stats  API
 */
public class Stat extends LibC {
    /*
     * [XSI] The following are symbolic names for the values of type mode_t.  They
     * are bitmap values.
     */
    /* File type */
    public final static int S_IFMT = 0170000;		/* [XSI] type of file mask */
    public final static int S_IFIFO = 0010000;		/* [XSI] named pipe (fifo) */
    public final static int S_IFCHR = 0020000;		/* [XSI] character special */
    public final static int S_IFDIR = 0040000;		/* [XSI] directory */
    public final static int S_IFBLK = 0060000;		/* [XSI] block special */
    public final static int S_IFREG = 0100000;		/* [XSI] regular */
    public final static int S_IFLNK = 0120000;		/* [XSI] symbolic link */
    public final static int S_IFSOCK = 0140000;		/* [XSI] socket */

    /* File mode */
    /* Read, write, execute/search by owner */
    public final static int S_IRWXU = 0000700;		/* [XSI] RWX mask for owner */
    public final static int S_IRUSR = 0000400;		/* [XSI] R for owner */
    public final static int S_IWUSR = 0000200;		/* [XSI] W for owner */
    public final static int S_IXUSR = 0000100;		/* [XSI] X for owner */
    /* Read, write, execute/search by group */
    public final static int S_IRWXG = 0000070;		/* [XSI] RWX mask for group */
    public final static int S_IRGRP = 0000040;		/* [XSI] R for group */
    public final static int S_IWGRP = 0000020;		/* [XSI] W for group */
    public final static int S_IXGRP = 0000010;		/* [XSI] X for group */
    /* Read, write, execute/search by others */
    public final static int S_IRWXO = 0000007;		/* [XSI] RWX mask for other */
    public final static int S_IROTH = 0000004;		/* [XSI] R for other */
    public final static int S_IWOTH = 0000002;		/* [XSI] W for other */
    public final static int S_IXOTH = 0000001;		/* [XSI] X for other */
    public final static int S_ISUID = 0004000;		/* [XSI] set user id on execution */
    public final static int S_ISGID = 0002000;		/* [XSI] set group id on execution */
    public final static int S_ISVTX = 0001000;		/* [XSI] directory restrcted delete */
    
    public static final int STAT_SIZE = VarPointer.lookup("sysSIZEOFSTAT", 4).getInt();

    private static final FunctionPointer statPtr  = FunctionPointer.lookup("stat");
    private static final FunctionPointer fstatPtr = FunctionPointer.lookup("fstat");

    /* pure static class */
    private Stat() { }

    /**
     * Get file status for named file
     * 
     * @param namestr
     * @param stat 
     * @return -1 is returned if an error occurs, otherwise zero is returned
     */
    public static int stat0(Address namestr, Address stat) {
        return statPtr.call2(namestr, stat);
    }
    
    /**
     * open or create a file for reading or writing
     * 
     * @param name String
     * @param stat 
     * @return -1 is returned if an error occurs, otherwise zero is returned
     */
    public static int stat(String name, Struct_Stat stat) {
        Pointer name0 = Pointer.createStringBuffer(name);
        stat.allocateMemory();
//System.err.println("Stat.stat:" + name);
//System.err.println("   mem " + stat.getMemory());  
        int result = stat0(name0.address(), stat.getMemory().address());
        name0.free();
        stat.read();
//System.err.println("   result: " + stat);
        stat.freeMemory();
        return result;
    }  
     
    /**
     * initiate a connection on a socket.
     * 
     * @param fd file descriptor
     * @param address ptr to a Stat buffer
     * @return -1 is returned if an error occurs, otherwise zero is returned
     */
     public static int fstat0(int fd, Address address) {
         return fstatPtr.call2(fd, address);
     }
     
    /**
     * initiate a connection on a socket.
     * 
     * @param fd file descriptor
     * @param stat Stat that will be filled with the current values
     * @return -1 is returned if an error occurs, otherwise zero is returned
     */
    public static int fstat(int fd, Struct_Stat stat) {
        stat.allocateMemory();
//System.err.println("Stat.fstat(" + fd + ", " + stat);
//System.err.println("   mem " + stat.getMemory());      
        int result = fstat0(fd, stat.getMemory().address());
        stat.read();

        stat.freeMemory();
        return result;
    }
    
//
//    /**
//     * To be safe, ask C how big a struct stat is...
//     * 
//     * @return size of a native struct stat, in bytes
//     */
//     public static int sizeofStat() {
//        return Call.call(ChannelConstants.NATIVE_POSIX_SIZEOFSTAT);
//     }
     
    /** C struct stat
//    struct stat {
//        dev_t		st_dev;		/* [XSI] ID of device containing file             4 0
//        ino_t	  	st_ino;		/* [XSI] File serial number                       4 4
//        mode_t	 	st_mode;	/* [XSI] Mode of file (see below)             2 8
//        nlink_t		st_nlink;	/* [XSI] Number of hard links                 2 10
//        uid_t		st_uid;		/* [XSI] User ID of the file                      4 12
//        gid_t		st_gid;		/* [XSI] Group ID of the file                     4 16
//        dev_t		st_rdev;	/* [XSI] Device ID                                4 20
//        time_t		st_atime;	/* [XSI] Time of last access                  4 24
//        long		st_atimensec;	/* nsec of last access                        4 28
//        time_t		st_mtime;	/* [XSI] Last data modification time          4 32
//        long		st_mtimensec;	/* last data modification nsec                4 36
//        time_t		st_ctime;	/* [XSI] Time of last status change           4 40
//        long		st_ctimensec;	/* nsec of last status change                 4 44
//        off_t		st_size;	/* [XSI] file size, in bytes                      8 48
//        blkcnt_t	st_blocks;	/* [XSI] blocks allocated for file                8
//        blksize_t	st_blksize;	/* [XSI] optimal blocksize for I/O                4
//        __uint32_t	st_flags;	/* user defined flags for file                4
//        __uint32_t	st_gen;		/* file generation number                     4
//        __int32_t	st_lspare;	/* RESERVED: DO NOT USE!                          4
//        __int64_t	st_qspare[2];	/* RESERVED: DO NOT USE!                      16
//     };
     */
     public final static class Struct_Stat extends Structure {
        /** mode_t */
        public int st_mode;
        
        /** time_t Last data modification time */
        public int st_mtime;
        
        /** file size, in bytes */
        public long st_size;
        
        public void read() {
            Pointer p = getMemory();
            st_mode = p.getShort(8) & 0xFFFF;
            st_mtime = p.getInt(32);
            st_size = p.getLong(48);
        }

        public void write() {
            throw new IllegalStateException("NYI");
        }

        public int size() {
            return STAT_SIZE;
        }
        
        public String toString() {
            return "Struct_Stat{mode: " + st_mode + ", mtimw: " + st_mtime + ", size: " + st_size + "}";
        }
                 
    } /* StructStat */

}
