/*
 * Copyright 2008 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.squawk.builder.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.jar.Manifest;

import com.sun.squawk.builder.Build;
import com.sun.squawk.builder.BuildException;
import com.sun.squawk.builder.Command;
import com.sun.squawk.builder.Target;
import com.sun.squawk.builder.util.FileSet;

/**
 * Command to build a UEI-compliant squawk emulator.
 */
public class UEICommand extends Command {
    
    protected final static String EXEC_PATH = System.getProperty("user.dir");
    
    // Workaround for pathnames that include spaces
    protected final static File SQUAWK_DIR = (!EXEC_PATH.contains(" ") ? new File(EXEC_PATH) : null);
    
    protected final static File UEI_MODULE_DIR = new File(SQUAWK_DIR, "uei");
    protected final static File J2SE_MODULE_DIR = new File(UEI_MODULE_DIR, "emulator-j2se");
    protected final static File J2ME_MODULE_DIR = new File(UEI_MODULE_DIR, "emulator-j2me");
    
    protected final static String[] UEI_DIRECTORIES = { "bin", "lib", "doc", "squawk", "logs", "temp" };
    protected final static String[] BIN_FILES = { "squawk", "squawk.suite", "squawk.suite.api", "squawk.sym", "squawk.jar",
                                                  "squawk_classes.jar", "build-commands.jar", "build.properties" };
    protected final static String[] BIN_MODULE_JARS = { "romizer", "translator", "cldc", "debugger", "debugger-proxy" };
    protected final static String[] VANILLA_FILES = { "squawk", "squawk.suite", "squawk.suite.api", "squawk.suite.metadata",
                                                      "squawk.sym", "squawk.jar", "squawk_classes.jar" };
    
    protected final static String MODULE_MANIFEST = getPath("resources", "META-INF", "MANIFEST.MF");
    
    protected final static File DEFAULT_DIRECTORY = new File(UEI_MODULE_DIR, "squawk-emulator");
    
    // FIXME HACK: quick disabling of UEI command when uei module does not exist
    protected final static boolean UEI_ACTIVE = UEI_MODULE_DIR.exists();
    
    protected final Target TARGET_EMULATOR_J2SE;
    protected final Target TARGET_EMULATOR_J2ME;
    
    protected final String EMULATOR_FILENAME;
    protected final File   PREVERIFIER;
    
    protected PrintStream stdout;
    protected PrintStream stderr;
    protected PrintStream vbsout;
    
