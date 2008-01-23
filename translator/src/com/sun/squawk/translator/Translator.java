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

import java.io.*;

import javax.microedition.io.Connector;
import java.util.Hashtable;
import java.util.Stack;

import com.sun.squawk.util.Assert;
//import com.sun.squawk.SuiteCreator.*;
import com.sun.squawk.io.connections.*;
import com.sun.squawk.translator.ci.*;
import com.sun.squawk.util.ComputationTimer;
import com.sun.squawk.util.Tracer;
import com.sun.squawk.*;

/**
 * The Translator class presents functionality for loading and linking
 * classes from class files (possibly bundled in jar files) into a
 * {@link Suite}.<p>
 *
 */
public final class Translator implements TranslatorInterface {
    
    public final static boolean TRACING_ENABLED = true;

    /*---------------------------------------------------------------------------*\
     *                      Translator options/optimization flags                *
    \*---------------------------------------------------------------------------*/

    /**
     * Property that turns on help about other translator properties.
     */
    private final static String HELP_PROPERTY = "translator.help";

    /**
     * Set to true if the translator should use as much memory as necessary to do a best effort translation.
     *  (This used to be based on VM.isHosted().
     */
    private final static String OPTIMIZECONSTANTOBJECTS_PROPERTY = "translator.optimizeConstantObjects";
    private final static boolean OPTIMIZECONSTANTOBJECTS = true;
    private static boolean optimizeConstantObjects = OPTIMIZECONSTANTOBJECTS;

    /**
     * Returns true if the translator should use as much memory as necessary to do a best effort translation.
     *  (This used to be based on VM.isHosted().
     */
    public static boolean shouldOptimizeConstantObjects() {
        return optimizeConstantObjects;
    }
    
    /**
     * Set to true if the translator should delete uncalled methods
     */
    private final static String DEADMETHODELIMINATION_PROPERTY = "translator.deadMethodElimination";
    private final static boolean DEADMETHODELIMINATION = true;
    private static boolean deadMethodElimination = DEADMETHODELIMINATION;

    /**
     * Returns true if the translator should eliminate dead methods
     */
    public static boolean shouldDoDeadMethodElimination() {
        return deadMethodElimination;
    }

    /**
     * Set to true if the translator should delete uncalled private constructors
     */
    private final static String DELETEUNUSEDPRIVATECONSTRUCTORS_PROPERTY = "translator.deleteUnusedPrivateConstructors";
    private final static boolean DELETEUNUSEDPRIVATECONSTRUCTORS = true;
    private static boolean deleteUnusedPrivateConstructors = DELETEUNUSEDPRIVATECONSTRUCTORS;

    /**
     * Returns true if the translator should eliminate uncalled private constructors
     */
    public static boolean shouldDeleteUnusedPrivateConstructors() {
        return deleteUnusedPrivateConstructors;
    }
    
    /** If true, start deleting USED methods, to test error handling (Should throw abstract method error, or exit with fatalVMError.
     */
    public final static boolean FORCE_DME_ERRORS = false;
    
     /**
     * Set to true if the translator should print verbose progress
     */
    private final static String VERBOSE_PROPERTY = "translator.verbose";
    private final static boolean VERBOSE = false;
    private static boolean verbose = VERBOSE;
    
    protected final Stack lastClassNameStack = new Stack();;
    
    /**
     * Returns true if the translator should print verbose progress
     */
    public static boolean verbose() {
        return verbose;
    }
    
    private int progressCounter = 0;
    
    /**
     * Returns true if the translator should print verbose progress
     */
    public void traceProgress() {
        if (verbose) {
            progressCounter++;
            Tracer.trace(".");
            if (progressCounter % 40 == 0) {
                Tracer.trace("\n");
            }
        }
    }
    
    /*---------------------------------------------------------------------------*\
     *                     Implementation of TranslatorInterface                 *
    \*---------------------------------------------------------------------------*/
    
    public final static int BY_METHOD = 1;
    public final static int BY_CLASS = 2;
    public final static int BY_SUITE = 3;
    public final static int BY_TRANSLATION = 4;
    
    /**
     * Should we translate a method at a time, class at a time, suite at a time, or bundle of suites at a time?
     */
    private int translationStrategy = 0;

    /**
     * The suite context for the currently open translator.
     */
    private Suite suite;

    /**
     * The suite type of the final suite. Note that when stripping a suite, we actually translate the unstriped suite.
     * This is set in close().
     */
    private int suiteType;

    /**
     * The loader used to locate and load class files.
     */
    private ClasspathConnection classPath;

    /**
     * The database of methods, callers, overrides, etc.
     */
    public MethodDB methodDB;
    
    /**
     * A DeadMethodEliminator is created in close() if we do dead method elimination.
     */
     DeadMethodEliminator dme;
     
