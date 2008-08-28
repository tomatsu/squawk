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

import com.sun.squawk.platform.posix.GCFSocketsImpl;

/**
 *
 * This class provides access to the platform-specific implementations of various features.
 */
public class Platform {
    /**
     * Basic kinds of PLATFORM_TYPE, as defined in build.properties
     */
    public final static int BARE_METAL = 0;
    public final static int DELEGATING = 1;
    public final static int NATIVE = 2;
    public final static int SOCKET = 3;
    
    private static GCFSockets gcfSockets;
    
    private Platform() { }
    
    /**
     * @return true if this should run on the bare metal.
     */
    public static boolean isBareMetal() {
        return /*VAL*/999/*PLATFORM_TYPE*/ == BARE_METAL;
    }
    
   /**
     * @return true if this should run by delegating all IO to another JVM (HotSpot) via JNI.
     */
    public static boolean isDelegating() {
        return /*VAL*/999/*PLATFORM_TYPE*/ == DELEGATING;
    }

    /**
     * @return true if this should run on the native OS.
     */
    public static boolean isNative() {
        return /*VAL*/999/*PLATFORM_TYPE*/ == NATIVE;
    }

    /**
     * @return true if this should run by delegating all IO to another process (HotSpot) via sockets.
     */
    public static boolean isSocket() {
        return /*VAL*/999/*PLATFORM_TYPE*/ == SOCKET;
    }
    
    public static synchronized GCFSockets getGCFSockets() {
/*if[PLATFORM_TYPE_NATIVE]*/
        if (gcfSockets == null) {
            gcfSockets = new GCFSocketsImpl();
        }
        return gcfSockets;
/*else[PLATFORM_TYPE_NATIVE]*/ 
//     return null;
/*end[PLATFORM_TYPE_NATIVE]*/
    }
    
    /**
     * Create the correct kind of SystemEvents Handler, or null if none needed.
     * 
     * @return
     */
    public static SystemEvents createSystemEvents() {
/*if[PLATFORM_TYPE_NATIVE]*/
        return new com.sun.squawk.platform.posix.SystemEventsImpl();
/*else[PLATFORM_TYPE_NATIVE]*/
//     return null;
/*end[PLATFORM_TYPE_NATIVE]*/
    }
    
}
