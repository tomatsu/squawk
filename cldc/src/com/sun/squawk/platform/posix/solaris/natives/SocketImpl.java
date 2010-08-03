//if[!AUTOGEN_JNA_NATIVES]
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

/* **** HAND_GENERATED FILE
 */

package com.sun.squawk.platform.posix.solaris.natives;

import com.sun.cldc.jna.*;

public class SocketImpl extends com.sun.squawk.platform.posix.natives.SocketImpl {

    /*----------------------------- defines -----------------------------*/

    private final static int[] intConstants = {
        /* public final static int AF_INET = */2,
        /* public final static int SOCK_STREAM =  */ 2,
        /* public final static int SOCK_DGRAM =  */ 1,
        /* public final static int SOCK_RAW =  */ 4,
        /* public final static int INADDR_ANY =  */ 0,
        /* public final static int INET_ADDRSTRLEN =  */ 16,
        /* public final static int SOL_SOCKET =  */ 65535,
        /* public final static int SO_DEBUG =  */ 1,
        /* public final static int SO_ACCEPTCONN =  */ 2,
        /* public final static int SO_REUSEADDR =  */ 4,
        /* public final static int SO_KEEPALIVE =  */ 8,
        /* public final static int SO_DONTROUTE =  */ 16,
        /* public final static int SO_BROADCAST =  */ 32,
        /* public final static int SO_OOBINLINE =  */ 256,
        /* public final static int IPPROTO_TCP = */ 6,
        /* public final static int TCP_NODELAY = */ 1
    };

    private static boolean[] intConstantCheck;

    public int initConstInt(int index) {
        if (Native.DEBUG) {
             intConstantCheck = Native.doInitCheck(intConstantCheck, intConstants.length, index);
        }
        return intConstants[index];
    }

    /*----------------------------- methods -----------------------------*/

}
