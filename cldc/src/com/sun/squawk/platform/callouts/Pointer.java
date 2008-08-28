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
import com.sun.squawk.GC;
import com.sun.squawk.NativeUnsafe;
import com.sun.squawk.UWord;
import com.sun.squawk.Unsafe;
import com.sun.squawk.VM;
import com.sun.squawk.realtime.OffsetOutOfBoundsException;
import com.sun.squawk.realtime.RawMemoryFloatAccess;
import com.sun.squawk.realtime.SizeOutOfBoundsException;
import com.sun.squawk.vm.CID;

/**
 * A pointer to native memory, based on RTSJ-like RawMemoryAccesss semantics.
 * 
 * Unlike JNA, these Ptrs have a size, which allows the accessors to have bounds checks...
 */
public class Pointer extends RawMemoryFloatAccess {
    public static final Pointer NULL = new Pointer(0, 0);
    
    /**
     * Create a pointer and allocate backing native memory of the requestsed size.
     * This backing memory can be freed later using {@link #free()}.
     * 
     * @param size the number of bytes that can be referenced from this pointer
     * @throws com.sun.squawk.realtime.SizeOutOfBoundsException if the size is negative or too large
     * @throws java.lang.OutOfMemoryError if the backing memory cannot be allocated
     */
    public Pointer(int size) throws SizeOutOfBoundsException, OutOfMemoryError {
        super(null, size);
    }
    
    /**
     * Create a pointer that refers to a memory range from [base..base+size).
     * 
     * This pointer should NOT be freed.
     * 
     * @param base the base address of the pointer
     * @param size the number of bytes that can be referenced from this pointer
     * 
     * @throws SecurityException if the memory range intersets the Java heap
     *
     * @exception OffsetOutOfBoundsException    Thrown if the address is invalid.
     *
     * @exception SizeOutOfBoundsException Thrown if the size is negative or
     *            extends into an invalid range of memory.
     */
    public Pointer(long base, int size) {
        super(null, base, size);
    }
    
    /**
     * Create a pointer that refers to a memory range from [base..base+size).
     * 
     * This pointer should NOT be freed.
     * 
     * @param base the base address of the pointer
     * @param size the number of bytes that can be referenced from this pointer
     * 
     * @throws SecurityException if the memory range intersets the Java heap
     *
     * @exception OffsetOutOfBoundsException    Thrown if the address is invalid.
     *
     * @exception SizeOutOfBoundsException Thrown if the size is negative or
     *            extends into an invalid range of memory.
     */
    public Pointer(Address base, UWord size) {
        super(null, (base.toUWord().toPrimitive() & 0x00000000FFFFFFFFL), size.toPrimitive());
    }
   
    /**
     * Create a pointer that refers to a memory range from [base..base+size).
     * 
     * This pointer should NOT be freed.
     * 
     * @param base the base address of the pointer
     * @param size the number of bytes that can be referenced from this pointer
     * 
     * @throws SecurityException if the memory range intersets the Java heap
     *
     * @exception OffsetOutOfBoundsException    Thrown if the address is invalid.
     *
     * @exception SizeOutOfBoundsException Thrown if the size is negative or
     *            extends into an invalid range of memory.
     */
    public Pointer(Address base, int size) {
         super(null, (base.toUWord().toPrimitive() & 0x00000000FFFFFFFFL), size);
    }
    
    public String toString() {
        Address addr = getAddress();
        int size = this.getSize().toInt();
        if (addr.isZero()) {
            return "Pointer(null)";
        } else {
            return "Pointer(0x" + Integer.toHexString(addr.toUWord().toPrimitive()) + ", " + size + ")";
        }
    }
        
    /** 
     * Craete a ptr that's a based on an offset to some other pointer.
     * 
     * @param base pointer
     * @param offset from pointer
     * @param size of the subset of the base pointer
     */
    Pointer(Pointer base, int offset, int size) {
        super(base, offset, size);
    }
    
    /**
     * Gets the virtual memory location at which the memory region is mapped.
     *
     * @return The virtual address to which this is mapped (for reference
     *         purposes). Same as the base address if virtual memory is not supported.
     *
     *  @throws IllegalStateException Thrown if the raw memory object is not in the
     *      mapped state.
     */
    public final Address address() {
        return getAddress();
    }
    
    /**
     * Gets the size of the memory
     * @return the size of the native memory area
     */
    public final UWord size() {
        return getSize();
    }
    
    /**
     * Zero memory for the given number of bytes.
     * @param size number of bytes
     */
    public void clear(int size) {
        Address addr = getAddress();
        if (getSize().lo(UWord.fromPrimitive(size))) {
            throw new IllegalArgumentException();
        }
        if (addr.isZero()) {
             throw new IllegalStateException();
        }
        VM.setBytes(addr, (byte)0, size);
    }
    
    /**
     * Zero memory
     */
    public void clear() {
        Address addr = getAddress();
        if (addr.isZero()) {
            throw new IllegalStateException();
        }
        VM.setBytes(addr, (byte) 0, getSize().toPrimitive());
    }

