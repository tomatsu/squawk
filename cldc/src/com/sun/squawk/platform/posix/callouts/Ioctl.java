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

import com.sun.squawk.platform.callouts.FunctionPointer;

/**
 *  java wrapper around #include <sys/ioctl.h>
 */
public class Ioctl {
    /*
     * Ioctl's have the command encoded in the lower word, and the size of
     * any in or out parameters in the upper word.  The high 3 bits of the
     * upper word are used to encode the in/out status of the parameter.
     */

    /**
     * WARNING: This is different on different systems
     * SOLARIS: 0xFF
     * MAC OS X: 0x1FFF
     *  LINUX, not used???
     */
    public final static int IOCPARM_MASK = 0x1fff;		/* parameter length, at most 13 bits */

    public final static int IOCPARM_MAX = (IOCPARM_MASK + 1);	/* max size of ioctl args */
    /* no parameters */

    public final static int IOC_VOID = 0x20000000;
    /* copy parameters out */
    public final static int IOC_OUT = 0x40000000;
    /* copy parameters in */
    public final static int IOC_IN = 0x80000000;
    /* copy paramters in and out */
    public final static int IOC_INOUT = (IOC_IN | IOC_OUT);
    /* mask for IN/OUT/VOID */
    public final static int IOC_DIRMASK = 0xe0000000;

    private static int _IOC(int inout, char group, int num, int len) {
        return (inout | ((len & IOCPARM_MASK) << 16) | (((int) group) << 8) | (num));
    }

    private static int _IO(char g, int n) {
        return _IOC(IOC_VOID, (g), (n), 0);
    }

    private static int _IOR(char g, int n) {
        return _IOC(IOC_OUT, (g), (n), 4);
    }

    private static int _IOW(char g, int n) {
        return _IOC(IOC_IN, (g), (n), 4);
    }
    /* this should be _IORW, but stdio got there first */

    private static int _IOWR(char g, int n) {
        return _IOC(IOC_INOUT, (g), (n), 4);
    }
    
    public final static int FIOCLEX = _IO('f', 1);		/* set close on exec on fd */
    public final static int FIONCLEX = _IO('f', 2);		/* remove close on exec */
    public final static int FIONREAD = _IOR('f', 127);	/* get # bytes to read */
    public final static int FIONBIO = _IOW('f', 126);	/* set/clear non-blocking i/o */
    public final static int FIOASYNC = _IOW('f', 125);	/* set/clear async i/o */
    public final static int FIOSETOWN = _IOW('f', 124);	/* set owner */
    public final static int FIOGETOWN = _IOR('f', 123);	/* get owner */
    public final static int FIODTYPE = _IOR('f', 122);	    /* get d_type */
    
    private static final FunctionPointer ioctlPtr = FunctionPointer.lookup("ioctl");

    /**
     * Perorm IO control operation <code>op</code> on device <code>fd</code>.
     * 
     * @param fd
     * @param op
     * @param i1
     * @return
     */
    public static int ioctl(int fd, int op, int i1) {
        return ioctlPtr.call3(fd, op, i1);
    }

    /**
     * Perorm IO control operation <code>op</code> on device <code>fd</code>.
     * 
     * @param fd
     * @param op
     * @param i1
     * @param i2 
     * @return
     */
    public static int ioctl(int fd, int op, int i1, int i2) {
        return ioctlPtr.call4(fd, op, i1, i2);
    }

    /**
     * Perorm IO control operation <code>op</code> on device <code>fd</code>.
     * 
     * @param fd
     * @param op
     * @param i1
     * @param i2 
     * @param i3 
     * @return
     */
    public static int ioctl(int fd, int op, int i1, int i2, int i3) {
        return ioctlPtr.call5(fd, op, i1, i2, i3);
    }

    private Ioctl() {
    }
}
