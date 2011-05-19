
/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */

package com.sun.squawk.translator;

import com.sun.squawk.ExceptionHandler;
import com.sun.squawk.Klass;
import com.sun.squawk.Suite;
import com.sun.squawk.VM;
import com.sun.squawk.util.*;
import com.sun.squawk.Method;
import com.sun.squawk.Field;
import com.sun.squawk.Modifier;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import com.sun.squawk.translator.ir.ReferenceRecordingVisitor;
import com.sun.squawk.translator.ir.IR;
import com.sun.squawk.translator.ir.Instruction;


/**
 * Detect and remove unused classes.
 *
 * After calling computeClassesUsed(), the translator can use isMarked() to determine unused classes.
 *
 * @author dw29446
 */
public class DeadClassEliminator {

    private Translator translator;
    private Hashtable referencedClasses = new Hashtable();

    public boolean isMarked(Klass klass) {
        return referencedClasses.get(klass) != null;
    }

    /**
     * Mark this ref, and refs to superclass, interfaces, and component type (if an array).
     *
     * @return true if this is was unmarked
     */
    public boolean markClass(Klass klass) {
        if (klass != null && !isMarked(klass)) {
            referencedClasses.put(klass, klass);
            return true;
        }
        return false;
    }

    /** Creates a new instance of DeadClassEliminator */
    public DeadClassEliminator(Translator translator) {
        this.translator = translator;
    }

    /*---------------------------------------------------------------------------*\
     *                         Track unused classes                              *
    \*---------------------------------------------------------------------------*/

    private static Hashtable systemRoots = new Hashtable();
        private static String[] systemRootsArray = {
            "com.sun.squawk.VM",
            "com.sun.squawk.ResourceFile",
            "com.sun.squawk.ManifestProperty",
            "com.sun.squawk.Suite",
            "com.sun.squawk.KlassMetadata",
            "com.sun.squawk.KlassMetadata$Full",
            "com.sun.squawk.MethodMetadata",
            "com.sun.squawk.FullMethodMetadata",
            "com.sun.squawk.vm.FieldOffsets",
            "com.sun.squawk.vm.MethodOffsets",
            "com.sun.squawk.Klass",
            "com.sun.squawk.StringOfBytes"
        };

    static {
        for (int i = 0; i < systemRootsArray.length; i++) {
            String name = systemRootsArray[i];
            systemRoots.put(name, name);
        }
    }

    /**
     * Is this a class that might be called by the system through some basic mechanism,
     * such as "main", called by interpreter, etc.
     *
     * @param klass the klass
     * @return true if this is a basic root
     */
    private static boolean isBasicRoot(Klass klass) {

        if (klass.getSystemID() >= 0) {             // system klasses are basic.
            return true;
        }

        if (klass.isSynthetic()) {
            return true;
        }

        if (klass.hasMain()) {
            return true;
        }

        // @TODO: This is probably too general!
    /*    if (klass.hasDefaultConstructor()) {
            return true;
        } */

        if (systemRoots.get(klass.getInternalName()) != null) {
            return true;
        }

        return false;
    }


    /**
     * Given a SquawkVector of Strings, return a sorted array of those strings.
     */
    private String[] sortStringVector(SquawkVector v) {
        String[] tmp = new String[v.size()];
        v.copyInto(tmp);
        Arrays.sort(tmp, new Comparer() {
            public int compare(Object a, Object b) {
                String astr = (String)a;
                String bstr = (String)b;

                return astr.compareTo(bstr);
            }
        });
        return tmp;
    }

    /**
     * Given a SquawkVector of Strings, print it sorted
     *
     * @param v a SquawkVector of Strings
     * @param prefix string to print before element.
     */
    private void printVectorSorted(SquawkVector v, String prefix) {
        String[] results = sortStringVector(v);
        for (int i = 0; i < results.length; i++) {
            Tracer.trace(prefix);
            Tracer.traceln(results[i]);
        }
    }


