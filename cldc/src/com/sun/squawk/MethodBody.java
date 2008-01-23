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

package com.sun.squawk;

import com.sun.squawk.util.*;
import com.sun.squawk.vm.*;


/**
 * An instance of <code>MethodBody</code> represents the Squawk bytecode for
 * a method as well as all the other information related to the bytecode
 * such as exception handler tables, oop map for the activation frame etc.
 *
 */
public final class MethodBody {

    /**
     * Configuration option.
     * <p>
     * If set true then a long or double local variable will be
     * referenced as slot+0. If set false then it is addressed as slot+1.
     * <p>
     * Setting this false is will produce the correct offsets when the locals
     * are allocated at a negative offset from the frame pointer (which is common
     * for virtually all C ABIs).
     */
    public final static boolean LOCAL_LONG_ORDER_NORMAL = false;
    
    
    /**
     * The method info is capable of encoding the types of the parameters and locals,
     * but this is currently only used for debugging when using the CheneyCollector (see checkActivationForAddresses()).
     * 
     * Since the LARGE format can impose a 5% penalty on method calls (complicating extend and return),
     * only generate type tables when asserts are on.
     */
    public static final boolean ENABLE_SPECIFIC_TYPE_TABLES = Klass.ASSERTIONS_ENABLED;
    
    /**
     * This was sketched out, but not used. Optimize away in the mean time...
     */
    public static final boolean ENABLE_RELOCATION_TABLES = false;
    
    
    /**
     * Debug LARGE format.
     */
    public static final boolean FORCE_LARGE_FORMAT = /*VAL*/false/*DEBUG_CODE_ENABLED*/;

    /**
     * The enclosing method.
     */
    private final Method definingMethod;

    /**
     * The index of this method's definition in the symbols table
     * of its defining class.
     */
    private final int index;

    /**
     * The maximum size (in words) of the operand stack during execution
     * of this method.
     */
    private final int maxStack;

    /**
     * The number of words required by the parameters.
     */
    private final int parametersCount;

    /**
     * The exception handler table.
     */
    private final ExceptionHandler[] exceptionTable;

    /**
     * The debug information for the method.
     */
    private final MethodMetadata metadata;

    /**
     * The type map of the parameters and locals.
     */
    private final Klass[] localTypes;

    /**
     * The Squawk bytecode.
     */
    private final byte[] code;

/*if[TYPEMAP]*/
    /**
     * The type map describing the type of the value (if any) written to memory by each instruction in 'code'.
     */
    private final byte[] typeMap;
/*end[TYPEMAP]*/

    /**
     * Create a <code>MethodBody</code> representing a dummy object for the <code>ObjectGraphLoader</code>.
     */
    MethodBody() {
    	this.definingMethod = null;
    	this.index = -1;
    	this.maxStack = -1;
    	this.parametersCount = -1;
    	this.exceptionTable = null;
    	this.metadata = null;
    	this.localTypes = null;
    	this.code = null;
/*if[TYPEMAP]*/
        this.typeMap = null;
/*end[TYPEMAP]*/
    }
    