    /**
     * Parses the system property named <code>name</code> as a boolean. Use <code>defaultValue</code> if
     * there is no system property by that name, or if the value is not a boolean.
     *
     * @param name  the name pf the property.
     * @param defaultValue the default value to use.
     * @return the specified property value or the default value.
     */
    private boolean getBooleanProperty(String name, boolean defaultValue) {
        String result = System.getProperty(name);

        if (result != null) {
            result = result.toLowerCase();
            if (result.equals("true")) {
                return true;
            } else if (result.equals("false")) {
                return false;
            } else {
                System.err.println("Illformed boolean value " + result + "for translator property " + name + ". Using default value " + defaultValue);
                // fall through to pick up default
            }
        }
        return defaultValue;
    }

    /**
     * Parses the system property named <code>name</code> as an int. Use <code>defaultValue</code> if
     * there is no system property by that name, or if the value is not an int.
     *
     * @param name  the name pf the property.
     * @param defaultValue the default value to use.
     * @return the specified property value or the default value.
     */
    private int getIntProperty(String name, int defaultValue) {
        String result = System.getProperty(name);

        if (result != null) {
            return Integer.parseInt(result);
        }

        return defaultValue;
    }

    /**
     * Read translator properties and set corresponding options.
     */
    private void setOptions() {
        boolean showHelp         = getBooleanProperty(HELP_PROPERTY,  false);
        optimizeConstantObjects  = getBooleanProperty(OPTIMIZECONSTANTOBJECTS_PROPERTY,  OPTIMIZECONSTANTOBJECTS);
	    deadMethodElimination    = getBooleanProperty(DEADMETHODELIMINATION_PROPERTY, DEADMETHODELIMINATION);
        verbose                       = getBooleanProperty(VERBOSE_PROPERTY, VERBOSE);
        verbose |= VM.isVerbose() | VM.isVeryVerbose() | Tracer.isTracing("converting");
        
        if (showHelp || verbose || VM.isVeryVerbose()) {
            VM.println("Translator properties and current values:");
            VM.println("    " + HELP_PROPERTY                                + "=" + showHelp);
            VM.println("    " + VERBOSE_PROPERTY                           + "=" + verbose);
            if (!getBooleanProperty(VERBOSE_PROPERTY, VERBOSE)) {
                VM.println("        " + VERBOSE_PROPERTY  + " implicitly set by squawk -verbose, -veryverbose, or -traceconverting");
            }
            VM.println("    " + OPTIMIZECONSTANTOBJECTS_PROPERTY  + "=" + optimizeConstantObjects);
            VM.println("    " + DEADMETHODELIMINATION_PROPERTY    + "=" + deadMethodElimination);
        }
        
        if (deadMethodElimination) {
            translationStrategy = BY_SUITE;
        } else if (optimizeConstantObjects) {
            translationStrategy = BY_CLASS;
        } else {
            translationStrategy = BY_METHOD;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void open(Suite suite, String classPath) {
        this.suiteType = -9999; // This is set for real in close().
        this.suite = suite;
        this.classFiles = new Hashtable();
        setOptions();
        try {
            String url = "classpath://" +  classPath;
            this.classPath = (ClasspathConnection)Connector.open(url);
        } catch (IOException ioe) {
        	throw new LinkageError("Error while setting class path from '"+ classPath + "': " + ioe);
        }
        methodDB = new MethodDB(this);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isValidClassName(String name) {
        return name.indexOf('/') == -1 && ConstantPool.isLegalName(name.replace('.', '/'), ConstantPool.ValidNameFormat.CLASS);
    }

    /**
     * {@inheritDoc}
     */
    public void load(Klass klass) {
        Assert.that(VM.isHosted() || VM.getCurrentIsolate().getLeafSuite() == suite);
        int state = klass.getState();
        if (state < Klass.STATE_LOADED) {
            if (klass.isArray()) {
                load(klass.getComponentType());
            } else {
            	lastClassNameStack.push(klass.getName());
                ClassFile classFile = getClassFile(klass);
                load(classFile);
                lastClassNameStack.pop();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void convert(Klass klass) {
    	lastClassNameStack.push(klass.getName());
        int state = klass.getState();
        if (state < Klass.STATE_CONVERTING) {
            if (klass.isArray()) {
                convert(Klass.OBJECT);
                klass.changeState(Klass.STATE_CONVERTED);
            } else {
                traceProgress();
                
                ClassFile classFile = getClassFile(klass);
                classFile.convertPhase1(this, translationStrategy != BY_METHOD);
                if (klass.hasGlobalStatics()) {
                    // record globals now.
                    recordGlobalStatics(klass);
                }
                
                if (translationStrategy == BY_METHOD || translationStrategy == BY_CLASS) {
                    // if NOT inlining, then generate squawk code now.
                    classFile.convertPhase2(this, translationStrategy == BY_METHOD);
                    classFiles.remove(klass.getName());
                }       
            }
        }
        lastClassNameStack.pop();
    }
    
    private void recordGlobalStatics(Klass klass) {
        
    }
    
    /**
     * Generate squawk code for methods of <code>klass</code> when doing whole-suite translation (inlining, etc.)
     *
     * @param   klass  the klass to generate code for
     */
    void convertPhase2(Klass klass) {
        Assert.that(translationStrategy != BY_METHOD);
        convert(klass);
        if (klass.getState() < Klass.STATE_CONVERTED) {
            if (!VM.isVerbose()) { // "squawk -verbose" will show the class names as it finishes loading them, which is progress enough
                traceProgress();
            }
        	lastClassNameStack.push(klass.getName());
            ClassFile classFile = getClassFile(klass);
        	classFile.convertPhase2(this, false);
            classFiles.remove(klass.getName());
            lastClassNameStack.pop();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void close(int suiteType) throws NoClassDefFoundError {
        long time = 0;
        this.suiteType = suiteType;
        
        if (verbose()) {
            Tracer.trace("[Translator: computing closure...");
            time = System.currentTimeMillis();
        }
        
        computeClosure();
        
        if (translationStrategy == BY_SUITE || translationStrategy == BY_TRANSLATION) {
            if (verbose()) {
                time = System.currentTimeMillis() - time;
                Tracer.traceln(time + "ms.]");
                Tracer.trace("[Translator: whole-suite optimizing and inlining...");
                time = System.currentTimeMillis();
            }
            // bytecode optimizations and inlining go here
            
            if (Translator.shouldDoDeadMethodElimination()) {
                dme = new DeadMethodEliminator(this);
                dme.computeMethodsUsed();
            }
            
            if (verbose()) {
                time = System.currentTimeMillis() - time;
                Tracer.traceln(time + "ms.]");
                Tracer.trace("[Translator: phase2...");
                time = System.currentTimeMillis();
            }
            
            for (int cno = 0; cno < suite.getClassCount(); cno++) {
	            convertPhase2(suite.getKlass(cno));
            }
        }
        classFiles.clear();
        
        if (verbose()) {
            time = System.currentTimeMillis() - time;
            Tracer.traceln(time + "ms.]");
        }
        Assert.always(lastClassNameStack.empty());
    }
    
   /**
     * {@inheritDoc}
     */
    public void printTraceFlags(PrintStream out) {
        if (Translator.TRACING_ENABLED) {
            out.println("    -traceloading         trace class loading");
            out.println("    -traceconverting      trace method conversion (includes -traceloading)");
            out.println("    -tracejvmverifier     trace verification of JVM/CLDC bytecodes");
            out.println("    -traceemitter         trace Squawk bytecode emitter");
            out.println("    -tracesquawkverifier  trace verification of Squawk bytecodes");
            out.println("    -traceclassinfo       trace loading of class meta-info (i.e. implemented");
            out.println("                          interfaces, field meta-info & method meta-info)");
            out.println("    -traceclassfile       trace low-level class file elements");
            out.println("    -traceir0             trace the IR built from the JVM bytecodes");
            out.println("    -traceir1             trace optimized IR with JVM bytcode offsets");
            out.println("    -traceir2             trace optimized IR with Squawk bytcode offsets");
            out.println("    -tracemethods         trace emitted Squawk bytecode methods");
            out.println("    -tracemaps            trace stackmaps read from class files");
            out.println("    -traceDME             trace trace Dead Method Elimination");
            out.println("    -tracecallgraph       print table of methods and callees (only when doing DME)");
            out.println("    -tracefilter:<string> filter trace with simple string filter");
        }
    }
    private void printOne(PrintStream out, String baseName, String rest, boolean asParameters) {
        out.print("    -");
        if (!asParameters) {
            out.print("Dtranslator.");
        }
        out.print(baseName);
         if (asParameters) {
            out.print(":");
        } else {
            out.print("=");
        }
        out.println(rest);
    }
    
    /**
     * {@inheritDoc}
     */
    public void printOptionProperties(PrintStream out, boolean asParameters) {
        printOne(out, "optimizeConstantObjects", "<bool> Reorder class objects to allow small indexes for common objects.\n" +
                "                          <bool> must be true or false (default is true)", asParameters);
        printOne(out, "deadMethodElimination", "<bool> Remove uncalled (and uncallable) methods.\n." +
                "                         <bool> must be true or false (default is true)", asParameters);
    }
    
    /**
     * If "arg" match the  option with the basic name "baseName" then set the option to 
     * the appropriate value, and return true.
     *
     * @param arg the argument on the command line "-foo:value"
     * @param baseName the basic name of the option: "foo"
     * @return true if "arg" is the option that matches baseName.
     */
    private boolean processOptionAs(String arg, String baseName) {
        String optionStr = "-" + baseName + ":";
        if (arg.startsWith(optionStr)) {
            String val = arg.substring(optionStr.length()).toUpperCase();
            VM.setProperty("translator." + baseName, val);
            return true;
        }
        return false;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean processOptionProperties(String arg) {
        return arg.startsWith("-") &&
                (processOptionAs(arg, "optimizeConstantObjects")
                || processOptionAs(arg, "deadMethodElimination"));
    }

    /*---------------------------------------------------------------------------*\
     *                                   Misc                                    *
    \*---------------------------------------------------------------------------*/

    public Suite getSuite() {
        return suite;
    }
    
    public int getSuiteType() {
        return suiteType;
    }
    
    public int getTranslationStrategy() {
        return translationStrategy;
    }

    /*---------------------------------------------------------------------------*\
     *                    Class lookup, creation and interning                   *
    \*---------------------------------------------------------------------------*/

    /**
     * The table of class files for classes.
     */
    private Hashtable classFiles;

    /**
     * Gets the array dimensionality indicated by a given class name.
     *
     * @return  the number of leading '['s in <code>name</code>
     */
    public static int countArrayDimensions(String name) {
        int dimensions = 0;
        while (name.charAt(dimensions) == '[') {
            dimensions++;
        }
        return dimensions;
    }

    /**
     * Gets the class file corresponding to a given instance class. The
     * <code>klass</code> must not yet be converted and it must not be a
     * {@link Klass#isSynthetic() synthetic} class.
     *
     * @param   klass  the instance class for which a class file is requested
     * @return  the class file for <code>klass</code>
     */
    ClassFile getClassFile(Klass klass) {
        Assert.that(!klass.isSynthetic(), "synthethic class has no classfile");
        String name = klass.getName();
        ClassFile classFile = (ClassFile)classFiles.get(name);
        if (classFile == null) {
            classFile = new ClassFile(klass);
            classFiles.put(name, classFile);
        }
        return classFile;
    }

	public String getLastClassName() {
		if (lastClassNameStack.empty()) {
			return null;
		}
		return (String) lastClassNameStack.peek();
	}

    /**
     * Gets the connection that is used to find the class files.
     *
     * @return  the connection that is used to find the class files
     */
    public ClasspathConnection getClassPath() {
        return classPath;
    }

    /*---------------------------------------------------------------------------*\
     *                     Class loading and resolution                          *
    \*---------------------------------------------------------------------------*/

    /**
     * Loads a class's defintion from a class file.
     *
     * @param  classFile  the class file definition to load
     */
    private void load(final ClassFile classFile) {
        final ClassFileLoader loader = new ClassFileLoader(this);
        ComputationTimer.time("loading", new ComputationTimer.Computation() {
            public Object run() {
                loader.load(classFile);
                return null;
            }
        });
    }

    /**
     * Load and converts the closure of classes in the current suite.
     */
    public void computeClosure() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int cno = 0 ; cno < suite.getClassCount() ; cno++) {
                Klass klass = suite.getKlass(cno);
                Assert.always(klass != null);
                if (klass.getState() < Klass.STATE_LOADED) {
                    load(klass);
                    changed = true;
                }
                if (klass.getState() < Klass.STATE_CONVERTING) {
                    convert(klass);
                    changed = true;
                }
            }
        }
    }
    
    public byte [] getResourceData(String name) {
        Assert.that(VM.isHosted() || VM.getCurrentIsolate().getLeafSuite() == suite);
        try {
            byte[] bytes = classPath.getBytes(name);
            ResourceFile resourceFile = new ResourceFile(name, bytes);
            suite.installResource(resourceFile);
            return bytes;
        } catch (IOException e) {
            return null;
        }
    }
     
    /*---------------------------------------------------------------------------*\
     *                           Reversable parameters                           *
    \*---------------------------------------------------------------------------*/

    public static final boolean REVERSE_PARAMETERS = /*VAL*/true/*REVERSE_PARAMETERS*/;

    /*---------------------------------------------------------------------------*\
     *                          Debugging                                         *
    \*---------------------------------------------------------------------------*/

    public static void trace(MethodBody mb) {
        if (Translator.TRACING_ENABLED ) {
            Method method = mb.getDefiningMethod();
            Tracer.traceln("++++ Method for " + method + " ++++");
            new MethodBodyTracer(mb).traceAll();
            Tracer.traceln("---- Method for " + method + " ----");
        }
    }

}
