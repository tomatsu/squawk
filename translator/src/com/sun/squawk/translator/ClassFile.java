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

package com.sun.squawk.translator;

import java.util.Enumeration;
import java.util.Vector;
import com.sun.squawk.util.*;
import com.sun.squawk.translator.ci.*;
import com.sun.squawk.*;
import java.util.Hashtable;

/**
 * This represents a class that has not yet been loaded and linked.
 *
 */
public final class ClassFile {


    /*---------------------------------------------------------------------------*\
     *               Global constants for zero length arrays.                    *
    \*---------------------------------------------------------------------------*/

    /**
     * A zero length array of methods.
     */
    public static final Code[] NO_METHODS = {};

    /**
     * A zero length array of Objects.
     */
    public static final Object[] NO_OBJECTS = {};

    /**
     * A zero length array of Squawk bytecode methods.
     */
    public static final byte[][] NO_SUITE_METHODS = {};


    /*---------------------------------------------------------------------------*\
     *                           Fields of ClassFile                             *
    \*---------------------------------------------------------------------------*/

    /**
     * The class defined by this class file.
     */
    private final Klass definedClass;

    /**
     * The code for the virtual methods defined in this class file. The
     * elements corresponding to abstract and native methods will be null.
     */
    private Code[] virtualMethods;

    /**
     * The code for the static methods defined in this class file.
     */
    private Code[] staticMethods;

    /**
     * The constant pool of this class file.
     */
    private ConstantPool constantPool;
    
    /**
     * If true, try to do dead string elimination
     */
    private boolean safeToDoDeadStringElim;
    
    private final static String DEAD_STRING_ELIMINATION_MSG = "DEAD STRING ELIMINATION";

    /**
     * If true, try to do dead class elimination
     */
    private boolean safeToDoDeadClassElim;

    private final static String DEAD_CLASS_ELIMINATION_MSG = "DEAD CLASS ELIMINATION";

    
    private static Hashtable vm2cClasses;
    
    static {
        // @todo: FIX VM2C so it doesn't depend on constant table for strings.
        // We shouldn't do dead string elimination on vm2c classes as long as vm2c relies on 
        // the constant table for string constants.
        vm2cClasses = new Hashtable();
        vm2cClasses.put("com.sun.squawk.VM", "VM2C Class");
        vm2cClasses.put("com.sun.squawk.GarbageCollector", "VM2C Class");
        vm2cClasses.put("com.sun.squawk.Lisp2GenerationalCollector", "VM2C Class");
        vm2cClasses.put("com.sun.squawk.Lisp2GenerationalCollector$MarkingStack", "VM2C Class");
    }
    


    /*---------------------------------------------------------------------------*\
     *                               Constructor                                 *
    \*---------------------------------------------------------------------------*/

    /**
     * Creates a new <code>ClassFile</code> instance.
     *
     * @param   klass      the class defined by this class file
     */
    public ClassFile(Klass klass) {
        this.definedClass = klass;
        this.staticMethods  = NO_METHODS;
        this.virtualMethods = NO_METHODS;
        if (vm2cClasses.get(klass.getInternalName()) == null) {
            safeToDoDeadStringElim = Arg.get(Arg.DEAD_STRING_ELIMINATION).getBool();
        }
        safeToDoDeadClassElim = Arg.get(Arg.DEAD_CLASS_ELIMINATION).getBool();
    }


    /*---------------------------------------------------------------------------*\
     *                                Setters                                    *
    \*---------------------------------------------------------------------------*/

    /**
     * Sets the constant pool for this class.
     *
     * @param constantPool  the constant pool for this class
     */
    public void setConstantPool(ConstantPool constantPool) {
        Assert.that(this.constantPool == null || this.constantPool == constantPool, "cannot reset the constant pool");
        this.constantPool = constantPool;
    }