    /**
     * Given the classes's access and the suite type,
     * determine the final accessibility of the class outside of this suite.
     *
     * @param klass the klass
     * @return true if an external suite could possibly access this klass.
     */
    public boolean isExternallyVisible(Klass klass) {
        int modifiers = klass.getModifiers();
        int suiteType = translator.getSuiteType();

        if (VM.isInternal(klass)) {
            // if the symbol wasn't marked as "export" or "dynamic" in the library.proprties file,
            // then there is no way that this is externally visible.
            return false;
        } else {
            // It's declared externally visible, but is it really?
            Assert.that(Modifier.isPackagePrivate(modifiers) || Modifier.isProtected(modifiers) || Modifier.isPublic(modifiers));

            switch (suiteType) {
                case Suite.APPLICATION:
                    return VM.isDynamic(klass);

                case Suite.LIBRARY:
                    // what can we do here
                    return true;
                default:
                    // extendable and debuggable suites leave all symbols externally visible.
                    return true;
            }
        }
    }

    /**
     * Use a mark stack to avoid deep recursion.
     */
    SquawkVector markStack;

    void shallowMark(Klass klass) {
        if (klass != null && !isMarked(klass)) {
            markStack.addElement(klass);
        }
    }

    private void scanMethod(ClassFile classFile, Code code, Method m) {
        boolean dontExpectCode = m.isHosted() || m.isAbstract() || m.isNative();
        Assert.always(dontExpectCode || code != null, "code for method " + m);
        Assert.always(m != null, "method for code " + code);

        if (code != null && m != null
                && (!Arg.get(Arg.DEAD_METHOD_ELIMINATION).getBool() ||
                    translator.dme.isMarkedUsed(m))) {
            shallowMark(m.getReturnType());
            Klass[] parameters = m.getParameterTypes();
            for (int j = 0; j < parameters.length; j++) {
                shallowMark(parameters[j]);
            }
            ClassReferenceRecordingVisitor visitor = new ClassReferenceRecordingVisitor(this);
            IR ir = code.getIR();
            for (Instruction instruction = ir.getHead() ; instruction != null ; instruction = instruction.getNext()) {
                instruction.visit(visitor);
            }

            ExceptionHandler[] exceptionHandlers = code.getCodeParser().getExceptionHandlers();
            for (int i = 0; i < exceptionHandlers.length; i++) {
                Klass handlerklass = exceptionHandlers[i].getKlass();
                shallowMark(handlerklass);
            }
        }
    }

    /**
     * Mark the class, and scan methods for references to other classes.
     * Push new references onto the markStack.
     */
    public void scanClassMethods(Klass klass) {
        ClassFile classFile = translator.lookupClassFile(klass);
        if (classFile == null) {
            return;
        }

        for (int i = 0; i < classFile.getStaticMethodCount(); i++) {
            scanMethod(classFile, classFile.getStaticMethod(i), klass.getMethod(i, true));
        }
        for (int i = 0; i < classFile.getVirtualMethodCount(); i++) {
            scanMethod(classFile, classFile.getVirtualMethod(i), klass.getMethod(i, false));
        }
    }

    /**
     * Mark the class, and scan methods for references to other classes.
     * Push new references onto the markStack.
     */
    public void scanClassFields(Klass klass) {
        for (int i = 0; i < klass.getFieldCount(true); i++) {
            shallowMark(klass.getField(i, true).getType());
        }
        for (int i = 0; i < klass.getFieldCount(false); i++) {
            shallowMark(klass.getField(i, false).getType());
        }
    }

