//if[!FLASH_MEMORY]
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

package com.sun.squawk.io.j2me.socket;

import com.sun.squawk.VM;
import java.io.*;
import javax.microedition.io.*;

/**
 *
 * Simple test case reads HTTP page
 * Usage: Test <host> [<port>]
 */
public class Test {

    private static boolean runTwiddler = true;

    private static void getPage(String host, String port, String path, boolean byByte)
            throws IOException {
        StreamConnection c = (StreamConnection) Connector.open("socket://" + host + ":" + port);
        OutputStream out = c.openOutputStream();
        InputStream in = c.openInputStream();
        // specify 1.0 to get non-persistent connections.
        // Otherwise we have to parse the replies to detect when full reply is received.
        String command = "GET /" + path + " HTTP/1.0\r\nHost: " + host + "\r\n\r\n";
        byte[] data = command.getBytes();

        out.write(data, 0, data.length);

        long time = System.currentTimeMillis();
        if (byByte) {
            int b = 0;
            while ((b = in.read()) != -1) {
                System.out.print((char) b);
            }
        } else {
            int n = 0;
            while ((n = in.read(data, 0, data.length)) != -1) {
                for (int i = 0; i < n; i++) {
                    System.out.print((char) data[i]);
                }
            }
        }
        time = System.currentTimeMillis() - time;
        System.out.println("-------------- Took " + time + "ms");
        c.close();
    }

    /**
     * test code
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        try {
            String host = args[0];
            String port = "80";
            String path = "index.html";
            if (args.length > 1) {
                port = args[1];
            }
            if (args.length > 2) {
                path = args[2];
            }
            System.err.println("creating twiddler");
            Thread twiddler = new Thread(new Runnable() {
                public void run() {
                    while (runTwiddler) {
                        VM.print('$');
//                        try {
//                            Thread.sleep(10);
//                        } catch (InterruptedException ex) {
//                            ex.printStackTrace();
//                        }
                        Thread.yield();
                    }
                }
            }, "Twiddler Thread");
            twiddler.setPriority(Thread.MIN_PRIORITY);
            VM.setAsDaemonThread(twiddler);
            twiddler.start();
            try {
                Thread.sleep(2);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }

            // DNS, web server, or proxy cache be faster 2nd time around, so do twice...
            System.out.println("---------- Read in buffer (add to cache): ");
            getPage(host, port, path, false);
            System.out.println("---------- Read in buffer (cached): ");
            getPage(host, port, path, false);

            System.out.println("---------- Read in by byte: ");
            getPage(host, port, path, true);

            runTwiddler = false;

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private Test() {
    }

}