    /**
     * Creates a <code>MethodBody</code> representing the implementation details
     * of a method.
     *
     * @param definingMethod    the method in which the method body was defined
     * @param index             the index of the method in the symbols table
     * @param maxStack          the maximum size in words of the operand stack
     * @param locals            the types of the local variables (excludes parameters)
     * @param exceptionTable    the exception handler table
     * @param lnt               the table mapping instruction addresses to the
     *                          source line numbers that start at the addresses.
     *                          The table is encoded as an int array where the high
     *                          16-bits of each element is an instruction address and
     *                          the low 16-bits is the corresponding source line
     * @param lvt               the table describing the symbolic information for
     *                          the local variables in the method
     * @param code              the Squawk bytecode
     * @param typeMap           the type map describing the type of the value (if any) written
     *                          to memory by each instruction in 'code'
     * @param reverseParameters true if the parameters are pushed right-to-left
     */
    public MethodBody(
                       Method                definingMethod,
                       int                   index,
                       int                   maxStack,
                       Klass[]               locals,
                       ExceptionHandler[]    exceptionTable,
                       int[]                 lnt,
                       ScopedLocalVariable[] lvt,
                       byte[]                code,
                       byte[]                typeMap,
                       boolean               reverseParameters
                     ) {
        this.definingMethod  = definingMethod;
        this.index           = index;
        this.maxStack        = maxStack;
        this.exceptionTable  = exceptionTable;
        this.metadata        = MethodMetadata.create(definingMethod.getOffset(), lvt, lnt);
        this.code = code;
/*if[TYPEMAP]*/
        this.typeMap = typeMap;
/*end[TYPEMAP]*/

        /*
         * Make an array of classes with both the parameter and local types.
         */
        Klass[] parms   = definingMethod.getRuntimeParameterTypes(reverseParameters);
        parametersCount = parms.length;

        localTypes = new Klass[parms.length+locals.length];

        int j = 0;
        for (int i = 0 ; i < parms.length ; i++, j++) {
            localTypes[j] = parms[i];
        }
        for (int i = 0 ; i < locals.length ; i++, j++) {
            localTypes[j] = locals[i];
        }

        Assert.that(parametersCount >= 0);
        Assert.that(maxStack >= 0);
    }

    /**
     * Produce String for debugging
     *
     * @return the string
     */
    public String toString() {
        return "[bytecode for "+definingMethod.getDefiningClass().getName()+"."+definingMethod.getName();
    }

    /**
     * Gets the index of this method's definition in the symbols table
     * of its defining class.
     *
     * @return  the index of this method's definition
     */
    public int getIndex() {
        return index;
    }

    /**
     * Gets the bytecode.
     *
     * @return the bytecode
     */
    public byte[] getCode() {
        return code;
    }

/*if[TYPEMAP]*/
    /**
     * Gets the type map describing the types in activation frame expected by each bytecode.
     *
     * @return the type map describing the types in activation frame expected by each bytecode
     */
    public byte[] getTypeMap() {
        return typeMap;
    }
/*end[TYPEMAP]*/

    /**
     * Get the type map.
     *
     * @return the type map
     */
    public Klass[] getTypes() {
        return localTypes;
    }

    /**
     * Gets the class that defined this method.
     *
     * @return the class that defined this method
     */
    public Method getDefiningMethod() {
        return definingMethod;
    }

    /**
     * Gets the class that defined this method.
     *
     * @return the class that defined this method
     */
    public Klass getDefiningClass() {
        return definingMethod.getDefiningClass();
    }

    /**
     * Get the number of parameters.
     *
     * @return the number
     */
    public int getParametersCount() {
        return parametersCount;
    }

    /**
     * Get the exception table.
     *
     * @return the number
     */
    public ExceptionHandler[] getExceptionTable() {
        return exceptionTable;
    }

    /**
     * Get the number of stack words needed.
     *
     * @return the number
     */
    public int getMaxStack() {
        return maxStack;
    }

    /**
     * Gets the debug information (if any) pertaining to this method body.
     *
     * @return  the debug information pertaining to this method body or null
     *          if there isn't any
     */
    public MethodMetadata getMetadata() {
        return metadata;
    }


    /*-----------------------------------------------------------------------*\
     *                                Encoding                               *
    \*-----------------------------------------------------------------------*/

    /**
     * Minfo format encodings.
     */
    private final static int FMT_LARGE   = 0x80,   // specifies a large minfo section
                             FMT_E       = 0x01,   // specifies that there is an exception table
                             FMT_R       = 0x02,   // specifies that there is a relocation table
                             FMT_T       = 0x04,   // specifies that there is a type table
                             FMT_I       = 0x08;   // specifies that the method is only invoked by the interpreter