    /**
     * Mark the class, and scan methods for references to other classes.
     * Push new references onto the markStack.
     */
    public void scanClassDeep(Klass klass) {
        if (markClass(klass)) {
            shallowMark(klass.getSuperclass());
            shallowMark(klass.getComponentType());
            Klass[] interfaces = klass.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                shallowMark(interfaces[i]);
            }
            if (!klass.isSynthetic()) {
                scanClassMethods(klass);
            }
            scanClassFields(klass);

//            for (int i = 0; i < klass.getObjectCount(); i++) {
//                Object obj = klass.getObject(i);
//                if (obj instanceof Klass) {
//                    Klass refKlass = (Klass)obj;
//                    if (!isMarked(refKlass)) {
//                        System.out.println("******** LATE FIND: " + obj);
//                    }
//                    shallowMark(refKlass);
//                }
//            }
        }
    }

    public void computeClassesUsed() {
        boolean trace = (Translator.TRACING_ENABLED && Tracer.isTracing("DCE")) || VM.isVeryVerbose();
        //Enumeration e;

        SquawkVector foundClasses = new SquawkVector(); // used for tracing
        SquawkVector unusedClasses = new SquawkVector(); // used to delete classes during stripping...

        markStack = new SquawkVector(); // stack of classes to be marked
        Suite suite = translator.getSuite();

        // Preserve classes that might be called autmatically by system, beyond the powers of analysis:
        foundClasses.removeAllElements();
        for (int cno = 0; cno < suite.getClassCount(); cno++) {
            Klass klass = suite.getKlass(cno);
            if (klass != null) {
                if (isBasicRoot(klass)) {
                    if (trace) {
                        foundClasses.addElement(klass.toString());
                    }
                    scanClassDeep(klass);
                }
            }
        }
        if (trace && foundClasses.size() != 0) {
            Tracer.traceln("[translator DCE: ==== System roots:  " + foundClasses.size() + " =====");
            printVectorSorted(foundClasses, "System root: ");
        }

        // Preserve all externally visible classes
        foundClasses.removeAllElements();
        for (int cno = 0; cno < suite.getClassCount(); cno++) {
            Klass klass = suite.getKlass(cno);
            if (klass != null) {
                if (isExternallyVisible(klass) && !isMarked(klass)) {
                    if (trace) {
                        foundClasses.addElement(klass.toString());
                    }
                    scanClassDeep(klass);
                }
            }
        }
        if (trace && foundClasses.size() != 0) {
            Tracer.traceln("[translator DCE: ==== Visible roots:  " + foundClasses.size() + " =====");
            printVectorSorted(foundClasses, "Visible root: ");
        }

        // Now mark all
        int len;
        while ((len = markStack.size()) > 0) {
            Klass klass = (Klass)markStack.lastElement();
            markStack.removeElementAt(len - 1);
            scanClassDeep(klass);
        }

        // report unused classes:
        foundClasses.removeAllElements();
        for (int cno = 0; cno < suite.getClassCount(); cno++) {
            Klass klass = suite.getKlass(cno);
            if (klass != null) {
                if (!isMarked(klass)) {
                    if (trace) {
                        foundClasses.addElement(klass.toString());
                        if (VM.isCrossSuitePrivate(klass)) {
                            System.out.println(klass  + " is internal, why are we deleting???");
                        }
                    }
                    unusedClasses.addElement(klass);
                }
            }
        }
        if (trace || VM.isVeryVerbose()) {
            if (foundClasses.size() != 0) {
                Tracer.traceln("[translator DCE: ==== Unused classes:  " + foundClasses.size() + " (used classes: " + referencedClasses.size() + ") =====");
                printVectorSorted(foundClasses, "    ");
            }
        }
        Klass[] unusedKlasses = new Klass[unusedClasses.size()];
        unusedClasses.copyInto(unusedKlasses);
        suite.setUnusedClasses(unusedKlasses);
    }

}

class ExampleDeadClass {
    ExampleDeadClass(int b) {}
}

class ClassReferenceRecordingVisitor extends ReferenceRecordingVisitor {

    private DeadClassEliminator dce;

    ClassReferenceRecordingVisitor(DeadClassEliminator dce) {
        this.dce = dce;
    }

    protected void recordKlass(Klass klass) {
        dce.shallowMark(klass);
    }

    protected void recordMethod(Method method) {
        dce.shallowMark(method.getDefiningClass());

    }

    protected void recordField(Field field) {
        dce.shallowMark(field.getDefiningClass());
        dce.shallowMark(field.getType());
    }

}