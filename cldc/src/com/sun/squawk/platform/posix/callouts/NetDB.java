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

import com.sun.squawk.Address;
import com.sun.squawk.platform.callouts.*;

/**
 *
 * java wrapper around #include <netdb.h>
 */
public class NetDB {
    
    /* pure static class */
    private NetDB() {}
    
    private static final VarPointer h_errnoPtr = VarPointer.lookup("h_errno", 4);
        
    private static final FunctionPointer gethostbynamePtr = FunctionPointer.lookup("gethostbyname");
    
    /**
     * The gethostbyname() function returns a pointer to an
     *    object with the hostent structure describing an internet host referenced by name.
     *
     * @param name the host name (char*)
     * @return the address of struct hostent.
     */
    private static Address gethostbyname0(Address name) {
        int addr;
        addr = gethostbynamePtr.call1(name);
        return Address.fromPrimitive(addr);
    }

    /**
     * The gethostbyname() function returns a HostEnt structure describing an internet host referenced by name.
     *
     * @param name the host name (char*)
     * @return the address of struct hostent.
     */
    public static Struct_HostEnt gethostbyname(String name) {
        Pointer name0 = Pointer.createStringBuffer(name);
        Address addr = gethostbyname0(name0.address());
        name0.free();
        if (addr.isZero()) {
            return null;
        } else {
            Struct_HostEnt result = new Struct_HostEnt();
            result.setMemory(new Pointer(addr, result.size()));
            result.read();
            return result;
        }
    }
    
    public final static int HOST_NOT_FOUND = 1; /* Authoritative Answer Host not found */

    public final static int TRY_AGAIN = 2; /* Non-Authoritative Host not found, or SERVERFAIL */

    public final static int NO_RECOVERY = 3; /* Non recoverable errors, FORMERR, REFUSED, NOTIMP */

    public final static int NO_DATA = 4; /* Valid name, no data record of requested type */


    public static int h_errno() {
        return h_errnoPtr.getInt();
    }
    
    /** C STRUCTURE HostEnt
            struct  hostent {
                     char    *h_name;         official name of host 
                     char    **h_aliases;     alias list 
                     int     h_addrtype;      host address type 
                     int     h_length;        length of address 
                     char    **h_addr_list;   list of addresses from name server 
             };
             #define h_addr  h_addr_list[0]  address, for backward compatibility 
    */
    public static class Struct_HostEnt extends Structure {

        public String h_name;        /* official name of host */

        public String[] h_aliases_NOT_IMPLEMENTED;    /* alias list */

        public int h_addrtype;     /* host address type */

        public int h_length;       /* length of address */

        public Pointer[] h_addr_list;  /* list of addresses from name server */

        public void read() {
            Pointer p = getMemory();
            h_name = p.getString(0);
            h_aliases_NOT_IMPLEMENTED = null; // p.getPointer(4, ???);
            h_addrtype = p.getInt(8);
            h_length = p.getInt(12);
            if (h_length != 4) {
                System.err.println("WARNING: Unexpected h_length value");
            }
            // just look at first address...
            Pointer adrlist = p.getPointer(16, h_length);
//System.err.println(" adrlist  " + adrlist);

            h_addr_list = new Pointer[1];
            for (int i = 0; i < 1; i++) {
                Pointer addr = adrlist.getPointer(i*4, Socket.Struct_SockAddr.SIZEOF_SockAddr);
                //System.err.println(" addr  " + addr);

                h_addr_list[i] = addr;
            }

        }

        public void write() {
        }

        public int size() { return 5 * 4; }
    } /* HostEnt */

/*if[DEBUG_CODE_ENABLED]*/
    public static void main(String[] args) {
        String[] hosts = {"localhost",
            "www.sun.com",
            "127.0.0.1",
            "adfadfadf.adfadf",
            ""
        };

        for (int i = 0; i < hosts.length; i++) {
            System.err.println("Trying lookup of " + hosts[i]);
            Struct_HostEnt hostent = gethostbyname(hosts[i]);
           // System.err.println("result: " + hostent);
            if (hostent == null) {
                System.err.println(" lookup error  " + h_errno());
            } else {
                int len = hostent.h_addr_list.length;
                for (int j = 0; j < len; j++) {
                    Pointer addr = hostent.h_addr_list[j];
                    String addrstr = Socket.inet_ntoa(addr);
                    System.err.println("   addr  " + addrstr);
                }

            }

        }
    }
/*end[DEBUG_CODE_ENABLED]*/
}
