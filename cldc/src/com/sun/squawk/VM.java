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

import java.io.IOException;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Hashtable;

/*if[OLD_IIC_MESSAGES]*/
import com.sun.squawk.io.ServerConnectionHandler;
/*end[OLD_IIC_MESSAGES]*/
import com.sun.squawk.io.mailboxes.Mailbox;
import com.sun.squawk.peripheral.PeripheralRegistry;
import com.sun.squawk.pragma.GlobalStaticFields;
import com.sun.squawk.pragma.InterpreterInvokedPragma;
import com.sun.squawk.pragma.NotInlinedPragma;
import com.sun.squawk.util.Assert;
import com.sun.squawk.util.IntHashtable;
import com.sun.squawk.util.SquawkHashtable;
import com.sun.squawk.util.SquawkVector;
import com.sun.squawk.vm.ChannelConstants;
import com.sun.squawk.vm.Global;
import com.sun.squawk.vm.HDR;
import com.sun.squawk.vm.Native;
import com.sun.squawk.vm.SC;
import com.sun.squawk.vm.FieldOffsets;

/**
 * This is a Squawk VM specific class that is used to communicate between
 * executing Java software and the low level core VM that is expressed
 * in machine code. There are two parts to this. There are a set of native
 * methods that are used to invoke very low level operations like manipulating
 * memory at a very low level or performing I/O. In the other direction there
 * are a number of methods that the low level core may call. These are used to
 * implement high level operations that are better done in Java than in machine
 * code.
 * <p>
 * A special version of this class exists for the romizer. The romizer version
 * only implements the methods used to manipulate memory.
 *
 * @version 1.0
 */
public class VM implements GlobalStaticFields {

    /*
     * Note regarding methods marked with InterpreterInvokedPragma.
     *
     * These methods must only be called from the VM interpreter or jitted code.
     * In a system where parameters are pushed onto the stack in the right-to-left
     * order (x86, ARM, etc.) the translator makes sure that these methods 
     * are changed so that the normal Java left-to-right
     * convention is used so that parameter pushed onto the Java runtime stack
     * do not need to be reordered. The net result of this is that these methods
     * must not be called from regular Java code.
     */
    
    /**
     * The VM class is never to be instantiated. All methods and fields are static.
     */
    private VM(){}
    
    /**
     * Address of the start of the object memory in ROM.
     */
    private static Address romStart;

    /**
     * Address of the first byte after the end of the object memory in ROM.
     */
    private static Address romEnd;

    /**
     * Address of the start of the object memory containing the bootstrap suite.
     */
    private static Address bootstrapStart;

    /**
     * Address of the first byte after the end of the object memory containing the bootstrap suite.
     */
    private static Address bootstrapEnd;

    /**
     * The hash of the object memory containing the bootstrap suite in it's canonical (i.e. relative to
     * address 0) form.
     */
    private static int bootstrapHash;

    /**
     * The verbosity level.
     */
    private static int verboseLevel;

    /**
     * Flag to say that synchronization is enabled.
     */
    private static boolean synchronizationEnabled;

    /**
     * Flag to say that exception handling is enabled.
     */
    private static boolean exceptionsEnabled;

    /**
     * Pointer to the preallocated OutOfMemoryError object.
     */
    private static OutOfMemoryError outOfMemoryError;

    /**
     * Pointer to the preallocated a VMBufferDecoder used by the do_throw code.
     */
    private static VMBufferDecoder vmbufferDecoder;

    /*
     * Create the isolate of the currently executing thread.
     */
    private static Isolate currentIsolate;

    /**
     * The next hashcode to be allocated.
     */
    private static int nextHashcode;

    /**
     * Allow Runtime.gc() to cause a collection.
     */
    private static boolean allowUserGC;

    /**
     * Flag to show if the extend bytecode can be executed. This variable is used to guard
     * a section of code that must never require allocation. This is usually because it
     * is using an Address variable (which is invalidated by a garbage collection).
     */
    static boolean extendsEnabled;

    /**
     * Flags if the VM was built with memory access type checking enabled.
     */
    private static boolean usingTypeMap;

/*if[OLD_IIC_MESSAGES]*/
    /**
     * The list of ServerConnectionHandlers. For JavaDriverManager
     */
    private static ServerConnectionHandler serverConnectionHandlers;
/*end[OLD_IIC_MESSAGES]*/

    /**
     * The C array of the null terminated C strings representing the command line
     * arguments that will be converted to a String[] and passed to the {@link JavaApplicationManager}.
     */
    private static Address argv;

    /**
     * The number of elements in the {@link #argv} array.
     */
    private static int argc;
    
    /**
     * True IFF the first isolate has already been invoked with true indicate first invocation.
     */
    private static boolean isFirstIsolateInitialized;
    
    /**
     * The name of the class to invoke main on when an isolate is being initialized.
     */
    private static String isolateInitializer;
    
    /**
     * Global hashtable of registered mailboxes.
     */
    private static SquawkHashtable registeredMailboxes;
    
    /**
     * 
     */
    private static PeripheralRegistry peripheralRegistry;
    
     /**
     * Used by interepreter to provide info on index out of bounds exceptions
     */
    private static int reportedIndex;
    
   /**
     * Used by interepreter to provide info on index out of bounds exceptions
     */
    private static Object reportedArray;
    
    /**
     * Manage shutdown hooks
     */
    private static CallbackManager shutdownHooks;
    
    /**
     * Count the number of exceptions thrown.
     */
    private static int throwCount;
    
    
    /*=======================================================================*\
     *                          VM callback routines                         *
    \*=======================================================================*/

    /**
     * Squawk startup routine.
     *
     * @param bootstrapSuite        the bootstrap suite
     */
    static void startup(Suite bootstrapSuite) throws InterpreterInvokedPragma {

        /*
         * Set default for allowing Runtime.gc() to work.
         */
        VM.allowUserGC = true;

        /*
         * Initialize the garbage collector, suite manager then allocate a VMBufferDecoder
         * for use by the code in do_throw() and the OutOfMemoryError.
         */
        GC.initialize(bootstrapSuite);

        vmbufferDecoder  = new VMBufferDecoder();
        outOfMemoryError = new OutOfMemoryError();

        /*
         * Create the root isolate and manually initialize com.sun.squawk.Klass.
         */
        String[] args  = new String[argc];
        currentIsolate = new Isolate("com.sun.squawk.JavaApplicationManager", args, bootstrapSuite);
        currentIsolate.initializeClassKlass();

        /*
         * Initialise threading.
         */
        VMThread.initializeThreading();
        synchronizationEnabled = true;

        /*
         * Fill in the args array with the C command line arguments.
         */
        GC.copyCStringArray(argv, args);

        /*
         * Start the isolate guarded with an exception handler. Once the isolate
         * has been started enter the service operation loop.
         */
        try {
            exceptionsEnabled = true;
            shutdownHooks = new CallbackManager(true);
            currentIsolate.primitiveThreadStart();
            ServiceOperation.execute();
        } catch (Throwable ex) {
            fatalVMError();
        }
    }

    /**
     * This is the native method that is called by the VM for native
     * declarations that are unsatisifed by the translator.
     *
     * @param id the identifier of the unknown native method
     */
    static void undefinedNativeMethod(int id) throws InterpreterInvokedPragma {
        throw new Error("Undefined native method: " + id);
    }

    /**
     * Start running the current thread.
     */
    static void callRun() throws InterpreterInvokedPragma {
        VMThread.currentThread().callRun(); // never returns. When callRun is done, it deschedules itself forever.
    }

    /**
     * Read a static reference variable.
     *
     * @param klass  the class of the variable
     * @param offset the offset (in words) to the variable
     * @return the value
     */
    static Object getStaticOop(Klass klass, int offset) throws InterpreterInvokedPragma {
        Object ks = currentIsolate.getClassStateForStaticVariableAccess(klass, offset);
        return NativeUnsafe.getObject(ks, offset);
    }

    /**
     * Read a static int variable.
     *
     * @param klass  the class of the variable
     * @param offset the offset (in words) to the variable
     * @return the value
     */
    static int getStaticInt(Klass klass, int offset) throws InterpreterInvokedPragma {
        Object ks = currentIsolate.getClassStateForStaticVariableAccess(klass, offset);
        return (int)NativeUnsafe.getUWord(ks, offset).toPrimitive();
    }

    /**
     * Read a static long variable.
     *
     * @param klass  the class of the variable
     * @param offset the offset (in words) to the variable
     * @return the value
     */
    static long getStaticLong(Klass klass, int offset) throws InterpreterInvokedPragma {
        Object ks = currentIsolate.getClassStateForStaticVariableAccess(klass, offset);
        return NativeUnsafe.getLongAtWord(ks, offset);
    }

    /**
     * Write a static reference variable.
     *
     * @param value  the value
     * @param klass  the class of the variable
     * @param offset the offset (in words) to the variable
     */
    static void putStaticOop(Object value, Klass klass, int offset) throws InterpreterInvokedPragma {
        Object ks = currentIsolate.getClassStateForStaticVariableAccess(klass, offset);
        NativeUnsafe.setObject(ks, offset, value);
    }

    /**
     * Write a static int variable.
     *
     * @param value  the value
     * @param klass  the class of the variable
     * @param offset the offset (in words) to the variable
     */
    static void putStaticInt(int value, Klass klass, int offset) throws InterpreterInvokedPragma {
        Object ks = currentIsolate.getClassStateForStaticVariableAccess(klass, offset);
        NativeUnsafe.setUWord(ks, offset, UWord.fromPrimitive(value));
    }

    /**
     * Write a static long variable.
     *
     * @param value  the value
     * @param klass  the class of the variable
     * @param offset the offset (in words) to the variable
     */
    static void putStaticLong(long value, Klass klass, int offset) throws InterpreterInvokedPragma {
        Object ks = currentIsolate.getClassStateForStaticVariableAccess(klass, offset);
        NativeUnsafe.setLongAtWord(ks, offset, value);
    }

    /**
     * Optionally cause thread rescheduling.
     */
    static void yield() throws InterpreterInvokedPragma {
        VMThread.yield();
    }

    /**
     * Throws a NullPointerException.
     */
    static void nullPointerException() throws InterpreterInvokedPragma {
        throw new NullPointerException();
    }