    /**
     * Sets the virtual methods for this class.
     *
     * @param  methods  the virtual methods declared by this class
     */
    public void setVirtualMethods(Code[] methods) {
        Assert.that(this.virtualMethods == NO_METHODS, "cannot reset the virtual methods");
        this.virtualMethods = methods;
    }

    /**
     * Sets the static methods for this class.
     *
     * @param  methods  the static methods declared by this class
     */
    public void setStaticMethods(Code[] methods) {
        Assert.that(this.staticMethods == NO_METHODS, "cannot reset the static methods");
        this.staticMethods = methods;
    }


    /*---------------------------------------------------------------------------*\
     *                                Getters                                    *
    \*---------------------------------------------------------------------------*/

    /**
     * Gets the class defined by this class file.
     *
     * @return  the class defined by this class file
     */
    public Klass getDefinedClass() {
        return definedClass;
    }

    /**
     * Gets the constant pool of this class.
     *
     * @return the constant pool of this class
     */
    public ConstantPool getConstantPool() {
        return constantPool;
    }
    
    int getStaticMethodCount() {
        return staticMethods.length;
    }
    
    Code getStaticMethod(int i) {
        if (i >= staticMethods.length || i < 0) {
            throw new RuntimeException("bad index: " + i + " max is: " + staticMethods.length);
        }
        
        if (staticMethods.length == 0) {
            throw new RuntimeException("bad index: " + i + " max is: " + staticMethods.length);
        }
        return staticMethods[i];
    }
    
    int getVirtualMethodCount() {
        return virtualMethods.length;
    }
    
    Code getVirtualMethod(int i) {
        return virtualMethods[i];
    }


    /*---------------------------------------------------------------------------*\
     *                    Table of constant and class references                 *
    \*---------------------------------------------------------------------------*/

    /**
     * Hashtable of constant objects.
     */
    private ArrayHashtable objectTable = new ArrayHashtable();

    /**
     * Index to the next available object table entry.
     */
    private int nextIndex;

    /**
     * Add an object to the object table.
     *
     * @param object the object to add
     */
    public void addConstantObject(Object object) {
        ObjectCounter counter = (ObjectCounter)objectTable.get(object);
        if (counter == null) {
            counter = new ObjectCounter(object, nextIndex++);
            objectTable.put(object, counter);
        } else {
            counter.inc();
        }
    }

    private static int[] INT_ARRAY_DUMMY = new int[0];

    /**
     * Sorts the object table according to the access count. Elements with the same
     * access count are sorted by class name and then by value. This guarantees a
     * deterministic sort order for object tables in the bootstrap suite.
     */
    private void sortObjectTable() {
        ObjectCounter[] list = new ObjectCounter[objectTable.size()];
        Enumeration e = objectTable.elements();
        for (int i = 0 ; i < list.length ; i++) {
            list[i] = (ObjectCounter)e.nextElement();
        }
        Arrays.sort(list, new Comparer() {
            public int compare(Object o1, Object o2) {
                if (o1 == o2) {
                    return 0;
                }
                ObjectCounter t1 = (ObjectCounter)o1;
                ObjectCounter t2 = (ObjectCounter)o2;
                if (t1.getCounter() < t2.getCounter()) {
                    return 1;
                } else if (t1.getCounter() > t2.getCounter()) {
                    return -1;
                } else {
                    o1 = t1.getObject();
                    o2 = t2.getObject();

                    // Now do ordering based on class
                    Class class1 = o1.getClass();
                    Class class2 = o2.getClass();
                    if (class1 != class2) {
                        return class1.getName().compareTo(class2.getName());
                    }

                    // Now order based on value
                    if (class1 == Klass.class) {
                        return ((Klass)o1).getName().compareTo(((Klass)o2).getName());
                    } else if (class1 == String.class) {
                        return ((String)o1).compareTo((String)o2);
                    } else if (class1 == INT_ARRAY_DUMMY.getClass()) {
                        int[] arr1 = (int[])o1;
                        int[] arr2 = (int[])o2;
                        for (int i = 0; ; ++i) {
                            if (i == arr1.length) {
                                Assert.that(arr2.length != i);
                                return -1;
                            }
                            if (i == arr2.length) {
                                return 1;
                            }
                            int diff = arr1[i] - arr2[i];
                            if (diff != 0) {
                                return diff;
                            }
                        }
                    } else {
                        // Need to add another 'else' clause if this ever occurs
                        throw Assert.shouldNotReachHere("unknown object table type: " + class1);
                    }
                }
            }
        });
//System.err.println("object table for " + definedClass.getInternalName());
        for (int i = 0 ; i < list.length ; i++) {
            ObjectCounter oc = list[i];
//System.err.println("  " + i + "\t" + oc.getCounter() + "\t" + oc.getClass() + "\t" + oc.getObject());
            oc.setIndex(i);
        }
    }

