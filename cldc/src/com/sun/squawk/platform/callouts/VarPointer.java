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
import com.sun.squawk.VM;
import com.sun.squawk.vm.ChannelConstants;

/**
 * A pointer to a native variable that can be read/wrote from Java.
 * 
 *  This needs to be extended to handle cases where ints are not 4 bytes, etc.
 */
public final class VarPointer extends Pointer {
    private final static boolean DEBUG = false;

    private String name; // for debugging/tracing

    VarPointer(String name, Address varAddr, int size) {
        super(varAddr, size);
        this.name = name;
    }

    /**
     * Dynamically look up a native variable by name.
     * 
     * Look up the symbol in the default list of loaded libraries.
     * 
     * @param varName 
     * @param size the size of the variable in bytes
     * @return an objecta that can be used to call the named function
     * @throws RuntimeException if there is no function by that name.
     */
    public static VarPointer lookup(String varName, int size) {
        Pointer name0 = Pointer.createStringBuffer(varName);
        int result = VM.execSyncIO(ChannelConstants.DLSYM, 0, name0.address().toUWord().toInt(), 0, 0, 0, 0, null, null);
        name0.free();
        if (DEBUG) {
            VM.print("Var Lookup for ");
            VM.print(varName);
            VM.print(", size: ");
            VM.print(size);
            VM.print(" returned ");
            VM.print(result);
            VM.println();
        }
        if (result == 0) {
            if (varName.charAt(0) != '_') {
                return lookup("_" + varName, size);
            }
            throw new RuntimeException("Can't find native symbol " + varName);
        }
        return new VarPointer(varName, Address.fromPrimitive(result), size);
    }

    public String toString() {
        return "VarPointer(" + name + ", " + address().toUWord().toInt() + ")";
    }

    public int getInt(long offset) {
        int result = super.getInt(offset);
        if (DEBUG) {
            VM.print(name);
            VM.print("  = ");
            VM.print(result);
            VM.println();
        }
        return result;
    }
 
    public int getInt() {
        return getInt(0);
    }

}