    /**
     * Encode the method header. The format of the header is described by the
     * following pseudo-C structures:
     * <p><hr><blockquote><pre>
     *  header {
     *      {
     *           u2 type;                               // class id of a float, long, double, Address, Offset or UWord local variable
     *           u4 index;                              // index of the local variable
     *      } type_table[type_table_size];
     *      {                                           // TBD: not yet designed/used
     *      } relocation_table[relocation_table_size];
     *      {
     *          u4 start_pc;
     *          u4 end_pc;
     *          u4 handler_pc;
     *          u2 catch_type;    // index into defining class's objectTable
     *      } exception_table[exception_table_size];
     *      u1 oopMap[oopMap_size];
     *      union {
     *          {
     *              u1 lo;      //  lllsssss
     *              u1 hi;      //  0pppppll
     *          } small_minfo;  //  'lllll' is locals_count, 'sssss' is max_stack, 'ppppp' is parameters_count
     *          {
     *              minfo_size type_table_size;         // exists only if 'T' bit in 'fmt' is set
     *              minfo_size relocation_table_size;   // exists only if 'R' bit in 'fmt' is set
     *              minfo_size exception_table_size;    // exists only if 'E' bit in 'fmt' is set
     *              minfo_size parameters_count;
     *              minfo_size locals_count;
     *              minfo_size max_stack;
     *              u1 fmt;                             // 1000ITRE
     *          } large_minfo;
     *      }
     *  }
     *
     * The minfo_size type is a u1 value if its high bit is 0, otherwise its a u2 value where
     * the high bit is masked off.
     *
     * </pre></blockquote><hr><p>
     *
     * The structures described above are actually stored in a byte array
     * encoded and decoded with a {@link ByteBufferEncoder} and
     * {@link ByteBufferDecoder} respectively.
     *
     * @param enc encoder
     */
    void encodeHeader(ByteBufferEncoder enc) {
        int start ;
        int localsCount = localTypes.length - parametersCount;

        /*
         * Encode the type table.
         */
        start = enc.getSize();
        if (MethodBody.ENABLE_SPECIFIC_TYPE_TABLES) {
            for (int i = 0 ; i < localTypes.length ; i++) {
                Klass k = localTypes[i];
                switch (k.getSystemID()) {
                    case CID.FLOAT:
                    case CID.LONG:
                    case CID.DOUBLE:
                    case CID.ADDRESS:
                    case CID.UWORD:
                    case CID.OFFSET:
                        enc.addUnsignedShort(k.getSystemID());
                        enc.addUnsignedInt(i);
                        break;
                }
            }
        }
        int typeTableSize = enc.getSize() - start;

        /*
         * Encode the relocation table.
         */
        start = enc.getSize();
        if (MethodBody.ENABLE_RELOCATION_TABLES) {
            // what goes here, anyway?
        }
        int relocTableSize = enc.getSize() - start;

        /*
         * Encode the exception table.
         */
        start = enc.getSize();
        if (exceptionTable != null) {
            for(int i = 0 ; i < exceptionTable.length ; i++) {
                ExceptionHandler handler = exceptionTable[i];
                enc.addUnsignedInt(handler.getStart());
                enc.addUnsignedInt(handler.getEnd());
                enc.addUnsignedInt(handler.getHandler());
                int handlerTypeIndex = definingMethod.getDefiningClass().getObjectIndex(handler.getKlass());
                enc.addUnsignedShort(handlerTypeIndex);
            }
        }
        int exceptionTableSize = enc.getSize() - start;

        /*
         * Encode the oopmap.
         */
        start = enc.getSize();
        int count = localTypes.length;
        int next = 0;
        while (count > 0) {
            int bite = 0;
            int n = (count < 8) ? count : 8;
            count -= n;
            for (int i = 0 ; i < n ; i++) {
                Klass k = localTypes[next++];
                if (k.isReferenceType()) {
                    bite |= (1<<i);
                }
            }
            enc.addUnsignedByte(bite);
        }
        int oopMapSize = enc.getSize() - start;

        Assert.that(oopMapSize == ((localsCount+parametersCount+7)/8));
        Assert.that(typeTableSize      < 32768);
        Assert.that(relocTableSize     < 32768);
        Assert.that(exceptionTableSize < 32768);
        Assert.that(localsCount        < 32768);
        Assert.that(parametersCount    < 32768);
        Assert.that(maxStack           < 32768);

        /*
         * Write the minfo area.
         *
         * The minfo is written in reverse. There are two formats, a compact one where there is no
         * type table, relocation table, exception table, and the number of words for local variables,
         * parameters, and stack are all less than 32 words, and there is a large format where the only
         * limits are that none of these values may exceed 32767.
         */
        if (!FORCE_LARGE_FORMAT     &&
            localsCount        < 32 &&
            parametersCount    < 32 &&
            maxStack           < 32 &&
            typeTableSize      == 0 &&
            relocTableSize     == 0 &&
            exceptionTableSize == 0 &&
            !definingMethod.isInterpreterInvoked()
           ) {
            /*
             * Small Minfo
             */
            enc.addUnencodedByte((localsCount<<5)     | (maxStack));         // byte 1 - lllsssss
            enc.addUnencodedByte((parametersCount<<2) | (localsCount>>3));   // byte 0 - 0pppppll
        } else {
            /*
             * Large Minfo
             */
            int fmt = FMT_LARGE;
            if (typeTableSize > 0) {
                writeMinfoSize(enc, typeTableSize);
                fmt |= FMT_T;
            }
            if (relocTableSize > 0) {
                writeMinfoSize(enc, relocTableSize);
                fmt |= FMT_R;
            }
            if (exceptionTableSize > 0) {
                writeMinfoSize(enc, exceptionTableSize);
                fmt |= FMT_E;
            }
            if (definingMethod.isInterpreterInvoked()) {
                fmt |= FMT_I;
            }
            writeMinfoSize(enc, parametersCount);
            writeMinfoSize(enc, localsCount);
            writeMinfoSize(enc, maxStack);
            enc.addUnsignedByte(fmt);
        }
    }