    /**
     * Throws an ArrayIndexOutOfBoundsException.
     */
    static void arrayIndexOutOfBoundsException() throws InterpreterInvokedPragma {
        if (reportedArray != null) {
            Object array = reportedArray;
            reportedArray = null;
            throw new ArrayIndexOutOfBoundsException("on " + GC.getKlass(array).getInternalName() + " of length " + GC.getArrayLength(array) + " with index " + reportedIndex);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * Throws an ArithmeticException.
     */
    static void arithmeticException() throws InterpreterInvokedPragma {
        throw new ArithmeticException();
    }

    /**
     * Throws an AbstractMethodError.
     */
    static void abstractMethodError() throws InterpreterInvokedPragma {
        throw new Error("AbstractMethodError");
    }

    /**
     * Set write barrier bit for the store of a reference to an array.
     *
     * @param array the array
     * @param index the array index
     * @param value the value to be stored into the array
     */
    static void arrayOopStore(Object array, int index, Object value) throws InterpreterInvokedPragma {
        Klass arrayKlass = GC.getKlass(array);
        Assert.that(arrayKlass.isArray());
        Assert.that(value != null);
        Klass componentType = arrayKlass.getComponentType();

        /*
         * Klass.isAssignableFrom() will not work before class Klass is initialized. Use the
         * synchronizationEnabled flag to show that the system is ready for this.
         */
        if (synchronizationEnabled == false || componentType.isAssignableFrom(GC.getKlass(value))) {
            NativeUnsafe.setObject(array, index, value);
        } else {
            throw new ArrayStoreException();
        }
    }

    /**
     * Find the virtual slot number for an object that corresponds to the slot in an interface.
     *
     * @param obj     the receiver
     * @param iklass  the interface class
     * @param islot   the virtual slot of the interface method
     * @return the virtual slot of the receiver
     */
    static int findSlot(Object obj, Klass iklass, int islot) throws InterpreterInvokedPragma {
/*if[FAST_INVOKEINTERFACE]*/
        throw Assert.shouldNotReachHere();
/*else[FAST_INVOKEINTERFACE]*/
//        Klass klass = GC.getKlass(obj);
//        return klass.findSlot(iklass, islot);
/*end[FAST_INVOKEINTERFACE]*/
    }

    /**
     * Synchronize on an object.
     *
     * @param oop the object
     */
    static void monitorenter(Object oop) throws InterpreterInvokedPragma {
        if (synchronizationEnabled) {
            VMThread.monitorEnter(oop);
        }
    }

    /**
     * Desynchronize on an object.
     *
     * @param oop the object
     */
    static void monitorexit(Object oop) throws InterpreterInvokedPragma {
        if (synchronizationEnabled) {
            VMThread.monitorExit(oop);
        }
    }

    /**
     * Test to see if an object is an instance of a class.
     *
     * @param obj the object (not null)
     * @param klass the class (not null)
     * @return true if is can
     */
    static boolean _instanceof(Object obj, Klass klass) throws InterpreterInvokedPragma {
        Assert.that(obj != null && klass != null);
        return klass.isAssignableFrom(GC.getKlass(obj));
    }

    /**
     * Check that an object can be cast to a class.
     *
     * @param obj the object (not null)
     * @param klass the class
     * @return the same object
     * @exception ClassCastException if the case is illegal
     */
    static Object checkcast(Object obj, Klass klass) throws InterpreterInvokedPragma {
        Assert.that(obj != null);
        if (!klass.isAssignableFrom(GC.getKlass(obj))) {
            throw new ClassCastException("Expected object of type " + klass.getName() + " but got object of type " + GC.getKlass(obj).getName());
        }
        return obj;
    }

    /**
     * Lookup the position of a value in a sorted array of numbers.
     *
     * @param key the value to look for
     * @param array the array
     * @return the index or -1 if the lookup fails
     */
    static int lookup_b(int key, byte[] array) throws InterpreterInvokedPragma {
        int low = 0;
        int high = array.length - 1;
        while (low <= high) {
            int mid = (low + high) / 2;
            int val = array[mid];
            if (key < val) {
                high = mid - 1;
            } else if (key > val) {
                low = mid + 1;
            } else {
                return mid;
            }
        }
        return -1;
    }

    /**
     * Lookup the position of a value in a sorted array of numbers.
     *
     * @param key the value to look for
     * @param array the array
     * @return the index or -1 if the lookup fails
     */
    static int lookup_s(int key, short[] array) throws InterpreterInvokedPragma {
        int low = 0;
        int high = array.length - 1;
        while (low <= high) {
            int mid = (low + high) / 2;
            int val = array[mid];
            if (key < val) {
                high = mid - 1;
            } else if (key > val) {
                low = mid + 1;
            } else {
                return mid;
            }
        }
        return -1;
    }

    /**
     * Lookup the position of a value in a sorted array of numbers.
     *
     * @param key the value to look for
     * @param array the array
     * @return the index or -1 if the lookup fails
     */
    static int lookup_i(int key, int[] array) throws InterpreterInvokedPragma {
        int low = 0;
        int high = array.length - 1;
        while (low <= high) {
            int mid = (low + high) / 2;
            int val = array[mid];
            if (key < val) {
                high = mid - 1;
            } else if (key > val) {
                low = mid + 1;
            } else {
                return mid;
            }
        }
        return -1;
    }

    /**
     * Initialize a class.
     *
     * @param klass the klass
     */
    static void class_clinit(Klass klass) throws InterpreterInvokedPragma {
        klass.initialiseClass();
    }

    /**
     * Allocate an instance.
     *
     * @param klass the klass of the instance
     * @return the new object
     * @exception OutOfMemoryException if allocation fails
     */
    static Object _new(Klass klass) throws InterpreterInvokedPragma {
        klass.initialiseClass();
        return GC.newInstance(klass);
    }

    /**
     * Allocate an array.
     *
     * @param klass the klass of the instance
     * @param size  the element count
     * @return      the new array
     * @exception OutOfMemoryException if allocation fails
     */
    static Object newarray(int size, Klass klass) throws InterpreterInvokedPragma {
        return GC.newArray(klass, size);
    }

    /**
     * Allocate and add a new dimension to an array.
     *
     * @param array  the array
     * @param length the element count
     * @return the same array as input
     * @exception OutOfMemoryException if allocation fails
     */
    static Object newdimension(Object[] array, int length) throws InterpreterInvokedPragma {
        return newdimensionPrim(array, length);
    }

    /**
     * Execute the equivalent of the JVMS lcmp instruction.
     *
     * @param value1 the value1 operand
     * @param value2 the value2 operand
     * @return 0, 1, or -1 accorting to the spec
     */
    static int _lcmp(long value1, long value2) throws InterpreterInvokedPragma {
        if (value1 > value2) {
            return 1;
        }
        if (value1 == value2) {
            return 0;
        }
        return -1;
    }

    /**
     * Called when an exception that has been thrown on the current thread needs
     * to be reported to an attached debugger. The actual stack unwinding and
     * execution of the exception handler is only performed once the debugger
     * continues.
     *
     * @throws the original exception.
     */
    static void reportException() throws Throwable, InterpreterInvokedPragma {
        VMThread thread = VMThread.currentThread();
        Assert.that(thread.getHitBreakpoint() != null);
        Assert.that(thread.frameOffsetAsPointer(thread.getHitBreakpoint().hitOrThrowFO).eq(getPreviousFP(getFP())));
        thread.reportException(VM.getCurrentIsolate().getDebugger());
    }

    /**
     * Called when current thread hits a breakpoint. The breakpoint is reported to
     * the event manager in the debugger. This thread is then suspended
     * until the debugger resumes it.
     *
     * @param hitFO   offset (in bytes) from top of stack of the frame reporting the breakpoint
     * @param hitBCI  the bytecode index of the instruction at which the breakpoint was set
     */
    static void reportBreakpoint(Offset hitFO, Offset hitBCI) throws InterpreterInvokedPragma {
        VMThread thread = VMThread.currentThread();
        Debugger debugger = VM.getCurrentIsolate().getDebugger();
        Assert.always(debugger != null);
        try {
            thread.reportBreakpoint(hitFO, hitBCI, debugger);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when current thread completes a step. The step is reported to
     * the thread in the debugger waiting for the event. This thread is then suspended
     * until the debugger resumes it.
     *
     * @param fo   offset (in bytes) from top of stack of the frame stepped to
     * @param bci  the bytecode index of the instruction stepped to
     */
    static void reportStepEvent(Offset fo, Offset bci) throws InterpreterInvokedPragma {
        VMThread thread = VMThread.currentThread();
        thread.reportStepEvent(fo, bci);
    }

    /**
     * Allocate and add a new dimension to an array.
     *
     * @param array   the array
     * @param length  the element count
     * @return the same array as input
     * @exception OutOfMemoryException if allocation fails
     */
    private static Object newdimensionPrim(Object[] array, int length) {
        Klass arrayClass   = GC.getKlass(array);
        Klass elementClass = arrayClass.getComponentType();
        if (length < 0) {
            throw new NegativeArraySizeException();
        }
        for (int i = 0 ; i < array.length ; i++) {
            if (array[i] == null) {
                array[i] = GC.newArray(elementClass, length);
            } else {
                newdimensionPrim((Object[])array[i], length);
            }
        }
        return array;
    }

    /*-----------------------------------------------------------------------*\
     *                       Global isolate management                       *
    \*-----------------------------------------------------------------------*/

    /**
     * Used to allocate isolate identifiers. A simple increasing postive counter
     * should suffice as it allows for more isolates than the VM could ever fit
     * in memory.
     */
    private static int nextIsolateID;

    static int allocateIsolateID() {
        if (nextIsolateID < Integer.MAX_VALUE) {
            return nextIsolateID++;
        }
        VM.println("exhausted isolate identifiers");
        fatalVMError();
        return -1;
    }

    static final class WeakIsolateListEntry extends Ref {
        private WeakIsolateListEntry next;
        WeakIsolateListEntry(Isolate isolate, WeakIsolateListEntry next) {
            super(isolate);
            this.next = next;
        }

        /**
         * Determines if list headed by this entry contains a given isolate
         */
        boolean contains(Isolate isolate) {
            if (get() == isolate) {
                return true;
            } else if (next != null) {
                return next.contains(isolate);
            } else {
                return false;
            }
        }

        /**
         * Copies the isolates from the list headed by this entry into a given SquawkSquawkVector.
         */
        void copyInto(SquawkVector set) {
            Object isolate = get();
            if (isolate != null) {
                set.addElement(isolate);
            }
            if (next != null) {
                next.copyInto(set);
            }
        }
    }

    /**
     * The weak list of isolates.
     */
    private static WeakIsolateListEntry isolates;

/*if[FLASH_MEMORY]*/
    /**
	 * Address of the 64 bit millisecond counter
	 */
    private static Address timeAddr;
/*end[FLASH_MEMORY]*/

    /**
     * Registers a newly created isolate.
     */
    static void registerIsolate(Isolate isolate) {
        if (isolates == null || !isolates.contains(isolate)) {
            isolates = new WeakIsolateListEntry(isolate, isolates);
        }
    }

    /**
     * Copies the isolates from the global isolate list into a given SquawkVector.
     */
    static void copyIsolatesInto(SquawkVector set) {
        pruneIsolateList();
        isolates.copyInto(set);
    }

    /**
     * Prunes the entries for dead isolates from the weakly linked list of isolates.
     */
    static void pruneIsolateList() {
        Assert.always(isolates != null);
//VM.println("VM::pruneIsolateList --- start --");

        WeakIsolateListEntry head = null;
        WeakIsolateListEntry last = null;
        WeakIsolateListEntry entry = isolates;
        isolates = null;

        while (entry != null) {
//VM.print("VM::pruneIsolateList - entry = ");
//VM.printAddress(entry);
            if (entry.get() != null) {
//VM.print(" entry.isolate = ");
//VM.printAddress(entry.get());
//VM.print(" [");
//VM.print(((Isolate)entry.get()).getMainClassName());
//VM.println("]");
                if (head == null) {
                    head = last = entry;
                } else {
                    last.next = entry;
                    last = entry;
                }
            } else {
//VM.println(" entry.isolate = null");
            }
            entry = entry.next;
        }

        // At least the primordial isolate must be alive
        Assert.always(last != null);

        last.next = null;
        isolates = head;
//VM.println("VM::pruneIsolateList --- start --");
    }

    /*-----------------------------------------------------------------------*\
     *                      Thread stack operations                          *
    \*-----------------------------------------------------------------------*/

    /**
     * Represents the method and bytecode index of the execution point in a
     * frame on a thread's call stack.
     */
    public final static class StackTraceElement {

        /**
         * The method body (a Klass.BYTECODE_ARRAY instance).
         */
        private final Object mp;

        /**
         * The bytecode index.
         */
        private final int bci;

        StackTraceElement(Object mp, int bci) {
            this.mp = mp;
            this.bci = bci;
        }

        /**
         * {@inheritDoc}
         */
        public boolean equals(Object o) {
            if (o instanceof StackTraceElement) {
                StackTraceElement ste = (StackTraceElement)o;
                return mp == ste.mp && bci == ste.bci;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public int hashCode() {
            return bci;
        }

        /**
         * @return the class containing the execution point represented by this stack trace element
         */
        public Klass getKlass() {
            return VM.asKlass(NativeUnsafe.getObject(mp, HDR.methodDefiningClass));
        }

        /**
         * @return  the method containing the execution point represented by this
         *          stack trace element , or null if this information is unavailable
         */
        public Method getMethod() {
            return getKlass().findMethod(mp);
        }

        private void printKnownMethod(PrintStream out, Klass klass, String methodName, int[] lnt) {
            out.print("at ");
            out.print(klass.getName());
            out.print('.');
            out.print(methodName);
            out.print('(');
            
            String src = klass.getSourceFileName();
            if (src != null) {
                out.print(src);
                out.print(':');
            }
            
            if (lnt != null) {
                int lno = Method.getLineNumber(lnt, bci);
                out.print(lno);
            } else {
                out.print("bci=");
                out.print(bci);
            }
            out.print(')');
        }
        
        private void printUnknownMethod(PrintStream out, Klass klass, String methodKind, int index) {
            out.print("in ");
            out.print(methodKind);
            out.print(" method #");
            out.print(index);
            out.print(" of ");
            out.print(klass.getName());
            out.print("(bci=");
            out.print(bci);
            out.print(')');
        }
        
        /**
         *  Can we guess the method name?
         */
        private String calcStaticMethodName(Klass klass, int index) {
            if (index == klass.getClinitIndex()) {
                return "<clinit>";
            } else if (index == klass.getDefaultConstructorIndex()) {
                return "<init>";
            } else if (index == klass.getMainIndex()) {
                return"main";
            }
            return null;
        }
        
        /**
         *  Can we guess the method name? Scheme is to look for metadata for original
         *  method in superclass (or interface if we can figure it out).
         */
        private String calcVirtualMethodName(Klass klass, int index, Object[] temp) {
            if (klass != null) {
                Method method = klass.lookupVirtualMethod(index);
                if (method != null) {
                    Assert.that(method.getOffset() == index);
                    return method.getName();
                } else {
                    int islot = klass.findISlot(index, temp);
                    if (islot >= 0) {
                        Klass iKlass = (Klass)temp[0];
                        method = iKlass.getMethod(islot, false);
                        if (method != null) {
                            return method.getName();
                        } else {
                            return null; // found the interface definition, but no name. give up!
                        }
                    }
                }
                return calcVirtualMethodName(klass.getSuperclass(), index, temp);
            }
            return null;
        }
        
        private void printToVM(Klass klass, String methodName, int[] lnt) {
            VM.print("at ");
            VM.print(klass.getInternalName());
            VM.print('.');
            VM.print(methodName);
            VM.print('(');
            String src = klass.getSourceFileName();
            if (src != null) {
                VM.print(src);
                VM.print(':');
            }
            
            if (lnt != null) {
                int lno = Method.getLineNumber(lnt, bci);
                VM.print(lno);
            } else {
                VM.print("bci=");
                VM.print(bci);
            }
            VM.println(')');
        }
        
        private void printToVM(Klass klass, String methodKind, int index) {
            VM.print("in ");
            VM.print(methodKind);
            VM.print(" method #");
            VM.print(index);
            VM.print(" of ");
            VM.print(klass.getInternalName());
            VM.print("(bci=");
            VM.print(bci);
            VM.println(')');
        }
        
        /**
         * {@inheritDoc}
         */
        void printToVM() {
            // make this code fail-safe, since it's used for printing stack traces, and could cascade errors.
            try {
                Klass klass = getKlass();
                Method method = klass.findMethod(mp);
                if (method == null) {
                    int index = klass.getMethodIndex(mp, true); // check static methods first
                    if (index >= 0) {
                        String methodName = calcStaticMethodName(klass, index);
                        if (methodName != null) {
                            printToVM(klass, methodName, null);
                        } else {
                            printToVM(klass, "static", index);
                        }
                    } else {
                        index = klass.getMethodIndex(mp, false);
                        if (index >= 0) {
                            // Virtual Method. Note that 0-9 are from java.lang.Object.
                            printToVM(klass, "virtual", index);
                        }
                    }
                } else {
                    printToVM(klass, method.getName(), method.getLineNumberTable());
                }
            } catch (Throwable e) {
                VM.println("Exception thrown in StackTraceElement.printToVM()");
            }
        }
        
        public void print(PrintStream out) {
             // make this code fail-safe, since it's used for printing stack traces, and could cascade errors.
            try {
                Klass klass = getKlass();
                Method method = klass.findMethod(mp);
                if (method == null) {
                    int index = klass.getMethodIndex(mp, true); // check static methods first
                    if (index >= 0) {
                       String methodName = calcStaticMethodName(klass, index);
                        if (methodName != null) {
                            printKnownMethod(out, klass, methodName, null);
                        } else {
                            printUnknownMethod(out, klass, "static", index);
                        }
                    } else {
                        index = klass.getMethodIndex(mp, false);
                        if (index >= 0) {
                            // Virtual Method. Note that 0-9 are from java.lang.Object.
                            String methodName = calcVirtualMethodName(klass, index, new Object[1]);
                            if (methodName != null) {
                                printKnownMethod(out, klass, methodName, null);
                            } else {
                                printUnknownMethod(out, klass, "virtual", index);
                            }
                        }
                    }
                } else {
                    printKnownMethod(out, klass, method.getName(), method.getLineNumberTable());
                }
            } catch (Throwable e) {
                if (VM.isVerbose()) {
                    printVMStackTrace(e, "***", "Exception thrown in StackTraceElement.toString:");
                }
                VM.print("*** Error decoding this StackTraceElement:\n    ");
                printToVM(); // print it the simple way
                out.print("------");
            }
        }
        
    } /* StackTraceElement */
    
    /**
     * Returns an array of stack trace elements, each representing one stack frame in the current call stack.
     * The zeroth element of the array represents the top of the stack, which is the frame of the caller's
     * method. The last element of the array represents the bottom of the stack, which is the first method
     * invocation in the sequence.
     *
     * @param count  how many frames from the stack to reify, starting from the frame
     *               of the method that called this one. A negative value specifies that
     *               all frames are to be reified.
     * @return the reified call stack
     */
    public static StackTraceElement[] reifyCurrentStack(int count) {
        Address ip;
        if (!VMThread.currentThread().isAlive()) {
            return new StackTraceElement[0];
        }
        
        /*
         * Count the number of frames and allocate the array.
         */
        Object stack = VMThread.currentThread().getStack();
        int frames = 0;
        Address fp = VM.getFP();

        // Skip frame for this method
        fp = VM.getPreviousFP(fp);

        while (!fp.isZero()) {
            frames++;
            fp = VM.getPreviousFP(fp);
        }

        // Skip unrequested frames
        if (count >= 0 && count < frames) {
            frames = count;
        }

        if (frames <= 0) {
            return new StackTraceElement[0];
        }

        StackTraceElement[] trace = new StackTraceElement[frames];

        fp = VM.getFP();
        for (int i = 0; i != frames; ++i) {
            ip = VM.getPreviousIP(fp);
            fp = VM.getPreviousFP(fp);
            Assert.always(!fp.isZero());
            Object mp = VM.getMP(fp);
            int bci = ip.diff(Address.fromObject(mp)).toInt();

            // The allocation of the StackTraceElement object may cause
            // a collection and so the fp need's to be saved as a
            // stack offset and restored after the allocation
            Offset fpOffset = fp.diff(Address.fromObject(stack));
            trace[i] = new StackTraceElement(mp, bci);
            fp = Address.fromObject(stack).addOffset(fpOffset);
        }
        return trace;
    }
    
    /**
     * Throws an exception. This routine will search for a handler of the
     * exception being thrown, reset the return ip and fp of the activation record that
     * it was called with and then 'return' to the handler in question.
     *
     * @param exception the exception to throw
     */
    static void throwException(Throwable exception) {
        Object throwingStack = VMThread.getOtherThreadStack();
        VMThread throwingThread = VMThread.getOtherThread();

        /*
         * Check that exceptions are enabled and then disable them.
         */
        if (exceptionsEnabled) {
            exceptionsEnabled = false;
        } else {
            Assert.shouldNotReachHere("do_throw called recursively");
        }

        throwCount++;
        /*
         * Check that no memory allocation is done in this routine because
         * this function must be able to function in out-of-memory conditions.
         */
        boolean oldState = GC.setAllocationEnabled(false);

        /*
         * Get the class of the exception being thrown.
         */
        Klass exceptionKlass = GC.getKlass(exception);

        HitBreakpoint hbp = throwingThread.getHitBreakpoint();
        if (hbp != null && hbp.getState() == HitBreakpoint.EXC_REPORTED) {
            /*
             *The debugger has now reported the deferredException and re-thrown the exception.
             * So jump straight to handler.
             */
/*if[DEBUG_CODE_ENABLED]*/
VM.println("=== In throwException, HitBreakpoint.EXC_REPORTED:");
hbp.dumpState();
/*end[DEBUG_CODE_ENABLED]*/

            Assert.that(exception == hbp.getException());
            Assert.that(hbp.getCatchMethod() != null);

            NativeUnsafe.setAddress(throwingStack, SC.lastFP, throwingThread.frameOffsetAsPointer(hbp.catchFO));
            NativeUnsafe.setUWord(throwingStack, SC.lastBCI, hbp.catchBCI.toUWord());
            ServiceOperation.pendingException = exception;

            throwingThread.clearBreakpoint();

            exceptionsEnabled = true;
            GC.setAllocationEnabled(oldState);
            return;
        }

        /*
         * Get the fp, ip, mp, and relative ip of the frame before the
         * one that is currently executing.
         */
        Address fp   = NativeUnsafe.getAddress(throwingStack, SC.lastFP);
        UWord bci    = NativeUnsafe.getUWord(throwingStack, SC.lastBCI);
        Object mp    = getMP(fp);
        Klass klass = (Klass)NativeUnsafe.getObject(mp, HDR.methodDefiningClass);

        Object throwMP = mp;
        Offset throwFO = throwingThread.framePointerAsOffset(fp);
        Offset throwBCI = NativeUnsafe.getUWord(throwingStack, SC.lastBCI).toOffset();

        // Rewind BCI by 1 to be within the instruction that caused the exception
        throwBCI = throwBCI.sub(1);

        /*
         * Loop looking for an exception handler. (The VM must put a catch-all
         * handler at the base of all user thread activations.)
         */
        while(true) {

            /*
             * Setup the preallocated VMBufferDecoder to decode the header
             * of the method for the frame being tested.
             */
            int size   = MethodBody.decodeExceptionTableSize(mp);
            int offset = MethodBody.decodeExceptionTableOffset(mp);
            vmbufferDecoder.reset(mp, offset);
            int end = offset + size;

            UWord start_bci;
            UWord end_bci;
            UWord handler_bci;

            /*
             * Iterate through the handlers for this method.
             */
            while (vmbufferDecoder.getOffset() < end) {
                start_bci     = UWord.fromPrimitive(vmbufferDecoder.readUnsignedInt());
                end_bci       = UWord.fromPrimitive(vmbufferDecoder.readUnsignedInt());
                handler_bci   = UWord.fromPrimitive(vmbufferDecoder.readUnsignedInt());
                int handler  = vmbufferDecoder.readUnsignedShort();

                /*
                 * If the ip and exception matches then setup the activation
                 * for this routine so that the return will go back to the
                 * handler code.
                 *
                 * Note that the relip address is now past the instruction that
                 * caused the call. Therefore the match is > start_ip && <= end_ip
                 * rather than >= start_ip && < end_ip.
                 */
                if (bci.hi(start_bci) && bci.loeq(end_bci)) {
                    Klass handlerKlass = (Klass)klass.getObject(handler);
                    if (exceptionKlass == handlerKlass || handlerKlass == Klass.THROWABLE || handlerKlass.isAssignableFrom(exceptionKlass)) {
                        GC.setAllocationEnabled(oldState);
                        exceptionsEnabled = true;

                        /*
                         * Report exception to debugger. Both the code on the application side and the debugger side
                         * must be careful not to wedge the system if an exception occurs somewhere in the debugger's
                         * exception reporting code. In some cases cases we must silently squash the exception.
                         *
                         * Also, don't bother reporting exceptions in cases where the VM has some internal error.
                         */
                        if (VM.isThreadingInitialized() &&
                            exception != outOfMemoryError &&
                            hbp == null &&  // prevents recursion for errors in debugger code
                            VM.getCurrentIsolate().getDebugger() != null)
                        {
                            throwingThread.recordExceptionToReport(throwFO, throwBCI, exception, throwingThread.framePointerAsOffset(fp), handler_bci.toOffset());

                            Assert.that(ServiceOperation.pendingException == exception);
                            ServiceOperation.pendingException = null;
                            Assert.that(throwMP == getMP(NativeUnsafe.getAddress(throwingStack, SC.lastFP)));

                            /*
                             * threadswitchmain() in the interpreter will notice Thread.debug == EXC_HIT, and call
                             * VM.reportException() (on the throwing thread) to report the exception to
                             * the debugger isolate.
                             */
                            return;
                        }

                        NativeUnsafe.setAddress(throwingStack, SC.lastFP, fp);
                        NativeUnsafe.setUWord(throwingStack, SC.lastBCI, handler_bci);
                        return;
                    }
                }
            }

            /*
             * Backup to the previous frame and loop.
             */
            Address ip = getPreviousIP(fp);
            fp         = getPreviousFP(fp);
            Assert.that(!fp.isZero());
            mp         = getMP(fp);
            klass      = (Klass)NativeUnsafe.getObject(mp, HDR.methodDefiningClass);
            bci        = ip.diff(Address.fromObject(mp)).toUWord();
        }
    }

    /*-----------------------------------------------------------------------*\
     *                      Floating point operations                        *
    \*-----------------------------------------------------------------------*/

    /**
     * Performs a math operation.
     *
     * @param code  the opcode
     * @param a     the first operand
     * @param b     the second operand
     * @return the result
     */
    public native static double math(int code, double a, double b);

    /**
     * Converts a float into bits.
     *
     * @param value the input
     * @return the result
     */
    public native static int floatToIntBits(float value);

    /**
     * Converts a double into bits.
     *
     * @param value the input
     * @return the result
     */
    public native static long doubleToLongBits(double value);

    /**
     * Converts bits into a float.
     *
     * @param value the input
     * @return the result
     */
    public native static float intBitsToFloat(int value);

    /**
     * Converts bits into a double.
     *
     * @param value the input
     * @return the result
     */
    public native static double longBitsToDouble(long value);
    

    /*=======================================================================*\
     *                           Romizer support                             *
    \*=======================================================================*/

    /**
     * Determines if code running at the moment is running inside of a Squawk VM
     * or a JSE VM.
     *
     * @return true if running in a hosted environment, ie in a JSE VM
     */
    public static boolean isHosted() {
        return false;
    }

    /**
     * Get the endianess.
     *
     * @return true if the system is big endian
     */
    public static native boolean isBigEndian();
    
    /**
     * On a hosted system , this calls System.setProperty(), otherwise calls Isolate.currentIsolate().setProperty()
     */
    public static void setProperty(String name, String value) {
        Isolate.currentIsolate().setProperty(name, value);
    }


    /*=======================================================================*\
     *                              Native methods                           *
    \*=======================================================================*/

    /*-----------------------------------------------------------------------*\
     *                           Raw memory interface                        *
    \*-----------------------------------------------------------------------*/

    /**
     * Get the current frame pointer.
     *
     * @return the frame pointer
     */
    native static Address getFP();

    /**
     * Gets the method pointer from a frame pointer.
     *
     * @param fp the frame pointer
     * @return the method pointer
     */
    native static Object getMP(Address fp);

    /**
     * Gets the pointer to the frame of the caller of a given current frame.
     *
     * @param fp   a frame pointer
     * @return the pointer to the frame that is the calling context for <code>fp</code>
     *
     * @vm2c code( return getObject(_fp, FP_returnFP); )
     */
    native static Address getPreviousFP(Address fp);

    /**
     * Gets the previous instruction pointer from a frame pointer.
     *
     * @param fp the frame pointer
     * @return the previous instruction pointer
     *
     * @vm2c code( return getObject(_fp, FP_returnIP); )
     */
    native static Address getPreviousIP(Address fp);

    /**
     * Set the previous frame pointer.
     *
     * @param fp the frame pointer
     * @param pfp the previous frame pointer
     *
     * @vm2c code( setObject(_fp, FP_returnFP, pfp); )
     */
    native static void setPreviousFP(Address fp, Address pfp);

    /**
     * Set the previous instruction pointer.
     *
     * @param fp the frame pointer
     * @param pip the previous instruction pointer
     *
     * @vm2c code( setObject(_fp, FP_returnIP, pip); )
     */
    native static void setPreviousIP(Address fp, Address pip);

    /*-----------------------------------------------------------------------*\
     *                          Oop/int convertion                           *
    \*-----------------------------------------------------------------------*/

    /**
     * Casts an object to class Klass without using <i>checkcast</i>.
     *
     * @param object the object to be cast
     * @return the object cast to be a Klass
     *
     * @vm2c code( return object; )
     */
    native static Klass asKlass(Object object);

    /**
     * Get the hash code for an object in ROM
     *
     * @param   anObject the object
     * @return  the hash code
     */
    native static int hashcode(Object anObject);

    /**
     * Allocate a segment of virtual memory to be used as a stack.
     * If sucessfull the memory returnd is such that the all but
     * the second page is accessable. The MMU is setup to disable access
     * to the second page in order to provide a guard for accidential
     * stack overflow.
     *
     * @param   size the size of the memory
     * @return  the allocated memory or zero if none was available
     */
    native static Address allocateVirtualStack(int size);

    /**
     * Add to the VM's class state cache
     *
     * @param   klass the class
     * @param   state the class state
     */
    native static void addToClassStateCache(Klass klass, Object state);

    /**
     * Invalidate the class cache.
     *
     * @return true if it was already invalid.
     */
    native static boolean invalidateClassStateCache();

    /**
     * Removes the youngest object that is pending a monitor enter operation.
     *
     * @return the next object
     */
    native static Object removeVirtualMonitorObject();

    /**
     * Tests to see if an object has a virtual monitor object.
     *
     * @param object the object
     * @return true if is does
     */
    native static boolean hasVirtualMonitorObject(Object object);

    /**
     * Execute the equivalent of the lcmp instruction on page 312 of the JVMS.
     *
     * @param value1 the value1 operand
     * @param value2 the value2 operand
     * @return 0, 1, or -1 accorting to the spec
     */
    //native static int lcmp(long value1, long value2);

    /*-----------------------------------------------------------------------*\
     *                       Access to global memory                         *
    \*-----------------------------------------------------------------------*/

    /*
     * The Squawk VM supports four types of variables. These are local
     * variables, instance variables, static varibles, and global variables.
     * Static variables are those defined in a class using the static keyword
     * these are allocated dynamically by the VM when their classes are
     * initialized, and these variables are created on a per-isolate basis.
     * Global variables are allocated by the romizer and used in place of
     * the static variables in a hard-wired set of system classes. This is
     * done in cases where certain components of the system must have static
     * state before the normal system that support things like static variables
     * are running. Global variables are shared between all isolates.
     * <p>
     * The classes com.sun.squawk.VM and com.sun.squawk.GC are included in the hard-wired
     * list and the translator will resplace the normal getstatic and putstatic
     * bytecodes will invokenative instructions that one of the following.
     * Currently only 32/64 bit references and 32 bit integers are supported.
     * <p>
     * Because the transformation is done automatically there is little evidence
     * of the following routines being used in the system code. One exception to
     * this is the garbage collector which will need to treat all the reference
     * types as roots.
     */

    /**
     * Gets the number of global integer variables.
     *
     * @return  the number of global integer variables
     *
     * @vm2c code( return GLOBAL_INT_COUNT; )
     */
    native static int getGlobalIntCount();

    /**
     * Gets the value of an global integer variable.
     *
     * @param  index   index of the entry in the global integer table
     * @return the value of entry <code>index</code> in the global integer table
     *
     * @vm2c code( return Ints[index]; )
     */
    native static int getGlobalInt(int index);

    /**
     * Sets the value of an global integer variable.
     *
     * @param  value   the value to set
     * @param  index   index of the entry to update in the global integer table
     *
     * @vm2c code( Ints[index] = value; )
     */
    native static void setGlobalInt(int value, int index);

    /**
     * Gets the number of global pointer variables.
     *
     * @return  the number of global pointer variables
     *
     * @vm2c code( return GLOBAL_ADDR_COUNT; )
     */
    native static int getGlobalAddrCount();

    /**
     * Gets the value of an global pointer variable.
     *
     * @param  index   index of the entry in the global pointer table
     * @return the value of entry <code>index</code> in the global pointer table
     *
     * @vm2c code( return Addrs[index]; )
     */
    native static Address getGlobalAddr(int index);

    /**
     * Sets the value of an global pointer variable.
     *
     * @param  value   the value to set
     * @param  index   index of the entry to update in the global pointer table
     *
     * @vm2c code( Addrs[index] = value; )
     */
    native static void setGlobalAddr(Address value, int index);

    /**
     * Gets the number of global object pointer variables.
     *
     * @return  the number of global object pointer variables
     *
     * @vm2c code( return GLOBAL_OOP_COUNT; )
     */
    native static int getGlobalOopCount();

    /**
     * Gets the value of an global object pointer variable.
     *
     * @param  index   index of the entry in the global object pointer table
     * @return the value of entry <code>index</code> in the global object pointer table
     *
     * @vm2c code( return Oops[index]; )
     */
    native static Object getGlobalOop(int index);

    /**
     * Sets the value of an global object pointer variable.
     *
     * @param  value   the value to set
     * @param  index   index of the entry to update in the global object pointer table
     *
     * @vm2c code( Oops[index] = value; )
     */
    native static void setGlobalOop(Object value, int index);

    /**
     * Gets the address of the global object pointer table.
     *
     * @return  the address of the global object pointer table
     *
     * @vm2c code( return Oops; )
     */
    native static Address getGlobalOopTable();


    /*-----------------------------------------------------------------------*\
     *                        Low level VM logging                           *
    \*-----------------------------------------------------------------------*/

    /**
     * The identifier denoting the standard output stream.
     */
    public static final int STREAM_STDOUT = 0;

    /**
     * The identifier denoting the standard error output stream.
     */
    public static final int STREAM_STDERR = 1;

    /**
     * The identifier denoting the stream used to capture the symbolic information
     * relating to methods in dynamically loaded classes.
     */
    static final int STREAM_SYMBOLS = 2;
    
    /**
     * The maximum priority that a system thread can have.
     */
    public static final int MAX_SYS_PRIORITY = VMThread.MAX_SYS_PRIORITY;

    /**
     * Sets the stream for the VM.print... methods to one of the STREAM_... constants.
     *
     * @param stream  the stream to use for the print... methods
     * @return the current stream used for VM printing
     */
    public static int setStream(int stream) {
        Assert.always(stream >= STREAM_STDOUT && stream <= STREAM_SYMBOLS, "invalid stream specifier");
        return setStream0(stream);
    }

    /**
     * Sets the stream for the VM.print... methods to one of the STREAM_... constants.
     *
     * @param stream  the stream to use for the print... methods
     * @return the current stream used for VM printing
     *
     * @vm2c proxy( setStream )
     */
    private static int setStream0(int stream) {
        return execSyncIO(ChannelConstants.INTERNAL_SETSTREAM, stream);
    }

    /**
     * Prints a character to the VM stream.
     *
     * @param ch      the character to print
     * @vm2c code( fprintf(streams[currentStream], "%c", ch);
     *             fflush(streams[currentStream]); )
     */
    static void printChar(char ch) {
        execSyncIO(ChannelConstants.INTERNAL_PRINTCHAR, ch);
    }

    /**
     * Prints an integer to the VM stream.
     *
     * @param val     the integer to print
     *
     * @vm2c code( fprintf(streams[currentStream], "%i", val);
     *             fflush(streams[currentStream]); )
     */
    static void printInt(int val) {
        execSyncIO(ChannelConstants.INTERNAL_PRINTINT, val);
    }

    /**
     * Prints a long to the VM stream.
     *
     * @param val     the long to print
     *
     * @vm2c code( fprintf(streams[currentStream], format("%L"), val);
     *             fflush(streams[currentStream]); )
     */
    static void printLong(long val) {
        int i1 = (int)(val >>> 32);
        int i2 = (int)val;
        execSyncIO(ChannelConstants.INTERNAL_PRINTLONG, i1, i2);
    }

    /**
     * Prints an unsigned word to the VM stream. This will be formatted as an unsigned 32 bit or 64 bit
     * value depending on the underlying platform.
     *
     * @param val     the word to print
     *
     * @vm2c code( fprintf(streams[currentStream], format("%A"), val);
     *             fflush(streams[currentStream]); )
     */
    public static void printUWord(UWord val) {
        int i1 = (int)(val.toPrimitive() >> 32);
        int i2 = (int)val.toPrimitive();
        execSyncIO(ChannelConstants.INTERNAL_PRINTUWORD, i1, i2);
    }

    /**
     * Prints an offset to the VM stream. This will be formatted as a signed 32 bit or 64 bit
     * value depending on the underlying platform.
     *
     * @param val     the offset to print
     *
     * @vm2c code( fprintf(streams[currentStream], format("%O"), val);
     *             fflush(streams[currentStream]); )
     */
    public static void printOffset(Offset val) {
        int i1 = (int)(val.toPrimitive() >> 32);
        int i2 = (int)val.toPrimitive();
        execSyncIO(ChannelConstants.INTERNAL_PRINTOFFSET, i1, i2);
    }

    /**
     * Prints a string to the VM stream.
     *
     * @param str     the string to print
     *
     * @vm2c code( printJavaString(str, streams[currentStream], null, 0);
     *             fflush(streams[currentStream]); )
     */
    static void printString(String str) {
        executeCIO(-1, ChannelConstants.INTERNAL_PRINTSTRING, -1, 0, 0, 0, 0, 0, 0, str, null);
    }

/*if[FLOATS]*/
    /**
     * Prints a float to the VM stream.
     *
     * @param val     the float to print
     */
    static void printFloat(float val) {
        execSyncIO(ChannelConstants.INTERNAL_PRINTFLOAT, (int)val);
    }

    /**
     * Prints a double to the VM stream.
     *
     * @param val     the double to print
     */
    static void printDouble(double dval) {
        long val = (long)dval;
        int i1 = (int)(val >>> 32);
        int i2 = (int)val;
        execSyncIO(ChannelConstants.INTERNAL_PRINTDOUBLE, i1, i2);
    }
/*end[FLOATS]*/

    /**
     * Prints an address to the VM stream. This will be formatted as an unsigned 32 bit or 64 bit
     * value depending on the underlying platform.
     *
     * @param val     the address to print
     *
     * @vm2c code( fprintf(streams[currentStream], format("%A"), val);
     *             fflush(streams[currentStream]); )
     */
    public static void printAddress(Object val) {
        executeCIO(-1, ChannelConstants.INTERNAL_PRINTADDRESS, -1, 0, 0, 0, 0, 0, 0, val, null);
    }

    /**
     * Prints the name of a global oop to the VM stream.
     *
     * @param index   the index of the variable to print
     *
     * @vm2c code( fprintf(streams[currentStream], "Global oop:%d", index);
     *             fflush(streams[currentStream]); )
     */
    static void printGlobalOopName(int index) {
        execSyncIO(ChannelConstants.INTERNAL_PRINTGLOBALOOPNAME, index);
    }

    /**
     * Prints the name and current value of every global to the VM stream.
     */
    static void printGlobals() {
        execSyncIO(ChannelConstants.INTERNAL_PRINTGLOBALS, 0);
    }

    /**
     * Prints a line detailing the build-time configuration of the VM.
     */
    static void printConfiguration() {
        execSyncIO(ChannelConstants.INTERNAL_PRINTCONFIGURATION, 0);
    }

    /**
     * Prints the string representation of an object to the VM stream.
     *
     * @param obj   the object whose toString() result is to be printed
     */
    public static void printObject(Object obj) {
        print(String.valueOf(obj));
    }

    /**
     * Prints a character to the VM output stream.
     *
     * @param x the value
     */
    public static void print(char x) {
        printChar(x);
    }

    /**
     * Prints a string to the VM output stream.
     *
     * @param x the string
     */
    public static void print(String x) {
        printString(x);
    }

    /**
     * Prints an integer to the VM output stream.
     *
     * @param x the value
     */
    public static void print(int x) {
        printInt(x);
    }

    /**
     * Prints a long to the VM output stream.
     *
     * @param x the value
     */
    public static void print(long x) {
        printLong(x);
    }

/*if[FLOATS]*/
    /**
     * Prints a float to the VM output stream.
     *
     * @param x the value
     */
    public static void print(float x) {
        printFloat(x);
    }

    /**
     * Prints a double to the VM output stream.
     *
     * @param x the value
     */
    public static void print(double x) {
        printDouble(x);
    }
/*end[FLOATS]*/

    /**
     * Prints a boolean to the VM output stream.
     *
     * @param b the value
     */
    public static void print(boolean b) throws NotInlinedPragma {
        print(b ? "true" : "false");
    }

    /**
     * Prints a character followed by a new line to the VM output stream.
     *
     * @param x the value
     */
    public static void println(char x) {
        printChar(x);
        println();
    }

    /**
     * Prints a string followed by a new line to the VM output stream.
     *
     * @param x the string
     */
    public static void println(String x) throws NotInlinedPragma {
        printString(x);
        println();
    }

    /**
     * Prints an integer followed by a new line to the VM output stream.
     *
     * @param x the value
     */
    public static void println(int x) throws NotInlinedPragma {
        printInt(x);
        println();
    }

    /**
     * Prints a boolean followed by a new line to the VM output stream.
     *
     * @param x the value
     */
    public static void println(boolean x) throws NotInlinedPragma {
        print(x);
        println();
    }

    /**
     * Prints a long followed by a new line to the VM output stream.
     *
     * @param x the value
     */
    public static void println(long x) throws NotInlinedPragma {
        printLong(x);
        println();
    }

/*if[FLOATS]*/
    /**
     * Prints a float followed by a new line to the VM output stream.
     *
     * @param x the value
     */
    public static void println(float x) throws NotInlinedPragma {
        printFloat(x);
        println();
    }

    /**
     * Prints a double followed by a new line to the VM output stream.
     *
     * @param x the value
     */
    public static void println(double x) throws NotInlinedPragma {
        printDouble(x);
        println();
    }
/*end[FLOATS]*/

    /**
     * Prints a new line to the VM output stream.
     */
    public static void println() throws NotInlinedPragma {
        print("\n");
    }

    /*-----------------------------------------------------------------------*\
     *               Native functions for interrupts                         *
    \*-----------------------------------------------------------------------*/

    /**
     * @see  JavaDriverManager#setupInterrupt
     */
    native static void setupInterrupt(int interrupt, String handler);

    /*
     * These functions below should really be conditionally included only if
     * the build property KERNEL_SQUAWK_HOSTED is true. However, that
     * would mean that Native.java would have to regenerated every time
     * the property was changed. For now, we'll just let them use up a
     * little of the native function identifier space and remove them
     * once the examples have served their purpose.
     */

    native static void sendInterrupt(int signum);
    native static void setupAlarmInterval(int start, int period);
    native static long getInterruptStatus(int interrupt, int id);

    /*-----------------------------------------------------------------------*\
     *                        Miscellaneous functions                        *
    \*-----------------------------------------------------------------------*/

    public static native void finalize(Object o);

    /**
     * Pauses the interpreter in kernel mode. Calling this is turned into a OPC_PAUSE
     * bytecode by the translator.
     */
    native static void pause();

    /**
     * Determines if the VM in currently in the kernel context.
     *
     * @return true if the VM in currently in the kernel context
     */
    native static boolean isInKernel();

    /**
     * A call to this method is inserted by the translator when a call to an undefined
     * native method if found.
     */
    static void undefinedNativeMethod() {
        throw new Error("Undefined native method");
    }

    /**
     * Gets the address of the start of the object memory in ROM.
     *
     * @return the address of the start of the object memory in ROM
     */
    public static Address getRomStart() {
        return romStart;
    }

    /**
     * Gets the address of the first byte after the end of the object memory in ROM.
     *
     * @return the address of the first byte after the end of the object memory in ROM
     */
    static Address getRomEnd() {
        return romEnd;
    }

    /**
     * Determines if a given object is within ROM.
     *
     * @param object   the object to test
     * @return true if <code>object</code> is within ROM
     */
    static boolean inRom(Address object) {
        /*
         * Need to account for the object's header on the low
         * end and zero sized objects on the high end
         */
        return object.hi(romStart) && object.loeq(romEnd);
    }

    /**
     * Gets the offset (in bytes) of an object in ROM from the start of ROM
     *
     * @param object  the object to test
     * @return the offset (in bytes) of <code>object</code> in ROM from the start of ROM
     */
    static Offset getOffsetInRom(Address object) {
        Assert.that(inRom(object));
        return object.diff(romStart);
    }

    /**
     * Gets an object in ROM given a offset (in bytes) to the object from the start of ROM.
     *
     * @param offset   the offset (in bytes) of the object to retrieve
     * @return the object at <code>offset</code> bytes from the start of ROM
     */
    static Address getObjectInRom(Offset offset) {
        return romStart.addOffset(offset);
    }

    /**
     * Gets the address at which the object memory containing the bootstrap suite starts.
     *
     * @return  the bootstrap object memory start address
     */
    public static Address getBootstrapStart() {
        return bootstrapStart;
    }

    /**
     * Gets the address at which the object memory containing the bootstrap suite ends.
     *
     * @return  the bootstrap object memory end address
     */
    public static Address getBootstrapEnd() {
        return bootstrapEnd;
    }

    /**
     * Gets the hash of the object memory containing the bootstrap suite in it's canonical (i.e. relative to
     * address 0) form.
     *
     * @return the hash of the bootstrap object memory
     */
    public static int getBootstrapHash() {
        return bootstrapHash;
    }

    /**
     * The system-dependent path-separator character. This character is used to
     * separate filenames in a sequence of files given as a <em>path list</em>.
     * On UNIX systems, this character is <code>':'</code>; on Windows
     * systems it is <code>';'</code>.
     *
     * @return  the system-dependent path-separator character
     */
    public static char getPathSeparatorChar() {
        return (char)execSyncIO(ChannelConstants.INTERNAL_GETPATHSEPARATORCHAR, 0);
    }

    /**
     * The system-dependent default name-separator character.  This field is
     * initialized to contain the first character of the value of the system
     * property <code>file.separator</code>.  On UNIX systems the value of this
     * field is <code>'/'</code>; on Microsoft Windows systems it is <code>'\'</code>.
     *
     * @see     java.lang.System#getProperty(java.lang.String)
     */
    public static char getFileSeparatorChar() {
        return (char)execSyncIO(ChannelConstants.INTERNAL_GETFILESEPARATORCHAR, 0);
    }

    /**
     * Halts the VM because of a fatal condition.
     *
     * @vm2c code( fatalVMError(""); )
     */
    public native static void fatalVMError();
    
    /**
     * Switches from executing the Thread.currentThread to Thread.otherThread. This operation
     * will cause these two variables to be swapped, and the execution to continue after
     * the threadSwitch() of the next thread. This function also sets up VM.currentIsolate
     * when Thread.otherThread is not the service thread.
     * <p>
     * If Thread.otherThread is a new thread the method VMExtension.callrun() to be entered.
     * <p>
     * <b>**THE CALLER OF THIS METHOD MUST THROW THE NotInlinedPragma EXCEPTION**</b>
     */
    native static void threadSwitch();

    /**
     * Collects the garbage.
     * <p>
     * <b>**THE CALLER OF THIS METHOD MUST THROW THE NotInlinedPragma EXCEPTION**</b>
     *
     * @param forceFullGC  forces a collection of the whole heap
     */
    private native static void executeGC(boolean forceFullGC);

    /**
     * Copies an object graph for serialization.
     *
     * @param object    the root of the object graph to copy
     * @param cb        the ObjectMemorySerializer.ControlBlock
     */
    private native static void executeCOG(Object object, ObjectMemorySerializer.ControlBlock cb);

    /**
     * Gets the number of backward branch instructions the VM has executed.
     *
     * @return the number of backward branch instructions the VM has executed or -1 if instruction
     *         profiling is disabled
     *
     * @vm2c code( return getBranchCount(); )
     */
    public native static long getBranchCount();

    /**
     * Enables a dynamically loaded class to call this.
     *
     * @return the number of instructions the VM has executed or -1 if instruction
     *         profiling is disabled
     */
    public static long branchCount() {
        return getBranchCount();
    }

    /**
     * Start the VM tracing if tracing support is enabled.
     */
    public static void startTracing() {
        setGlobalInt(1, Global.getOffset(Global.tracing));
    }

    /**
     * Gets the flag indicating if the VM is running in verbose mode.
     *
     * @return true if the VM is running in verbose mode
     */
    public static boolean isVerbose() {
        return verboseLevel > 0;
    }

    /**
     * Gets the flag indicating if the VM is running in very verbose mode.
     *
     * @return true if the VM is running in very verbose mode
     */
    public static boolean isVeryVerbose() {
        return verboseLevel > 1;
    }

    /**
     * Sets the flag indicating if the VM is running in verbose mode.
     *
     * @param level  indicates if the VM should run in verbose mode
     */
    static void setVerboseLevel(int level) {
        verboseLevel = level;
    }

    /**
     * Create a Channel I/O context.
     *
     * @param hibernatedContext the handle for a hibernated I/O session
     * @return the channel I/O context
     */
    static int createChannelContext(byte[] hibernatedContext) {
        return execSyncIO(ChannelConstants.GLOBAL_CREATECONTEXT, 0, 0, 0, 0, 0, 0, hibernatedContext, null);
    }

    /**
     * Delete a channel I/O context.
     *
     * @param context the channel I/O context
     */
    static void deleteChannelContext(int context) {
        execSyncIO(context, ChannelConstants.CONTEXT_DELETE, 0, 0);
    }

    /**
     * Hibernate a channel context.
     *
     * @param context the channel I/O handle
     * @return        the serialized IO sub-system
     * @throws IOException if something went wrong when serializing the IO sub-system
     */
    static byte[] hibernateChannelContext(int context) throws IOException {

        // Get buffer size
        int bufferSize = execSyncIO(context, ChannelConstants.CONTEXT_HIBERNATE, 0, 0);

        // Check that serialization succeeded
        if (bufferSize < 0) {
            raiseChannelException(context);
        }

        // Get cio data
        try {
            byte[] cioData = new byte[bufferSize];
            int result = execSyncIO(context, ChannelConstants.CONTEXT_GETHIBERNATIONDATA, 0, cioData.length, 0, 0, 0, 0, null, cioData);
            if (result != ChannelConstants.RESULT_OK) {
                if (result == ChannelConstants.RESULT_EXCEPTION) {
                    raiseChannelException(context);
                }
                throw new IOException("Bad result from hibernateChannelContext "+ result);
            }
            return cioData;
        } catch (OutOfMemoryError e) {
            throw new IOException("insufficient memory to serialize IO state");
        }
    }

    /**
     * Gets the current time.
     *
     * @return the time in microseconds
     *
     * @vm2c proxy( sysTimeMicros )
     */
    public static long getTimeMicros() {
        // Must get high word first as it causes the value to be setup that will be accessed via the INTERNAL_LOW_RESULT call
        long high = execSyncIO(ChannelConstants.INTERNAL_GETTIMEMICROS_HIGH, 0);
        long low  = execSyncIO(ChannelConstants.INTERNAL_LOW_RESULT, 0);
        return (high << 32) | (low & 0x00000000FFFFFFFFL);
    }

    /**
     * Gets the current time.
     *
     * @return the time in milliseconds
     *
     * @vm2c proxy( sysTimeMillis )
     */
    public static long getTimeMillis() {
/*if[!FLASH_MEMORY]*/
    	// Must get high word first as it causes the value to be setup that will be accessed via the INTERNAL_LOW_RESULT call
    	long high = execSyncIO(ChannelConstants.INTERNAL_GETTIMEMILLIS_HIGH, 0);
    	long low  = execSyncIO(ChannelConstants.INTERNAL_LOW_RESULT, 0);
    	return (high << 32) | (low & 0x00000000FFFFFFFFL);
/*else[FLASH_MEMORY]*/
//    	if (timeAddr.isZero()) {
//    		timeAddr = Address.fromPrimitive(execSyncIO(ChannelConstants.GET_CURRENT_TIME_ADDR, 0));
//    	}
//		return NativeUnsafe.getLong(timeAddr, 0);
/*end[FLASH_MEMORY]*/
    }

    /**
     * Poll for a completed event.
     *
     * @return the event number or zero for none
     */
    static int getEvent() {
        return execSyncIO(ChannelConstants.GLOBAL_GETEVENT, 0);
    }

    /**
     * Pause execution until an event occurs.
     *
     * @param time the maximum time to wait
     */
    static void waitForEvent(long time) {
        int low  = (int)time;
        int high = (int)(time>>32);
        execSyncIO(ChannelConstants.GLOBAL_WAITFOREVENT, high, low);
    }

    /**
     * Switch to the service stack and call 'GC.collectGarbage()'
     *
     * @param forceFullGC  forces a collection of the whole heap
     * @throws NotInlinedPragma  as the frame of this method will be the inner most frame on the
     *                           current thread's stack. The inner most frame on any stack does
     *                           not have it's local variables scanned by the garbage collector.
     *                           As such, this method <b>must not</b> use any local variables.
     */
    public static void collectGarbage(boolean forceFullGC)  throws NotInlinedPragma {
        executeGC(forceFullGC);
    }

    /**
     * Make a copy of the object graph in RAM rooted at a given object.
     *
     * @param object    the root of the object graph to copy
     * @return the ObjectMemorySerializer.ControlBlock instance that contains the serialized object graph and
     *                  its metadata
     * @throws OutOfMemoryError if there was insufficient memory to do the copy
     */
    static ObjectMemorySerializer.ControlBlock copyObjectGraph(Object object) {
        Assert.always(GC.inRam(object));

        /*
         * Free up as much memory as possible.
         */
        collectGarbage(true);

        ObjectMemorySerializer.ControlBlock cb = new ObjectMemorySerializer.ControlBlock();

        int graphSize = (int)(GC.totalMemory() - GC.freeMemory());
        byte[] bits = new byte[GC.calculateOopMapSizeInBytes(graphSize)];
        cb.oopMap = new com.sun.squawk.util.BitSet(bits);
        executeCOG(object, cb);

        if (cb.memory == null) {
            throw VM.getOutOfMemoryError();
        }

        // Adjust the oop map to be exactly the right size
        byte[] memory = (byte[])cb.memory;
        byte[] newBits = new byte[GC.calculateOopMapSizeInBytes(memory.length)];
        GC.arraycopy(bits, 0, newBits, 0, newBits.length);
        cb.oopMap = new com.sun.squawk.util.BitSet(newBits);

        return cb;
    }
    
    static boolean executingHooks;
        
    /**
     * Halt the VM in the normal way. Any registered shutdown hooks will be run.
     *
     * @param   code the exit status code.
     *
     * @see Isolate#addLifecycleListener
     */
    public static void stopVM(int code) {
        // thread-safe in Squawk only! This is a system class.
        if (executingHooks) return;
        
        executingHooks = true;
        
        shutdownHooks.runHooks();
        haltVM(code);
    }
    
    /**
     * Halt the VM without running exit hooks.
     *
     * @param   code the exit status code.
     */
    public static void haltVM(int code) {
        execSyncIO(ChannelConstants.INTERNAL_STOPVM, code);
        Assert.shouldNotReachHere();
    }

    /**
     * Registers a new virtual-machine shutdown hook.
     *
     * <p> The Java virtual machine <i>shuts down</i> in response to two kinds
     * of events:
     *
     *   <ul>
     *
     *   <p> <li> The program <i>exits</i> normally, when the last non-daemon
     *   thread exits or when the <tt>{@link #stopVM stopVM}</tt> method is invoked, or
     *
     *   <p> <li> The virtual machine is <i>terminated</i> in response to a
     *   user interrupt, such as typing <tt>^C</tt>, or a system-wide event,
     *   such as user logoff or system shutdown.
     *
     *   </ul>
     *
     * <p> A <i>shutdown hook</i> is a runnable.  When the virtual machine begins its 
     * shutdown sequence it will
     * start all registered shutdown hooks in some unspecified order and let
     * them run serially (this may result in lower memory requirements than running 
     * all hooks concurrently).  Note that daemon threads will
     * continue to run during the shutdown sequence, as will non-daemon threads
     * if shutdown was initiated by invoking the <tt>{@link #stopVM stopVM}</tt>
     * method.
     *
     * <p> Once the shutdown sequence has begun it can be stopped only by
     * invoking the <tt>{@link #haltVM haltVM}</tt> method, which forcibly
     * terminates the virtual machine.
     *
     * <p> Once the shutdown sequence has begun it is impossible to register a
     * new shutdown hook or de-register a previously-registered hook.
     * Attempting either of these operations will cause an
     * <tt>{@link IllegalStateException}</tt> to be thrown.
     *
     * <p> Shutdown hooks run at a delicate time in the life cycle of a virtual
     * machine and should therefore be coded defensively.  They should, in
     * particular, be written to be thread-safe and to avoid deadlocks insofar
     * as possible.  They should also not rely blindly upon services that may
     * have registered their own shutdown hooks and therefore may themselves in
     * the process of shutting down.
     *
     * <p> Shutdown hooks should also finish their work quickly.  When a
     * program invokes <tt>{@link #stopVM stopVM}</tt> the expectation is
     * that the virtual machine will promptly shut down and exit.  When the
     * virtual machine is terminated due to user logoff or system shutdown the
     * underlying operating system may only allow a fixed amount of time in
     * which to shut down and exit.  It is therefore inadvisable to attempt any
     * user interaction or to perform a long-running computation in a shutdown
     * hook.
     *
     * <p> Uncaught exceptions are handled in shutdown hooks just as in any
     * other thread - the VM prints the exception's stack trace to <tt>{@link System#err}</tt>;
     * it does not cause the virtual machine to exit or halt.
     *
     * <p> In rare circumstances the virtual machine may <i>abort</i>, that is,
     * stop running without shutting down cleanly.  This occurs when the
     * virtual machine is terminated externally, for example with the
     * <tt>SIGKILL</tt> signal on Unix or the <tt>TerminateProcess</tt> call on
     * Microsoft Windows.  The virtual machine may also abort if a native method goes awry
     * by, for example, corrupting internal data structures or attempting to
     * access nonexistent memory.  If the virtual machine aborts then no
     * guarantee can be made about whether or not any shutdown hooks will be
     * run. <p>
     *
     * @param   iso the isolate context to run the hook in.
     * @param   hook
     *          A <tt>{@link Runnable}</tt> object
     *
     * @throws  IllegalArgumentException
     *          If the specified hook has already been registered
     *
     * @throws  IllegalStateException
     *          If the virtual machine is already in the process
     *          of shutting down
     *
     * @see #removeShutdownHook
     * @see #haltVM(int)
     * @see #stopVM(int)
     */
    public static void addShutdownHook(Isolate iso, Runnable hook) {
        // thread-safe in Squawk only! This is a system class.
        if (executingHooks) {
            throw new IllegalStateException("Shutdown in progress");
        }
        
        shutdownHooks.add(iso, hook);
    }
    
    
    /**
     * De-registers a previously-registered virtual-machine shutdown hook. <p>
     *
     * @param   iso the isolate context the hook was registered with.
     * @param hook the hook to remove
     * @return <tt>true</tt> if the specified hook had previously been
     * registered and was successfully de-registered, <tt>false</tt>
     * otherwise.
     *
     * @throws  IllegalStateException
     *          If the virtual machine is already in the process of shutting
     *          down
     *
     * @see #addShutdownHook
     * @see #stopVM(int)
     */
    public static boolean removeShutdownHook(Isolate iso, Runnable hook) {
        if (executingHooks) {
            throw new IllegalStateException("Shutdown in progress");
        }
        
        return shutdownHooks.remove(iso, hook);
    }
    
        /**
     * Registers a new virtual-machine shutdown hook.
     *
     * <p> The Java virtual machine <i>shuts down</i> in response to two kinds
     * of events:
     *
     *   <ul>
     *
     *   <p> <li> The program <i>exits</i> normally, when the last non-daemon
     *   thread exits or when the <tt>{@link #stopVM stopVM}</tt> method is invoked, or
     *
     *   <p> <li> The virtual machine is <i>terminated</i> in response to a
     *   user interrupt, such as typing <tt>^C</tt>, or a system-wide event,
     *   such as user logoff or system shutdown.
     *
     *   </ul>
     *
     * <p> A <i>shutdown hook</i> is a runnable.  When the virtual machine begins its 
     * shutdown sequence it will
     * start all registered shutdown hooks in some unspecified order and let
     * them run serially (this may result in lower memory requirements than running 
     * all hooks concurrently).  Note that daemon threads will
     * continue to run during the shutdown sequence, as will non-daemon threads
     * if shutdown was initiated by invoking the <tt>{@link #stopVM stopVM}</tt>
     * method.
     *
     * <p> Once the shutdown sequence has begun it can be stopped only by
     * invoking the <tt>{@link #haltVM haltVM}</tt> method, which forcibly
     * terminates the virtual machine.
     *
     * <p> Once the shutdown sequence has begun it is impossible to register a
     * new shutdown hook or de-register a previously-registered hook.
     * Attempting either of these operations will cause an
     * <tt>{@link IllegalStateException}</tt> to be thrown.
     *
     * <p> Shutdown hooks run at a delicate time in the life cycle of a virtual
     * machine and should therefore be coded defensively.  They should, in
     * particular, be written to be thread-safe and to avoid deadlocks insofar
     * as possible.  They should also not rely blindly upon services that may
     * have registered their own shutdown hooks and therefore may themselves in
     * the process of shutting down.
     *
     * <p> Shutdown hooks should also finish their work quickly.  When a
     * program invokes <tt>{@link #stopVM stopVM}</tt> the expectation is
     * that the virtual machine will promptly shut down and exit.  When the
     * virtual machine is terminated due to user logoff or system shutdown the
     * underlying operating system may only allow a fixed amount of time in
     * which to shut down and exit.  It is therefore inadvisable to attempt any
     * user interaction or to perform a long-running computation in a shutdown
     * hook.
     *
     * <p> Uncaught exceptions are handled in shutdown hooks just as in any
     * other thread - the VM prints the exception's stack trace to <tt>{@link System#err}</tt>;
     * it does not cause the virtual machine to exit or halt.
     *
     * <p> In rare circumstances the virtual machine may <i>abort</i>, that is,
     * stop running without shutting down cleanly.  This occurs when the
     * virtual machine is terminated externally, for example with the
     * <tt>SIGKILL</tt> signal on Unix or the <tt>TerminateProcess</tt> call on
     * Microsoft Windows.  The virtual machine may also abort if a native method goes awry
     * by, for example, corrupting internal data structures or attempting to
     * access nonexistent memory.  If the virtual machine aborts then no
     * guarantee can be made about whether or not any shutdown hooks will be
     * run. <p>
     *
     * @param   hook
     *          A <tt>{@link Thread}</tt> object
     *
     * @throws  IllegalArgumentException
     *          If the specified hook has already been registered
     *
     * @throws  IllegalStateException
     *          If the virtual machine is already in the process
     *          of shutting down
     *
     * @see #removeShutdownHook
     * @see #haltVM(int)
     * @see #stopVM(int)
     */
    public static void addShutdownHook(Thread hook) {
        // thread-safe in Squawk only! This is a system class.
        if (executingHooks) {
            throw new IllegalStateException("Shutdown in progress");
        }
        
        shutdownHooks.add(VMThread.asVMThread(hook).getIsolate(), hook);
    }
    
    
    /**
     * De-registers a previously-registered virtual-machine shutdown hook. <p>
     *
     * @param hook the shutdown hook to remove
     * @return <tt>true</tt> if the specified hook had previously been
     * registered and was successfully de-registered, <tt>false</tt>
     * otherwise.
     *
     * @throws  IllegalStateException
     *          If the virtual machine is already in the process of shutting
     *          down
     *
     * @see #addShutdownHook
     * @see #stopVM(int)
     */
    public static boolean removeShutdownHook(Thread hook) {
        if (executingHooks) {
            throw new IllegalStateException("Shutdown in progress");
        }
        
        return shutdownHooks.remove(VMThread.asVMThread(hook).getIsolate(), hook);
    }

    /**
     * Copy memory from one area to another.
     *
     * @param      src          the source address.
     * @param      srcPos       the byte offset into src.
     * @param      dst          the destination address.
     * @param      dstPos       the byte offset into dst.
     * @param      length       the number of bytes to be copied.
     * @param      nvmDst       the destination buffer is in NVM
     *
     * @vm2c proxy
     */
    native static void copyBytes(Address src, int srcPos, Address dst, int dstPos, int length, boolean nvmDst);

    /**
     * Allocate a chunk of zeroed memory from RAM.
     *
     * @param   size        the length in bytes of the object and its header (i.e. the total number of
     *                      bytes to be allocated).
     * @param   klass       the class of the object being allocated
     * @param   arrayLength the number of elements in the array being allocated or -1 if a non-array
     *                      object is being allocated
     * @return a pointer to a well-formed object or null if the allocation failed
     */
    native static Object allocate(int size, Object klass, int arrayLength);

    /**
     * Zero a word-aligned block of memory.
     *
     * @param      start        the start address of the memory area
     * @param      end          the end address of the memory area
     */
    native static void zeroWords(Address start, Address end);

    /**
     * Fill a block of memory with the 0xDEADBEEF pattern.
     *
     * @param      start        the start address of the memory area
     * @param      end          the end address of the memory area
     *
     * @vm2c code( if (ASSUME || TYPEMAP) {
     *                 while (start < end) {
     *                     if (ASSUME) {
     *                         *((UWord *)start) = DEADBEEF;
     *                     }
     *                     setType(start, AddressType_UNDEFINED, HDR_BYTES_PER_WORD);
     *                     start = (UWord *)start + 1;
     *                 }
     *             } )
     */
    native static void deadbeef(Address start, Address end);
    
    /** 
     * Perform a shallow copy of the original object, without calling a constructor
     *
     * @param original the iobject to copy
     * @return a copy of the original object.
     */
    public static Object shallowCopy(Object original) {
        Klass klass = GC.getKlass(original);
        Object copy = GC.newInstance(klass);
        VM.copyBytes(Address.fromObject(original), 0, Address.fromObject(copy), 0, klass.getInstanceSize() * HDR.BYTES_PER_WORD, false);
        return copy;
    }

    /**
     * Call a static method.
     *
     * @param klass  the klass of the method
     * @param slot   the offset into the static vtable
     */
    native static void callStaticNoParm(Klass klass, int slot);

    /**
     * Call a static method passing a single parameter.
     *
     * @param parm  the parameter
     * @param klass the klass of the method
     * @param slot  the offset into the static vtable
     */
    native static void callStaticOneParm(Klass klass, int slot, Object parm);

    /**
     * Get the sentinal OutOfMemoryException object
     *
     * @return the object
     */
    public static OutOfMemoryError getOutOfMemoryError() {
        return outOfMemoryError;
    }

    /**
     * Print thread name as safely as possible. 
     * 
     * Called by error reporting code, so doesn't assert, or intentionally throw exceptions!
     *
     * @param thr the thread to print
     */
    public static void printThread(VMThread thr) {
        if (thr == null) {
            VM.print("VM.printThread(thr == null)");
            return;
        }
        try {
            String thrName = thr.getName();
            if (thrName != null) {
                VM.print(thrName);
                return;
            }
        } catch (OutOfMemoryError e) {
            VM.print("Uncaught out of memory error while printing thread name ");
        } catch (Throwable exc2) {
             VM.print("Uncaught exception while printing thread name ");
        }
        // backup case:
        VM.print("Thread-");
        VM.print(thr.getThreadNumber());
    }
    
    /**
     * Print branch count as safely as possible. 
     * 
     * Called by error reporting code, so doesn't assert, or intentionally throw exceptions!
     */
    public static void printBC() {
        long bc = VM.branchCount(); // if vmcore compiled with tracing, this will work.
        if (bc >= 0) {
            VM.print("after ");
            VM.print(bc);
            VM.print(" branches ");
        }
    }
    
    /**
     * Prints the backtrace using VM.print(). This code should not throw an exception unless VM.print()
     * or VM.StackTraceElement.printToVM() is broken.
     *
     * @param exc exception to report
     * @param origExcName klass name of exception to report
     * @param message the exception's message
     */
    private static void printVMStackTrace(Throwable exc, String origExcName, String message) {
        VM.println(origExcName);
        if (message != null) {
            VM.print(": ");
            VM.println(message);
        }
        VM.StackTraceElement[] trace = (VM.StackTraceElement[])NativeUnsafe.getObject(exc, (int)FieldOffsets.java_lang_Throwable$trace);
        if (exc != VM.getOutOfMemoryError() && trace != null) {
            for (int i = 0; i != trace.length; ++i) {
                VM.print("    ");
                trace[i].printToVM();
            }
        }
    }
    
    /**
     * Safely print exception and stack trace to System.err. Handles exceptions in 
     * Throwable.toString and printStackTrace, including OutOfMemoryExceptions.
     *
     * In all cases, this should print the message, the thread name, and the orginal exception (cclass name or toString).
     *
     * @param exc excption to report
     * @param msg message to print before exception.
     * @return Secondary exception, or null if none.
     */
    public static Throwable printExceptionAndTrace(Throwable exc, String msg) {
        String origExcName = "unknown";
        
        // print preamble. Should never fail:
        try {
            VM.println(msg);
            VM.print("    ");
            VM.printBC();
            VM.print("on thread ");
            VM.printThread(VMThread.currentThread());
            VM.println();
            origExcName = GC.getKlass(exc).getInternalName();
        } catch (Throwable e) {
            VM.println("Error in VM.printExceptionAndTrace");
            VM.fatalVMError();
        }
        
        try {
           /* 
            * Try to print stack trace normally, via streams. If that fails, try to print to
            * using VM.print(). Catches errors in io redirection, remote printing, etc...
            */
            String excMesg = "error calling Throwable.getMessage()";
            try {
                excMesg = exc.getMessage(); // get this in "try", in case it throws error
                exc.printStackTrace();
                return null;
            } catch (OutOfMemoryError exc2) {
                VM.println("Uncaught out of memory error while printing stack trace ");
                VM.print("    original exception: ");
                printVMStackTrace(exc, origExcName, excMesg);
                return exc2;
            } catch (Throwable exc2) {
                VM.println("Uncaught exception while printing stack trace ");
                VM.print("    original exception: ");
                printVMStackTrace(exc, origExcName, excMesg);
                VM.print("    secondary exception: ");
                printVMStackTrace(exc2, GC.getKlass(exc2).getInternalName(), null);
                return exc2;
            }
        } catch (OutOfMemoryError exc2) {
            VM.println("Uncaught out of memory error while VM.printing stack trace ");
            VM.print("    original exception: ");
            VM.println(origExcName);
            return exc2;
        } catch (Throwable exc2) {
            VM.println("Uncaught exception while VM.printing stack trace ");
            VM.print("    original exception: ");
            VM.println(origExcName);
            VM.print("    original msg: ");
            VM.println(msg);
            VM.print("    secondary exception: ");
            VM.println(GC.getKlass(exc2).getInternalName());
            return exc2;
        }
    }
    
    /*-----------------------------------------------------------------------*\
     *                          I/O natives                                  *
    \*-----------------------------------------------------------------------*/

    /**
     * Executes a channel I/O operation.
     * <p>
     * Globals are used for passing the parameters and returning the result as performing an IO operation requires
     * switching to the service thread. Using the service thread for IO operations means that they will be performed
     * on a stack chunk not managed by the collector. This means stack overflow checking of IO operations
     * is the responsibility of the underlying system.
     *
     * @param context   the I/O context
     * @param op        the opcode
     * @param channel   the channel number
     * @param i1        an integer parameter
     * @param i2        an integer parameter
     * @param i3        an integer parameter
     * @param i4        an integer parameter
     * @param i5        an integer parameter
     * @param i6        an integer parameter
     * @param send      an outgoing reference parameter
     * @param receive   an incoming reference parameter (i.e. an array of some type)
     */
    private native static void executeCIO(int context, int op, int channel, int i1, int i2, int i3, int i4, int i5, int i6, Object send, Object receive);

    /**
     * Gets the result of the last service operation.
     *
     * @vm2c code(
                    int res = com_sun_squawk_ServiceOperation_result;
                    com_sun_squawk_ServiceOperation_result = 0xDEADBEEF;
                    return res;
                );
     */
    private native static int serviceResult();

    /**
     * Gets the result of the last message I/O operation.
     */
    private native static Address addressResult();

    /*-----------------------------------------------------------------------*\
     *                      Non-blocking I/O                                 *
    \*-----------------------------------------------------------------------*/

    /**
     * Executes a non-blocking I/O operation whose result is guaranteed to be available immediately.
     * This mechanism requires 2 calls to the IO sub-system. The first sets up the globals used to pass the parameters and
     * initiates the operation. The second retrieves the result from the global that the operation stored its result in.
     *
     * @param op        the opcode
     * @param i1        an integer parameter
     * @param i2        an integer parameter
     * @param i3        an integer parameter
     * @param i4        an integer parameter
     * @param i5        an integer parameter
     * @param i6        an integer parameter
     * @param send      an outgoing array parameter
     * @param receive   an incoming array parameter
     * @return          the integer result value
     */
    public static int execSyncIO(int op, int i1, int i2, int i3, int i4, int i5, int i6, Object send, Object receive) {
        executeCIO(-1, op, -1, i1, i2, i3, i4, i5, i6, send, receive);
        return serviceResult();
    }

    /**
     *
     * Executes a non-blocking I/O operation whose result is guaranteed to be available immediately.
     * This mechanism requires 2 calls to the IO sub-system. The first sets up the globals used to pass the parameters and
     * initiates the operation. The second retrieves the result from the global that the operation stored its result in.
     *
     * @param context   the I/O context
     * @param op        the opcode
     * @param i1        an integer parameter
     * @param i2        an integer parameter
     * @param i3        an integer parameter
     * @param i4        an integer parameter
     * @param i5        an integer parameter
     * @param i6        an integer parameter
     * @param send      an outgoing array parameter
     * @param receive   an incoming array parameter
     * @return          the integer result value
     */
    public static int execSyncIO(int context, int op, int i1, int i2, int i3, int i4, int i5, int i6, Object send, Object receive) {
        executeCIO(context, op, -1, i1, i2, i3, i4, i5, i6, send, receive);
        return serviceResult();
    }

    /**
     * Executes a non-blocking I/O operation whose result is guaranteed to be available immediately.
     *
     * @param op        the opcode
     * @param i1        an integer parameter
     * @return          the integer result value
     */
    public static int execSyncIO(int op, int i1) {
        executeCIO(-1, op, -1, i1, 0, 0, 0, 0, 0, null, null);
        return serviceResult();
    }

    /**
     * Executes a non-blocking I/O operation whose result is guaranteed to be available immediately via {@link #serviceResult}.
     *
     * @param op        the opcode
     * @param i1        an integer parameter
     * @param i2        an integer parameter
     * @return          the integer result value
     */
    static int execSyncIO(int op, int i1, int i2) {
        executeCIO(-1, op, -1, i1, i2, 0, 0, 0, 0, null, null);
        return serviceResult();
    }


    /**
     * Executes a non-blocking I/O operation whose result is guaranteed to be available immediately via {@link #serviceResult}.
     *
     * @param  context   the I/O context
     * @param  op        the opcode
     * @param  i1        an integer parameter
     * @param  i2        an integer parameter
     * @return           the result status
     */
    static int execSyncIO(int context, int op, int i1, int i2) {
        executeCIO(context, op, -1, i1, i2, 0, 0, 0, 0, null, null);
        return serviceResult();
    }

    /*-----------------------------------------------------------------------*\
     *                        Blocking I/O                                   *
    \*-----------------------------------------------------------------------*/

    /**
     * Gets the exception message for  the last channel IO operation.
     *
     * @param context the channel context
     * @return the message
     */
    private static String getExceptionMessage(int context) {
        StringBuffer sb = new StringBuffer();
        for (;;) {
            char ch = (char)execSyncIO(context, ChannelConstants.CONTEXT_GETERROR, 0, 0);
            if (ch == 0) {
                return sb.toString();
            }
            sb.append(ch);
        }
    }

    /**
     * Raises an exception that occurred in the last channel IO operation.
     *
     * @param context the channel context
     * @throws IOException
     */
    private static void raiseChannelException(int context) throws IOException {
        String name = getExceptionMessage(context);
        Object exception = null;
        try {
            Class exceptionClass = Class.forName(name);
            try {
                exception = exceptionClass.newInstance();
            } catch (IllegalAccessException ex1) {
            } catch (InstantiationException ex1) {
            }
        } catch (ClassNotFoundException ex) {
        }
        if (exception != null) {
            if (exception instanceof IOException) {
                throw (IOException)exception;
            } else if (exception instanceof RuntimeException) {
                throw (RuntimeException)exception;
            } else if (exception instanceof Error) {
                throw (Error)exception;
            }
        }
        throw new IOException(name);
    }

    /**
     * Executes a I/O operation that may block. This requires at least 2 calls to the IO sub-system: the first to execute the operation
     * and the second to get the status of the operation (success = 0, failure < 0 or blocked > 0). If the status is success, then a
     * third call to the IO sub-system is made to retrieve the result of the operation. If the status indicates that an exception
     * occcurred in the IO sub-system, then an IOException is thrown. If the status indicates that the IO sub-system is blocked,
     * then the status value is used as an event number to block the current thread and put it on a queue of threads waiting for an event.
     *
     * @param op        the opcode
     * @param channel   the channel number
     * @param i1        an integer parameter
     * @param i2        an integer parameter
     * @param i3        an integer parameter
     * @param i4        an integer parameter
     * @param i5        an integer parameter
     * @param i6        an integer parameter
     * @param send      an outgoing array parameter
     * @param receive   an incoming array parameter
     * @return          the integer result value
     */
    public static int execIO(int op, int channel, int i1, int i2, int i3, int i4, int i5, int i6, Object send, Object receive) throws IOException {
        int context = currentIsolate.getChannelContext();
        if (context == 0) {
            throw new IOException("No native I/O peer for isolate");
        }
        for (;;) {
            executeCIO(context, op, channel, i1, i2, i3, i4, i5, i6, send, receive);
            int result = serviceResult();
            if (result == ChannelConstants.RESULT_OK) {
                return execSyncIO(context, ChannelConstants.CONTEXT_GETRESULT, 0, 0);
            } else if (result < 0) {
                if (result == ChannelConstants.RESULT_EXCEPTION) {
                    raiseChannelException(context);
                }
                throw new IOException("Bad result from cioExecute "+ result);
            } else {
                VMThread.waitForEvent(result);
                context = currentIsolate.getChannelContext(); // Must reload in case of hibernation.
            }
        }
    }

    /**
     * Executes a message I/O operation.
     *
     * @param op        the opcode
     * @param key       the message key
     * @param data      the message data or null
     * @return          the Address result or null
     */
    public static Address execMessageIO(int op, Object key, Object data, int status) throws IOException {
        for (; ; ) {
            executeCIO( -1, op, ChannelConstants.CHANNEL_MESSAGEIO, status, 0, 0, 0, 0, 0, key, data);
            int result = serviceResult();
            if (result == ChannelConstants.RESULT_OK) {
                return addressResult();
            } else if (result < 0) {
                if (result == ChannelConstants.RESULT_MALLOCFAILURE) {
                    throw outOfMemoryError;
                } else if (result == ChannelConstants.RESULT_BADPARAMETER) {
                    throw new IOException("Bad parameter(s) to connection");
                }
                throw Assert.shouldNotReachHere("execMessageIO result = " + result);
            } else {
                VMThread.waitForEvent(result);
            }
        }
    }

    /**
     * Executes an I/O operation that returns a <code>long</code> value.
     *
     * @param op        the opcode
     * @param channel   the channel identifier
     * @param i1        an integer parameter
     * @param i2        an integer parameter
     * @param i3        an integer parameter
     * @param i4        an integer parameter
     * @param i5        an integer parameter
     * @param i6        an integer parameter
     * @param send      a outgoing reference parameter
     * @param receive   an incoming reference parameter (i.e. an array of some type)
     * @return          the long result
     */
    public static long execIOLong(int op, int channel, int i1, int i2, int i3, int i4, int i5, int i6, Object send, Object receive) throws IOException {
        long low     = execIO(op, channel, i1, i2, i3, i4, i5, i6, send, receive);
        int  context = currentIsolate.getChannelContext();
        long high    = execSyncIO(context, ChannelConstants.CONTEXT_GETRESULT_2, 0, 0);
        return (high << 32) | (low & 0x00000000FFFFFFFFL);
    }

/*if[!FLASH_MEMORY]*/
    /**
     * Executes an I/O operation on the graphics channel and return the result.
     *
     * @param op        the opcode
     * @param i1        an integer parameter
     * @param i2        an integer parameter
     * @param i3        an integer parameter
     * @param i4        an integer parameter
     * @param i5        an integer parameter
     * @param i6        an integer parameter
     * @param send      a outgoing reference parameter
     * @param receive   an incoming reference parameter (i.e. an array of some type)
     * @return the event code to wait on or zero
     */
    public static int execGraphicsIO(int op, int i1, int i2, int i3, int i4, int i5, int i6, Object send, Object receive) {
        try {
            int chan = currentIsolate.getGuiOutputChannel();
            return execIO(op, chan, i1, i2, i3, i4, i5, i6, send, receive);
        } catch(IOException ex) {
            throw new RuntimeException("Error executing graphics channel: " + ex);
        }
    }

    /**
     * Gets the next available event on the GUI input channel, blocking until there is one.
     *
     * @return the GUI event value
     */
    public static long getGUIEvent() {
        try {
            int channel = currentIsolate.getGuiInputChannel();
            return VM.execIOLong(ChannelConstants.READLONG, channel, 0, 0, 0, 0, 0, 0, null, null);
        } catch(IOException ex) {
            throw new RuntimeException("Error executing event channel: " + ex);
        }
    }
/*else[FLASH_MEMORY]*/ 
//    /**
//     * Not used on embedded devices.
//     */
//    public static int execGraphicsIO(int op, int i1, int i2, int i3, int i4, int i5, int i6, Object send, Object receive) {
//        throw Assert.shouldNotReachHere();
//    }
//
//    /**
//     * Not used on embedded devices.
//     */
//    public static long getGUIEvent() {
//        throw Assert.shouldNotReachHere();
//    }
/*end[FLASH_MEMORY]*/
    
/*if[FLASH_MEMORY]*/
    /**
     * Waits for an interrupt.
     *
     * @param irq   mask for interrupt
     * @throws IOException
     */
    public static void waitForInterrupt(int irq) throws IOException {
        executeCIO(0, ChannelConstants.IRQ_WAIT,0,irq,0,0,0,0,0,null,null);
        int result = serviceResult();
        if (result < 0) {
            if (result == ChannelConstants.RESULT_EXCEPTION) {
                raiseChannelException(0);
            }
            throw new IOException("Bad result from cioExecute "+ result);
        } else if (result != ChannelConstants.RESULT_OK) {
            VMThread.waitForEvent(result);
        }
    }

    /**
     * Wait until it's possible that we can go to deep sleep. It's possible if
     * the thread scheduler has nothing to do for at least a certain length of time
     *
     * @param minimumDeepSleepTime the minimum time (in millis) that it's worth deep sleeping
     *
     * @return the target wake up time (in System clock millis) that we should return from deep sleep
     */
    public static long waitForDeepSleep(long minimumDeepSleepTime) {
		int lowParam  = (int)minimumDeepSleepTime;
		int highParam = (int)(minimumDeepSleepTime>>32);

        executeCIO(0, ChannelConstants.WAIT_FOR_DEEP_SLEEP,0,highParam,lowParam,0,0,0,0,null,null);
        int result = serviceResult();
        VMThread.waitForEvent(result);

        long highResult = execSyncIO(ChannelConstants.DEEP_SLEEP_TIME_MILLIS_HIGH, 0);
        long lowResult  = execSyncIO(ChannelConstants.DEEP_SLEEP_TIME_MILLIS_LOW, 0);
        return (highResult << 32) | (lowResult & 0x00000000FFFFFFFFL);
    }

    /**
     * Call the main method of the specified class
     *
     * @param className the name of the class whose main method is to be run
     * @param args the arguments to be passed to the main method
     * @throws ClassNotFoundException if the class is not found
     */
    public static void invokeMain(String className, String[] args) throws ClassNotFoundException {
        Klass.forName(className).main(args);
    }
/*end[FLASH_MEMORY]*/

    /**
     * Mark the specified thread to be a daemon thread (won't prevent VM from exiting).
     * If this thread is alive, an IllegalThreadStateException is thrown.
     *
     * @param t The thread
     */
    public static void setAsDaemonThread(Thread t) {
    	VMThread.asVMThread(t).setDaemon(true);
    }
    
    /**
     * Sets the given thread to the given priority, bounded by MAX_SYS_PRIORITY (eg. allowing
     * higher than normal priority levels)
     * Note that threads created by a thread with "system" priority do not inherit the system priority level, but default to
     * NORM_PRIORITY.
     *
     * Should only be called by system code. This interface likely to change to more RTSJ-like scheme.
     * 
     * @param t The thread
     * @param level the system priority level (currently supports normal priorities as well as 11, and 12)
     * @exception  IllegalArgumentException  If the priority is not in the
     *             range <code>MIN_PRIORITY</code> to
     *             <code>MAX_SYS_PRIORITY</code>.
     */
    public static void setSystemThreadPriority(Thread t, int level) {
    	VMThread.asVMThread(t).setSystemPriority(level);
    }

    /**
     * Gets a new IO channel.
     *
     * @param type the channel type
     * @return the identifier for the newly created channel
     */
    public static int getChannel(int type) throws IOException {
        int context = currentIsolate.getChannelContext();
        if (context == 0) {
            throw new IOException("no native I/O peer for isolate");
        }
        return execSyncIO(context, ChannelConstants.CONTEXT_GETCHANNEL, type, 0);
    }

    /**
     * Frees a channel.
     *
     * @param channel the identifier of the channel to free
     */
    public static void freeChannel(int channel) throws IOException {
        int context = currentIsolate.getChannelContext();
        if (context == 0) {
            throw new IOException("no native I/O peer for isolate");
        }
        executeCIO(context, ChannelConstants.CONTEXT_FREECHANNEL, channel, 0, 0, 0, 0, 0, 0, null, null);
    }

/*if[OLD_IIC_MESSAGES]*/
    /**
     * Adds a new ServerConnectionHandler to the list of active handlers.
     *
     * @param sch the ServerConnectionHandler to add
     * @throws IllegalArgumentException if there is already a handler registered with the same name as <code>sch</code>
     */
    static void addServerConnectionHandler(ServerConnectionHandler sch) {
        if (ServerConnectionHandler.lookup(serverConnectionHandlers, sch.getConnectionName()) != null) {
            throw new IllegalArgumentException();
        }
        sch.setNext(serverConnectionHandlers);
        serverConnectionHandlers = sch;
    }

    /**
     * Finds the first ServerConnectionHandler in the list that has an active message waiting.
     *
     * @return the ServerConnectionHandler or null if none have a waiting message
     */
    static ServerConnectionHandler getNextServerConnectionHandler() throws IOException {
        Address res = execMessageIO(ChannelConstants.INTERNAL_SEARCH_SERVER_HANDLERS, null, serverConnectionHandlers, 0);
        return (ServerConnectionHandler)res.toObject();
    }
/*end[OLD_IIC_MESSAGES]*/


    /*=======================================================================*\
     *                           USER VISIBLE STATS                          *
    \*=======================================================================*/
    
    /**
     * Virtual machine statistics.<p>
     *
     * Note that these statistics are simple counters that may wrap to negative values. Negative values can simply be treated as unsigned values.<p>
     *
     * The expeceted usage of these counters is to be polled periodically, so the absolute value is not as important as the difference between different readings.
     * You will need to be prepared to handle a counter wrapping during two readings, but if samples are frequent, you shouldn't have to worry about the counters
     * wrapping more than once.<p>
     * <pre>
     * <b>EXAMPLE 1 (Perf meter):</b>
     *
     *      totalTimeDiff = totalTime[n]  - totalTime[n-1];
     *      gcTimeDiff    =  gcTime[n] - gcTime[n-1];
     *      sysTimeDiff   = sysTime[n] - sysTime[n-1];
     *      userTimeDiff  = totalTime[n] - (gcTime[n] + sysTime[n]);
     *      System.out.println("User time: " + (userTimeDiff * 100) / totalTimeDiff + "%");
     *      System.out.println("GC time: " + (gcTimeDiff * 100) / totalTimeDiff + "%");
     *      System.out.println("System/Idle time: " + (sysTimeDiff * 100) / totalTimeDiff + "%");
     * </pre>
     *  
     */
    public static class Stats {
        
        /**
         *  Get the total time the VM was idle. <p>
         *
         *  This typically means waiting for timeouts or IO.
         *
         * @return wait time in milliseconds
         */
        public static long getTotalWaitTime() {
            return VMThread.getTotalWaitTime();
        }
        
        /**
         *  Get the total time taken by all garbage collections.
         *
         * @return the total GC time in milliseconds
         */
        public static long getTotalGCTime() {
            return GC.getCollector().getTotalGCTime();
        }
        
        /**
         *  Get the total time taken by all full garbage collections.
         *
         * @return the full GC time in milliseconds
         */
        public static long getTotalFullGCTime() {
            return GC.getCollector().getTotalFullGCTime();
        }
        
        /**
         *  Get the total time taken by all partial garbage collections.
         *
         * @return the partial GC time in milliseconds
         */
        public static long getTotalPartialGCTime() {
            return GC.getCollector().getTotalPartialGCTime();
        }
        
        /**
         * Get the time taken by the last garbage collection.
         *
         * @return the last GC time in milliseconds
         */
        public static long getLastGCTime() {
            return GC.getCollector().getLastCollectionTime();
        }
        
        /**
         * Get the time taken by the slowest garbage collection.
         *
         * @return the longest GC time in milliseconds
         */
        public static long getMaxGCTime() {
            return Math.max(getMaxFullGCTime(), getMaxPartialGCTime());
        }
        
        /**
         * Get the time taken by the slowest full garbage collection.
         *
         * @return the longest full GC time in milliseconds
         */
        public static long getMaxFullGCTime() {
            return GC.getCollector().getMaxFullCollectionTime();
        }
        
        /**
         * Get the time taken by the slowest partial garbage collection.
         *
         * @return the longest partial GC time in milliseconds
         */
        public static long getMaxPartialGCTime() {
            return GC.getCollector().getMaxPartialCollectionTime();
        }
        
        /**
         * Get the total number of full garbage collections since reboot.
         *
         * @return full gc count
         */
        public static int getFullGCCount() {
            return GC.getFullCollectionCount();
        }
        
        /**
         * Get the total number of partial garbage collections since reboot.
         *
         * @return partial gc count
         */
        public static int getPartialGCCount() {
            return GC.getPartialCollectionCount();
        }
        
        /**
         * Get the number of bytes freed in the last collection.
         *
         * @return bytes freed
         */
        public static long getBytesLastFreed() {
            return GC.getCollector().getBytesLastFreed();
        }
        
        /**
         * Get the number of bytes freed since reboot.<p>
         *
         * Watch out for overflow.
         *
         * @return total bytes freed
         */
        public static long getBytesFreedTotal() {
            return GC.getCollector().getBytesFreedTotal();
        }
        
        /**
         *Get the number of bytes allocated since reboot.<p>
         *
         * Watch out for overflow.
         *
         * @return total bytes allocated
         */
        public static long getBytesAllocatedTotal() {
            return GC.getBytesAllocatedTotal();
        }
        
        /**
         * Get the number of objects allocated since reboot.
         *
         * Watch out for overflow.
         *
         * @return total objects allocated
         */
        public static int getObjectsAllocatedTotal() {
            return VM.getGlobalInt(Global.getOffset(Global.newCount));
        }
        
        /**
         * Get the number of Thread objects allocated since reboot.
         *
         *  @return threads allocated
         */
        public static int getThreadsAllocatedCount() {
            return VMThread.getThreadsAllocatedCount();
        }
        
        /**
         * Return count of thread context switching since reboot.<p>
         *
         * This does not include system-level switches that occur for GC, exception throwing, etc.
         *
         *  @return switch count
         */
        public static int getThreadSwitchCount() {
            return VMThread.getThreadSwitchCount();
        }
        
        /**
         * Return the number of times that a thread was blocked trying to synchronize on an object.<p>
         *
         * Note that this counts the initial contention. A thread may be released to aquire the lock,
         * but another thread (potentially higher priority) runs first, and actually acquires the lock.
         * The first thread will then have to wait again.
         *
         *  @return contended enters
         */
        public static int getContendedMontorEnterCount() {
            return VMThread.getContendedMontorEnterCount();
        }
        
        /**
         * Return the number of monitors allocated.<p>
         *
         * Often, uncontended locking is handled by the interpreter in the pendingMonitors cache. But if the
         * cache is full, or there is contention, or Object.wait() is used, or a threadd is switched out while
         * holding a virtual monitor, then a real monitor has to be allocated for an object. It is possible for
         * the monitor for an object to come and go, so there is the possibility of "monitor object thrashing".
         *
         * @return total monitors allocated
         */
        public static int getMonitorsAllocatedCount() {
            return VMThread.getMonitorsAllocatedCount();
        }
        
        /**
         * Return the number of stacks allocated. <p>
         *
         * Stacks are allocated for each thread, and as more frames are needed, new stacks
         * are created to replace the original stacks (typically at 2x the size of the original stack).
         * The default stack size is about 160 words.
         *
         *  @return total stacks allocated
         */
        public static int getStacksAllocatedCount() {
            return VMThread.getStacksAllocatedCount();
        }
        
        /**
         * Return size of the largest stack ever allocated.
         *
         *  @return largest stack size, in words
         */
        public static int getMaxStackSize() {
            return VMThread.getMaxStackSize();
        }
        
        /**
         * Return number of exceptions thrown.
         *
         * @return exceptions thrown
         */
        public static int getThrowCount() {
            return throwCount;
        }
        
        /**
         * tag for data sent by sendStatData()
         */
        public static final int STAT_WALL_TIME               = 0;
        public static final int STAT_WAIT_TIME               = 1;
        public static final int STAT_GC_TIME                 = 2;
        public static final int STAT_FULL_GC_TIME            = 3;
        public static final int STAT_PARTIAL_GC_TIME         = 4;
        public static final int STAT_LAST_GC_TIME            = 5;
        public static final int STAT_MAX_FULLGC_TIME         = 6;
        public static final int STAT_MAX_PARTGC_TIME         = 7;
        public static final int STAT_FIRST_COUNT_STAT        = 8;  // -------- divider between time-based and count-based stats
        public static final int STAT_FULL_GC_COUNT           = 9;
        public static final int STAT_PARTIAL_GC_COUNT        = 10;
        public static final int STAT_BYTES_LAST_FREED        = 11;
        public static final int STAT_BYTES_TOTAL_FREED       = 12;
        public static final int STAT_BYTES_TOTAL_ALLOCATED   = 13;
        public static final int STAT_OBJECTS_TOTAL_ALLOCATED = 14;
        public static final int STAT_THREADS_ALLOCATED       = 15;
        public static final int STAT_THREAD_SWITCH_COUNT     = 16;
        public static final int STAT_CONTENDED_MONITOR_COUNT = 17;
        public static final int STAT_MONITORS_ALLOCATED      = 18;
        public static final int STAT_STACKS_ALLOCATED        = 19;
        public static final int STAT_MAX_STACK_SIZE          = 20;
        public static final int STAT_THROW_COUNT             = 21;
        public static final int STAT_BRANCH_COUNT            = 22;
        public static final int STAT_HEAP_FREE               = 23;
        public static final int STAT_HEAP_TOTAL              = 24;
        public static final int NUM_STAT_VALUES              = 25;
        
        private long[] values;
        
        /**
         * Create a new Stats object,
         *
         * This is primarily used when calling {@link #sendStatData}.
         */
        public Stats() {
            values = new long[NUM_STAT_VALUES];
        }
        
        /**
         * Take a sample of all data and store into <code>values</code>.<p>
         *
         * This is simply a convenience method.
         *
         * @param values must have length of at least {@link #NUM_STAT_VALUES}.
         */
        public static void readAllValues(long[] values) {
            values[STAT_WALL_TIME]               = VM.getTimeMillis();
            values[STAT_WAIT_TIME]               = VM.Stats.getTotalWaitTime();
            values[STAT_GC_TIME]                 = VM.Stats.getTotalGCTime();
            values[STAT_FULL_GC_TIME]            = VM.Stats.getTotalFullGCTime();
            values[STAT_PARTIAL_GC_TIME]         = VM.Stats.getTotalPartialGCTime();
            values[STAT_LAST_GC_TIME]            = VM.Stats.getLastGCTime();
            values[STAT_MAX_FULLGC_TIME]         = VM.Stats.getMaxFullGCTime();
            values[STAT_MAX_PARTGC_TIME]         = VM.Stats.getMaxPartialGCTime();
            values[STAT_FULL_GC_COUNT]           = VM.Stats.getFullGCCount();
            values[STAT_PARTIAL_GC_COUNT]        = VM.Stats.getPartialGCCount();
            values[STAT_BYTES_LAST_FREED]        = VM.Stats.getBytesLastFreed();
            values[STAT_BYTES_TOTAL_FREED]       = VM.Stats.getBytesFreedTotal();
            values[STAT_BYTES_TOTAL_ALLOCATED]   = VM.Stats.getBytesAllocatedTotal();
            values[STAT_OBJECTS_TOTAL_ALLOCATED] = VM.Stats.getObjectsAllocatedTotal();
            values[STAT_THREADS_ALLOCATED]       = VM.Stats.getThreadsAllocatedCount();
            values[STAT_THREAD_SWITCH_COUNT]     = VM.Stats.getThreadSwitchCount();
            values[STAT_CONTENDED_MONITOR_COUNT] = VM.Stats.getContendedMontorEnterCount();
            values[STAT_MONITORS_ALLOCATED]      = VM.Stats.getMonitorsAllocatedCount();
            values[STAT_STACKS_ALLOCATED]        = VM.Stats.getStacksAllocatedCount();
            values[STAT_MAX_STACK_SIZE]          = VM.Stats.getMaxStackSize();
            values[STAT_THROW_COUNT]             = VM.Stats.getThrowCount();
            values[STAT_BRANCH_COUNT]            = VM.branchCount();
            values[STAT_HEAP_FREE]               = GC.freeMemory();
            values[STAT_HEAP_TOTAL]              = GC.totalMemory();
        }
      
        /**
         * Take a sample of all statics and send to output stream <code>dos</code>. <p>
         *
         * The format of the data is:
         * <pre>
         *     byte tag     = 0
         *     long value
         *     byte tag     = 1
         *     long value
         *        ...
         *     byte tag     = NUM_STAT_VALUES - 1
         *     long value
         *</pre>
         *     
         * The data can be read by a DataInputStream.
         *
         * @param dos output stream to send values.
         */
        public void sendStatData(java.io.DataOutputStream dos) {
            readAllValues(values);
            try {
                for (int i = 0; i < NUM_STAT_VALUES; i++) {
                    dos.writeByte(i);
                    dos.writeLong(values[i]);
                    //System.out.println("Wrote tag: " + i + " value: " + values[i]);
                }
                dos.flush();
            } catch (IOException ex) {
                System.out.println("Error writing stat "+ ex);
                ex.printStackTrace();
            }
            
        }
        
    } /* Stats */
    
   /*=======================================================================*\
     *                           Core VM functions                           *
    \*=======================================================================*/


    /**
     * Enable or disable Runtime.gc()
     *
     * @param value true to enable
     */
    public static void allowUserGC(boolean value) {
         allowUserGC = value;
    }

    /**
     * Tests if Runtime.gc() is allowed.
     *
     * @return true if calls to Runtime.gc() are allowed
     */
    public static boolean userGCAllowed() {
        return allowUserGC;
    }
    
    /**
     * Determines if the VM was built with memory access type checking enabled.
     *
     * @return true if the VM was built with memory access type checking enabled
     */
    public static boolean usingTypeMap() {
        return usingTypeMap;
    }

    /**
     * Gets the next available hashcode.
     *
     * @return the hashcode
     */
    public static int getNextHashcode() {
        do {
            nextHashcode++;
        } while (nextHashcode == 0);
        return nextHashcode;
    }

    /**
     * Gets the isolate of the currently executing thread.
     *
     * @return the isolate
     */
    public static Isolate getCurrentIsolate() {
        return currentIsolate;
    }

    /**
     * Gets the names of all manifest properties embedded in the leaf suite
     * and all of its parents.
     * @return enumeration over the names
     */
    public static Enumeration getManifestPropertyNames() {
    	Hashtable names = new Hashtable();
    	Suite suite = VM.getCurrentIsolate().getLeafSuite();            
        while (suite != null) {
        	Enumeration additions = suite.getManifestPropertyNames();
        	while (additions.hasMoreElements()) {
				Object propertyName = additions.nextElement();
				names.put(propertyName, propertyName);
			}
            suite = suite.getParent();
        }
    	return names.keys();
    }

    /**
     * Gets the value of an {@link Suite#PROPERTIES_MANIFEST_RESOURCE_NAME} property embedded in the suite.
     *
     * @param name the name of the property whose value is to be retrieved
     * @return the property value
     */
    public static String getManifestProperty(String name) {
        // look for the property in the current leaf suite, and then up the chain of parent suites until we find it
        Suite suite = VM.getCurrentIsolate().getLeafSuite();            
        while (suite != null) {
            String value = suite.getManifestProperty(name);
            if (value != null) {
                return value;
            }
            suite = suite.getParent();
        }

        return null;
    }
    
    /**
     * A helper method to provide access to the manifest of midlet suites from outside the bootstrap.
     */
    public static Hashtable getManifestPropertiesOfSuite(String uri) {
    	Hashtable properties = new Hashtable();
    	Suite suite = Suite.getSuite(uri);
    	Enumeration additions = suite.getManifestPropertyNames();
    	while (additions.hasMoreElements()) {
			String propertyName = (String)additions.nextElement();
			properties.put(propertyName, suite.getManifestProperty(propertyName));
		}
    	return properties;
    }
    
    /**
     * If the suite is registered, unregister it with the garbage collector. Otherwise do nothing.
     * 
     * @param uri the suite to unregister.
     */
    public static void unregisterSuite(String uri) {
    	ObjectMemory objectMemory = GC.lookupReadOnlyObjectMemoryBySourceURI(uri);
    	if (objectMemory != null) {
    		GC.unRegisterReadOnlyObjectMemory(objectMemory);
    	}
    }
    
    /**
     * Determines if the current isolate is set and initialized.
     *
     * @return true if the current isolate is set and initialized
     */
    public static boolean isCurrentIsolateInitialized() {
        return currentIsolate != null && currentIsolate.isClassKlassInitialized();
    }

    /**
     * Determines if the threading system is initialized.
     *
     * @return true if the threading system is initialized.
     */
    public static boolean isThreadingInitialized() {
        return synchronizationEnabled;
    }

    /**
     * Sets the isolate of the currently executing thread.
     *
     * @param isolate the isolate
     */
    static void setCurrentIsolate(Isolate isolate) {
        currentIsolate = isolate;
    }

/*if[FINALIZATION]*/
    /**
     * Eliminates a finalizer.
     *
     * @param obj the object of the finalizer
     */
    public static void eliminateFinalizer(Object obj) {
        GC.eliminateFinalizer(obj);
    }
/*end[FINALIZATION]*/

    /*=======================================================================*\
     *                          Native method lookup                         *
    \*=======================================================================*/

    /**
     * Determines if a given native method can be linked to by classes dynamically
     * loaded into the Squawk VM.
     *
     * @param name   the fully qualified name of a native method
     * @return true if the method can be linked to
     */
    static boolean isLinkableNativeMethod(String name) {
        return lookupNative(name) != -1;
    }

    /**
     * Gets the identifier for a native method.
     *
     * @param name   the fully qualified name of the native method
     * @return the identifier for the method or -1 if the method does not exist or cannot be dynamically bound to
     */
    public static int lookupNative(String name) {
        String table = Native.LINKABLE_NATIVE_METHODS;
        String last = null;
        int id = 0;
        int start = 0;
        int end = table.indexOf(' ');
        while (end != -1) {
            int sharedSubstringLength = table.charAt(start++) - '0';
            String entryName = table.substring(start, end);

            // Prepend prefix shared with previous entry (if any)
            if (sharedSubstringLength != 0) {
                Assert.that(last != null);
                entryName = last.substring(0, sharedSubstringLength) + entryName;
            }

            if (entryName.equals(name)) {
                return id;
            }

            start = end + 1;
            end = table.indexOf(' ', start);
            last = entryName;
            id++;
        }
        return -1;
    }

    /**
     * Determines if all the symbolic information for a class should be stripped. This
     * is used during the bootstrap process by the romizer to strip certain classes
     * based on their names.
     *
     * @param klass         the class to consider
     * @return true if the class symbols should be stripped
     */
    public static boolean stripSymbols(Klass klass) {
        return false;
    }

    /**
     * Determines if all the symbolic information for a field or method should be stripped. This
     * is used during the bootstrap process by the romizer to strip certain fields and methods
     * based on their names.
     *
     * @param klass         the class to consider
     * @return true if the class symbols should be stripped
     */
    public static boolean stripSymbols(Member member) {
        return false;
    }
    
    /**
     * Determines if the field or method is internal, so should be retained (even if symbol gets stripped)
     *
     * @param member        the method or field to consider
     * @return true if the class symbols should be stripped
     */
    public static boolean isInternal(Member member) {
        return false;
    }
    
     /**
     * Determines if the klass is internal, so should be retained (even if symbol gets stripped)
     *
     * @param member        the method or field to consider
     * @return true if the class symbols should be stripped
     */
    public static boolean isInternal(Klass klass) {
        return false;
    }

    /**
     * Support routine to get the object representing the class of a given object.
     * This takes into account whether or not the VM is running in hosted mode or
     * not. The returned object can only be used for identity comparisons.
     */
    public static Object getClass(Object object) {
        return GC.getKlass(object);
    }

    /**
     * Support routine to test whether a given object is an array.
     * This takes into account whether or not the VM is running in hosted mode or
     * not.
     */
    public static boolean isArray(Object o) {
        return (GC.getKlass(o).isArray());
    }

    static boolean isFirstIsolateInitialized() {
        return isFirstIsolateInitialized;
    }
    
    static String getIsolateInitializerClassName() {
        return isolateInitializer;
    }

    static void setFirstIsolateInitialized(boolean initialized) {
        isFirstIsolateInitialized = initialized;
    }

    static void setIsolateInitializerClassName(String initializer) {
        isolateInitializer = initializer;
    }
    
    /*=======================================================================*\
     *               Inter-isolate communication support                     *
    \*=======================================================================*/

    /**
     * Register named mailbox with the system.
     *
     * @param name the public name of the mailboz
     * @param mailbox the mailbox to use with that name.
     * @return false if the name is already registered to a mailbox.
     */
    public static boolean registerMailbox(String name, Mailbox mailbox) {
        if (registeredMailboxes == null) {
            registeredMailboxes = new SquawkHashtable();
        } else if (registeredMailboxes.get(name) != null) {
            return false;
        }
        
        registeredMailboxes.put(name, mailbox);
        return true;
    }
    
    /**
     * Unregister named mailbox with the system.
     *
     * @param name the public name of the mailboz
     * @param mailbox the mailbox to use with that name.
     */
    public static void unregisterMailbox(String name, Mailbox mailbox) {
        if (registeredMailboxes == null ||
            registeredMailboxes.get(name) == null) {
            throw new IllegalStateException("Mailbox " + name + " is not registered");
        }
        
        registeredMailboxes.remove(name);

    }
    
    public static Mailbox lookupMailbox(String name) {
        if (registeredMailboxes != null) {
            return (Mailbox)registeredMailboxes.get(name);
        }
        return null;
    }

    /**
     * Answer the time in millis until another thread is runnable. Will return
     * zero if another thread is already runnable, otherwise the delta until the
     * first thread on the timer queue becomes runnable, otherwise Long.MAX_VALUE 
     * if there are no threads on the timer queue. This method takes no account of
     * events. 
     * 
     * @return time in millis
     */
    public static long getTimeBeforeAnotherThreadIsRunnable() {
        return VMThread.getTimeBeforeAnotherThreadIsRunnable();
    }
    
    public static PeripheralRegistry getPeripheralRegistry() {
        if (peripheralRegistry == null) {
            peripheralRegistry = new PeripheralRegistry();
        }
        return peripheralRegistry;
    }
    
    /**
     * Answer an array of threads that are runnable now, in the order
     * they appear in the runnable queue.
     * 
     * @return the runnable threads
     */
    public static Thread[] getRunnableThreads() {
    	return VMThread.getRunnableThreads();
    }
    
	private static Object keyedGlobalsMutex;
	private static IntHashtable keyeGlobals;
	
	public static Object getKeyedGlobalsMutex() {
		if (keyedGlobalsMutex == null) {
			keyedGlobalsMutex = new Object();
			keyeGlobals = new IntHashtable();
		}
		return keyedGlobalsMutex;
	}

	/**
	 * Return the global registered using setGlobal with key.
	 * 
	 * All clients must wrap access to keyed globals in a synchronised block:
	 * <code>
	 * synchronized (VM.getGlobalsMutex()) {
	 *   // access/manipulate globals here ...
	 * }  
	 * </code>
	 * @param key
	 * @return the object last placed with putKeyedGlobal
	 */
	public static Object getKeyedGlobal(int key) {
		return keyeGlobals.get(key);
	}

	/**
	 * Set the global registered for key.
	 * 
	 * All clients must wrap access to keyed globals in a synchronised block:
	 * <code>
	 * synchronized (VM.getGlobalsMutex()) {
	 *   // access/manipulate globals here ...
	 * }  
	 * </code>
	 * @param key
	 * @param value
	 * @return value
	 */
	public static Object putKeyedGlobal(int key, Object value) {
		if (value == null) {
			keyeGlobals.remove(key);
		} else {
			keyeGlobals.put(key, value);
		}
		return value;
	}

}
