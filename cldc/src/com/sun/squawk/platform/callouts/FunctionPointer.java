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


package com.sun.squawk.platform.callouts;

import com.sun.squawk.Address;
import com.sun.squawk.NativeUnsafe;
import com.sun.squawk.VM;
import com.sun.squawk.vm.ChannelConstants;

/**
 * A pointer to a native function that can be called from Java
 */
public final class FunctionPointer {
    private final static boolean DEBUG = false;

    private Address funcAddr;
    private String name; // for debugging/tracing

    FunctionPointer(String name, Address funcAddr) {
        this.funcAddr = funcAddr;
        this.name = name;
    }

    /**
     * Dynamically look up a native function by name.
     * 
     * Look up the symbol in the default list of loaded libraries.
     * 
     * @param funcName
     * @return an objecta that can be used to call the named function
     * @throws RuntimeException if there is no function by that name.
     */
    public static FunctionPointer lookup(String funcName) {
        Pointer name0 = Pointer.createStringBuffer(funcName);
        int result = VM.execSyncIO(ChannelConstants.DLSYM, 0, name0.address().toUWord().toInt(), 0, 0, 0, 0, null, null);
        name0.free();
        if (DEBUG) {
            VM.print("Function Lookup for ");
            VM.print(funcName);
            VM.print(" = ");
            VM.printAddress(Address.fromPrimitive(result));
            VM.println();
        }
        if (result == 0) {
            throw new RuntimeException("Can't find native symbol " + funcName);
        }
        return new FunctionPointer(funcName, Address.fromPrimitive(result));
    }

    public String toString() {
        return "FunctionPointer(" + name + ", " + funcAddr.toUWord().toInt() + ")";
    }

    /**
     * Call a function pointer with no arguments
     * @return return value
     */
    public int call0() {
        if (DEBUG) {
            VM.print(name);
            VM.println(".call0");
        }
        return NativeUnsafe.call0(funcAddr);
    }

    /**
     * Call a function pointer with one arguments
     */
    public int call1(int i1) {
        if (DEBUG) {
            VM.print(name);
            VM.println(".call1");
        }
        return NativeUnsafe.call1(funcAddr, i1);
    }

    /**
     * Call a function pointer with two arguments
     */
    public int call2(int i1, int i2) {
        if (DEBUG) {
            VM.print(name);
            VM.println(".call2");
        }
        return NativeUnsafe.call2(funcAddr, i1, i2);
    }

    /**
     * Call a function pointer with three arguments
     */
    public int call3(int i1, int i2, int i3) {
        if (DEBUG) {
            VM.print(name);
            VM.println(".call3");
        }
        return NativeUnsafe.call3(funcAddr, i1, i2, i3);
    }

    /**
     * Call a function pointer with four arguments
     */
    public int call4(int i1, int i2, int i3, int i4) {
        if (DEBUG) {
            VM.print(name);
            VM.println(".call4");
        }
        return NativeUnsafe.call4(funcAddr, i1, i2, i3, i4);
    }

    /**
     * Call a function pointer with five arguments
     */
    public int call5(int i1, int i2, int i3, int i4, int i5) {
        if (DEBUG) {
            VM.print(name);
            VM.println(".call5");
        }
        return NativeUnsafe.call5(funcAddr, i1, i2, i3, i4, i5);
    }

    /**
     * Call a function pointer with one arguments
     */
    public int call1(Address a1) {
        return call1(a1.toUWord().toPrimitive());
    }

    /**
     * Call a function pointer with two arguments
     */
    public int call2(Address a1, Address a2) {
        return call2(a1.toUWord().toPrimitive(), a2.toUWord().toPrimitive());
    }

    /**
     * Call a function pointer with two arguments
     */
    public int call2(Address a1, int i2) {
        return call2(a1.toUWord().toPrimitive(), i2);
    }

    /**
     * Call a function pointer with two arguments
     */
    public int call2(int i1, Address a2) {
        return call2(i1, a2.toUWord().toPrimitive());
    }

    /**
     * Call a function pointer with three arguments
     */
    public int call3(int i1, Address a2, int i3) {
        return call3(i1, a2.toUWord().toPrimitive(), i3);
    }
    
    public int call3(int i1, Address a2, Address a3) {
        return call3(i1, a2.toUWord().toPrimitive(), a3.toUWord().toPrimitive());
    }

    /**
     * Call a function pointer with five arguments
     */
    public int call5(int i1, Address a2, Address a3, Address a4, Address a5) {
        return call5(i1,
                a2.toUWord().toPrimitive(),
                a3.toUWord().toPrimitive(),
                a4.toUWord().toPrimitive(),
                a5.toUWord().toPrimitive());
    }
 
    public int call5(int i1, int i2, int i3, Address a4, int i5) {
        return call5(i1, i2, i3,
                a4.toUWord().toPrimitive(),
                i5);
    }

    public int call5(int i1, int i2, int i3, Address a4, Address a5) {
        return call5(i1, i2, i3,
                a4.toUWord().toPrimitive(),
                a5.toUWord().toPrimitive());
    }
}
