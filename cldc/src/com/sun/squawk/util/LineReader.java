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

package com.sun.squawk.util;

import java.io.*;
import java.util.Vector;

/**
 * This class provides for reading lines from a reader. This is functionality
 * normally provided by the non-J2ME class BufferedReader.
 *
 */
public final class LineReader {

    /**
     * The source reader.
     */
    private Reader in;

    /**
     * Creates a new LineReader to parse the lines from a given Reader.
     *
     * @param reader   the reader providing the input to be parsed into lines
     */
    public LineReader(Reader reader) {
        in = reader;
    }

    /**
     * Read a line of text.  A line is considered to be terminated by any one
     * of a line feed ('\n'), a carriage return ('\r'), or a carriage return
     * followed immediately by a linefeed.
     *
     * @return     A String containing the contents of the line, not including
     *             any line-termination characters, or null if the end of the
     *             stream has been reached
     *
     * @throws  IOException  If an I/O error occurs
     */
    public String readLine() throws IOException {
        boolean trucking = true;
        boolean eol = true;
        StringBuffer sb = new StringBuffer();
        while (trucking) {
            int c = in.read();
            if (c == '\n' || c == -1) {
                trucking = false;
                eol = eol && (c == -1);
            } else {
                eol = false;
                if (c != '\r') {
                    sb.append((char)c);
                }

            }
        }
        if (eol) {
            return null;
        }
        return sb.toString();
    }

    /**
     * Read all the lines from the input stream and add them to a given Vector.
     *
     * @param v   the vector to add to or null if it should be created first by this method.
     * @return the vector to which the lines were added
     *
     * @throws  IOException  If an I/O error occurs
     */
    public Vector readLines(Vector v) throws IOException {
        if (v == null) {
            v = new Vector();
        }
        for (String line = readLine(); line != null; line = readLine()) {
            v.addElement(line);
        }
        return v;
    }
}