    /**
     * Get the index of an object in the object table.
     *
     * @param object the object to index
     * @param recordUse if true, count this as an "emmitted use"
     * @return the index
     * @throws java.util.NoSuchElementException if the object table does not contain <code>object</code>
     */
    public int getConstantObjectIndex(Object object, boolean recordUse) {
        ObjectCounter counter = (ObjectCounter)objectTable.get(object);
        if (counter == null) {
            throw new java.util.NoSuchElementException();
        }

        if (recordUse) {
            counter.incEmittedCounter();
		}
        return counter.getIndex();
    }

    /**
     * Force a reference to an object in the object table
     *
     * @param object the object to reference
     * @throws java.util.NoSuchElementException if the object table does not contain <code>object</code>
     */
    public void referenceConstantObject(Object object) {
        ObjectCounter counter = (ObjectCounter)objectTable.get(object);
        if (counter == null) {
            throw new java.util.NoSuchElementException();
        }

        counter.incEmittedCounter();
    }

    public void reportActualUsage() {
        boolean firstTime = true;
        Enumeration e = objectTable.elements();
        while (e.hasMoreElements()) {
            ObjectCounter counter = (ObjectCounter)e.nextElement();
            if (counter.getEmittedCounter() == 0 && !(counter.getObject() instanceof Klass)) {
                if (firstTime) {
                    System.out.println("====== Object usage in class " + definedClass);
                    firstTime = false;
                }
                System.out.println("Actual object usage different for " + counter.getObject());
                System.out.println("    expected use: " + counter.getCounter() + ", actual use: " + counter.getEmittedCounter());
            }
        }
    }