    /**
     * Roundup the data in the ByteBufferEncoder so that it is modulo HDR.BYTES_PER_WORD in length
     * after some extra data is added.
     *
     * @param enc the encoder
     * @param extra the number of bytes that will be added
     */
    private void roundup(ByteBufferEncoder enc, int extra) {
        while ((enc.getSize()+extra) % HDR.BYTES_PER_WORD != 0) {
            enc.addUnsignedByte(0);
        }
    }

    /**
     * Write a length into the minfo
     *
     * @param enc the encoder
     * @param value the value
     */
    private void writeMinfoSize(ByteBufferEncoder enc, int value) {
        if (value < 128) {
            enc.addUnsignedByte(value);
        } else {
            Assert.that(value < 32768);
            enc.addUnsignedByte(value & 0xFF);
            enc.addUnsignedByte(0x80|(value>>8));
        }
    }

    /**
     * Return size of the method byte array.
     *
     * @return the size in bytes
     */
    int getCodeSize() {
        return code.length;
    }

    /**
     * Write the bytecodes to VM memory.
     *
     * @param oop address of the method object
     */
    void writeToVMMemory(Object oop) {
        for (int i = 0 ; i < code.length ; i++) {
            NativeUnsafe.setByte(oop, i, code[i]);
        }
    }

/*if[TYPEMAP]*/
    /**
     * Write the type map for the bytecodes to VM memory.
     *
     * @param oop address of the method object
     */
    void writeTypeMapToVMMemory(Object oop) {
        Assert.always(VM.usingTypeMap());
        Address p = Address.fromObject(oop);
        for (int i = 0 ; i < typeMap.length ; i++) {
            NativeUnsafe.setType(p, typeMap[i], 1);
            p = p.add(1);
        }
    }
/*end[TYPEMAP]*/

    /*-----------------------------------------------------------------------*\
     *                                Decoding                               *
    \*-----------------------------------------------------------------------*/

    /**
     * Determines if a given method is only invoked from the interpreter
     *
     * @param oop the pointer to the method
     * @return true if oop is an intrepreter invoked only method
     */
    public static boolean isInterpreterInvoked(Object oop) {
        int b0 = NativeUnsafe.getByte(oop, HDR.methodInfoStart) & 0xFF;
        if (b0 < 128) {
            return false;
        } else {
            return (b0 & FMT_I) != 0;
        }
    }

