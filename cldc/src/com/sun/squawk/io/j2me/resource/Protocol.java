//if[RESOURCE.CONNECTION]
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

package com.sun.squawk.io.j2me.resource;

import java.io.*;
import javax.microedition.io.*;
import com.sun.squawk.io.*;
import com.sun.squawk.io.connections.*;
import com.sun.squawk.*;
import com.sun.squawk.VMThread;

/**
 * This class implements the default "resource:" protocol for KVM.
 *
 * The default is to open a file based upon the resource name.
 *
 * @version 1.0 2/12/2000
 */
public class Protocol extends ConnectionBase implements InputConnection {

    private ClasspathConnection pathConnection;
	private byte [] resourceData;
    private String name;

    /**
     * Open the connection
     */
    public Connection open(String protocol, String name, int mode, boolean timeouts) throws IOException {
		pathConnection = null;
		resourceData = null;
        this.name = name;

		// look for the resource file in the current leaf suite, and then up the chain of parent suites until we find it
		Suite suite = VM.getCurrentIsolate().getLeafSuite();			
		while (suite != null) {
			resourceData = suite.getResourceData(name);
			if (resourceData != null) {
				return this;
			}
			suite = suite.getParent();
		}

		// couldn't find the specified resource file in the suite, so load it from the classpath
        String resourcePath = VMThread.currentThread().getIsolate().getClassPath();
        if (resourcePath == null) {
            resourcePath = ".";
        }
        if (pathConnection == null) {
            pathConnection = (ClasspathConnection)Connector.open("classpath://" + resourcePath);
        }
        return this;
    }

    public InputStream openInputStream() throws IOException {
		if (resourceData != null) {
			// the resource file is stored in one of the suites in memory, so create a new input stream from there...
			return new ByteArrayInputStream(resourceData);
		}  else {
			// otherwise open the resource file from the class path
			return pathConnection.openInputStream(name);
		}
    }

    public void close() throws IOException {
    }
}
