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

import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.*;

import com.sun.squawk.io.connections.ClasspathConnection;
import com.sun.squawk.security.HexEncoding;
import com.sun.squawk.util.*;

public class SuiteCreator {

    /**
     * Type of suite to create. This controls how much of the symbolic information is retained
     * in the suite when it is closed.
     */
    private int suiteType = Suite.APPLICATION;

    /**
     * Specify if the output should be big or little endian
     */
    private boolean bigEndian = VM.isBigEndian();
    
    /** use large offset to get past other well known exit values */
    private final static int IGNORE_CLASS_OFFSET = 100000;
    
    private final static String TRANSLATOR_SUITE_URI = "file://translator.suite";
    private final static String TRANSLATOR_CLASS_NAME = "com.sun.squawk.translator.Translator";
    
    private TranslatorInterface outerTranslator;

    /**
     * Prints the usage message.
     *
     * @param  errMsg  an optional error message
     */
    final void usage(String errMsg, String[] args) {
        PrintStream out = System.out;
    	for (int i=0; i < args.length; i++) {
    		out.print("Arg ");
    		out.print(i);
    		out.print(':');
    		out.println(args[i]);
    	}
        if (errMsg != null) {
            out.println(errMsg);
        }
        out.print("Usage: ");
        out.print(GC.getKlass(this).getName());
        out.print(
                " [-options] suite_name [prefixes...]\n " +
                "where options include:\n" +
                "\n" +
                "    -cp:<directories and jar/zip files separated by ':' (Unix) or ';' (Windows)>\n" +
                "                          paths where classes and suites can be found (required)\n" +
                "    -parent:<name>        name of parent suite (default is the bootstrap suite)\n" +
                "    -translator:<uri>[#<name>] the URI of a suite containing the class <name> which implements\n" +
                "                          (default=file://translator.suite#com.sun.squawk.translator.Translator)\n");

        
        if (outerTranslator != null) {
            outerTranslator.printOptionProperties(out, true);
            outerTranslator.printTraceFlags(out);
        }
       
        out.print(
                "    -strip:<t>            strip symbolic information according to <t>:\n" +
                "                            'd' - debug: retain all symbolic info\n" +
                "                            'a' - application (default): discard all symbolic info\n" +
                "                            'l' - library: discard symbolic info\n" +
                "                                  for private/package-private fields and methods\n" +
                "                            'e' - extendable library: discard symbolic info\n" +
                "                                  for private fields and methods\n" +
                "    -lnt                  retain line number tables\n" +
                "    -lvt                  retain local variable tables\n" +
                "    -endian:<value>       endianess ('big' or 'little') for generated suite (default="); 
        out.println(VM.isBigEndian() ? "'big')" : "'little')");
        out.print(
                "    -verbose, -v          provide more output while running\n" +
                "    -debug                start the suite creator in the debugger\n" +
                "    -help                 show this help message and exit\n" +
                "\n" +
                "Note: If no prefixes are specified, then all the classes found on the\n" +
                "      class path are used."
                );
    }
    
    /**
     * Scan through ALL of the arguments, looking for the translator ones. 
     * Returns the given translator args, or the default ones.
     *
     * @param args all of the args
     * @return a two element array of suite uri and classname.
     */
    private static String[] scanForTranslatorArgs(String[] args) {
        String[] result = {
            TRANSLATOR_SUITE_URI,
            TRANSLATOR_CLASS_NAME
        };
        
        for (int argnum = 0; argnum < args.length; argnum++) {
            String arg = args[argnum];
            if (arg.startsWith("-translator:")) {
                String t = arg.substring("-translator:".length());
                int hash = t.indexOf('#');
                if (hash != -1) {
                    result[0] = t.substring(0, hash);
                    result[1]  = t.substring(hash + 1);
                } else {
                    result[0]  = t;
                }
                return result;
            }
        }
        
        return result;
    }