    /**
     * Decodes the parameter count from the method header.
     *
     * @param oop the pointer to the method
     * @return the number of parameters
     */
    static int decodeParameterCount(Object oop) {
        int b0 = NativeUnsafe.getByte(oop, HDR.methodInfoStart) & 0xFF;
        if (b0 < 128) {
            return b0 >> 2;
        } else {
            return minfoValue3(oop);
        }
    }

    /**
     * Decodes the local variable count from the method header.
     *
     * @param oop the pointer to the method
     * @return the number of locals
     */
    static int decodeLocalCount(Object oop) {
        int b0 = NativeUnsafe.getByte(oop, HDR.methodInfoStart) & 0xFF;
        if (b0 < 128) {
            int b1 = NativeUnsafe.getByte(oop, HDR.methodInfoStart-1) & 0xFF;
            return (((b0 << 8) | b1) >> 5) & 0x1F;
        } else {
            return minfoValue2(oop);
        }
    }

    /**
     * Decodes the stack count from the method header.
     *
     * @param oop the pointer to the method
     * @return the number of stack words
     */
    static int decodeStackCount(Object oop) {
        int b0 = NativeUnsafe.getByte(oop, HDR.methodInfoStart) & 0xFF;
        if (b0 < 128) {
            int b1 = NativeUnsafe.getByte(oop, HDR.methodInfoStart-1) & 0xFF;
            return b1 & 0x1F;
        } else {
            return minfoValue1(oop);
        }
    }

    /**
     * Decodes the exception table size from the method header.
     *
     * @param oop the pointer to the method
     * @return the number of bytes
     */
    static int decodeExceptionTableSize(Object oop) {
        int b0 = NativeUnsafe.getByte(oop, HDR.methodInfoStart) & 0xFF;
        if (b0 < 128 || ((b0 & FMT_E) == 0)) {
            return 0;
        }
        return minfoValue4(oop);
    }

    /**
     * Decodes the relocation table size from the method header.
     *
     * @param oop the pointer to the method
     * @return the number of bytes
     */
    static int decodeRelocationTableSize(Object oop) {
        if (MethodBody.ENABLE_RELOCATION_TABLES) {
            int b0 = NativeUnsafe.getByte(oop, HDR.methodInfoStart) & 0xFF;
            if (b0 < 128 || ((b0 & FMT_R) == 0)) {
                return 0;
            }
            int offset = 4;
            if ((b0 & FMT_E) != 0) {
                offset++;
            }
            return minfoValue(oop, offset);
        } else {
            return 0;
        }
    }

    /**
     * Decodes the type table size from the method header.
     *
     * @param oop the pointer to the method
     * @return the number of bytes
     */
    static int decodeTypeTableSize(Object oop) {
        if (MethodBody.ENABLE_SPECIFIC_TYPE_TABLES) {
            int b0 = NativeUnsafe.getByte(oop, HDR.methodInfoStart) & 0xFF;
            if (b0 < 128 || ((b0 & FMT_T) == 0)) {
                return 0;
            }
            int offset = 4;
            if ((b0 & FMT_E) != 0) {
                offset++;
            }
            if ((b0 & FMT_R) != 0) {
                offset++;
            }
            return minfoValue(oop, offset);
        } else {
            return 0;
        }
    }
    
    /**
     * Decode a counter from the minfo area.
     *
     * This is the canonnical, unrolled form. Callers actually use the unrolled forms below.
     *
     * Note that these methods are also translated to C (as part of the vm2c process. So these
     * methods are actually the source for the interpreter too.
     *
     * @param oop the pointer to the method
     * @param offset the ordinal offset of the counter (e.g. 1st, 2nd, ...  etc.)
     * @return the value
     */
    private static int minfoValue(Object oop, int offset) {
        int p = HDR.methodInfoStart - 1;
        int val = -1;
        Assert.that(((NativeUnsafe.getByte(oop, p+1) & 0xFF) & FMT_LARGE) != 0);
        while(offset-- > 0) {
            val = NativeUnsafe.getByte(oop, p--) & 0xFF;
            if (val > 127) {
                p--;
            }
        }
        if (val > 127) {
            val = val & 0x7F;
            val = val << 8;
            val = val | (NativeUnsafe.getByte(oop, p-1) & 0xFF);
        }
        Assert.that(val >= 0);
        return val;
    }
    