    /**
     * Read a ptr value from memory at offset, and construct a new pointer representing the data stored there...
     * 
     * @param offset offset from <code>this</code> pointer that conatins a memory location that is an address.
     * @param size the size that the new pointer should have
     * @return a new pointer with e
     */
    public Pointer getPointer(int offset, int size) {
        Address ptr = Address.fromPrimitive(getInt(offset));
        if (ptr.isZero()) {
            return null;
        } else {
            return new Pointer(ptr, size);
        }
    }
    
    /**
     * Set the word at <code>offset</code> from <code>this</code> pointer to the the address contained in 
     *  <code>ptr</code>.
     * @param offset offset in bytes from this pointer's base to the word to be set.
     * @param ptr the value that will be set
     */
    public void setPointer(int offset, Pointer ptr) {
        setInt(offset, ptr.getAddress().toUWord().toPrimitive());
    }
    
    /**
     * Create a Java string from a C string pointer to by this pointer at offset
     * @param offset the byte offset of the c string from the base of this pointer
     * @return a java string
     */
    public String getString(int offset) {
        int len = 0;
        while (this.getByte(offset + len) != 0) {
            len++;
        }
        byte[] data = new byte[len];
        getBytes(offset, data, 0, len);
        return new String(data);
    }
 
    /**
     * Create a Java string from a C string pointer
     * @param cstr 
     * @return a java string
     */
    public static String NativeUnsafeGetString(Address cstr) {
        int len = 0;
        while (Unsafe.getByte(cstr, len) != 0) {
            len++;
        }
        byte[] data = new byte[len];
        Unsafe.getBytes(cstr, 0, data, 0, len);
        return new String(data);
    }

    /**
     * Copy string value to the location at <code>offset</code>. Convert the data in <code>value</code> to a
     * NULL-terminated C string, converted to native encoding.
     * 
     * @param offset the byte offset of the c string from the base of this pointer
     * @param value the string to copy
     */
    public final void setString(int offset, String value) {
        // optimize for 8-bit strings.
        if (GC.getKlass(value).getSystemID() == CID.STRING_OF_BYTES) {
            int len = value.length();
            checkMultiWrite(offset, len + 1, 1);
            VM.setData(getAddress(), offset, value, 0, len, 1);
            setByte((long) len, (byte) 0); // null terminate   
        } else {
            setString(offset, value.getBytes());
        }
    }
    
    /**
     * Copy bytes in <code>data</code> to the location at <code>offset</code>. Add 
     * NULL-termination. The bytes should already be converted to native encoding.
     * 
     * @param offset the byte offset of the c string from the base of this pointer
     * @param data
     */
    private final void setString(int offset, byte[] data) {
        int len = data.length;
        checkMultiWrite(offset, len + 1, 1);
        setBytes(offset, data, 0, len);
        setByte((long) len, (byte) 0); // null terminate
    }
    
    /**
     * Free the backing native memory for this pointer. After freeing the pointer,
     * all accesses to memory through this pointer will throw an exception.
     * 
     * @throws java.lang.IllegalStateException if free has already been called on this pointer.
     */
    public final void free() throws IllegalStateException {
        Address addr = getAddress();
        if (addr.isZero()) {
            throw new IllegalStateException();
        }
        NativeUnsafe.free(addr);
        addr = Address.zero();
    }

    /**
     * Create a native buffer containing the C-string version of the String <code>vaue</code>.
     * 
     * The returned point can be freed when not needed.
     * 
     * @param value the string to copy
     * @return Pointer the newly allocated memory
     * @throws OutOfMemoryError if the underlying memory cannot be allocated 
     */
    public static Pointer createStringBuffer(String value) throws OutOfMemoryError {
        // optimize for 8-bit strings.
        if (GC.getKlass(value).getSystemID() == CID.STRING_OF_BYTES) {
            Pointer result = new Pointer(value.length() + 1);
            result.setString(0, value);
            return result;
        } else {
            byte[] data = value.getBytes();
            Pointer result = new Pointer(data.length + 1);
            result.setString(0, data);
            return result;
        }
    }
 
    /**
     * Copy <code>len</code> bytes from <code>src</code> to <code>dst</code> starting at the given offsets.
     * Throws exception if the memory ranges specified for  <code>src</code> dst <code>dst</code> stray outside the 
     * valid ranges for those <code>Pointers</code>.
     * 
     * @param src Pointer to the source bytes
     * @param srcOffset offset in bytes to start copying from
     * @param dst Pointer to the destination bytes
     * @param dstOffset  offset in bytes to start copying to
     * @param len number of bytes to copy
     * 
     * @throws OffsetOutOfBoundsException Thrown if an offset is negative or greater than the size of the
     *      raw memory area.  The role of the {@link SizeOutOfBoundsException} somewhat overlaps
     *      this exception since it is thrown if the offset is within the object but outside the
     *      mapped area.
     *
     * @throws SizeOutOfBoundsException  Thrown if the object is not mapped,
     *      or if the requested memory raange falls in an invalid address range.
     */
    public static void copyBytes(Pointer src, int srcOffset, Pointer dst, int dstOffset, int len)
            throws OffsetOutOfBoundsException, SizeOutOfBoundsException  {
        dst.checkMultiWrite(dstOffset, len, 1);
        src.checkMultiRead(srcOffset, len, 1);
        VM.copyBytes(src.address(), srcOffset, dst.address(), dstOffset, len, false);
    }
}
