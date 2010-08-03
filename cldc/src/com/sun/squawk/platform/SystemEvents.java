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

package com.sun.squawk.platform;

import com.sun.cldc.jna.TaskExecutor;
import com.sun.squawk.VMThread;

/**
 *
 * @author dw29446
 */
public abstract class SystemEvents implements Runnable {

    private volatile boolean cancelRunLoop;
    protected TaskExecutor selectRunner;
    protected long max_wait = Long.MAX_VALUE;

    /**
     * Wait for an OS event, with a timeout. Signal VMThread when event occurs.
     *
     * Try not to allocate if there are no events...
     * @param timout in ms
     * @return number of events
     */
    protected abstract void waitForEvents(long timout);

    public abstract void waitForReadEvent(int fd);

    public abstract void waitForWriteEvent(int fd);

    protected SystemEvents() {
        selectRunner = new TaskExecutor("native IO handler", TaskExecutor.TASK_PRIORITY_MED, 0);
    }

    /**
     * Start
     */
    public void startIO() {
        Thread IOHandler = new Thread(this, "IOHandler");
        // IOHandler.setPriority(Thread.MAX_PRIORITY); do we want to do this?
        IOHandler.start();
    }

    /**
     * IOHandler run loop. Wait on select until IO occurs.
     */
    public void run() {
//VM.println("in SystemEvents.run()");
        while (!cancelRunLoop) {
            waitForEvents(Long.MAX_VALUE);
            VMThread.yield();
//VM.println("in SystemEvents.run() - woke up and try again");
        }
//VM.println("in SystemEvents.run() - cancelling");

        selectRunner.cancelTaskExecutor(); /* cancel the native thread that we use for blocking calls...*/
    }

    /**
     * Call to end the run() method.
     */
    public void cancelIOHandler() {
        cancelRunLoop = true;
    }

    /**
     * Set the maximum time that the system will wait in select
     *
     * @param max max wait time in ms. Must be > 0.
     */
    public void setMaxWait(long max) {
        if (max <= 0) {
            throw new IllegalArgumentException();
        }
        max_wait = max;
    }

}