    private static int minfoValue1(Object oop) {
        int p = HDR.methodInfoStart - 1;
        int val;
        Assert.that(((NativeUnsafe.getByte(oop, p+1) & 0xFF) & FMT_LARGE) != 0);
        val = NativeUnsafe.getByte(oop, p--) & 0xFF;
        if (val > 127) {
            val = val & 0x7F;
            val = val << 8;
            val = val | (NativeUnsafe.getByte(oop, p) & 0xFF);
        }
        Assert.that(val >= 0);
        return val;
    }
    
    private static int minfoValue2(Object oop) {
        int p = HDR.methodInfoStart - 1;
        int val;
        Assert.that(((NativeUnsafe.getByte(oop, p+1) & 0xFF) & FMT_LARGE) != 0);
        if (NativeUnsafe.getByte(oop, p--) < 0) {
            p--;
        }
        val = NativeUnsafe.getByte(oop, p--) & 0xFF;
        if (val > 127) {
            val = val & 0x7F;
            val = val << 8;
            val = val | (NativeUnsafe.getByte(oop, p) & 0xFF);
        }
        Assert.that(val >= 0);
        return val;
    }
    
    private static int minfoValue3(Object oop) {
        int p = HDR.methodInfoStart - 1;
        int val;
        Assert.that(((NativeUnsafe.getByte(oop, p+1) & 0xFF) & FMT_LARGE) != 0);
        if (NativeUnsafe.getByte(oop, p--) < 0) {
            p--;
        }
        if (NativeUnsafe.getByte(oop, p--) < 0) {
            p--;
        }
        val = NativeUnsafe.getByte(oop, p--) & 0xFF;
        if (val > 127) {
            val = val & 0x7F;
            val = val << 8;
            val = val | (NativeUnsafe.getByte(oop, p) & 0xFF);
        }
        Assert.that(val >= 0);
        return val;
    }
    
    private static int minfoValue4(Object oop) {
        int p = HDR.methodInfoStart - 1;
        int val;
        Assert.that(((NativeUnsafe.getByte(oop, p+1) & 0xFF) & FMT_LARGE) != 0);
        if (NativeUnsafe.getByte(oop, p--) < 0) {
            p--;
        }
        if (NativeUnsafe.getByte(oop, p--) < 0) {
            p--;
        }
        if (NativeUnsafe.getByte(oop, p--) < 0) {
            p--;
        }
        val = NativeUnsafe.getByte(oop, p--) & 0xFF;
        if (val > 127) {
            val = val & 0x7F;
            val = val << 8;
            val = val | (NativeUnsafe.getByte(oop, p) & 0xFF);
        }
        Assert.that(val >= 0);
        return val;
    }
            
    /**
     * Get the offset to the last byte of the Minfo area.
     *
     * @param oop the pointer to the method
     * @return the length in bytes
     */
    private static int getOffsetToLastMinfoByte(Object oop) {
        int p = HDR.methodInfoStart;
        int b0 = NativeUnsafe.getByte(oop, p--) & 0xFF;
        if (b0 < 128) {
            p--;
        } else {
            int offset = 3;
            if ((b0 & FMT_E) != 0) {
                offset++;
            }
            if ((b0 & FMT_R) != 0) {
                offset++;
            }
            if ((b0 & FMT_T) != 0) {
                offset++;
            }
            while(offset-- > 0) {
                int val = NativeUnsafe.getByte(oop, p--) & 0xFF;
                if (val > 127) {
                    p--;
                }
            }
        }
        return p + 1;
    }

