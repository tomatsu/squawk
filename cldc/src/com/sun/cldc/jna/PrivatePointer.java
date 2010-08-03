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


package com.sun.cldc.jna;

import com.sun.squawk.Address;
import com.sun.squawk.GC;
import com.sun.squawk.Klass;
import com.sun.squawk.util.Assert;

/**
 * Utilities used by JNA generated Java code. SHOULD NOT BE USED BY USER CODE.
 */
public class PrivatePointer  {

    private PrivatePointer() {}

    /**
     * Get a pointer to the interior of a Java array.
     * Check that the range requested is within the array bounds.
     * @param array
     * @param offset
     * @param len
     * @return
     */
    private static Address getPtrToArray(byte[] array, int offset, int len) {
        Assert.that(GC.setGCEnabled(false) == false);
        int alen = array.length;
        if (offset < 0 || (offset + len) > alen) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return Address.fromObject(array).add(offset);
    }

    /**
     * Get ready to allow creating array buffers.
     * @return previous state
     */
    public static boolean setUpArrayBufferState() {
        return GC.setGCEnabled(false);
    }

    /**
     * Clean up after creating array buffers.
     * @param oldState
     */
    public static void tearDownArrayBufferState(boolean oldState) {
        GC.setGCEnabled(oldState);
    }

    /**
     * Create a native buffer pointing to either the array data directly,
     * or to a copy of the array data.
     * bytes
     * The returned point can be released when not needed.
     *
     * @param array the array to access
     * @return Pointer the C-accessible version of the array data
     * @throws OutOfMemoryError if the underlying memory cannot be allocated
     * @throws IllegalArgumentException if array is not really an array
     */
    public static Address createArrayBuffer(Object array) throws OutOfMemoryError {
        Assert.always(GC.setGCEnabled(false) == false);
        Klass klass = GC.getKlass(array);
        if (!klass.isArray()) {
            throw new IllegalArgumentException();
        }
//        int length = GC.getArrayLength(array);
//        int elemsize = klass.getComponentType().getDataSize();
        return Address.fromObject(array);
    }

    /**
     * Create a native buffer pointing to either the array data directly,
     * or to a copy of the array data.
     * bytes
     * The returned pointer can be released when not needed.
     *
     * @param array the array to access
     * @param offset index of the first element to access
     * @param number number of elements to access
     * @return Pointer the C-accessible version of the array data
     * @throws OutOfMemoryError if the underlying memory cannot be allocated
     * @throws IllegalArgumentException if array is not really an array
     */
    public static Address createArrayBuffer(Object array, int offset, int number) throws OutOfMemoryError {
        Assert.always(GC.setGCEnabled(false) == false);
        Klass klass = GC.getKlass(array);
        if (!klass.isArray()) {
            throw new IllegalArgumentException();
        }

        int length = GC.getArrayLength(array);
        int elemsize = klass.getComponentType().getDataSize();
        Pointer.checkMultiBounds1(length + elemsize, offset, number, elemsize);
        return Address.fromObject(array).add(offset * elemsize);
    }


}