    public UEICommand(Build env) {
        super(env, "uei");
        // super(env, "builduei");
        
        EMULATOR_FILENAME = "emulator" + env.getPlatform().getExecutableExtension();
        PREVERIFIER = env.getPlatform().preverifier();
        
        TARGET_EMULATOR_J2SE = getTarget(J2SE_MODULE_DIR, getPath(SQUAWK_DIR, "cldc", "classes") + ":build-commands.jar", false);
        TARGET_EMULATOR_J2ME = getTarget(J2ME_MODULE_DIR, getPath(SQUAWK_DIR, "cldc", "classes"), true);
    }
    
    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return (UEI_ACTIVE ? "" : "<< currently disabled >> ") + "Builds the Unified Emulator Interface(UEI) module";
    }
    
    private void usage() {
        //PrintStream out = System.out;
        
        //Column     123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789
        stdout.println();
        stdout.println("usage: " + name + " [options] [directory]");
        stdout.println();
        stdout.println("This will build a UEI-compliant emulator in the specified directory.  If no");
        stdout.println("directory is supplied, it will be built in:");
        stdout.println("    " + DEFAULT_DIRECTORY);
        stdout.println();
        stdout.println("where options include:");
        stdout.println("    -help                   Display this help message");
        stdout.println("    -clean                  Cleans the specified UEI directory");
        stdout.println("    -verbose                Displays all build output");
        stdout.println();
    }
    
    /**
     * {@inheritDoc}
     */
    public void run(String args[]) throws BuildException {
        if (!UEI_ACTIVE) {
            throw new BuildException("UEI disabled - UEI module does not exist");
        }
        
        stdout = System.out;
        stderr = System.err;
        vbsout = new PrintStream(new OutputStream() { public void write(int b) {;} });
        
        File targetDirectory = DEFAULT_DIRECTORY;
        
        boolean clean = false;
        boolean verbose = env.verbose;
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("-")) {
                targetDirectory = new File(arg).getAbsoluteFile();
            } else if (arg.equalsIgnoreCase("-help")) {
                usage();
                return;
            } else if (arg.equalsIgnoreCase("-verbose")) {
                verbose = true;
            } else if (arg.equalsIgnoreCase("-clean")) {
                clean = true;
            } else {
                stdout.println("Unrecognized option: " + arg);
                usage();
                throw new BuildException("Unrecognized option: " + arg);
            }
        }
        
        if (verbose) {
            vbsout = stdout;
        }
        
        try {
            // Capture subprocess output to the verbose out
            System.setOut(vbsout);
            
            if (clean) {
                cleanUEI(targetDirectory);
                return;
            }
            
            buildUEI(targetDirectory);
            
        } finally {
            System.setOut(stdout);
        }
        
    }
    
    /**
     * Builds the UEI in the specified directory
     * 
     * @param directory The target emulator directory
     * 
     * @throws BuildException if the build fails
     */
    protected void buildUEI(File directory) throws BuildException {
        stdout.println("Using squawk in  " + SQUAWK_DIR);
        stdout.println("Using uei module " + UEI_MODULE_DIR);
        stdout.println("Building UEI in  " + directory);
        
        Build.mkdir(directory);
        
        for (String dirName : UEI_DIRECTORIES) {
            Build.mkdir(directory, dirName);
        }
        
        
        // Build squawk
        stdout.println("Building squawk...");
        builder();
        // Build uei modules
        TARGET_EMULATOR_J2SE.run(null);
        TARGET_EMULATOR_J2ME.run(null);
        
        
        
        // Romize emulator squawk
        stdout.println("Romizing emulator suite...");
        builder("rom", "-lnt", "-strip:d", getPath(SQUAWK_DIR, "cldc"), J2ME_MODULE_DIR.getPath());
        
        // Copy emulator squawk
        File binDir = new File(directory, "bin");
        for (String fileName : BIN_FILES) {
            copyFile(SQUAWK_DIR, fileName, binDir);
        }
        env.chmod(new File(binDir, "squawk"), "+x");
        
        // Copy emulator support jars
        copyFile(J2SE_MODULE_DIR, "classes.jar", binDir, "emulator.jar");
        for (String moduleName : BIN_MODULE_JARS) {
            copyFile(new File(SQUAWK_DIR, moduleName), "classes.jar", binDir, moduleName + ".jar");
        }

        copyFile(UEI_MODULE_DIR, "emulator", binDir, EMULATOR_FILENAME);
        env.chmod(new File(binDir, EMULATOR_FILENAME), "+x");
        copyFile(UEI_MODULE_DIR, "SquawkEmulator.properties", binDir);
        copyFile(PREVERIFIER.getParentFile(), PREVERIFIER.getName(), binDir);
        env.chmod(new File(binDir, PREVERIFIER.getName()), "+x");
        
        
        
        // Romize vanilla squawk
        stdout.println("Romizing vanilla suite...");
        builder("rom", "-metadata", "-lnt", "-strip:d", getPath(SQUAWK_DIR, "cldc"), getPath(SQUAWK_DIR, "imp"), getPath(SQUAWK_DIR, "debugger"));
        
        // Copy vanilla squawk
        File vanillaDir = new File(directory, "squawk");
        for (String fileName : VANILLA_FILES) {
            copyFile(SQUAWK_DIR, fileName, vanillaDir);
        }
        env.chmod(new File(vanillaDir, "squawk"), "+x");
        
        
        
        // Copy API jars
        stdout.println("Creating API jars...");
        
        File libDir = new File(directory, "lib");
        createJar(new File(libDir, "cldc11.jar"), getFile(SQUAWK_DIR, "cldc", "j2meclasses"), getFile(SQUAWK_DIR, "cldc", MODULE_MANIFEST));
        createJar(new File(libDir, "imp10.jar"), getFile(SQUAWK_DIR, "imp", "j2meclasses"), getFile(SQUAWK_DIR, "imp", MODULE_MANIFEST));
        createJar(new File(libDir, "debugger.jar"), getFile(SQUAWK_DIR, "debugger", "j2meclasses"), null);
        
        
        // Create API javadoc
        stdout.println("Creating API javadoc...");
        
        try {
            // Capture javadoc warnings to vbsout
            System.setErr(vbsout);
            env.getJavaCompiler().javadoc(new String[] {
                    "-d", getPath(directory, "doc"),
                    "-sourcepath", getPath(SQUAWK_DIR, "cldc", "preprocessed") + ":" + getPath(SQUAWK_DIR, "imp", "preprocessed"),
                    "-subpackages", "com:java:javax",
                    "-windowtitle", "Java 2 Platform ME CLDC-1.1/IMP-1.0",
                    "-doctitle",  "Java<sup><font size=-2>TM</font></sup> 2 Platform Micro Edition<br>CLDC-1.1 / IMP-1.0 API Specification",
                    "-header", "<b>Java<sup><font size=-2>TM</font></sup> 2 Platform<br><font size=-1>Micro Ed. CLDC-1.1 / IMP-1.0</font></b>",
                    "-bottom", "<font size=-1>Copyright 2008 Sun Microsystems, Inc.  All rights reserved.</font>",
                    "-quiet"}, true);
        } finally {
            System.setErr(stderr);
        }
        
        stdout.println("BUILD SUCCEEDED");
    }
    
    
    /**
     * Convenience method that gets the <code>Target</code> associated with the
     * specified module assuming that the module's entire source code is found
     * solely in a subdirectory "src", that the module's directory is a valid
     * <code>Target</code> name, and that preprocessing is enabled.
     * 
     * @param baseDir The base directory of the module.
     * @param classpath Additional classpath required by the module.
     * @param j2me Whether or not this is a j2me module.
     * @return The associated <code>Target</code>.
     */
    protected Target getTarget(File baseDir, String classpath, boolean j2me) {
        return new Target(classpath, j2me, baseDir.getPath(), new File[] { new File(baseDir, "src") }, true, env, baseDir.getName());
    }
    
    /**
     * Convenience method that concatenates the given array of strings into a
     * valid system-dependent abstract path.
     * 
     * @param path The sequence of file names.
     * @return The abstract file path specified by <code>path</code>.
     */
    protected static String getPath(String... path) {
        if (path.length == 0) {
            throw new IllegalArgumentException("Path must be non-empty");
        }
        
        return Build.join(path, 0, path.length, File.separator);
    }
    
    /**
     * Convenience method that gets the file path designated by
     * <code>path</code> relative to <code>parent</code>.
     * 
     * @param parent The parent directory.
     * @param path The file path relative to the parent directory.
     * @return The file path specified by <code>path</code> relative to
     *         <code>parent</code>.
     * 
     * @see UEICommand#getPath(String[])
     * @see UEICommand#getFile(File, String[])
     */
    protected static String getPath(File parent, String... path) {
        return getFile(parent, path).getPath();
    }
    
    /**
     * Convenience method that gets the file designated by <code>path</code>
     * relative to <code>parent</code>.
     * 
     * @param parent The parent directory.
     * @param path The file path relative to the parent directory.
     * @return The file specified by <code>path</code> relative to <code>parent</code>.
     * 
     * @see UEICommand#getPath(String[])
     */
    protected static File getFile(File parent, String... path) {
        return new File(parent, getPath(path));
    }
    
    /**
     * Convenience method that invokes <code>Build.main(String[])</code> with
     * the given arguments. Note that since an instance of <code>Build</code>
     * will only execute commands once, it is necessary to use the static
     * command line <code>main</code> entry point.
     * 
     * @param args The builder arguments.
     * 
     * @see Build#main(String[])
     */
    protected void builder(String... args) {
        vbsout.println("Executing builder: " + Build.join(args, 0, args.length, " "));
        
        Build.main(args);
    }

    /*
     * Convenience method that invokes <code>Build.exec(String)</code> with
     * the given arguments.
     * 
     * @param args The execution arguments.
     * 
     * @see Build#exec(String)
     */
    /*
    protected void buildExec(String... args) {
        String cmd = Build.join(args, 0, args.length, " ");
        
        vbsout.println("Executing process: " + cmd);
        
        env.exec(cmd);
    }
    */
    
    /**
     * Convenience method that copies a file from one directory to another.
     * 
     * @param srcPath The parent directory of the target file.
     * @param srcName The name of the target file.
     * @param destPath The destination directory.
     * 
     * @see UEICommand#copyFile(File, File)
     * @see UEICommand#copyFile(File, String, File, String)
     * @see Build#cp(File, File, boolean)
     */
    protected void copyFile(File srcPath, String srcName, File destPath) {
        copyFile(srcPath, srcName, destPath, srcName);
    }
    
    /**
     * Convenience method that copies and renames a file from one directory to
     * another.
     * 
     * @param srcPath The parent directory of the target file.
     * @param srcName The name of the target file.
     * @param destPath The destination directory.
     * @param destName The destination name.
     * 
     * @see UEICommand#copyFile(File, File)
     * @see Build#cp(File, File, boolean)
     */
    protected void copyFile(File srcPath, String srcName, File destPath, String destName) {
        copyFile(new File(srcPath, srcName), new File(destPath, destName));
    }
    
    /**
     * Convenience method that copies and renames a file.
     * 
     * @param src The target file.
     * @param dest The destination file.
     * 
     * @see Build#cp(File, File, boolean)
     */
    protected void copyFile(File src, File dest) {
        vbsout.println("copying " + src);
        vbsout.println("     to " + dest);
        
        Build.cp(src, dest, false);
    }
    
    /**
     * Convenience method that creates a jar-file using all the files in
     * <code>srcFolder</code> and the specified <code>manifest</code>.
     * 
     * @param dest The jar-file to be created.
     * @param srcFolder The source for the jar.
     * @param manifest The manifest for the jar. If this value is null, then no
     *            manifest will be included.
     * 
     * @throws BuildException if the build fails.
     * 
     * @see Build#createJar(File, FileSet[], Manifest)
     */
    protected void createJar(File dest, File srcFolder, File manifest) throws BuildException {
        Manifest mf = null;
        
        if (manifest != null) {
            try {
                mf = new Manifest(new FileInputStream(manifest));
            } catch (Exception e) {
                throw new BuildException("Error reading manifest: " + manifest, e);
            }
        }
        
        env.createJar(dest, new FileSet[] { new FileSet(srcFolder, (FileSet.Selector) null) }, mf);
    }
    
    /**
     * {@inheritDoc}
     */
    public void clean() {
        if (!UEI_ACTIVE) {
            return;
        }
        
        TARGET_EMULATOR_J2SE.clean();
        TARGET_EMULATOR_J2ME.clean();
        // clean(DEFAULT_DIRECTORY);
    }
    
    /**
     * Removes the emulator previously generated at the specified directory.
     * 
     * @param directory The target emulator directory.
     * 
     * @throws BuildException if the clean failed.
     */
    protected void cleanUEI(File directory) throws BuildException {
        stdout.println("Cleaning " + directory);
        
        if (directory.equals(DEFAULT_DIRECTORY)) {
            Build.clear(directory, true);
            return;
        }
        
        if (!directory.isDirectory()) {
            throw new BuildException("target " + directory + " is not a directory.");
        }
        
        stdout.println("CLEAN STUB: Please perform manual deletion of " + directory);
    }
    
}