    /**
     * Decodes the offset from the method header to the start of the oop map.
     *
     * @param oop the pointer to the method
     * @return the offset in bytes
     */
    static int decodeOopmapOffset(Object oop) {
        int vars = decodeLocalCount(oop) + decodeParameterCount(oop);
        int oopmapLth = (vars+7)/8;
        return getOffsetToLastMinfoByte(oop) - oopmapLth;
    }

    /**
     * Decodes the offset from the method header to the start of the exception table.
     *
     * @param oop the pointer to the method
     * @return the offset in bytes
     */
    static int decodeExceptionTableOffset(Object oop) {
        int vars = decodeLocalCount(oop) + decodeParameterCount(oop);
        int oopmapLth = (vars+7)/8;
        return getOffsetToLastMinfoByte(oop) - oopmapLth - decodeExceptionTableSize(oop);
    }

    /**
     * Decodes the offset from the method header to the start of the relocation table.
     *
     * @param oop the pointer to the method
     * @return the offset in bytes
     */
    static int decodeRelocationTableOffset(Object oop) {
        int vars = decodeLocalCount(oop) + decodeParameterCount(oop);
        int oopmapLth = (vars+7)/8;
        return getOffsetToLastMinfoByte(oop) - oopmapLth - decodeExceptionTableSize(oop) - decodeRelocationTableSize(oop);
    }

    /**
     * Decodes the offset from the method header to the start of the type table.
     *
     * @param oop the pointer to the method
     * @return the offset in bytes
     */
    static int decodeTypeTableOffset(Object oop) {
        int vars = decodeLocalCount(oop) + decodeParameterCount(oop);
        int oopmapLth = (vars+7)/8;
        return getOffsetToLastMinfoByte(oop) - oopmapLth - decodeExceptionTableSize(oop) - decodeRelocationTableSize(oop) - decodeTypeTableSize(oop);
    }

    /**
     * Decodes the oopmap and type table into an array of Klass instances.
     * <p>
     * This cannot be used by the garbage collector because it allocates an object.
     *
     * @param oop  the pointer to the method
     * @return the type map as an array of Klass instances
     */
    static Klass[] decodeTypeMap(Object oop) {

        int localCount     = decodeLocalCount(oop);
        int parameterCount = decodeParameterCount(oop);

        Klass types[] = new Klass[parameterCount+localCount];

        /*
         * Decodes the oopmap.
         */
        if (types.length > 0) {
            int offset = decodeOopmapOffset(oop);
            for (int i = 0 ; i < types.length ; i++) {
                int pos = i / 8;
                int bit = i % 8;
                int bite = NativeUnsafe.getByte(oop, offset+pos) & 0xFF;
                boolean isRef = ((bite>>bit)&1) != 0;
                types[i] = (isRef) ? Klass.OBJECT : Klass.INT;
            }
        }

        /*
         * Decodes the type table.
         */
        if (decodeTypeTableSize(oop) > 0) {
            int size   =  decodeTypeTableSize(oop);
            int offset =  decodeTypeTableOffset(oop);
            VMBufferDecoder dec = new VMBufferDecoder(oop, offset);
            int end = offset + size;
            while (dec.getOffset() < end) {
                int cid  = dec.readUnsignedShort();
                int slot = dec.readUnsignedInt();
                int slot2 = slot < parameterCount || LOCAL_LONG_ORDER_NORMAL ? slot + 1 : slot - 1;
                switch (cid) {
                    case CID.ADDRESS: types[slot]  = Klass.ADDRESS; break;
                    case CID.OFFSET:  types[slot]  = Klass.OFFSET;  break;
                    case CID.UWORD:   types[slot]  = Klass.UWORD;   break;
                    case CID.LONG:    types[slot]  = Klass.LONG;
                                      types[slot2] = Klass.LONG2;   break;
                    case CID.FLOAT:   types[slot]  = Klass.FLOAT;   break;
                    case CID.DOUBLE:  types[slot]  = Klass.DOUBLE;
                                      types[slot2] = Klass.DOUBLE2; break;
                    default: Assert.shouldNotReachHere();
                }
            }
        }
        return types;
    }

