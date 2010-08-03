
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


package com.sun.squawk.platform.windows;

import com.sun.squawk.platform.SystemEvents;
import com.sun.squawk.VM;
import com.sun.squawk.VMThread;
import com.sun.cldc.jna.*;
import com.sun.squawk.platform.windows.natives.*;
import com.sun.squawk.util.Assert;
import com.sun.squawk.util.IntSet;

/**
 *
 * @author dw29446
 */
public class SystemEventsImpl extends SystemEvents {

    /* We need 3 copies of file descriptor sets:
    - An array of ints (as an IntSet)
    - the master FD_SET (as a native bitmap),
    - a tmp FD_SET, because select bashes the set passed in.
     */
    Pointer masterReadSet;
    Pointer masterWriteSet;
    private Pointer tempReadSet;
    private Pointer tempWriteSet;
    private IntSet readSet;
    private IntSet writeSet;

    private Time.timeval zeroTime;
    private Time.timeval timeoutTime;

    private int maxFD = 0; // system-wide highwater mark....

    private BlockingFunction selectPtr;
    private Function cancelSelectPtr;
    protected Select select;

    private static int copyIntoFDSet(IntSet src, Pointer fd_set) {
//System.err.println("Copying from " + src + " to " + fd_set);
        int num = src.size();
        int[] data = src.getElements();
        int localMax = 0;
        FD_ZERO(fd_set);
        for (int i = 0; i < num; i++) {
            int fd = data[i];
            Assert.that(fd > 0);
            Select.INSTANCE.FD_SET(fd, fd_set);
            if (fd > localMax) {
                localMax = fd;
            }
        }
        return localMax;
    }


    /**
     * initializes a descriptor set fdset to the null set
     * @param fd_set
     */
    public static void FD_ZERO(Pointer fd_set) {
        fd_set.clear(Select.fd_set_SIZEOF);
    }

    /**
     * replaces an already allocated fdset_copy file descriptor set with a copy of fdset_orig.
     *
     * @param fdset_orig
     * @param fdset_copy
     */
    public static void FD_COPY(Pointer fdset_orig, Pointer fdset_copy) {
//        System.err.println("FD_COPY from: " + fdset_orig + " to: " + fdset_copy + " (size = " + FD_SIZE + ")");
        Pointer.copyBytes(fdset_orig, 0, fdset_copy, 0, Select.fd_set_SIZEOF);
    }

    /**
     * Allocate a new fd_struct in c memory.
     * @return pointer to new memory
     */
    public static Pointer FD_ALLOCATE() {
        return new Pointer(Select.fd_set_SIZEOF);
    }

    public SystemEventsImpl() {
        select = Select.INSTANCE;
        NativeLibrary jnaNativeLibrary = NativeLibrary.getDefaultInstance();
        selectPtr = jnaNativeLibrary.getBlockingFunction("squawk_select");
        selectPtr.setTaskExecutor(selectRunner);

        masterReadSet = FD_ALLOCATE();
        masterWriteSet = FD_ALLOCATE();

        readSet = new IntSet();
        writeSet = new IntSet();
        tempReadSet = FD_ALLOCATE();
        tempWriteSet = FD_ALLOCATE();

        zeroTime = new Time.timeval();
        zeroTime.tv_sec = 0;
        zeroTime.tv_usec = 0;
        zeroTime.allocateMemory();
        zeroTime.write();

        timeoutTime = new Time.timeval();
        timeoutTime.allocateMemory();
    }

    /**
     * Set up the temp fd_set based on the maset set and the IntSet.
     *
     * @param set the IntSet
     * @param master the master fd_set
     * @param temp the temp fd_set
     */
    private void setupTempSet(IntSet set, Pointer master, Pointer temp) {
         if (set.size() != 0) {
            FD_COPY(master, temp);
        } else {
            FD_ZERO(temp);
        }
    }

    /**
     * Print the FDs taht are set in fd_set
     *
     * @param fd_set the set of file descriptors in native format.
     */
    private void printFDSet(Pointer fd_set) {
        for (int i = 0; i < maxFD + 1; i++) {
            if (Select.INSTANCE.FD_ISSET(i, fd_set)) {
                VM.println("    fd: " + i);
            }
        }
    }

    /**
     * Blocking call to select until IO occurs, the timeout occurs, or the read/write sets need to be updated
     * (see updateSets() ??)
     * @param theTimout
     * @return number of file descriptors that have events
     */
    private int select(int nfds, Pointer readSet, Pointer writeSet, Pointer excSet, Pointer theTimout) {
        return selectPtr.call5(nfds, readSet, writeSet, excSet, theTimout);
    }
    
    /**
     * Poll the OS to see if there have been any events on the requested fds.
     *
     * Try not to allocate if there are no events...
     * @param timeout 
     */
    public void waitForEvents(long timeout) {
        if (maxFD <= 0) {
            return;
        }

        setupTempSet(readSet, masterReadSet, tempReadSet);
        setupTempSet(writeSet, masterWriteSet, tempWriteSet);

        Pointer theTimout;
        if (timeout == 0) {
            theTimout = zeroTime.getPointer();
        } else if (timeout == Long.MAX_VALUE) {
            theTimout = Pointer.NULL();
        } else {
            timeoutTime.tv_sec = timeout / 1000;
            timeoutTime.tv_usec = (timeout % 1000) * 1000;
            timeoutTime.write();
            theTimout = timeoutTime.getPointer();
        }

        int num = select(maxFD + 1, tempReadSet, tempWriteSet, Pointer.NULL(), theTimout);
        if (num < 0) {
            System.err.println("select error: " + LibCUtil.errno());
        }

        if (num > 0) {
            if (readSet.size() != 0) {
                for (int i = 0; i < readSet.size(); i++) {
                    int fd = readSet.getElements()[i];
                    if (Select.INSTANCE.FD_ISSET(fd, tempReadSet)) {
                        readSet.remove(fd);
                        VMThread.signalOSEvent(fd);
                        num--;
                    }
                }
            }
            if (writeSet.size() != 0) {
                for (int i = 0; i < writeSet.size(); i++) {
                    int fd = writeSet.getElements()[i];
                    if (Select.INSTANCE.FD_ISSET(fd, tempWriteSet)) {
                        writeSet.remove(fd);
                        VMThread.signalOSEvent(fd);
                        num--;
                    }
                }
            }
            if (num != 0) {
                System.err.println("Missed handling a select event?\n Read FDs set:");
                printFDSet(tempReadSet);
                System.err.println("Write FDs set:");
                printFDSet(tempWriteSet);
            }
            updateSets();
        }
    }

    /**
     * Update bit masks from IntSets, and update maxFD;
     */
    private void updateSets() {
        maxFD = copyIntoFDSet(readSet, masterReadSet);
        int mfd = copyIntoFDSet(writeSet, masterWriteSet);
        if (mfd > maxFD) {
            maxFD = mfd;
        }
    }

    public void waitForReadEvent(int fd) {
//VM.println("Waiting for read on fd: " + fd);
        readSet.add(fd);
        updateSets();
        VMThread.waitForOSEvent(fd); // read is ready, select will remove fd from readSet
    }

    public void waitForWriteEvent(int fd) {
//VM.println("Waiting for write on fd: " + fd);
        writeSet.add(fd);
        updateSets();
        VMThread.waitForOSEvent(fd);// write is ready, select will remove fd from writeSet
    }

//
//    public static int waitForWriteEvent(int fd) {
//
//    }

}