    /**
     * Processes the class prefixes to build the set of classes on the class path that must be loaded.
     *
     * @param   classPath the path to search for classes
     * @param   args      the command line arguments specifiying class name prefixes
     * @param   index     the index in <code>args</code> where the prefixes begin
     * @param   classes   the vector to which the matching class names will be added
     * @param   resourceNames   the vector t owhich the matching resource names will be added
     */
    void processClassPrefixes(String classPath, String[] args, int index, Vector classes, Vector resourceNames) {
        boolean all = (args.length == index);
        try {
            ClasspathConnection cp = (ClasspathConnection)Connector.open("classpath://" + classPath);
            DataInputStream dis = new DataInputStream(cp.openInputStream("//"));
            try {
                for (;;) {
                    String name = dis.readUTF();
                    String className = name;
                    boolean isClass = false;
                    if (className.endsWith(".class")) {
                        className = className.substring(0, name.length() - ".class".length());
                        int slashIndex = className.lastIndexOf('/');
                        boolean isValidClassName = true;
                        if (slashIndex != -1) {
                            String fileNamePart = className.substring(slashIndex + 1);
                            // Make sure the file name part is a valid class name, if not then it must be a resource
                            if (fileNamePart.indexOf('.') != -1) {
                                isValidClassName = false;
                            }
                        }
                        isClass = isValidClassName;
                    }
                    className = className.replace('/', '.');
                    boolean match = all;
                    if (!match) {
                        for (int i = index; i < args.length; ++i) {
                            if (className.startsWith(args[i])) {
                                match = true;
                                break;
                            }
                        }
                    }
                    if (match) {
                        if (isClass) {
                            classes.addElement(className);
                        } else {
                            // Adding the name as resource names should be with '/' and not '.'
                            resourceNames.addElement(name);
                        }
                    }
                }
            } catch (EOFException ex) {
            }
            dis.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Commmand line interface.
     *
     * @param args
     */
    public static void main(String args[]) throws Exception {
        SuiteCreator sc = new SuiteCreator();
        sc.run(args);
        Assert.shouldNotReachHere();
    }

    /**
     * Attempts to initialize the translator class for a given isolate that will be used to load a number of classes.
     *
     * @param uri        the URI of the suite containing a class implementing {@link TranslatorInterface}
     * @param className  the name of the class in the suite implementing {@link TranslatorInterface}
     * @param isolate    the isolate that will load a number of classes into its leaf suite
     */
    private static void initializeTranslator(String uri, String className, Isolate isolate) {

        try {
            Suite suite = Suite.getSuite(uri);
            Klass klass = suite.lookup(className);
            if (klass == null) {
                throw new Error("could not find the class '" + className + "' in the suite loaded from '" + uri + "'");
            }

            isolate.setTranslatorClass(klass);
        } catch (Error le) {
            System.err.println("** error opening translator suite from '" + uri + "':\n    " + le);
            System.err.println("** will use translator in bootstrap suite (if any)");
        }
    }

    /**
     * Parses and processes a given set of command line arguments to translate
     * a single suite.
     *
     * @param   args        the command line arguments
     * @return an Isolate that will load a number of classes into its leaf suite when started or null
     *         if there was a problem processing <code>args</code>
     */
    private Isolate processArgs(String[] args, Vector ignore) {
        int argc = 0;

        String classPath = null;
        String parentSuiteURI = null;
        boolean debugger = false;
        
        // create final vector by first specifying arguments
        Vector argVector = new Vector();

        Hashtable properties = new Hashtable();
        Hashtable propertiesHex = new Hashtable();
        String key = null;
        String keyHex = null;
        
        outerTranslator = VM.getCurrentIsolate().getTranslator();
        if (outerTranslator == null) {
            String[] result = scanForTranslatorArgs(args);
            initializeTranslator(result[0], result[1], VM.getCurrentIsolate());
            outerTranslator = VM.getCurrentIsolate().getTranslator();
        }
                
        while (argc != args.length) {
            String arg = args[argc];

            if (arg.charAt(0) != '-') {
                break;
            } else if (arg.startsWith("-cp:")) {
                classPath = ArgsUtilities.toPlatformPath(arg.substring("-cp:".length()), true);
            } else if (arg.startsWith("-parent:")) {
                parentSuiteURI = "file://" + arg.substring("-parent:".length()) + ".suite";
            } else if (arg.startsWith("-strip:") || arg.startsWith("-prune:")) {
                char type = arg.substring("-strip:".length()).charAt(0);
                argVector.addElement(arg); // pass argument through to loader
                if (type == 'a') {
                    suiteType = Suite.APPLICATION;
                } else if (type == 'd') {
                    suiteType = Suite.DEBUG;
                } else if (type == 'l') {
                    suiteType = Suite.LIBRARY;
                } else if (type == 'e') {
                    suiteType = Suite.EXTENDABLE_LIBRARY;
                } else {
                    usage("invalid suite type: " + type, args);
                    throw new RuntimeException();
                }
            } else if (arg.equals("-lnt")) {
                MethodMetadata.preserveLineNumberTables();
                argVector.addElement(arg); // pass argument through to loader
            } else if (arg.equals("-lvt")) {
                MethodMetadata.preserveLocalVariableTables();
                argVector.addElement(arg); // pass argument through to loader
            } else if (arg.startsWith("-endian:")) {
                String value = arg.substring("-endian:".length());
                if (value.equals("big")) {
                    bigEndian = true;
                } else if (value.equals("little")) {
                    bigEndian = false;
                } else {
                    usage("invalid endianess: " + value, args);
                    return null;
                }
            } else if (arg.startsWith("-translator:")
                       || arg.equals("-verbose") || arg.equals("-v")
                       || outerTranslator.isOption(arg)) {
                argVector.addElement(arg); // pass argument through to loader
            } else if (arg.equals("-debug")) {
                debugger = true;
            } else if (arg.startsWith("-trace")) {
                if (arg.startsWith("-tracefilter:")) {
                    String optArg = arg.substring("-tracefilter:".length());
                    Tracer.setFilter(optArg);
                } else {
                    Tracer.enableFeature(arg.substring("-trace".length()));
                    if (arg.equals("-traceconverting")) {
                        Tracer.enableFeature("loading"); // -traceconverting subsumes -traceloading
                    }
                }
            } else if (arg.startsWith("-h")) {
                usage(null, args);
                return null;
            } else if (arg.startsWith("-key:")) {
                key = arg.substring("-key:".length());
            } else if (arg.startsWith("-keyhex:")) {
                keyHex = arg.substring("-keyhex:".length());
            } else if (arg.startsWith("-value:")) {
                String value = arg.substring("-value:".length());
                if (key != null) {
                    properties.put(key, value);
                    key = null;
                } else if (keyHex != null) {
                    propertiesHex.put(keyHex, value);
                    keyHex = null;
                } else {
                    usage("must specify a key (or keyhex) for value: " + value, args);
                }
            } else {
                usage("Unknown option "+arg, args);
                return null;
            }
            argc++;
        }

        if (argc >= args.length) {
            usage("missing suite name", args);
            return null;
        }

        if (classPath == null) {
            usage("missing class path", args);
            return null;
        }

        String suiteName = args[argc++];

        // Parse class specifiers
        Vector classNames = new Vector();
        Vector resourceNames = new Vector();
        processClassPrefixes(classPath, args, argc, classNames, resourceNames);
        if (classNames.isEmpty()) {
            usage("no classes match the package specification", args);
            return null;
        }

        Vector dataVector = new Vector(classNames.size() + resourceNames.size() + 2);
        for (int i=0, maxI=classNames.size(); i < maxI; i++) {
            dataVector.addElement(classNames.elementAt(i));
        }
        StringBuffer ignoredClassesBuffer = new StringBuffer();
        for (int i=0, maxI=ignore.size(); i < maxI; i++) {
            int index = ((Integer) ignore.elementAt(i)).intValue();
            ignoredClassesBuffer.append(' ');
            ignoredClassesBuffer.append(dataVector.elementAt(index));
            dataVector.removeElementAt(index);
        }
        if (resourceNames.size() > 0) {
            // Add -resources ... and names of all resources to include
            dataVector.addElement("-resources");
            for (int i=0, maxI = resourceNames.size(); i < maxI; i++) {
                dataVector.addElement(resourceNames.elementAt(i));
            }
        }

        if (debugger) {
            // prepend debugger arguments
            int argNo = 0;
            if (classPath != null) {
                argVector.insertElementAt("-cp:"+classPath, argNo++);
            }
            if (parentSuiteURI != null) {
                argVector.insertElementAt("-suite:"+parentSuiteURI, argNo++);
            }
            argVector.addElement("-Dleaf.suite.name="+suiteName);
            //argVector.insertElementAt("-log:debug", argNum++);
            argVector.insertElementAt(SuiteCreator.Loader.class.getName(), argNo++);
        }
        
      // Add -key:, -keyhex:, -value: pairs
        // TODO Get rid of keyhex and find a way to provide support for passing in the JAD directly ?
        for (Enumeration keys = properties.keys(); keys.hasMoreElements(); ) {
            key = (String) keys.nextElement();
            String value = (String) properties.get(key);
            argVector.addElement("-key:" + key);
            argVector.addElement("-value:" + value);
        }
        for (Enumeration keys = propertiesHex.keys(); keys.hasMoreElements(); ) {
            key = (String) keys.nextElement();
            String value = (String) propertiesHex.get(key);
            argVector.addElement("-keyhex:" + key);
            argVector.addElement("-value:" + value);
        }

        args = new String[argVector.size() + dataVector.size()];
        for (int i = 0; i < argVector.size(); i++) {
            args[i] = (String)argVector.elementAt(i);
        }
        int j = 0;
        for (int i = argVector.size(); i < args.length; i++) {
            args[i] = (String)dataVector.elementAt(j++);
        }
        
        if (debugger) {
            return new Isolate("com.sun.squawk.debugger.sda.SDA", args, null, null);
        } else {
            Isolate isolate = new Isolate(SuiteCreator.Loader.class.getName(), args, classPath, parentSuiteURI);
            isolate.setProperty("leaf.suite.name", suiteName);
            return isolate;
        }
    }

    /**
     * Runs the suite creator.
     *
     * @param args   the command line args
     * @throws Exception if there was an error
     */
    private void run(String args[]) throws Exception {
        Isolate isolate;
        Vector ignore = new Vector();
        while (true) {
            isolate = processArgs(args, ignore);
            if (isolate == null) {
                System.exit(1);
            }
            isolate.start();
            isolate.join();
            if (isolate.getExitCode() == 0) {
                break;
            } else {
                // If an error occurred on creation of Suite, then remove the file indicated as causing the error and
                // attempt to create the suite again.  The file is encoded in the return code as the index + OFFSET of the
                // class that cause the problem, since index 0 would double as an exit code indicating success
                int exitCode = isolate.getExitCode();
                int classIndex = exitCode - IGNORE_CLASS_OFFSET;
                if (classIndex >= 0) {
                    ignore.addElement(new Integer(classIndex));
                } else {
                    // likely a serious out of memory error.
                    VM.getCurrentIsolate().exit(exitCode);
                }
            }
        }

        
        // Strip the symbols from the suite and close the stripped copy
        Suite leafSuite = isolate.getLeafSuite();
        Suite suite = leafSuite.strip(suiteType, leafSuite.getName(), leafSuite.getParent());
        suite.close();

        String uri = "file://" + suite.getName() + ".suite";
        DataOutputStream dos = Connector.openDataOutputStream(uri);
        suite.save(dos, uri, bigEndian);

        PrintStream out = new PrintStream(Connector.openOutputStream("file://" + suite.getName() + ".suite.api"));
        suite.printAPI(out);
        out.close();

        System.out.println("Created suite and wrote it into " + suite.getName() + ".suite");
        // Exit with the exitCode from the Loader status since its the one that does the heavy lifting of
        // loading and translating classes
        System.exit(isolate.getExitCode());
    }

    /**
     * This class is used to load a number of classes and resources into it's isolate's leaf suite.
     */
    public static class Loader {
        
        /**
         * Purely static class should not be instantiated.
         */
        private Loader() {}
    
        /**
         * Expecting command line that looks something like
         *
         * [options] className1 className2 ... [-resources resource1 resource2 ...] [-classes className1 className2 ...]
         * @param args
         * @throws Exception
         */
        public static void main(String[] args) throws Throwable {
            boolean inClasses = true;
            boolean verbose = false;
            int argnum = 0;
            int suiteType = Suite.APPLICATION;
            Isolate currentIsolate = Isolate.currentIsolate();
            
            // first, set up translator:
            String[] result = scanForTranslatorArgs(args);
            initializeTranslator(result[0], result[1], currentIsolate);
            TranslatorInterface translator  = currentIsolate.getTranslator();
            
            // parse out initial options
            for (argnum = 0; argnum < args.length; argnum++) {
                String arg = args[argnum];
                if (arg.startsWith("-translator:")) {
                    // ignore, handled by scanForTranslatorArgs() above.
                } else if (arg.equals("-lnt")) {
                    MethodMetadata.preserveLineNumberTables();
                } else if (arg.equals("-lvt")) {
                    MethodMetadata.preserveLocalVariableTables();
                } else if (arg.equals("-verbose") || arg.equals("-v")) {
                    verbose = true;
                    currentIsolate.setProperty("translator.verbose", "true");
                } else if (arg.startsWith("-strip:")) {
                    char type = arg.substring("-strip:".length()).charAt(0);
                    if (type == 'a') {
                        suiteType = Suite.APPLICATION;
                    } else if (type == 'd') {
                        suiteType = Suite.DEBUG;
                    } else if (type == 'l') {
                        suiteType = Suite.LIBRARY;
                    } else if (type == 'e') {
                        suiteType = Suite.EXTENDABLE_LIBRARY;
                    } else {
                        throw new RuntimeException("invalid suite type: " + type);
                    }
                } else if (!translator.processOption(arg)) {
                    break;
                }
            }
            
            // open classpath for resource loading later
            String url = "classpath://" +  currentIsolate.getClassPath();
            ClasspathConnection classPathConnection;
            try {
                classPathConnection  = (ClasspathConnection) Connector.open(url);
            } catch (IOException ioe) {
                if (VM.isHosted() || VM.isVeryVerbose()) {
                    System.err.println("IO error while opening class path : " + url);
                    ioe.printStackTrace();
                }
                throw new RuntimeException("IO error while opening class path : " + url);
            }

            Suite suite = currentIsolate.getLeafSuite();
            translator.open(suite, currentIsolate.getClassPath());

            String key = null;
            String keyHex = null;

            // If an exception occurs while we are loading classes, capture it and ignore all classes from then on
            // We still keep processing argument in order to get to the resources specified however.
            // This is a simple HACK which allows us to run the TCK JavaTest Agent even if classes for test
            // do not get included.  For this to work, JavaTest Agent must either be in bootstrap suite, or a
            // suite of its own.  Then the test to execute goes into its own suite which will include the agent.dat
            // for that test.
            for (int i = argnum, maxI = args.length; i < maxI; i++) {
                if (inClasses) {
                    i = loadClasses(args, i, verbose);
                } else {
                    i = loadResources(suite, args, i, classPathConnection);
                }
                if (i < maxI && args[i].startsWith("-")) {
                    String arg = args[i];
                    if (arg.equals("-classes")) {
                        inClasses = true;
                    } else if (arg.equals("-resources")) {
                        inClasses = false;
                    } else if (arg.equals("-verbose") || arg.equals("-v")) {
                        verbose = true;
                    } else if (arg.startsWith("-key:")) {
                        key = arg.substring("-key:".length());
                    } else if (arg.startsWith("-keyhex:")) {
                        keyHex = arg.substring("-keyhex:".length());
                    } else if (arg.startsWith("-value:")) {
                        String value = arg.substring("-value:".length());
                        if (key != null) {
                            suite.setProperty(key, value);
                            key = null;
                        } else if (keyHex != null) {
                            key = new String(HexEncoding.hexDecode(keyHex));
                            value = new String(HexEncoding.hexDecode(value));
                            suite.setProperty(key, value);
                            key = null;
                            keyHex = null;
                        } else {
                            throw new RuntimeException("No key specified for value: " + value);
                        }
                    } else {
                        throw new RuntimeException("Specified unknown option: " + arg);
                    }
                }
            }
            
            // Compute the complete class closure.
            translator.close(suiteType);
        }
        
        public static void installResourceFile(Suite suite, ResourceFile resourceFile) throws IOException {
            if (VM.isVerbose()) {
                System.out.println("[Including resource: " + resourceFile.name + "]");
            }
            suite.installResource(resourceFile);
        }

        protected static int loadClasses(String[] args, int startIndex, boolean verbose) {
            for (int i = startIndex; i != args.length; ++i) {
                String name = args[i];
                if (name.startsWith("-")) {
                    return i;
                }

                try {
                    Klass.getClass(name, false);
                    //Klass.forName(name, true, false);
//                } catch (ClassNotFoundException e) {
                    //                   e.printStackTrace();
                    //                  return i;
                } catch (NoClassDefFoundError e) {
                    System.out.println("Encountered error loading class: " + name);
                    System.out.println("   " + e.getMessage());
                    if (verbose) {
                        e.printStackTrace();
                    }
                    // Add offset in case it was the first parameter
                    // Report back the index of the argument that failed to load
                    VM.getCurrentIsolate().exit(i + IGNORE_CLASS_OFFSET);
          /*      } catch (Error e) {
                    // may be out of memory, so get out quickly
                    VM.getCurrentIsolate().exit(999);*/
                }
                
            }
            return args.length;
        }

        public static int loadResources(Suite suite, String[] args, int startIndex, ClasspathConnection classPathConnection) {
            for (int i = startIndex; i != args.length; ++i) {
                String name = args[i];
                if (name.startsWith("-")) {
                    return i;
                }
                try {
                    byte[] bytes = classPathConnection.getBytes(name);
                    ResourceFile resource = new ResourceFile(name, bytes);
                    installResourceFile(suite, resource);
                } catch (IOException ioe) {
                    if (VM.isHosted() || VM.isVeryVerbose()) {
                        System.err.println("IO error while loading resource: " + name);
                        ioe.printStackTrace();
                    }
                    throw new RuntimeException("IO error while loading resource: " + name);
                }
            }
            return args.length;
        }
    }
}