    /**
     * Return the address of the first word of the object header.
     *
     * @param oop the pointer to the method
     * @return the VM address of the header
     */
    static Address oopToBlock(Object oop) {
        int offset = decodeTypeTableOffset(oop);
        while ((offset % HDR.BYTES_PER_WORD) != 0) {
            --offset;
        }
        offset -= HDR.BYTES_PER_WORD; // skip back the header length word
        return Address.fromObject(oop).add(offset);
    }


/*if[DEBUG_CODE_ENABLED]*/
    /*-----------------------------------------------------------------------*\
     *                                Verifing                               *
    \*-----------------------------------------------------------------------*/

    /**
     * Verify a new method.
     *
     * @param oop the pointer to the encoded method
     */
    void verifyMethod(Object oop) {
        /*
         * Check the basic parameters.
         */
        int localCount = localTypes.length - parametersCount;
        Assert.that(decodeLocalCount(oop)     == localCount);
        Assert.that(decodeParameterCount(oop) == parametersCount);
        Assert.that(decodeStackCount(oop)     == maxStack);

        /*
         * Check the oopmap.
         */
        if (localTypes.length > 0) {
            int offset = decodeOopmapOffset(oop);
            for (int i = 0 ; i < localTypes.length ; i++) {
                Klass k = localTypes[i];
                int pos = i / 8;
                int bit = i % 8;
                int bite = NativeUnsafe.getByte(oop, offset+pos) & 0xFF;
                boolean isOop = ((bite>>bit)&1) != 0;
                if (k.isReferenceType()) {
                    Assert.that(isOop == true);
                } else {
                    Assert.that(isOop == false);
                }
            }
        }

        /*
         * Check the exception table.
         */
        if (decodeExceptionTableSize(oop) == 0) {
            Assert.that(exceptionTable == null || exceptionTable.length == 0);
        } else {
            Assert.that(exceptionTable != null && exceptionTable.length > 0);
            int size   = decodeExceptionTableSize(oop);
            int offset = decodeExceptionTableOffset(oop);
            VMBufferDecoder dec = new VMBufferDecoder(oop, offset);
            for (int i = 0 ; i < exceptionTable.length ; i++) {
                ExceptionHandler handler = exceptionTable[i];
                Assert.that(dec.readUnsignedInt() == handler.getStart());
                Assert.that(dec.readUnsignedInt() == handler.getEnd());
                Assert.that(dec.readUnsignedInt() == handler.getHandler());
                Assert.that(getDefiningClass().getObject(dec.readUnsignedShort()) == handler.getKlass());
            }
            dec.checkOffset(offset + size);
        }

        /*
         * Check the type table.
         */
        if (decodeTypeTableSize(oop) == 0) {
            for (int i = 0 ; i < localTypes.length ; i++) {
                Klass k = localTypes[i];
                Assert.that(k != Klass.FLOAT && k != Klass.DOUBLE && k != Klass.LONG && !k.isSquawkPrimitive());
            }
        } else {
            int size   = decodeTypeTableSize(oop);
            int offset = decodeTypeTableOffset(oop);
            VMBufferDecoder dec = new VMBufferDecoder(oop, offset);
            for (int i = 0 ; i < localTypes.length ; i++) {
                Klass k = localTypes[i];
                if (k == Klass.FLOAT || k == Klass.DOUBLE || k == Klass.LONG || k.isSquawkPrimitive()) {
                    Assert.that(dec.readUnsignedShort() == k.getSystemID());
                    Assert.that(dec.readUnsignedInt() == i);
                }
            }
            dec.checkOffset(offset + size);
        }

        /*
         * Check the relocation table.
         */
        Assert.that(decodeRelocationTableSize(oop) == 0);

        /*
         * Check the bytecodes.
         */
        Assert.that(GC.getArrayLengthNoCheck(oop) == code.length);
        if (!VM.usingTypeMap()) {
            for (int i = 0; i < code.length; i++) {
                Assert.that(NativeUnsafe.getByte(oop, i) == code[i]);
            }
        }
    }
/*end[DEBUG_CODE_ENABLED]*/

}
