/*
 * Copyright 1999-2008 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.squawk.io.j2me.debug;

import java.io.*;
import javax.microedition.io.*;
import com.sun.squawk.io.*;
import com.sun.squawk.*;

/**
 * Connection to the J2ME debug stream
 *
 * The two valid urls are:
 * "debug:" or "debug:err", which send output to the VM-level standard out or standard error streams.
 *
 * Used to implement System.out and System.err streams.
 *
 * @version 1.0 2/4/2000
 */

public class Protocol extends ConnectionBase
    implements OutputConnection {

    protected boolean opened = false;
    protected boolean err;

    /**
     * Open the connection
     * @param name       the target for the connection
     * @param timeouts   a flag to indicate that the called wants
     *                   timeout exceptions
     */
     public Connection open(String protocol, String name, int mode, boolean timeouts) throws IOException {
         err = name.equals("err");
         if (!err && !name.equals("")) {
             throw new IllegalArgumentException( "Bad protocol option:" + name);
         }
         return this;
     }

    /**
     * Returns an output stream for this socket.
     *
     * @return     an output stream for writing bytes to this socket.
     * @exception  IOException  if an I/O error occurs when creating
     *                          the output stream.
     */
    public OutputStream openOutputStream() throws IOException {

        if (opened) {
            throw new IOException("Stream already opened");
        }
        opened = true;
        return new PrivateOutputStream(this);
    }
}

/**
 * Output stream for the connection
 */
class PrivateOutputStream extends OutputStream {

    /**
     * Pointer to the connection
     */
    Protocol parent;

    /**
     * Constructor
     *
     * @param  p  pointer to the parent connection
     */
    public PrivateOutputStream(Protocol p) {
        parent = p;
    }

    /**
     * Writes the specified byte to this output stream.
     *
     * @param      b   the <code>byte</code>.
     * @exception  IOException  if an I/O error occurs. In particular,
     *             an <code>IOException</code> may be thrown if the
     *             output stream has been closed.
     */
    synchronized public void write(int b) throws IOException {

        if (parent == null) {
            throw new IOException("Connection closed");
        }
        int old;
        if (parent.err) {
            old = VM.setStream(VM.STREAM_STDERR);
            VM.print((char)b);
        } else {
            old = VM.setStream(VM.STREAM_STDOUT);
            VM.print((char)b);
        }
        VM.setStream(old);
    }

    /**
     * Close the stream
     *
     * @exception  IOException  if an I/O error occurs.
     */
    synchronized public void close() {
        if (parent != null) {
            parent.opened = false;
            parent = null;
        }
    }
}


