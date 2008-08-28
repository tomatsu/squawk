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

import com.sun.squawk.platform.SystemEvents;
import com.sun.squawk.VM;
import com.sun.squawk.VMThread;
import com.sun.squawk.platform.callouts.*;
import com.sun.squawk.platform.posix.callouts.*;
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
    
    private Time.Struct_TimeVal zeroTime;
    private Time.Struct_TimeVal timeoutTime;

    private int maxFD = 0; // system-wide highwater mark....
    
    private static int copyIntoFDSet(IntSet src, Pointer fd_set) {
//System.err.println("Copying from " + src + " to " + fd_set);
        int num = src.size();
        int[] data = src.getElements();
        int localMax = 0;
        Select.FD_ZERO(fd_set);
        for (int i = 0; i < num; i++) {
            int fd = data[i];
            Assert.that(fd > 0);
            Select.FD_SET(fd, fd_set);
            if (fd > localMax) {
                localMax = fd;
            }
        }
        return localMax;
    }

    public SystemEventsImpl() {
        masterReadSet = Select.FD_ALLOCATE();
        masterWriteSet = Select.FD_ALLOCATE();

        readSet = new IntSet();
        writeSet = new IntSet();
        tempReadSet = Select.FD_ALLOCATE();
        tempWriteSet = Select.FD_ALLOCATE();

        zeroTime = new Time.Struct_TimeVal();
        zeroTime.tv_sec = 0;
        zeroTime.tv_usec = 0;
        zeroTime.allocateMemory();
        zeroTime.write();

        timeoutTime = new Time.Struct_TimeVal();
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
            Select.FD_COPY(master, temp);
        } else {
             Select.FD_ZERO(temp);
        }
    }
    
    /**
     * Print the FDs taht are set in fd_set
     * 
     * @param fd_set the set of file descriptors in native format.
     */
    private void printFDSet(Pointer fd_set) {
        for (int i = 0; i < maxFD + 1; i++) {
            if (Select.FD_ISSET(i, fd_set)) {
                VM.println("    fd: " + i);
            }
        }
    }
    
    /**
     * Poll the OS to see if there have been any events on the requested fds.
     * 
     * Try not to allocate if there are no events...
     * @return number of events
     */
    private int pollEvents(long timeout) {
        if (maxFD <= 0) {
            return -1;
        }
        
        setupTempSet(readSet, masterReadSet, tempReadSet);
        setupTempSet(writeSet, masterWriteSet, tempWriteSet);

        Pointer theTimout;
        if (timeout == 0) {
            theTimout = zeroTime.getMemory();
        } else if (timeout == Long.MAX_VALUE) {
            theTimout = Pointer.NULL;
        } else {
            timeoutTime.tv_sec = timeout / 1000;
            timeoutTime.tv_usec = (timeout % 1000) * 1000;
            timeoutTime.write();
            theTimout = timeoutTime.getMemory();
        }

        int num = Select.select(maxFD + 1, tempReadSet, tempWriteSet, Pointer.NULL, theTimout);
        if (num < 0) {
            System.err.println("select error: " + LibC.errno());
        }

        if (num > 0) {
            if (readSet.size() != 0) {
                for (int i = 0; i < readSet.size(); i++) {
                    int fd = readSet.getElements()[i];
                    if (Select.FD_ISSET(fd, tempReadSet)) {
                        readSet.remove(fd);
                        VMThread.signalOSEvent(fd);
                        num--;
                    }
                }
            }
            if (writeSet.size() != 0) {
                for (int i = 0; i < writeSet.size(); i++) {
                    int fd = writeSet.getElements()[i];
                    if (Select.FD_ISSET(fd, tempWriteSet)) {
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
        return num;
    }

   /**
     * Poll the OS to see if there have been any events on the requested fds.
     * 
     * Try not to allocate if there are no events...
     * @return number of events
     */
    public int pollEvents() {
        return pollEvents(0);
    }

    /**
     * Wait for an OS event, with a timeout.
     * 
     * Try not to allocate if there are no events...
     * @param timout in ms
     * @return number of events
     */
    public int waitForEvents(long timout) {
        return pollEvents(timout);
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