    /**
     * Gets the object table as an array of objects that have been sorted by frequency of access.
     *
     * @return the sorted object array
     */
    private Object[] getConstantObjectArray() {
        Object[] list = new Object[objectTable.size()];
        Enumeration e = objectTable.elements();
        for (int i = 0 ; i < list.length ; i++) {
            list[i] = e.nextElement();
        }
        Arrays.sort(list, new Comparer() {
            public int compare(Object o1, Object o2) {
                if (o1 == o2) {
                    return 0;
                }
                ObjectCounter t1 = (ObjectCounter)o1;
                ObjectCounter t2 = (ObjectCounter)o2;
                if (t1.getIndex() < t2.getIndex()) {
                    return -1;
                } else if (t1.getIndex() > t2.getIndex()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
//System.err.println(""+definedClass+" list = "+list.length);
        boolean firstTime = true;
        for (int i = 0 ; i < list.length ; i++) {
//System.err.println("    "+list[i]);
            ObjectCounter oc = ((ObjectCounter) list[i]);
            if (oc.getEmittedCounter() > 0) {
                list[i] = oc.getObject();
            } else {
                if (oc.getObject() instanceof String && safeToDoDeadStringElim) {
                    list[i] = DEAD_STRING_ELIMINATION_MSG;
                    if (Translator.TRACING_ENABLED && Tracer.isTracing("DSE", definedClass.getName())) {
                        if (firstTime) {
                            Tracer.traceln("Stripping objects from " + definedClass);
                            firstTime = false;
                        }
                        Tracer.traceln("    " + oc.getObject());
                    }
                } else if (oc.getObject() instanceof Klass && safeToDoDeadClassElim && safeToDoDeadStringElim) {
                    list[i] = DEAD_CLASS_ELIMINATION_MSG;
                    if (Translator.TRACING_ENABLED && Tracer.isTracing("DSE", definedClass.getName())) {
                        if (firstTime) {
                            Tracer.traceln("Stripping class from " + definedClass);
                            firstTime = false;
                        }
                        Tracer.traceln("    " + oc.getObject());
                    }
                } else {
                    //Tracer.traceln("Unused object " + oc.getObject() + " of type " + oc.getObject().getClass() + " in object table of " + definedClass);
                    list[i] = oc.getObject();
                }
            }
        }
        return list;
    }

    /*---------------------------------------------------------------------------*\
     *                       Class loading and converting                        *
    \*---------------------------------------------------------------------------*/

    /**
     * Converts a set of methods from their Java bytecode form to their
     * Squawk bytecode form.
     *
     * @param translator   the translation context
     * @param isStatic     specifies static or virtual methods
     * @param phase        the convertion phase number to perform (1 or 2) or 0 for both
     * @param bodies       {@link Vector} to insert method bodies into
     */
    private void convertMethods(Translator translator, boolean isStatic, int phase, Vector bodies) {
        Code[] methodsCode = isStatic ? staticMethods : virtualMethods;
        for (int i = 0 ; i < methodsCode.length ; i++) {
            Method method = definedClass.getMethod(i, isStatic);
            Code code = methodsCode[i];
            if (method.isAbstract() && (phase == 0 || phase == 1)) {
                translator.methodDB.recordMethod(method, 0);
            }
            
            if (!method.isHosted() && !method.isAbstract() && !method.isNative()) {
                Assert.that(code != null);
                if (phase == 0 || phase == 1) {
                    code.convert(translator, method, method.getOffset(), 1, null);
                    int size = 0;
                    if (Translator.TRACING_ENABLED) {
                        size = code.getIR().size();
                    }
                    translator.methodDB.recordMethod(method, size);
                }
                
                if (phase == 0 || phase == 2) {
                    code.convert(translator, method, method.getOffset(), 2, bodies);
                    methodsCode[i] = null; // Allow GC
                }
//if (phase == 0) {
//    VM.println("Finished converting " + method);
//    System.gc();
//}
            }
        }
    }

    /**
     * Performs a pre-pass over all of the methods in the class, generating IR for each method.
     * Changes the state of the definedClass from <code>STATE_LOADED</code> to <code>STATE_CONVERTING</code>.
     *
     * @param translator   the translation context
     * @param generateIR   generateIR now if true. otherwise wait for pass2.
     */
    void convertPhase1(Translator translator, boolean generateIR) {
        int state = definedClass.getState();
        Assert.that(state == Klass.STATE_LOADED, "class must be loaded before conversion");
        Assert.that(!definedClass.isSynthetic(), "synthetic classes should not require conversion");
        Assert.that(!definedClass.isPrimitive(), "primitive types should not require conversion");

        /*
         * Convert this type's super class first
         */
        Klass superClass = definedClass.getSuperclass();
        if (superClass != null) {
            translator.convert(superClass);
        }

        /*
         * Write trace message
         */
        if (Translator.TRACING_ENABLED && Tracer.isTracing("converting", definedClass.getName())) {
            Tracer.traceln("[converting " + definedClass + "]");
        }

        /*
         * Generate IR if doing two-pass translation
         */
        if (generateIR) {
            // This conversion first builds the IR for all the methods.
            convertMethods(translator, true, 1, null);
            convertMethods(translator, false, 1, null);
        }
        
        definedClass.changeState(Klass.STATE_CONVERTING);
    }

    /**
     * Generate squawk bytecodes for methods of this class from either the IR generated in phase1,
     * or generate IR and squawk bytecode in one pass.
     * Changes the state of the definedClass from <code>STATE_CONVERTING</code> to <code>STATE_CONVERTED</code>.
     *
     * @param translator   the translation context
     * @param doInOnePass  pass1 didn't actually generateIR, so do it all now, in one pass.
     */
    void convertPhase2(Translator translator, boolean doInOnePass) {
        int state = definedClass.getState();
        Assert.that(state == Klass.STATE_CONVERTING, "class must be loaded before conversion");
        Assert.that(!definedClass.isSynthetic(), "synthetic classes should not require conversion");
        Assert.that(!definedClass.isPrimitive(), "primitive types should not require conversion");

        /*
         * Convert this type's super class first
         */
        Klass superClass = definedClass.getSuperclass();
        if (superClass != null) {
            translator.convertPhase2(superClass);
        }
        
        Vector bodies = null;
/*if[SUITE_VERIFIER]*/
        bodies = new Vector();
/*end[SUITE_VERIFIER]*/

        try {
            if (doInOnePass) {
                // generate IR and squawk code in one pass:
                convertMethods(translator, true, 0, bodies);
                convertMethods(translator, false, 0, bodies);
            } else {
                if (Arg.get(Arg.OPTIMIZE_CONSTANT_OBJECTS).getBool()) {
                    sortObjectTable();
                }
                
                // Now generate squawk code from IR.
                convertMethods(translator, true, 2, bodies);
                convertMethods(translator, false, 2, bodies);
//                if (Arg.get(Arg.VERBOSE).getBool()) {
//                    reportActualUsage();
//                }
            }
        } catch (NoClassDefFoundError e) {
            definedClass.changeState(Klass.STATE_ERROR);
            throw e;
        }
        Object[] objectTable = getConstantObjectArray();
        definedClass.setObjectTable(objectTable);

/*if[SUITE_VERIFIER]*/
        for (int i = 0; i < bodies.size(); i++) {
            MethodBody body = (MethodBody)bodies.elementAt(i);
            if (body != null) {
                new com.sun.squawk.translator.ir.verifier.Verifier().verify(body);
            }
        }
/*end[SUITE_VERIFIER]*/

        definedClass.changeState(Klass.STATE_CONVERTED);

        /*
         * Write trace message
         */
        if (Translator.TRACING_ENABLED && Tracer.isTracing("converting", definedClass.getName())) {
            Tracer.traceln("[converted " + definedClass + "]");
        }
    }

}

/**
 * Class used to keep track of the number of times a constant object is referenced in a class.
 */
final class ObjectCounter {

    /**
     * The object being counted.
     */
    private Object object;

    /**
     * The index of the object in the object table.
     */
    private int index;

    /**
     * Use counter.
     */
    private int counter;

    /**
     * Use in emmitted code counter.
     */
    private int emittedUseCounter;

    /**
     * Constructor.
     *
     * @param index the initial index
     */
    public ObjectCounter(Object object, int index) {
        this.object = object;
        this.index  = index;
		this.counter = 1;
		this.emittedUseCounter = 0;
    }

    /**
     * Get the object being counted.
     *
     * @return the object
     */
    public Object getObject() {
        return object;
    }

    /**
     * Get the index.
     *
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Set the index.
     *
     * @param index the index
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * Add 1 to the counter.
     */
    public void inc() {
        counter++;
    }

    /**
     * Get the counter value.
     *
     * @return the value
     */
    public int getCounter() {
        return counter;
    }
    
     /**
     * Add 1 to the counter.
     */
    public void incEmittedCounter() {
        emittedUseCounter++;
    }

    /**
     * Get the counter value.
     *
     * @return the value
     */
    public int getEmittedCounter() {
        return emittedUseCounter;
    }

    /**
     * Gets a String representation.
     *
     * @return the string
     */
    public final String toString() {
        return "index = "+index+" counter = "+counter+" object = "+object;
    }
}
