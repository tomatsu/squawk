/*
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

package com.sun.squawk.builder;

import com.sun.squawk.SquawkRetroWeaver;
import com.sun.squawk.builder.ccompiler.*;
import com.sun.squawk.builder.commands.*;
import com.sun.squawk.builder.gen.Generator;
import com.sun.squawk.builder.launcher.Launcher;
import com.sun.squawk.builder.platform.Platform;
import com.sun.squawk.builder.platform.Windows_X86;
import com.sun.squawk.builder.util.DirSet;
import com.sun.squawk.builder.util.FileSet;
import com.sun.squawk.builder.util.FileVisitor;
import com.sun.squawk.builder.util.SubstitutionProperties;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.sourceforge.retroweaver.event.WeaveListener;

/**
 * This is the launcher for building parts (or all) of the Squawk VM as well as launching commands.
 * Run it with '-h' to see what commands and targets are available.
 *
 */
public class Build {
// TODO Go through and clean up the references to directories so that they are consistent 

    /*---------------------------------------------------------------------------*\
     *                               Runtime options                             *
    \*---------------------------------------------------------------------------*/

    /**
     * Enables/disables verbose execution.
     * <p>
     * The default value is <code>false</code>.
     */
    public boolean verbose;

    /**
     * Enables/disables information level output.
     * <p>
     * The default value is <code>false</code>.
     */
    public boolean info;

    /**
     * Enables/disables brief level output.
     * <p>
     * The default value is <code>true</code>.
     */
    public boolean brief = true;

    /**
     * Enables/disables checking (and updating if necessary) of a command's dependencies before running the command itself.
     * <p>
     * The default value is <code>true</code>.
     */
    public boolean checkDependencies = true;

    /**
     * Enables/disables running the javadoc checking tool after a Java compilation.
     * <p>
     * The default value is <code>false</code>.
     */
    public boolean runDocCheck;

    /**
     * Enables/disables generation of javadoc after a Java compilation.
     * <p>
     * The default value is <code>false</code>.
     */
    public boolean runJavadoc;

    /**
     * If {@link runJavadoc} is <code>true</code>, then javadoc is only generated for  public and protected members.
     * <p>
     * The default value is <code>false</code>.
     */
    public boolean runJavadocAPI;

    /**
     * Determines if the Squawk system will be built as a dynamic library.
     */
    public boolean dll;

    /**
     * Extra arguments to be passed to the Java compiler.
     */
    public String javacOptions = "";

    /**
     * Determines if {@link JavaCommand}s are run in the same JVM process as the builder.
     */
    public boolean forkJavaCommand;

    /**
     * Extra arguments to be passed to the Java VM when executing a JavaCommand.
     */
    public String javaOptions = "";

    /**
     * The C compiler.
     */
    private CCompiler ccompiler;

    /**
     * Gets the compiler used for compiling and linking C code.
     *
     * @return C compiler
     */
    public CCompiler getCCompiler() {
        return ccompiler;
    }

    /**
     * The object providing access to JDK tools.
     */
    private JDK jdk;

    /**
     * Gets the instance through which JDK tools can be accessed.
     * @return the JDK instance
     */
    public JDK getJDK() {
        return jdk;
    }

    /**
     * The host platform. This is used to access tools required for running the builder and has no
     * relationship with the target platform that a Squawk executable will be built for.
     */
    private Platform platform;

    /**
     * Gets the object that represents the host platform.
     *
     * @return  the object that represents this builder's host platform
     */
    public Platform getPlatform() {
        return platform;
    }

    /**
     * The Java source preprocessor.
     */
    private Preprocessor preprocessor;

    /**
     * Gets the Java source preprocessor.
     *
     * @return the Java source preprocessor
     */
    public Preprocessor getPreprocessor() {
        return preprocessor;
    }

    /**
     * The C function macroizer.
     */
    private Macroizer macroizer;

    /**
     * Gets the C function macroizer.
     *
     * @return the C function macroizer
     */
    public Macroizer getMacroizer() {
        return macroizer;
    }

    /**
     * The properties that drive the preprocessing.
     */
    private Properties properties;

    /**
     * The most recent date that the file(s) from which the properties are loaded was modified.
     */
    private long propertiesLastModified;
    
    private String specfifiedBuildDotOverrideFileName;
    
    protected boolean isInitialized;
    
    protected boolean isJava5SyntaxSupported;
    
    /**
     * Gets the name of the build.override file specified by the -override: or -override arg. Or null if no
     * -override: arg was specified.
     *
     * @return String or null
     */
    public String getspecfifiedBuildDotOverrideFileName() {
        return specfifiedBuildDotOverrideFileName;
    }

    protected List<File> possibleModuleDirs = new ArrayList<File>();

    public void addPossibleModuleDir(File dir) {
        possibleModuleDirs.add(dir);
    }
    
    public List<File> getPossibleModuleDirs() {
        return possibleModuleDirs;
    }
    
    /**
     * The interface to run a Java source compiler.
     */
    private JavaCompiler javaCompiler;

    /**
     * Gets the object used to do a Java compilation as well as run javadoc.
     *
     * @return Java compiler
     */
    public JavaCompiler getJavaCompiler() {
        return javaCompiler;
    }

    /**
     * The arguments extracted by the last call to {@link #extractBuilderArgs}.
     */
    private List<String> builderArgs = new ArrayList<String>();

    /**
     * Gets the builder specific arguments passed to the last invocation of {@link #mainProgrammatic}.
     * These can be used to pass through to a invocation of the builder via another Build instance.
     *
     * @return  the builder specific arguments
     */
    public List<String> getBuilderArgs() {
        return new ArrayList<String>(builderArgs);
    }

    /*---------------------------------------------------------------------------*\
     *                              Commands  and Targets                        *
    \*---------------------------------------------------------------------------*/

    /**
     * The set of builder commands and targets.
     */
    private Map<String, Command> commands = new TreeMap<String, Command>();

    /**
     * Adds a command to the set of builder commands.
     *
     * @param command the command
     * @return <code>command</code>
     * @throws IllegalArgumentException if their is already a command registered with <code>command.getName()</code>
     */
    private Command addCommand(Command command) {
        String name = command.getName();
        if (commands.containsKey(name)) {
            throw new IllegalArgumentException("cannot overwrite existing command: " + command.getName());
        }
        commands.put(name, command);
        return command;
    }

    /**
     * Creates and installs a Target.
     *
     * @param j2me           compiles for a J2ME platform
     * @param baseDir        the parent directory for primary source directory and the output directory(s). This will also be the name of the target.
     * @param dependencies   the space separated names of the Java compilation targets that this target depends upon
     * @param extraClassPath the class path in addition to that derived from <code>dependencies</code>
     * @param extraSourceDirs any extra directories to be searched for sources (can be null)
     * @return the created and installed command
     */
    public Target addTarget(boolean j2me, String baseDir, String dependencies, String extraClassPath, String extraSourceDirs) {
        File primarySrcDir = new File(baseDir, "src");
        File[] srcDirs;
        if (extraSourceDirs != null) {
            StringTokenizer st = new StringTokenizer(extraSourceDirs);
            srcDirs = new File[st.countTokens() + 1];
            srcDirs[0] = primarySrcDir;
            for (int i = 1; i != srcDirs.length; ++i) {
                srcDirs[i] = new File(baseDir, st.nextToken());
            }
        } else {
            srcDirs = new File[] { primarySrcDir };
        }

        StringBuffer extraBuffer = new StringBuffer();
        if (extraClassPath != null && extraClassPath.length() != 0) {
            String string = toPlatformPath(extraClassPath, true);
            StringTokenizer tokenizer = new StringTokenizer(string, File.pathSeparator);
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                File tokenFile = new File(token);
                if (!tokenFile.isAbsolute()) {
                    tokenFile = new File(baseDir, token);
                }
                extraBuffer.append(tokenFile);
                if (tokenizer.hasMoreTokens()) {
                    extraBuffer.append(File.pathSeparatorChar);
                }
            }
        }

        Target command = new Target(extraBuffer.toString(), j2me, baseDir, srcDirs, true, this, new File(baseDir).getName().toLowerCase());
        if (dependencies != null) {
            command.dependsOn(dependencies);
        }
        addCommand(command);
        return command;
    }

    /**
     * Creates and installs a Target.
     *
     * @param j2me           compiles for a J2ME platform
     * @param baseDir        the parent directory for primary source directory and the output directory(s). This will also be the name of the command.
     * @param dependencies   the space separated names of the Java compilation targets that this command depends upon
     * @param extraClassPath the class path in addition to that derived from <code>dependencies</code>
     * @return the created and installed command
     */
    public Target addTarget(boolean j2me, String baseDir, String dependencies, String extraClassPath) {
        return addTarget(j2me, baseDir, dependencies, extraClassPath, null);
    }

    /**
     * Creates and installs a Target.
     *
     * @param j2me           compiles for a J2ME platform
     * @param baseDir        the parent directory for primary source directory and the output directory(s). This will also be the name of the command.
     * @param dependencies   the space separated names of the Java compilation targets that this command depends upon
     * @return the created and installed command
     */
    public Target addTarget(boolean j2me, String baseDir, String dependencies) {
        return addTarget(j2me, baseDir, dependencies, null, null);
    }

    /**
     * Creates and installs a JavaCommand.
     *
     * @param j2me           compiles for a J2ME platform
     * @param baseDir        the directory in which the sources and output directories exist. This will also be the name of the command.
     * @param classPath      the class path to be used for compilation
     * @return the created and installed command
     */
    private JavaCommand addJavaCommand(String name, String classPath, boolean bootclasspath, String extraJVMArgs, String mainClassName, String dependencies) {
        JavaCommand command = new JavaCommand(name, classPath, bootclasspath, extraJVMArgs, mainClassName, this);
        command.dependsOn(dependencies);
        addCommand(command);
        return command;
    }

    /**
     * Creates and installs a command that launches an application in Squawk.
     *
     * @param name String
     * @param classPath String
     * @param extraVMArgs String
     * @param mainClassName String
     * @param args String
     * @param description 
     * @return the created and installed command
     */
    public Command addSquawkCommand(String name, String classPath, String extraVMArgs, String mainClassName, String args, final String description) {
        final String commandWithArgs = "squawk" + getPlatform().getExecutableExtension() + " " +
                                       (classPath == null ? "" : "-cp:" + toPlatformPath(classPath, true) + " ") +
                                       (extraVMArgs == null ? "" : " " + extraVMArgs + " ") +
                                       mainClassName + " " +
                                       (args == null ? "" : args);
        Command command = new Command(this, name) {
            public void run(String[] args) throws BuildException {
                env.exec(commandWithArgs + " " + join(args));
            }
            public String getDescription() {
                if (description != null) {
                    return description;
                }
                return super.getDescription();
            }

        };
        addCommand(command);
        return command;
    }

    /**
     * Creates and installs a command that generates a source file.
     *
     * @param name     the name of the class in the package com.sun.squawk.builder.gen that does the generating
     * @param baseDir  the base directory in which the generated file is found
     * @return the created Command
     */
    private Command addGen(final String name, final String base) {
        Command command = new GeneratorCommand(name, base);
        addCommand(command);
        return command;
    }

    class GeneratorCommand extends Command {
        private final File baseDir;
        GeneratorCommand(final String name, final String base) {
            super(Build.this, name);
            this.baseDir = new File(base);
        }

        private Generator generator(String name) {
            String className = "com.sun.squawk.builder.gen." + name;
            try {
                return (Generator) Class.forName(className).newInstance();
            } catch (Exception e) {
                throw new BuildException("Error instantiating " + className, e);
            }
        }

        public void run(String[] args) throws BuildException {
            Generator gen = generator(name);
            boolean replaced = gen.run(baseDir);
            log(replaced && verbose, "[created/updated " + gen.getGeneratedFile(baseDir) + "...]");
        }

        public String getDescription() {
            Generator gen = generator(name);
            return "generates the file " + gen.getGeneratedFile(baseDir);
        }
    }

    protected void addSiblingBuilderDotPropertiesFiles(File rootDir, List<File> files) {
        File[] siblingDirs = rootDir.listFiles();
        if (siblingDirs == null) {
            return;
        }
        for (File siblingDir : siblingDirs) {
            File dotPropertiesFile = new File(siblingDir, "builder.properties");
            if (dotPropertiesFile.canRead()) {
                log(verbose, "Reading builder.properties: " + dotPropertiesFile.getPath());
                files.add(dotPropertiesFile);
            }
        }
    }

    protected void processBuilderDotPropertiesFiles(List<File> dotPropertiesFiles) {
    	for (File dotProperties: dotPropertiesFiles) {
    		processBuilderDotPropertiesFile(dotProperties);
    	}
    }
    
    /**
     * Installs commands from a properties file.
     *
     * @param pluginsFile  the properties file to load from
     */
    protected void processBuilderDotPropertiesFile(File dotPropertiesFile) {
        log(verbose, "Reading commands from: " + dotPropertiesFile.getPath());
        Properties properties = new Properties();
        InputStream in = null;
        try {
            in = new FileInputStream(dotPropertiesFile);
            properties.load(in);
        } catch (IOException e) {
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
            }
            in = null;
        }
        int propertyIndex = 0;
        String currentType = "";
        String currentName = "";
        HashMap<String, String> attributes = new HashMap<String, String>();
        ClassLoader loader = Build.class.getClassLoader();
        final String classpathKey = "classpath";
        if (properties.containsKey(classpathKey)) {
            String value = properties.getProperty(classpathKey);
            properties.remove(classpathKey);
            // handle classpath=path1:path2
            URL[] urls = null;
            String[] classpath = Build.toPlatformPath(value, true).split(File.pathSeparator);
            try {
                urls = new URL[classpath.length];
                for (int i = 0; i < classpath.length; i++) {
                    File f = new File(classpath[i]);
                    String url = f.getAbsolutePath();

                    // Make sure the url class loader recognises directories
                    if (f.isDirectory()) {
                        url += "/";
                    }
                    url = "file://" + fixURL(url);
                    urls[i] = new URL(url);
                }
            } catch (MalformedURLException e) {
                throw new BuildException("badly formed plugins path", e);
            }
            loader = new URLClassLoader(urls, loader) {
                protected String findLibrary(String libname) {
                    String mappedName = System.mapLibraryName(libname);
                    URL url = findResource(mappedName);
                    try {
                        File file = new File(url.toURI());
                        return file.getAbsolutePath();
                    } catch (URISyntaxException e) {
                    }
                    return null;
                }
            };
        }
        // Sort the properties to process them in a decent order
        ArrayList<String> sortedPropertyNames = new ArrayList<String>(properties.size());
        for (Enumeration<?> names = properties.propertyNames(); names.hasMoreElements();) {
            sortedPropertyNames.add((String) names.nextElement());
        }
        Collections.sort(sortedPropertyNames);
        for (String propertyName : sortedPropertyNames) {
            propertyIndex++;
            String value = properties.getProperty(propertyName);
            if (propertyName.indexOf('.') == -1) {
                // handle command name=command class
                try {
                    Class<?> pluginClass = loader.loadClass(value);
                    Constructor<?> cons = pluginClass.getConstructor(new Class[] { Build.class });
                    Command command = (Command) cons.newInstance(new Object[] { this });
                    addCommand(command);
                } catch (InvocationTargetException e) {
                    throw new BuildException("error creating " + value + " plugin: ", e);
                } catch (IllegalArgumentException e) {
                    throw new BuildException("error creating " + value + " plugin: ", e);
                } catch (IllegalAccessException e) {
                    throw new BuildException("error creating " + value + " plugin: ", e);
                } catch (InstantiationException e) {
                    throw new BuildException("error creating " + value + " plugin: ", e);
                } catch (NoSuchMethodException e) {
                    throw new BuildException("error creating " + value + " plugin: ", e);
                } catch (ClassNotFoundException e) {
                    throw new BuildException("error creating " + value + " plugin: ", e);
                }
            } else {
                // handle command type.name.attribute=value
                StringTokenizer tokenizer = new StringTokenizer(propertyName, ".");
                try {
                    String type = tokenizer.nextToken();
                    String name = tokenizer.nextToken();
                    String attribute = tokenizer.nextToken();
                    if (!type.equals(currentType) && !name.equals(currentName)) {
                        if (!attributes.isEmpty()) {
                            processBuilderDotPropertiesFile(currentType, currentName, dotPropertiesFile, propertyIndex, attributes);
                        }
                        attributes.clear();
                        currentType = type;
                        currentName = name;
                    }
                    attributes.put(attribute, value);
                } catch (NoSuchElementException e) {
                    throw new BuildException("Bad format on property at index " + propertyIndex + " in file " + dotPropertiesFile.getPath());
                }
            }
        }
        if (!attributes.isEmpty()) {
            processBuilderDotPropertiesFile(currentType, currentName, dotPropertiesFile, propertyIndex, attributes);
        }
    }
    
    protected void processBuilderDotPropertiesFile(String type, String name, File dotPropertiesFile, int propertyIndex, HashMap<String, String> attributes) {
        if (type.equals("Target")) {
            Target target = addTarget(Boolean.valueOf(attributes.get("j2me")), dotPropertiesFile.getParentFile().getPath(), attributes.get("dependsOn"), attributes.get("extraClassPath"), attributes.get("extraSourceDirs"));
            String triggers = attributes.get("triggers");
            if (triggers != null) {
                target.triggers(triggers);
            }
            String extraArgs = attributes.get("extraArgs");
            if (extraArgs != null) {
                target.addExtraArg(extraArgs);
            }
            target.addCopyJ2meDirs(attributes.get("copyJ2meDirs"));
        } else {
            throw new BuildException("Unsupported type " + type + " on property at index " + propertyIndex + " in file " + dotPropertiesFile.getPath());
        }
    }
    
    /**
     * Copy files from sourceRootPath, contained in subPaths, to destinationRootPath.  If any of the files being copy already exist
     * in siblingRootPath, then ignore it.
     * 
     * @param sourceRootPath
     * @param destinationRootPath
     * @param siblingRootPath
     * @param commentOutOverrideAnnotation
     * @param subPaths
     */
    public void copy(String sourceRootPath, String destinationRootPath, String siblingRootPath, String... subPaths) {
        copy(sourceRootPath, destinationRootPath, siblingRootPath, false, subPaths);
    }
    
    /**
     * Copy files from sourceRootPath, contained in subPaths, to destinationRootPath.  If any of the files being copy already exist
     * in siblingRootPath, then ignore it.
     * 
     * @param sourceRootPath
     * @param destinationRootPath
     * @param siblingRootPath
     * @param commentOutOverrideAnnotation
     * @param subPaths
     */
    public void copy(String sourceRootPath, String destinationRootPath, String siblingRootPath, boolean commentOutOverrideAnnotation, String... subPaths) {
        for (String subPath: subPaths) {
            boolean goDeep = subPath.equals("**");
            if (goDeep) {
                subPath = "";
            }
            File sourceDir = new File(sourceRootPath, subPath);
            File[] sourceFiles = sourceDir.listFiles();
            File destinationDir = new File(destinationRootPath, subPath);
            boolean didNoCopy = true;
            File siblingDir = null;
            if (siblingRootPath != null) {
                siblingDir = new File(siblingRootPath, subPath);
            }
            if (sourceFiles != null) {
                for (File sourceFile: sourceFiles) {
                    if (sourceFile.isDirectory()) {
                        if (goDeep) {
                            String path = sourceFile.getName();
                            copy(sourceRootPath + File.separator + path, destinationRootPath + File.separator + path, siblingRootPath + File.separator + path, commentOutOverrideAnnotation, "**");
                        }
                        continue;
                    }
                    if (siblingDir != null) {
                        File siblingFile = new File(siblingDir, sourceFile.getName());
                        if (siblingFile.exists()) {
                            continue;
                        }
                    }
                    if (didNoCopy) {
                        mkdir(destinationDir);
                        didNoCopy = false;
                    }
                    File destinationFile = new File(destinationDir, sourceFile.getName());
                    cp(sourceFile, destinationFile, false, commentOutOverrideAnnotation);
                }
            }
            if (!goDeep && didNoCopy) {
                throw new RuntimeException("Did not find any entries to copy from: " + sourceDir.getPath());
            }
        }
    }
    
    /**
     * Installs the built-in commands.
     */
    private void installBuiltinCommands() {
    	
    	List<File> dotPropertiesFiles = new ArrayList<File>();
    	addSiblingBuilderDotPropertiesFiles(new File("."), dotPropertiesFiles);
    	processBuilderDotPropertiesFiles(dotPropertiesFiles);

        addGen("OPC",                "cldc/src");
        addGen("OperandStackEffect", "translator/src");
        addGen("Mnemonics",          "translator/src");
        addGen("Verifier",           "translator/src");
        addGen("SwitchDotC",         "vmcore/src");

        // Add the "clean" command
        addCommand(new Command(this, "clean") {
            public String getDescription() {
                return "cleans one or more targets (all targets if no argument is given)";
            }
            public void run(String[] args) {
                // We do this in order to clean up some references that may still exist to .jar files that may be about to be deleted
                // This is only really an issue for Windows, and it cannot really be fixed as the .jar files are used by javac code
                // We should file bugs against javac for this TODO
                // The following is ONLY a suggestion, there is no guarantee it will work, but hoping it will increase likelihood of success.
                // On Windows I have proven that this does indeed work, at end of GC files we're closed :(
                System.gc();
                System.runFinalization();
                if (args.length == 0) {
                    log(info, "[cleaning all...]");
                    for (Iterator<Command> iterator = commands.values().iterator(); iterator.hasNext(); ) {
                        Command cmd = iterator.next();
                        cmd.clean();
                    }
                    clearFilesMarkedAsSvnIgnore(new File("."));
                    Build.clear(new File("builder", "classes"), true);
                } else {
                    for (int i = 0; i != args.length; ++i) {
                        Command cmd = getCommand(args[i]);
                        if (cmd == null) {
                            throw new BuildException("unknown target for cleaning: " + args[i]);
                        }
                        log(info, "[cleaning " + cmd + "...]");
                        cmd.clean();
                    }
                }
            }
        });

        // Add the "copyphoneme" command
        addCommand(new Command(this, "copyphoneme") {

            @Override
            public void clean() {
                delete(new File("cldc/phoneme"));
                delete(new File("imp/phoneme"));
            }

            public String getDescription() {
                return "copies the source code from the phoneME source tree into ours in order to be able to compile cldc and imp";
            }
            
            public void run(String[] args) {
//			When I remove the phoneme source from our source tree
//            	String phoneMeSourceRoot = "../phoneme/";
            	String phoneMeSourceRoot = "phoneme/";
                copy(phoneMeSourceRoot + "cldc/src/javaapi/cldc1.1", "cldc/phoneme", "cldc/src", "java/io", "java/lang", "java/lang/ref", "java/util", "javax/microedition/io", "com/sun/cldc/io/");
            	copy(phoneMeSourceRoot + "midp/src/core/javautil/classes", "cldc/phoneme", "cldc/src", "java/lang");
            	copy(phoneMeSourceRoot + "midp/src/core/javautil/reference/classes", "cldc/phoneme", "cldc/src", "java/lang");
            	copy(phoneMeSourceRoot + "midp/src/core/javautil/classes", "imp/phoneme", "imp/src", "java/util");
            	copy(phoneMeSourceRoot + "midp/src/core/javautil/reference/classes", "imp/phoneme", "imp/src", "java/util");
            	// copy(phoneMeSourceRoot + "midp/src/core/javautil/reference/classes", "imp/phoneme", "imp/src", "com/sun/midp/util/DateParser.java");
                cp(new File(phoneMeSourceRoot + "midp/src/core/javautil/reference/classes", "com/sun/midp/util/DateParser.java"), new File("imp/phoneme", "com/sun/midp/util/DateParser.java"), false);

            	copy(phoneMeSourceRoot + "midp/src/protocol/http/classes", "imp/phoneme", "imp/src", "javax/microedition/io");
            	cp(new File(phoneMeSourceRoot + "midp/src/ams/ams_api/reference/classes", "javax/microedition/midlet/MIDletStateChangeException.java"), new File("imp/phoneme", "javax/microedition/midlet/MIDletStateChangeException.java"), false);
            	cp(new File(phoneMeSourceRoot + "midp/src/ams/ams_api/reference/classes", "javax/microedition/midlet/package.html"),                    new File("imp/phoneme", "javax/microedition/midlet/package.html"), false);
            	cp(new File(phoneMeSourceRoot + "midp/src/ams/ams_api/reference/classes", "com/sun/midp/midlet/MIDletTunnel.java"), new File("imp/phoneme", "com/sun/midp/midlet/MIDletTunnel.java"), false);
            	copy(phoneMeSourceRoot + "midp/src/rms/rms_api/classes", "imp/phoneme", "imp/src", "javax/microedition/rms");
            	copy(phoneMeSourceRoot + "midp/src/rms/rms_api/reference/classes", "imp/phoneme", "imp/src", "javax/microedition/rms");
            	copy(phoneMeSourceRoot + "midp/src/rms/rms_exc/reference/classes", "imp/phoneme", "imp/src", "javax/microedition/rms");
            }
        });

        // Add the "copyjavacard3" command
        addCommand(new Command(this, "copyjavacard3") {
            public String getDescription() {
                return "copies the source code from the javacard3 source tree into ours in order to be able to compile it to run on Squawk";
            }
            
            public void run(String[] args) {
                File sdkSource = new File("../api/sdk-src");
                delete(sdkSource);
                sdkSource.mkdirs();
                File sdkLib = new File("../api/sdk-lib");
                delete(sdkLib);
                sdkLib.mkdirs();
                File cryptoLib = new File(sdkLib, "crypto");
                cryptoLib.mkdirs();
                String sourceRoot = "../bundles/sdk/src/";
                copy(sourceRoot + "api", sdkSource.getPath(), "../api/src", true, "**");
                extractJar(new File("../bundles/sdk/src/crypto.jar"), cryptoLib);
                retroweave(sdkLib, cryptoLib);
                boolean success = new File(sdkLib, "weaved").renameTo(new File(sdkLib, "crypto-weaved"));
                if (!success) {
                    throw new BuildException("Failed to rename dir");
                }
            }
        });

        // Add the "jvmenv" command
        addCommand(new Command(this, "jvmenv") {
            public String getDescription() {
                return "displays required environment variables for running the Squawk VM";
            }
            public void run(String[] args) {
                platform.showJNIEnvironmentMessage(System.out);
            }
        });

        // Add the "genspec" command
        addJavaCommand("genspec", "build.jar:build-commands.jar:cldc/classes", true, "", "com.sun.squawk.builder.bytecodespec.sbdocgen", "cldc").
            setDescription("generates the Squawk bytecode specification in doc/spec");

        // Add the "romize" command
        addJavaCommand("romize", "hosted-support/classes.jar:romizer/classes.jar:cldc/classes.jar:translator/classes.jar", false, "", "com.sun.squawk.Romizer", "romizer").
            setDescription("processes a number of classes to produce a .suite file");

        // Add the "traceviewer" command
        addJavaCommand("traceviewer", "hosted-support/classes:cldc/classes:translator/classes", false, "", "com.sun.squawk.traces.TraceViewer", "hosted-support").
            setDescription("the Squawk VM execution trace GUI viewer");

        // Add the "profileviewer" command
        addJavaCommand("profileviewer", "hosted-support/classes:cldc/classes", false, "", "com.sun.squawk.traces.ProfileViewer", "hosted-support").
            setDescription("the Squawk VM execution profile GUI viewer");

        // Add the "gctf" command
        addJavaCommand("gctf", "hosted-support/classes:cldc/classes", false, "", "com.sun.squawk.traces.GCTraceFilter", "hosted-support").
            setDescription("filter that converts method addresses in a garbage collector trace to signatures");

        // Add the "ht2html" command
        addJavaCommand("ht2html", "hosted-support/classes:cldc/classes", false, "", "com.sun.squawk.ht2html.Main", "hosted-support").
            setDescription("converts a heap trace to a set of HTML files");

        // Add the "rom" command
        addCommand(new RomCommand(this)).dependsOn("SwitchDotC");

        // Add the "spp" command
        addCommand(new SppFilePreprocessCommand(this));

        // Add the "documentor" command
        addCommand(new DocumentorCommand(this));

        // Add the "export" command
        addCommand(new ExportCommand(this));

        // Add the "jam" command
        addCommand(new JamCommand(this));
        
        // Add the "squawk" command
        addJavaCommand("squawk", "hosted-support/classes:cldc/classes", false, "-Djava.library.path=.", "com.sun.squawk.vm.Main", "hosted-support").
            setDescription("use Java based launcher to start Squawk VM");

        // Add the "makeapi" command
        addCommand(new MakeAPI(this));

        // Add the "makeueistubs" command
        addCommand(new MakePlatformStubs(this)).dependsOn("imp");

        // Add the "map" command
        addJavaCommand("map", "hosted-support/classes:mapper/classes:cldc/classes:translator/classes", false, "", "com.sun.squawk.ObjectMemoryMapper", "mapper").
            setDescription("suite file symbolic mapper/disassembler");

        // Add the "omfconv" command
        addJavaCommand("omfconv", "hosted-support/classes:mapper/classes:cldc/classes:translator/classes", false, "", "com.sun.squawk.ObjectMemoryFileEndianessConverter", "mapper").
            setDescription("object memory file endianess converter");

        // Add the "sdproxy" command
        addJavaCommand("sdproxy", "romizer/classes:hosted-support/classes:debugger/classes:debugger-proxy/classes:cldc/classes:translator/classes", false, "", "com.sun.squawk.debugger.sdp.SDP", "debugger-proxy").
            setDescription("Debugger proxy for translating between Squawk VM and normal JDPA debuggers");

        // Add the "hexdump" command
        addJavaCommand("hexdump", "hosted-support/classes", false, "", "com.sun.squawk.util.HexDump", "hosted-support").
            setDescription("hex file dump");

        // Add "systemproperties" command
        addJavaCommand("systemproperties", "hosted-support/classes:cldc/classes", false, "", "com.sun.squawk.io.j2se.systemproperties.Protocol", "hosted-support").
            setDescription("shows the default system properties");

        // Add "squawk.jar" command
        addCommand(new Command(this, "squawk.jar") {
            public void run(String[] args) {
                String cmd = "jar cf squawk.jar @" + new File("hosted-support", "squawk.jar.filelist");
                log(info, "[running '" + cmd + "' ...]");
                exec(cmd);
            }
            public String getDescription() {
                return "(re)builds squawk.jar (the classes required by the embedded JVM in Squawk)";
            }
        });

        // Add the "comp_test" command
        addCommand(new Command(this, "comp_test") {  // compiler test: run only 1 test
            public void run(String[] args) {
                System.out.println("Running compiler test ");
                String testName = "com.sun.squawk.compiler.tests." + args[0];
                System.out.println (testName);
                exec("java -cp " + toPlatformPath("compiler/classes:hosted-support/classes:cldc/classes ", true) + testName);
            }
        });

        addCommand(new Command(this, "arm_asm_tests") {  // run JUnit tests for the ARM assembler
            public void run(String[] args) {
                System.out.println("Running JUnit test for the ARM assembler ");
                exec("java -cp " + toPlatformPath("compilertests/junit-3.8.1.jar:compiler/classes:hosted-support/classes:cldc/classes:compilertests/classes ", true) +
                     "com.sun.squawk.compiler.asm.arm.tests.ArmTests");
            }
        });


        // Add "vm2c" target
        // We need to compute the classpath for tools.jar in case we were launched from a JRE
        // vm2c needs the tools.jar to get to the javac compiler
        URL toolsJarURL = Launcher.getToolsJar(verbose);
        String toolsJarPathEntry;
        String toolsJarPath;
        if (toolsJarURL == null) {
            	toolsJarPath = "";
            	toolsJarPathEntry = "";
        } else {
            File f;
            try {
                f = new File(toolsJarURL.toURI());
            } catch(URISyntaxException e) {
                f = new File(toolsJarURL.getPath());
            }
            toolsJarPath = f.getPath();
            toolsJarPathEntry = toolsJarPath + ":";
        }
        addTarget(false,  "vm2c", null, toolsJarPath, null);

        // Add "runvm2c" target
        addJavaCommand("runvm2c", toolsJarPathEntry + "vm2c/classes:cldc/classes", false, "", "com.sun.squawk.vm2c.Main", "vm2c").
        	setDescription("runs the VM Java source file to C converter");
        
        // Add the "user-compile" command
        addCommand(new Command(this, "user-compile") {
            public String getDescription() {
                return "compile a user-project";
            }
            public void run(String[] args) {
                int argi = 0;
                String cp = "";
                while (args[argi].startsWith("-")) {
                    if (args[argi].startsWith("-cp:")) {
                        cp = args[argi].substring("-cp:".length());
                    } else {
                        throw new BuildException("malformed option " + args[argi]);
                    }
                    argi++;
                }
                String userBaseDir = args[argi];
                log(brief, "[compiling user project at " + userBaseDir + "...]");
                
                Target compileTarget = addTarget(true, userBaseDir, "cldc imp", cp);
                compileTarget.run(NO_ARGS);
            }
        });
        
        // Add the "user-suite" command
        addCommand(new Command(this, "user-suite") {
            public String getDescription() {
                return "link a user-project";
            }
            public void run(String[] args) {
                int argi = 0;
                String parent = "";
                String cp = "-cp:.";
                while (args[argi].startsWith("-")) {
                    if (args[argi].startsWith("-parent:")) {
                        parent = args[argi].substring("-parent:".length());
                    } if (args[argi].startsWith("-cp:")) {
                        cp = args[argi];
                    } else {
                        throw new BuildException("malformed option " + args[argi]);
                    }
                    argi++;
                }
                String userBaseDir = args[argi];
                String userModule = new File(userBaseDir).getName();
                Command compileCommand = getCommand("user-compile");
                String[] cmpArgs = new String[2];
                cmpArgs[0] = cp;
                cmpArgs[1] = userBaseDir;
                compileCommand.run(cmpArgs);
                
                String suitepath = "";
                try {
                    suitepath =  new File(".").getCanonicalPath()  + ":" +  new File(userBaseDir).getCanonicalPath();
                } catch (IOException e) {
                    System.out.println("Bad suite path for " + userBaseDir);
                    System.out.println(e);
                }
                if (parent.length() != 0) {
                    suitepath = suitepath + ":" + parent;
                }
                System.out.println("linking user project at " + userBaseDir);
                
                Command romize = getCommand("romize");
                String jarfile = userBaseDir+"/j2meclasses";
                String[] rargs = new String[9];
                rargs[0] ="-cp:" + jarfile;
                 rargs[1] = "-suitepath:" + suitepath;
                 rargs[2] = "-boot:squawk";
                if (parent.length() == 0) {
                    rargs[3] = "-metadata"; // dummay arg
                } else {
                    rargs[3] = "-parent:" + parent + "/" + parent;
                }
                rargs[4] = "-o:" + userBaseDir + "/" + userModule;
                rargs[5] = "-v";
                rargs[6] = "-metadata";
                rargs[7] = userBaseDir + "/j2meclasses";
                rargs[8] = userBaseDir + "/res";
                romize.run(rargs);
            }
        });
        
        // Add the "user-clean" command
        addCommand(new Command(this, "user-clean") {
            public String getDescription() {
                return "clean a user-project";
            }
            public void run(String[] args) {
                String userBaseDir = args[0];
                log(brief, "[cleaning user project at " + userBaseDir + ']');
                
                Target compileTarget = addTarget(true, userBaseDir, "cldc imp");
                compileTarget.clean();
                Build.delete(new File(userBaseDir, userBaseDir + ".suite"));
                Build.delete(new File(userBaseDir, userBaseDir + ".suite.api"));
                Build.delete(new File(userBaseDir, userBaseDir + ".suite.metadata"));
                Build.delete(new File(userBaseDir, userBaseDir + ".sym"));
            }
        });
        
        // Add the "uei" command
        addCommand(new UEICommand(this));
               
    }

    /**
     * Gets the command with a given name.
     *
     * @param name   the name of the command to get
     * @return the command registered with the given name or null if there is no such command
     */
    public Command getCommand(String name) {
        return commands.get(name);
    }

    /**
     * Gets the command with a given name.
     *
     * @param name   the name of the command to get
     * @return the command registered with the given name or null if there is no such command
     */
    public Command getCommandForced(String name) {
        Command command = commands.get(name);
        if (command == null) {
            throw new BuildException("command not found: " + name);
        }
        return command;
    }

    /**
     * Runs a command. The dependencies of the command are run first if {@link #checkDependencies} is <code>true</code>.
     * The command will not be run if <code>hasBeenRun(getCommand(name)) == true</code>.
     *
     * @param name  the name of the command to run. The special value "<all>" will run all the targets.
     * @param args  its arguments
     */
    public void runCommand(final String name, String[] args) {
        if (name.equals("<all>")) {
            for (Command command: commands.values()) {
                if (command instanceof Target) {
                    run((Target) command, NO_ARGS);
                }
            }
            runCommand("squawk.jar", NO_ARGS);
        } else {
            Command command = getCommand(name);
            if (command == null) {
                throw new BuildException("unknown command: " + name);
            }
            run(command, args);
        }
    }

    /**
     * Runs a command. The dependencies of the command are run first if {@link #checkDependencies} is <code>true</code>.
     * The command will not be run if <code>hasBeenRun(command) == true</code>.
     *
     * @param command  the command to run
     * @param args     its arguments
     */
    public void run(Command command, String[] args) {
        if (checkDependencies) {
            for (String dependencyName: command.getDependencyNames()) {
                Command dependency = getCommandForced(dependencyName);
                if (!runSet.contains(dependency)) {
                    run(dependency, NO_ARGS);
                }
            }
        }
        if (!runSet.contains(command)) {
            if (command instanceof Target) {
                log(brief, "[building " + command.getName() + "...]");
            } else if (command instanceof GeneratorCommand) {
                log(info, "[generating " + command.getName() + "...]");
            } else {
                log(brief, "[running " + command.getName() + "...]");
            }
            if (verbose) {
            	log(info, "  description: " + command.getDescription());
            	for (int i=0; i < args.length; i++) {
                	log(info, "  arg[" + i +"]: " + args[i]);
            	}
            	log(info, "  description: " + command.getDescription());
            }
            command.run(args);
            runSet.add(command);

            for (String triggeredCommandName: command.getTriggeredCommandNames()) {
                Command triggeredCommand = getCommandForced(triggeredCommandName);
                run(triggeredCommand, NO_ARGS);
            }
        }
    }

    /**
     * The set of commands that have been run.
     */
    private Set<Command> runSet = new HashSet<Command>();

    private boolean isWantingPpcCompilerOnMac;

    /**
     * Clears the set of commands that have been run.
     */
    public void clearRunSet() {
        runSet.clear();
    }

    /**
     * Determines if a given command has been run.
     *
     * @param command   the command to test
     * @return true if <code>command</code> has not been run since the last call to {@link #clearRunSet}.
     */
    public boolean hasBeenRun(Command command) {
        return runSet.contains(command);
    }

    public static final String[] NO_ARGS = {};

    /*---------------------------------------------------------------------------*\
     *                          General utilities                                *
    \*---------------------------------------------------------------------------*/

    /**
     * Converts a given file or class path to the correct format for the
     * underlying platform. For example, if the underlying platform uses
     * '/' to separate directories in a file path then any instances of
     * '\' in <code>path</code> will be converted to '/'.
     *
     * @param   path         to the path to convert
     * @param   isClassPath  specifies if <code>path</code> is a class path
     * @return  the value of <code>path</code> reformatted (if necessary) to
     *                be correct for the underlying platform
     */
    public static String toPlatformPath(String path, boolean isClassPath) {
        char fileSeparatorChar = File.separatorChar;
        if (fileSeparatorChar == '/') {
            path = path.replace('\\', '/');
        } else if (fileSeparatorChar == '\\') {
            path = path.replace('/', '\\');
        } else {
            throw new RuntimeException("OS with unknown separator: '" + fileSeparatorChar + "'");
        }
        if (isClassPath) {
            char pathSeparatorChar = File.pathSeparatorChar;
            if (pathSeparatorChar == ':') {
                path = path.replace(';', ':');
            } else if (pathSeparatorChar == ';') {
                // Need special processing so as to not convert "C:\" into "C;\"
                char[] pathChars = path.toCharArray();
                int start = 0;
                for (int i = 0; i != pathChars.length; ++i) {
                    if (pathChars[i] == ':' || pathChars[i] == ';') {
                        if (i - start == 1) {
                            // If there is only a single character between the start of the
                            // current path component and the next ':', we assume that this
                            // is a drive letter and so need to leave the ':' unchanged
                        } else {
                            pathChars[i] = ';';
                            start = i + 1;
                        }
                    }
                }

                path = new String(pathChars);
            } else {
                throw new RuntimeException("OS with unknown path separator: '"+ pathSeparatorChar+"'");
            }
        }
        return path;
    }

    /**
     * Fix a URL path if on Windows. This is a workaround that is implemented by javamake
     * as well. Here is the original description of the problem from the javamake sources:
     *
     *    On Windows, if a path is specified as "file://c:\...", (i.e. with the drive name) URLClassLoader works
     *    unbelievably slow. However, if an additional slash is added, like : "file:///c:\...", the speed becomes
     *    normal. To me it looks like a bug, but, anyway, I am taking measure here.
     *
     * @param path the path to fix
     * @return fixed URL
     */
    public String fixURL(String path) {
        if (getPlatform() instanceof Windows_X86) {
            if (path.charAt(1) == ':') {
                path = "/" + path;
            }
        }
        return path;
    }

    /**
     * Converts a given class path string to a URL array appropriate for creating a URLClassLoader.
     *
     * @param cp  a class path string
     * @return an array of "file://" URLs created from <code>cp</code>
     */
    public URL[] toURLClassPath(String cp) {
        String[] paths = toPlatformPath(cp, true).split(File.pathSeparator);
        URL[] urls = new URL[paths.length];
        for (int i = 0; i != paths.length; ++i) {
            File path = new File(paths[i]);
            try {
                String url = "file://" + fixURL(path.getAbsolutePath());
                if (path.isDirectory()) {
                    url += "/";
                }
                urls[i] = new URL(url);
            } catch (MalformedURLException e) {
                throw new BuildException("badly formed class path: " + cp, e);
            }
        }
        return urls;
    }

    /**
     * Folds an array of objects into a single string.
     *
     * @param   parts   the array to fold
     * @param   offset  the offset at which folding starts
     * @param   length  the numbers of elements to fold
     * @param   delim   the delimiter to place between the folded elements
     * @return  the folded string
     */
    public static String join(Object[] parts, int offset, int length, String delim) {
        StringBuffer buf = new StringBuffer(1000);
        for (int i = offset; i != (offset+length); i++) {
            buf.append(parts[i]);
            if (i != (offset+length)-1) {
                buf.append(delim);
            }
        }
        return buf.toString();
    }

    /**
     * Folds an array of objects into a single string.
     *
     * @param   parts   the array to fold
     * @return  the folded string
     */
    public static String join(Object[] parts) {
        return join(parts, 0, parts.length, " ");
    }

    /**
     * Folds a list of objects into a single string. The toString method is used to convert each object into a String.
     *
     * @param   list    the list to fold
     * @param   offset  the offset at which folding starts
     * @param   length  the numbers of elements to fold
     * @param   delim   the delimiter to place between the folded elements
     * @return  the folded string
     */
    public static String join(List<String> list, int offset, int length, String delim) {
        StringBuffer buf = new StringBuffer(1000);
        for (int i = offset; i != (offset+length); i++) {
            buf.append(list.get(i));
            if (i != (offset+length)-1) {
                buf.append(delim);
            }
        }
        return buf.toString();
    }

    /**
     * Ensures a specified directory exists, creating it if necessary.
     *
     * @param  path  the directory to test
     * @return the directory
     * @throws BuildException if <code>path</code> is not a directory or could not be created
     */
    public static File mkdir(File path) {
        if (!path.exists()) {
            if (!path.mkdirs()) {
                throw new BuildException("Could not create directory: " + path, 1);
            }
        } else {
            if (!path.isDirectory()) {
                throw new BuildException("Path is not a directory: " + path, 1);
            }
        }
        return path;
    }

    /**
     * Ensures a specified sub-directory exists, creating it if necessary.
     *
     * @param baseDir   the file in which the sub-directory will or does exist
     * @param subDir    the name of the sub-directory
     * @return the sub-directory
     */
    public static File mkdir(File baseDir, String subDir) {
        return mkdir(new File(baseDir, subDir));
    }

    /**
     * Searches for a file (or directory if <code>isDir == true</code>) based on a given name.
     *
     * @param dir     the directory to search
     * @param name    the name of the file or directory to find
     * @param isDir   true if <code>name</code> denotes a directory, false otherwise
     * @return the found file or null if it wasn't found
     */
    public static File find(File dir, final String name, final boolean isDir) {
        final File[] result = new File[1];
        new FileVisitor() {
            public boolean visit(File file) {
                if (file.getName().equals(name) && file.isDirectory() == isDir) {
                    result[0] = file;
                    return false;
                } else {
                    return true;
                }
            }
        }.run(dir);
        return result[0];
    }

    /**
     * Deletes a file or directory. This method does nothing if the file or directory
     * denoted by <code>file</code> does not exist.
     *
     * @param  file       the file or directory to delete
     * @throws BuildException if the deletion did not succeed
     */
    public static void delete(File file) throws BuildException {
    	(new FileVisitor() {
    		public boolean visit(File file) {
                if (!file.delete()) {
                    throw new BuildException("cannot remove file/directory " + file.getPath());
                }
                return true;
    		}
    	}).run(file);
    }

	/**
	 * Clears a directory. This method does nothing if <code>dir.exists() == false</code>.
	 *
	 * @param  dir       the file or directory to clear
	 * @param  deleteDir if true, <code>dir</code> is deleted once cleared
	 * @throws BuildException if the <code>dir</code> is a file or clearing did not succeed
	 */
	public static void clear(final File dir, boolean deleteDir) throws BuildException {
	    if (dir.exists()) {
	        if (dir.isDirectory()) {
	            new FileVisitor() {
	                public boolean visit(File file) {
	                    if (!file.equals(dir)) {
	                        if (!file.delete()) {
	                            throw new BuildException("cannot remove file/directory " + file.getPath());
	                        }
	                    }
	                    return true;
	                }
	            }.run(dir);
	
	            if(deleteDir) {
	                Build.delete(dir);
	            }
	        } else {
	            throw new BuildException("cannot clear non-directory " + dir.getPath());
	        }
	    }
	}
    
    /**
     * Remove all entries in dir that are listed in the SVN ignore properties of that dir.
     *
     * @param  dir       the file or directory to clear
     * @param	except dont delete these though
     * @throws BuildException if the <code>dir</code> is a file or clearing did not succeed
     */
    public static void clearFilesMarkedAsSvnIgnore(final File dir, String... except) throws BuildException {
        if (!dir.exists()) {
            return;
        }
        if (!dir.isDirectory()) {
            throw new BuildException("cannot clear non-directory " + dir.getPath());
        }
        List<File> ignoreEntries = SvnUtil.getIgnoreEntries(new File("."));
        ignoreEntries.remove(new File("./.hg"));
        for (File file : ignoreEntries) {
            String name = file.getName();
            boolean delete = true;
            for (String doNotDeleteName : except) {
                if (name.equals(doNotDeleteName)) {
                    delete = false;
                    break;
                }
            }
            if (delete) {
                delete(file);
            }
        }
    }

    /**
     * Copies a file. The parent directory of <code>to</code> is created if it doesn't exist.
     *
     * @param from    the source file
     * @param to      the destination file
     * @param append  if true, the content of <code>from</code> is appended to <code>to</code>
     * @throws BuildException if the copy failed
     */
    public static void cp(File from, File to, boolean append) {
        cp(from, to, append, false);
    }
    
    /**
     * Copies a file. The parent directory of <code>to</code> is created if it doesn't exist.
     *
     * @param from    the source file
     * @param to      the destination file
     * @param append  if true, the content of <code>from</code> is appended to <code>to</code>
     * @throws BuildException if the copy failed
     */
    public static void cp(File from, File to, boolean append, boolean commentOutOverrideAnnotation) {
        File toFileDir = to.getParentFile();
        mkdir(toFileDir);
        try {
            if (commentOutOverrideAnnotation) {
                BufferedInputStream input = new BufferedInputStream(new FileInputStream(from));
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(to, append));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));

                String line;
                while ((line = reader.readLine()) != null) {
                    int index = line.indexOf("@Override");
                    if (index != -1) {
                        line = line.substring(0, index -1) + "//" + line.substring(index);
                    }
                    writer.write(line);
                    writer.newLine();
                }
                try {writer.close();} catch (IOException e) {}
                try {reader.close();} catch (IOException e) {}
            } else {
                DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(from)));
                OutputStream output = new BufferedOutputStream(new FileOutputStream(to, append));

                byte[] content = new byte[(int)from.length()];
                input.readFully(content);
                output.write(content);
                try {input.close();} catch (IOException e) {}
                try {output.close();} catch (IOException e) {}
            }
        } catch (FileNotFoundException ex) {
            throw new BuildException("copying " + from + " to " + to + " failed", ex);
        } catch (IOException ex) {
            throw new BuildException("copying " + from + " to " + to + " failed", ex);
        }
    }

    /**
     * Executes the UNIX chmod command on a file.
     *
     * @param path   the file to chmod
     * @param mode   the mode of the chmod
     * @return true if the command succeeded, false otherwise
     */
    public boolean chmod(File path, String mode) {
        try {
            exec("chmod " + mode + " " + path);
            return true;
        } catch (BuildException e) {
            return false;
        }
    }

    /**
     * Logs a message to the console if a given condition is true.
     *
     * @param b   only write the message to the console if this is true
     * @param msg the message to write
     */
    public void log(boolean b, String msg) {
        if (b) {
            System.out.println(msg);
        }
    }

    /*---------------------------------------------------------------------------*\
     *                           Command line interface                          *
    \*---------------------------------------------------------------------------*/

    /**
     * Creates an instance of the builder.
     */
    public Build(String buildDotOverrideFileName) {
        File defaultProperties = new File("build.properties");
        if (defaultProperties.exists()) {
            properties = loadProperties(defaultProperties, null);
            if (buildDotOverrideFileName == null) {
                buildDotOverrideFileName = "build.override";
            } else {
                specfifiedBuildDotOverrideFileName = buildDotOverrideFileName;
            }
            File overideProperties = new File(buildDotOverrideFileName);
            if (overideProperties.exists()) {

                // Make it very clear to the user which properties in the standard properties
                // file are potentially being overridden
                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Using build override file: " + overideProperties.getPath() + " <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
                properties = loadProperties(overideProperties, properties);
            }
        } else {
            throw new BuildException("could not find build.properties");
        }

        platform = Platform.createPlatform(this);
        ccompiler = platform.createDefaultCCompiler();
        jdk = new JDK(platform.getExecutableExtension());
        preprocessor = new Preprocessor();
        preprocessor.properties = properties;
        macroizer = new Macroizer();
        installBuiltinCommands();
        javaCompiler = new JavaCompiler(this);
        isJava5SyntaxSupported = false;
        if (getProperty("JAVA5SYNTAX") != null) {
            isJava5SyntaxSupported = getBooleanProperty("JAVA5SYNTAX");
        }
    }
    
    /**
     * Prints some information describing the builder's configuration.
     */
    private void printConfiguration() {

        log(info, "os=" + platform);
        log(info, "java.home=" + jdk.getHome());
        log(info, "java.vm.version=" + System.getProperty("java.vm.version"));
        if (ccompiler != null) {
            log(info, "C compiler=" + ccompiler.name);
        }

        if (verbose) {
            log(info, "Builder properties:");
            Enumeration<?> keys = properties.propertyNames();
            while (keys.hasMoreElements()) {
                String name = (String)keys.nextElement();
                log(info, "    " + name + '=' + properties.getProperty(name));
            }
        }
    }

    /**
     * The command line entry point for the builder.
     *
     * @param args  the command line arguments
     */
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        String buildDotOverrideFileName = null;
        if (args != null && args.length > 0) {
        	String arg = args[0];
	        if (arg.startsWith("-override:")) {
	        	buildDotOverrideFileName = arg.substring("-override:".length());
		        String[] newArgs = new String[args.length - 1];
		        System.arraycopy(args, 1, newArgs, 0, newArgs.length);
		        args = newArgs;
	        } else if (arg.startsWith("-override")) {
                if (args.length == 1) {
                    throw new BuildException("Did not specify override file name for -override");
                }
                buildDotOverrideFileName = args[1];
                String[] newArgs = new String[args.length - 2];
                System.arraycopy(args, 2, newArgs, 0, newArgs.length);
                args = newArgs;
            }
        }
        Build builder = new Build(buildDotOverrideFileName);
        try {
            builder.mainProgrammatic(args);
            System.out.println("Total time: " + ((System.currentTimeMillis() - start) / 1000) + "s");
        } catch (BuildException e) {
            if (e.exitValue != 0) {
                System.err.println("build failed: " + e.getMessage());
                if (builder == null || builder.verbose) {
                    e.printStackTrace();
                } else {
                    if (e.getCause() != null) {
                        System.err.print("caused by: ");
                        e.getCause().printStackTrace(System.err);
                    }
                }
            }
            System.exit(e.exitValue);
        }
    }

    /**
     * The programmatic entry point for the builder.
     *
     * @param args  the equivalent to the command line arguments
     */
    public void mainProgrammatic(String... args) {
        int startArgsIndex = 0;
        if (args != null && args.length > 0) {
            String arg = args[0];
            if (arg.startsWith("-override:")) {
                startArgsIndex++;
            }
        }
        args = extractBuilderArgs(args, startArgsIndex);
        printConfiguration();

        String name = args[0];
        if (args.length > 1) {
            String[] newArgs = new String[args.length - 1];
            System.arraycopy(args, 1, newArgs, 0, newArgs.length);
            args = newArgs;
        } else {
            args = NO_ARGS;
        }

        runCommand(name, args);
    }

    /**
     * Lists the builder's commands.
     */
    private void listCommands(boolean javaCompilationCommands, PrintStream out) {
        for (Map.Entry<String, Command> entry: commands.entrySet()) {
            Command c = entry.getValue();
            if (javaCompilationCommands == (c instanceof Target)) {
                String name = entry.getKey();
                while (name.length() < 19) {
                    name += ' ';
                }
                out.println("    " + name + " " + c.getDescription());
            }
        }
    }

    /**
     * Print the usage message.
     *
     * @param errMsg  an optional error message
     */
    public void usage(String errMsg) {

        PrintStream out = System.err;

        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("Usage: build [ build-options] [ target | command [ command_options ] ] ");
        out.println("where build-options include:");
        out.println();
        out.println("    -jpda:<port>        start JVM listening for JPDA connection on 'port'");
        out.println("    -jmem:<mem>         java memory option shortcut for '-java:-Xmx<mem>'");
        out.println("    -java:<opts>        extra java options (e.g. '-java:-Xms128M')");
        out.println("    -fork               executes Java commands in a separate JVM process");
        out.println("    -info               informational execution");
        out.println("    -verbose            verbose execution");
        out.println("    -q                  quiet execution");
        out.println("    -64                 build for a 64 bit system");
        out.println("    -nodeps             do not check dependencies (default for commands)");
        out.println("    -deps               check dependencies (default for targets)");
        out.println("    -plugins:<file>     load commands from properties in 'file'");
        out.println("    -override <file>");
        out.println("    -override:<file>    file to use to override the build.properties file found locally, defaults to build.override");
        out.println("                        MUST BE SPECIFIED AS FIRST BUILD-OPTION");
        out.println("    -D<name>=<value>    sets a builder property");
        out.println("    -h                  show this usage message and exit");
        out.println();
        out.println("--- Options only applying to targets ---");
        out.println();
        out.println("    -javac:<opts>       extra javac options (e.g. '-javac:-g:none')");
        out.println("    -doccheck           run javadoc checker after compilation");
        out.println("    -javadoc            generate complete javadoc after compilation");
        out.println("    -javadoc:api        generate API javadoc after compilation");
        out.println("    -kernel             Alex????");
        out.println("    -hosted             Alex????");
        out.println();
        out.println("--- C compilation only options ---");
        out.println();
        out.println("    -comp:<name>        the C compiler used to build native executables");
        out.println("                        Supported: 'msc', 'gcc' or 'cc' or 'gcc-macosx' (any");
        out.println("                        other value disables C compilaton)");
        out.println("    -nocomp             disables C compilation (but not C file preprocessing)");
        out.println("    -cflags:<opts>      extra C compiler options (e.g. '-cflags:-g')");
        out.println("    -dll                builds squawk DLL instead of executable");
        out.println("    -o1                 optimize C compilation/linking for minimal size");
        out.println("    -o2                 optimize C compilation/linking for speed");
        out.println("    -o3                 optimize C compilation/linking for max speed");
        out.println("    -prod               build the production version of the VM");
        out.println("    -mac                build the interpreter loop using fast macros");
        out.println("    -tracing            enable tracing in the VM");
        out.println("    -profiling          enable profiling in the VM");
        out.println("    -assume             enable assertions in the VM");
        out.println("    -typemap            enable type checking in the VM");
        out.println("    -ppccompiler        Hack to make MAc OS X Intel create a ppc arch exe");
        out.println();
        out.println();
        out.println();
        out.println("The supported targets are:");
        listCommands(true, out);
        out.println();
        out.println("The supported commands are:");
        listCommands(false, out);
        out.println();
        out.println("If no command or target is given then all targets are brought up to date.");
    }

    /**
     * Loads a set of properties from a properties file.
     *
     * @param file     the properties file from which to load
     * @param defaults the properties being extended/overridden (if any)
     * @return the loaded properties or null if <code>file</code> does not exist
     * @throws BuildException if <code>file</code> exists and there was an error while loading
     */
    private Properties loadProperties(File file, Properties defaults) throws BuildException {
        try {
            Properties properties;
            if (defaults == null) {
                properties = new SubstitutionProperties();
            } else {
                properties = new SubstitutionProperties(defaults) {
                    private static final long serialVersionUID = 6268985530668173269L;

                    public Object put(Object key, Object value) {
                        String oldValue = defaults.getProperty((String)key);
                        if (oldValue != null && !oldValue.equals(value)) {
                            System.out.println(">>>>>>> Overwrote " + key + " property: " + oldValue + " --> " + value);
                        }
                        return super.put(key, value);
                    }
                };
            }
            FileInputStream inputStream =  new FileInputStream(file);
            try {
                properties.load(inputStream);
            } finally {
                inputStream.close();
            }

            if (file.lastModified() > propertiesLastModified) {
                propertiesLastModified = file.lastModified();
            }
            return properties;
        } catch (IOException e) {
            throw new BuildException("error loading properties from " + file, e);
        }
    }

    /**
     * Changes the C compiler based on a given name. The new compiler will share the options of the old compiler.
     * If <code>name</code> does not denote a compiler known to the builder, then C compilation is disabled.
     *
     * @param name  the name of the new C compiler to use
     */
    private void updateCCompiler(String name) {
        CCompiler.Options options = ccompiler.options;
        if (name.equals("msc")) {
            ccompiler = new MscCompiler(this, platform);
        } else if (name.equals("gcc")) {
            ccompiler = new GccCompiler(this, platform);
        } else if (name.equals("gcc-macox")) {
            ccompiler = new GccMacOSXCompiler(this, platform);
        } else if (name.equals("cc")) {
            ccompiler = new CcCompiler(this, platform);
        } else {
            System.out.println("Unknown C compiler '" + name + "' - C compilation disabled");
            ccompiler = null;
            return;
        }
        ccompiler.options = options;
    }

    /**
     * Updates one of the build properties and updates the {@link propertiesLastModified} field
     * if the new value differs from the old value.
     *
     * @param name    the property's name
     * @param value   the property's new value
     * @param isBooleanProperty specifies if this is a boolean property
     */
    private void updateProperty(String name, String value, boolean isBooleanProperty) {
        String old = isBooleanProperty ? properties.getProperty(name, "true") : properties.getProperty(name);
        if (!value.equals(old)) {
            properties.setProperty(name, value);
            propertiesLastModified = System.currentTimeMillis();
            log(verbose, "[build properties updated]");
        }
    }

    /**
     * Determines if a boolean property has the value "true". The value is implicitly
     * true if it does not have an explicit value.
     *
     * @param name  the property's name
     * @return true if the property is true
     */
    public boolean getBooleanProperty(String name) {
        return properties.getProperty(name, "true").equals("true");
    }

    /**
     * Gets the value of a builder property.
     *
     * @param name  the property's name
     * @return the property's value
     */
    public String getProperty(String name) {
        return properties.getProperty(name);
    }

    /**
     * Parses and extracts the command line arguments passed to the builder that apply to the
     * builder in general as opposed to the command about to be run.
     *
     * @param args  the command line arguments
     * @return <code>args</code> after the builder specific args have been extracted
     */
    private String[] extractBuilderArgs(String[] args, int startIndex) {

        builderArgs.clear();

        int argc = startIndex;
        CCompiler.Options cOptions = ccompiler.options;
        boolean production = false;

        // Reset the default state for -tracing and -assume
        cOptions.tracing = false;
        cOptions.assume = false;

        String depsFlag = null;
        boolean help = false;

        while (args.length > argc) {
            String arg = args[argc];
            if (arg.charAt(0) != '-') {
                // Finished parsing builder args
                break;
            }

            builderArgs.add(arg);
            if (arg.equals("-dll")) {
                dll = true;
            } else if (arg.startsWith("-D")) {
                try {
                    String name = arg.substring("-D".length(), arg.indexOf('='));
                    String value = arg.substring(arg.indexOf('=') + 1);
                    updateProperty(name, value, false);
                } catch (IndexOutOfBoundsException e) {
                    usage("malformed -D option: " + arg);
                    throw new BuildException("malformed option");
                }
            } else if (arg.startsWith("-comp:")) {
                String compName = arg.substring("-comp:".length()).toLowerCase();
                updateCCompiler(compName);
            } else if (arg.equals("-nocomp")) {
                RomCommand rom = (RomCommand)getCommand("rom");
                rom.enableCompilation(false);
            } else if (arg.equals("-comp")) {
                RomCommand rom = (RomCommand)getCommand("rom");
                rom.enableCompilation(true);
            } else if (arg.equals("-mac")) {
                cOptions.macroize = true;
            } else if (arg.startsWith("-plugins:")) {
                File pluginsSpecified = new File(arg.substring("-plugins:".length()));
                List<File> dotPropertiesFiles = new ArrayList<File>();
                if (pluginsSpecified.isFile()) {
                    dotPropertiesFiles.add(pluginsSpecified);
                } else {
                    File pluginsFile = new File(pluginsSpecified, "builder.properties");
                    if (pluginsFile.isFile() && pluginsFile.exists()) {
                        dotPropertiesFiles.add(pluginsFile);
                    } else {
                        addPossibleModuleDir(pluginsSpecified);
                        addSiblingBuilderDotPropertiesFiles(pluginsSpecified, dotPropertiesFiles);
                    }
                }
                processBuilderDotPropertiesFiles(dotPropertiesFiles);
            } else if (arg.startsWith("-cflags:")) {
                cOptions.cflags += " " + arg.substring("-cflags:".length());
            } else if (arg.startsWith("-jpda:")) {
                String port = arg.substring("-jpda:".length());
                javaOptions += " -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=" + port;
            } else if (arg.startsWith("-jmem:")) {
                String mem = arg.substring("-jmem:".length());
                javaOptions += " -Xms" + mem + " -Xmx" + mem;
            } else if (arg.equals("-fork")) {
                forkJavaCommand = true;
            } else if (arg.startsWith("-java:")) {
                javaOptions += " " + arg.substring("-java:".length());
            } else if (arg.startsWith("-javac:")) {
                javacOptions += " " + arg.substring("-javac:".length());
            } else if (arg.equals("-javadoc")) {
                runJavadoc = true;
            } else if (arg.equals("-javadoc:api")) {
                runJavadoc = true;
                runJavadocAPI = true;
            } else if (arg.equals("-doccheck")) {
                runDocCheck = true;
            } else if (arg.equals("-o1")) {
                cOptions.o1 = true;
            } else if (arg.equals("-o2")) {
                cOptions.o2 = true;
            } else if (arg.equals("-o3")) {
                cOptions.o2 = true;
                cOptions.o3 = true;
            } else if (arg.startsWith("-prod")) {
                production = true;
            } else if (arg.equals("-64")) {
                cOptions.is64 = true;
            } else if (arg.equals("-tracing")) {
                cOptions.tracing = true;
            } else if (arg.equals("-profiling")) {
                cOptions.profiling = true;
            } else if (arg.equals("-kernel")) {
                cOptions.kernel = true;
            } else if (arg.equals("-hosted")) {
                cOptions.hosted = true;
            } else if (arg.equals("-assume")) {
                cOptions.assume = true;
            } else if (arg.equals("-typemap")) {
                cOptions.typemap = true;
            } else if (arg.equals("-verbose")) {
                verbose = true;
                info = true;
                brief = true;
            } else if (arg.equals("-info")) {
                info = true;
                brief = true;
            } else if (arg.equals("-q")) {
                verbose = false;
                info = false;
                brief = false;
            } else if (arg.equals("-deps")) {
                depsFlag = "-deps";
            } else if (arg.equals("-nodeps")) {
                depsFlag = "-nodeps";
            } else if (arg.equals("-h")) {
                help = true;
            } else if (arg.equals("-ppccompiler")) {
                if (!platform.isMacOsX()) {
                    throw new BuildException("Can only force ppccompiler on Mac OS X");
                }
                isWantingPpcCompilerOnMac = true;
                ccompiler = platform.createDefaultCCompiler();
                ccompiler.options = cOptions;
            } else {
                usage("Unknown option "+arg);
                throw new BuildException("invalid option");
            }
            argc++;
        }

        if (help) {
            // Show help and exit
            usage(null);
            throw new BuildException("", 0);
        }

        // Synchronize MACROIZE property with '-mac'
        if (cOptions.macroize != getBooleanProperty("MACROIZE")) {
            cOptions.macroize |= getBooleanProperty("MACROIZE");
            updateProperty("MACROIZE", cOptions.macroize ? "true" : "false", true);
        }

        // Synchronize SQUAWK_64 property with '-64'
        if (cOptions.is64 != getBooleanProperty("SQUAWK_64")) {
            cOptions.is64 |= getBooleanProperty("SQUAWK_64");
            updateProperty("SQUAWK_64", cOptions.is64 ? "true" : "false", true);
        }
        preprocessor.processS64 = getBooleanProperty("SQUAWK_64");

        // Synchronize KERNEL_SQUAWK property with '-kernel'
        if (cOptions.kernel != getBooleanProperty("KERNEL_SQUAWK")) {
            cOptions.kernel |= getBooleanProperty("KERNEL_SQUAWK");
            updateProperty("KERNEL_SQUAWK", cOptions.kernel ? "true" : "false", true);
        }
        
        
          // Synchronize NATIVE_VERIFICATION property with '-nativeVerification'
        if (cOptions.nativeVerification != getBooleanProperty("NATIVE_VERIFICATION")) {
            cOptions.nativeVerification |= getBooleanProperty("NATIVE_VERIFICATION");
            updateProperty("NATIVE_VERIFICATION", cOptions.nativeVerification ? "true" : "false", true);
        }

        // Synchronize ASSERTIONS_ENABLED
        preprocessor.assertionsEnabled |= getBooleanProperty("ASSERTIONS_ENABLED");

        // Initialize C compiler floats flag
        cOptions.floatsSupported = getBooleanProperty("FLOATS");

        if (properties.getProperty("WRITE_BARRIER") != null) {
            log(info, "Warning: the WRITE_BARRIER is already set.  This property must not be set explicity as it is derived from the GC property");
        }
        if (properties.getProperty("LISP2_BITMAP") != null) {
            log(info, "Warning: the LISP2_BITMAP is already set.  This property must not be set explicity as it is derived from the GC property");
        }
        if (properties.getProperty("GC", "").equals("com.sun.squawk.Lisp2GenerationalCollector")) {
            properties.setProperty("WRITE_BARRIER", "true");
            cOptions.cflags += " -DWRITE_BARRIER";
        } else {
            properties.setProperty("WRITE_BARRIER", "false");
        }
        if (properties.getProperty("GC", "").startsWith("com.sun.squawk.Lisp2")) {
            properties.setProperty("LISP2_BITMAP", "true");
            cOptions.cflags += " -DLISP2_BITMAP";
        } else {
            properties.setProperty("LISP2_BITMAP", "false");
        }

        String optimize;
        optimize = properties.getProperty("O1");
        if (optimize !=null && optimize.equals("true")) {
            cOptions.o1 = true;
        }
        optimize = properties.getProperty("O2");
        if (optimize !=null && optimize.equals("true")) {
            cOptions.o2 = true;
        }
        optimize = properties.getProperty("O3");
        if (optimize !=null && optimize.equals("true")) {
            cOptions.o2 = true;
            cOptions.o3 = true;
        }

        // The -tracing, and -assume options are turned on by default if -production was not specified
        String productionProperty = properties.getProperty("PRODUCTION");
        if (!production && (productionProperty == null || !productionProperty.equals("true"))) {
            cOptions.tracing = true;
            cOptions.assume = true;
        }

        // If no arguments were supplied, then
        if (argc == args.length) {

            checkDependencies = true;
            if (depsFlag != null) {
                checkDependencies = (depsFlag == "-deps");
            }

            return new String[] { "<all>" };
        } else {
            String[] cmdAndArgs = new String[args.length - argc];
            System.arraycopy(args, argc, cmdAndArgs, 0, cmdAndArgs.length);

            checkDependencies = (getCommand(args[argc]) instanceof Target);
            if (depsFlag != null) {
                checkDependencies = (depsFlag == "-deps");
            }

            return cmdAndArgs;
        }


    }

    /*---------------------------------------------------------------------------*\
     *                 JDK tools (javac, javadoc) execution                      *
    \*---------------------------------------------------------------------------*/


    /**
     * The selector used for finding the Java source files in a directory.
     */
    public static final FileSet.Selector JAVA_SOURCE_SELECTOR = new FileSet.SuffixSelector(".java");

    /**
     * The selector used for finding the Java class files in a directory.
     */
    public static final FileSet.Selector JAVA_CLASS_SELECTOR = new FileSet.SuffixSelector(".class");

    /**
     * The selector used for finding the HTML files in a directory.
     */
    public static final FileSet.Selector HTML_SELECTOR = new FileSet.SuffixSelector(".html");

    /**
     * Runs javadoc over a set of Java source files.
     *
     * @param   baseDir      the parent directory under which the "javadoc" directory exists (or will be created) for the output
     * @param   classPath    the class path option used to compile the source files. This is used to find the javadoc
     *                       generated for the classes in the class path.
     * @param   srcDirs      the directories containing the input files
     * @param   packages     the names of the packages whose sources are to be processed
     */
    public void javadoc(File baseDir, String classPath, File[] srcDirs, String packages) {
        File docDir = mkdir(new File("docs"), "Javadoc");
        File javadocDir = mkdir(docDir, baseDir.getName());

        log(info, "[running javadoc (output dir: " + javadocDir + ") ...]");

        String srcPath = "";
        String linkOptions = "";
        if (classPath != null) {
            StringTokenizer st = new StringTokenizer(classPath, File.pathSeparator);
            while (st.hasMoreTokens()) {
                File path = new File(st.nextToken());
                if (path.getName().equals("classes")) {
                    String pathBase = path.getParent();
                    linkOptions += " -link ../" + pathBase;             //" -link ../../" + pathBase + "/javadoc";
                    srcPath += File.pathSeparator + new File(pathBase, "src");
                }
            }
        }

        for (int i = 0; i != srcDirs.length; ++i) {
            srcPath += File.pathSeparator + srcDirs[i];
        }

        javaCompiler.javadoc((" -d "+javadocDir+
             (classPath == null ? "" : " -classpath " + classPath) +
             " -taglet com.sun.squawk.builder.ToDoTaglet -tagletpath build.jar:build-commands.jar " +
             linkOptions +
             (runJavadocAPI ? "" : " -private") +
             " -quiet" +
             " -breakiterator" +
             " -sourcepath " + srcPath + " "+
             packages).split("\\s+"), false);
    }

    /**
     * Runs the Doc Check javadoc doclet over a set of Java source files.
     *
     * @param   baseDir      the parent directory under which the "doccheck" directory exists (or will be created) for the output
     * @param   classPath    the class path option used to compile the source files. This is used to find the javadoc
     *                       generated for the classes in the class path.
     * @param   srcDirs      the directories containing the input files
     * @param   packages     the names of the packages whose sources are to be processed
     */
    public void doccheck(File baseDir, String classPath, File[] srcDirs, String packages) {

        File doccheckDir = mkdir(baseDir, "doccheck");
        log(info, "[running doccheck (output dir: " + doccheckDir + ") ...]");

        String srcPath = "";
        for (int i = 0; i != srcDirs.length; ++i) {
            srcPath += File.pathSeparator + srcDirs[i];
        }

        exec(jdk.javadoc() + " -d "+ doccheckDir +
             (classPath == null ? "" : " -classpath " + classPath) +
             " -execDepth 2" +
             " -evident 5" +
             " -private" +
             " -sourcepath " + srcPath +
             " -docletpath " + new File("tools", "doccheck.jar") +
             " -doclet com.sun.tools.doclets.doccheck.DocCheck " +
             packages
            );
    }

    /**
     * Extracts a list of packages from a given set of Java source files.
     *
     * @param fs   the Java source files
     * @param packages the list to append any unique package names to
     */
    private void extractPackages(FileSet fs, List<String> packages) {
    	for (File file: fs.list()) {
            assert file.getPath().endsWith(".java");

            // Make file relative
            String packagePath = fs.getRelativePath(file.getParentFile());
            if (packagePath.length() > 0) {
                String pkg = packagePath.replace(File.separatorChar, '.');
                if (!packages.contains(pkg)) {
                    packages.add(pkg);
                }
            }
    	}
    }

    /**
     * Runs javadoc and doccheck doclet over a set of sources that have just been compiled.
     *
     * @param classPath the class path that was used to compile the sources
     * @param baseDir   the directory under which the "preprocessed" and "classes" directories exist
     * @param srcDirs   the directories containing the input files
     */
    private void runDocTools(String classPath, File baseDir, File[] srcDirs) {
        boolean preprocessed = srcDirs.length == 1 && srcDirs[0].getName().equals("preprocessed");
        List<String> packageList = new ArrayList<String>();
        for (File srcDir: srcDirs) {
            extractPackages(new FileSet(srcDir, JAVA_SOURCE_SELECTOR), packageList);
        }
        String packages = join(packageList, 0, packageList.size(), " ");

        if (runDocCheck) {
            doccheck(baseDir, classPath, srcDirs, packages);
        }
        if (runJavadoc) {
            if (preprocessed) {
                File preprocessedDir = new File(baseDir, "preprocessed");
                // Copy all the .html files in the dirs to the "preprocessed" dir
                for (int i = 0; i != srcDirs.length; ++i) {
                    File srcDir = srcDirs[i];
                    FileSet htmlFiles = new FileSet(srcDir, HTML_SELECTOR);
                    for (File htmlFile: htmlFiles.list()) {
                        File toHtmlFile = htmlFiles.replaceBaseDir(htmlFile, preprocessedDir);
                        cp(htmlFile, toHtmlFile, false);
                    }
                }
            }

            // Run javadoc
            javadoc(baseDir, classPath, srcDirs, packages);

            if (preprocessed) {
                // Copy the "doc-files" directories
                for (int i = 0; i != srcDirs.length; ++i) {
                    File srcDir = srcDirs[i];
                    DirSet docFileDirs = new DirSet(srcDir, new FileSet.NameSelector("doc-files"));
                    for (File docFileDir: docFileDirs.list()) {
                        if (docFileDir.isDirectory()) {
                            File toDocFileDir = docFileDirs.replaceBaseDir(docFileDir, new File(baseDir, "javadoc"));
                            FileSet docFiles = new FileSet(docFileDir, (FileSet.Selector)null);
                            for (File docFile: docFiles.list()) {
                                if (docFile.getPath().indexOf("CVS") == -1) {
                                    File toDocFile = docFiles.replaceBaseDir(docFile, toDocFileDir);
                                    cp(docFile, toDocFile, false);
                                }
                            }
                        }
                    }
                }
            }
        }
    }



    /**
     * Compiles a set of Java sources into class files with a Java source compiler. The sources
     * are initially {@link Preprocessor preprocessed} if <code>preprocess == true</code> and
     * the output is written into the "preprocessed" directory. The sources are then compiled
     * with the Java compiler into the "classes" directory. If <code>j2me == true</code> then
     * the class files in "classes" are preverified and written to "j2meclasses".
     *
     * @param   classPath  the class path
     * @param   baseDir    the base directory for generated directories (i.e. "preprocessed", "classes" and "j2meclasses")
     * @param   srcDirs    the set of directories that are searched recursively for the Java source files to be compiled
     * @param   j2me       specifies if the classes being compiled are to be deployed on a J2ME platform
     * @param   version    set the java language version (default is 1.5 if version is null)
     * @param   extraArgs  extra javac arguments
     * @param   preprocess runs the {@link Preprocessor} over the sources if true
     */
    public void javac(String compileClassPath, String preverifyClassPath, File baseDir, File[] srcDirs, boolean j2me, List<String> extraArgs, boolean preprocess) {

        // Preprocess the sources if required
        if (preprocess) {
            srcDirs = new File[] { preprocess(baseDir, srcDirs, j2me, false) };
        }

        // Get the javac output directory
        File classesDir = mkdir(baseDir, "classes");

        // Prepare and run the Java compiler
        javaCompiler.reset();
        if (!isJava5SyntaxSupported() && j2me) {
            // This is required to make the preverifier happy
            javaCompiler.arg("-target", "1.2");
            javaCompiler.arg("-source", "1.3");
        }
        if (extraArgs != null) {
            for (String arg: extraArgs) {
                javaCompiler.arg(arg);
            }
        }

        javaCompiler.arg("-g").args(javacOptions);
        javaCompiler.compile(compileClassPath, classesDir, srcDirs, j2me);
        
        if (isJava5SyntaxSupported() && j2me) {
            classesDir = retroweave(baseDir, classesDir);
        }

        // Run the doccheck and javadoc utils
        if (runDocCheck || runJavadoc) {
            runDocTools(compileClassPath, baseDir, srcDirs);
        }

        // Run the preverifier for a J2ME compilation
        if (j2me) {
            preverify(preverifyClassPath, baseDir);
        }
    }
    
    /**
     * Retroweave a given set of Java class files.
     *
     * @param   baseDir    the directory under which the "weaved" directory will be created
     * @param   classesDirs    the set of directories that are searched recursively for the .class files that will be weaved
     * @return the retroweaved output directory
     */
    public File retroweave(File baseDir, File classesDir) {
        final File retroweavedDir = mkdir(baseDir, "weaved");
        log(info, "[running 'retroweave " + classesDir + " into " + retroweavedDir + "...]");
        SquawkRetroWeaver weaver = new SquawkRetroWeaver();
        weaver.retroweave(classesDir, retroweavedDir, new WeaveListener() {
            public void weavingStarted(String msg) {
                log(verbose, "  " + msg);
            }
            public void weavingCompleted(String msg) {
                log(verbose, "  " + msg);
            }
            public void weavingError(String msg) {
                throw new BuildException("Weaving Error: " + msg);
            }
            public void weavingPath(String sourcePath) {
                log(verbose, sourcePath);
            }
        });
        return retroweavedDir;
    }
    
    /**
     * Preprocess a given set of Java source files.
     *
     * @param   baseDir    the directory under which the "preprocessed" directory exists
     * @param   srcDirs    the set of directories that are searched recursively for the source files to be compiled
     * @param   j2me       specifies if the classes being compiled are to be deployed on a J2ME platform
     * @return the preprocessor output directory
     */
    public File preprocess(File baseDir, File[] srcDirs, boolean j2me, boolean vm2c) {
        // Get the preprocessor output directory
        final File preprocessedDir;
        boolean resetJava5Syntax = false;
        if (vm2c && !isJava5SyntaxSupported()) {
            log(brief, "[preprocessing with forced JAVA5SYNTAX for vm2c]");
            preprocessedDir = mkdir(baseDir, "preprocessed-vm2c");
            updateProperty("JAVA5SYNTAX", "true", true);
            isJava5SyntaxSupported = true;
            resetJava5Syntax = true;
        } else {
            preprocessedDir = mkdir(baseDir, "preprocessed");
        }

        // Preprocess the sources
        preprocessor.processAssertions = j2me;
        preprocessor.verbose = verbose;
        // @TODO: Should be true for desktop builds. host == target?
        preprocessor.showLineNumbers = true;

        for (int i = 0; i != srcDirs.length; ++i) {

            File sourceDir = srcDirs[i];

            // A selector that matches a source file whose preprocessed copy does not exist,
            // is younger than the source file or has a last modification date
            // earlier than the last modification date of the properties
            FileSet.Selector outOfDate = new FileSet.DependSelector(new FileSet.SourceDestDirMapper(sourceDir, preprocessedDir)) {
                public boolean isSelected(File file) {
                    if (super.isSelected(file)) {
                        return true;
                    }
                    File preprocessedFile = getMapper().map(file);
                    long fileLastModified = preprocessedFile.lastModified();
                    return (fileLastModified < propertiesLastModified);
                }
            };

            FileSet.Selector selector = new FileSet.AndSelector(JAVA_SOURCE_SELECTOR, outOfDate);
            FileSet fs = new FileSet(sourceDir, selector);
            preprocessor.execute(fs, preprocessedDir);
        }

        if (resetJava5Syntax) {
            updateProperty("JAVA5SYNTAX", "false", true);
            isJava5SyntaxSupported = false;
        }

        return preprocessedDir;
    }

    /**
     * Run the CLDC preverifier over a set of classes in the "classes" directory
     * and write the resulting classes to the "j2meclasses" directory.
     *
     * @param   classPath  directories in which to look for classes
     * @param   baseDir    the directory under which the "j2meclasses" and "classes" directories
     */
    public void preverify(String classPath, File baseDir) {


        // Get the preverifier input and output directories
        File classesDir = isJava5SyntaxSupported()?new File(baseDir, "weaved"):new File(baseDir, "classes");
        File j2meclassesDir = mkdir(baseDir, "j2meclasses");

        // See if any of the classes actually need re-preverifying
        FileSet.Selector outOfDate = new FileSet.DependSelector(new FileSet.SourceDestDirMapper(classesDir, j2meclassesDir));
        if (!new FileSet(classesDir, outOfDate).list().iterator().hasNext()) {
            return;
        }

        log(info, "[running preverifier ...]");

        // Ensure that the preverifier is executable which may not be the case if
        // Squawk was checked out with a Java CVS client (Java doesn't know anything
        // about 'execute' file permissions and so cannot preserve them).
        chmod(platform.preverifier(), "+x");

        if (classPath == null) {
            classPath = "";
        } else {
            classPath = " -classpath " + toPlatformPath(classPath, true);
        }
        exec(platform.preverifier() + (verbose?" -verbose ":"") + classPath + " -d " + j2meclassesDir + " " + classesDir);
    }

    /*---------------------------------------------------------------------------*\
     *                     Jar-like tool                                         *
    \*---------------------------------------------------------------------------*/

    /**
     * Creates a jar file from a given set of files. This is roughly equivalent to the
     * functionality of the jar command line tool when using the 'c' switch.
     *
     * @param out      the jar file to create
     * @param fileSets    the files to put in the jar file
     * @param manifest the entries that will used to create manifest in the jar file.
     *                 If this value is null, then no manifest will be created
     */
    public void createJar(File out, FileSet[] fileSets, Manifest manifest) {
        try {
            FileOutputStream fos = new FileOutputStream(out);

            // Manifest version must exist otherwise a null manifest is written jar.
            if(manifest != null) {
                if(!manifest.getMainAttributes().containsKey(Attributes.Name.MANIFEST_VERSION)) {
                    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "0.1 [Squawk Builder]");
                }
            }
            ZipOutputStream zos = manifest == null ? new JarOutputStream(fos) : new JarOutputStream(fos, manifest);
            for (FileSet fs: fileSets) {
                for (File file: fs.list()) {
                    String entryName = fs.getRelativePath(file).replace(File.separatorChar, '/');
                    ZipEntry e = new ZipEntry(entryName);
                    e.setTime(file.lastModified());
                    if (file.length() == 0) {
                        e.setMethod(ZipEntry.STORED);
                        e.setSize(0);
                        e.setCrc(0);
                    }
                    zos.putNextEntry(e);
                    byte[] buf = new byte[1024];
                    int len;
                    InputStream is = new BufferedInputStream(new FileInputStream(file));
                    while ((len = is.read(buf, 0, buf.length)) != -1) {
                        zos.write(buf, 0, len);
                    }
                    is.close();
                    zos.closeEntry();
                }
            }
            zos.close();
        } catch (IOException e) {
            throw new BuildException("IO error creating jar file", e);
        }
    }

    public void extractJar(File source, File destination) {
        try {
            JarFile jarFile = new JarFile(source);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                File file = new File(destination, entry.getName());
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                    InputStream in = new BufferedInputStream(jarFile.getInputStream(entry));
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                    byte[] buffer = new byte[2048];
                    for (;;) {
                        int nBytes = in.read(buffer);
                        if (nBytes <= 0)
                            break;
                        out.write(buffer, 0, nBytes);
                    }
                    out.flush();
                    out.close();
                    in.close();
                }
            }
        } catch (IOException e) {
            throw new BuildException("IO error extracting jar file", e);
        }
    }

    /*---------------------------------------------------------------------------*\
     *                     Java command execution                                *
    \*---------------------------------------------------------------------------*/

    /**
     * Executes a Java program. The program will be executed in a JVM as a sub-process if
     * the '-fork' command line switch was used or if the command requires special JVM
     * switches or if the command must be run by extending the boot class path.
     *
     * @param classPath         the classpath
     * @param bootclasspath     specifies if the class path should be appended to boot class path
     * @param vmArgs            extra arguments to be passed to the JVM
     * @param mainClassName     the main class of the program to run
     * @param appArgs           the arguments to be passed to the application
     * @throws BuildException if a sub-process is used and it does not exit with 0
     */
    public void java(String classPath, boolean bootclasspath, String vmArgs, String mainClassName, String[] appArgs) {

        if (!forkJavaCommand && !bootclasspath && (vmArgs == null || vmArgs.length() == 0)) {
            if (info) {
                StringBuffer buffer = new StringBuffer();
                buffer.append("[exec ");
                buffer.append(mainClassName);
                buffer.append(".main(");
                for (int i=0; i < appArgs.length; i++) {
                    if (i > 0) {
                        buffer.append(", ");
                    }
                    buffer.append('\"');
                    buffer.append(appArgs[i]);
                    buffer.append('\"');
                }
                buffer.append(" ...]");
                log(info, buffer.toString());
            }
            Method main = null;
            URL[] cp = toURLClassPath(classPath);
            ClassLoader loader = new URLClassLoader(cp);
            try {
                Class<?> mainClass = loader.loadClass(mainClassName);
                main = mainClass.getMethod("main", new Class[] {String[].class});
            } catch (ClassNotFoundException e) {
                log(verbose, "[could not find class " + mainClassName + " - spawning new JVM]");
            } catch (NoSuchMethodException e) {
                log(verbose, "[could not find method \"main\" in class " + mainClassName + " - spawning new JVM]");
            }

            if (main != null) {
                try {
                    main.invoke(null, new Object[] { appArgs });
                    return;
                } catch (IllegalAccessException e) {
                    throw new BuildException("cannot reflectively invoke " + main, e);
                } catch (InvocationTargetException e) {
                    throw new BuildException("error invoking " + main, e.getTargetException());
                } catch (IllegalArgumentException e) {
                    throw new BuildException("error invoking " + main, e);
                }
            }
        }

        exec(jdk.java() + " " +
             (bootclasspath ? "-Xbootclasspath/a:" : "-cp ") +
             toPlatformPath(classPath, true) + " " +
             vmArgs + " " +
             javaOptions + " " +
             mainClassName + " " + join(appArgs));
    }
    
	/*---------------------------------------------------------------------------*\
     *                          System command execution                         *
    \*---------------------------------------------------------------------------*/

    /**
     * Executes a command in a sub-process
     *
     * @param cmd  the command to execute
     * @throws BuildException if the sub-process does not exit with 0
     */
    public void exec(String cmd) {
        exec(cmd, null, null);
    }

    /**
     * Executes a command in a sub-process
     *
     * @param cmd  the command to execute
     * @param envp array of strings, each element of which has environment
     *             variable settings in the format name=value, or null if the
     *             subprocess should inherit the environment of the current process.
     * @param dir  the working directory of the subprocess, or null if the subprocess
     *             should inherit the working directory of the current process.
     * @throws BuildException if the sub-process does not exit with 0
     */
    public void exec(String cmd, String[] envp, File dir) {
        log(info, "[exec '" + cmd + "' ...]");
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(cmd, envp, dir);
            StreamGobbler errorGobbler  = new StreamGobbler(process.getErrorStream(), System.err);
            StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), System.out);
            errorGobbler.start();
            outputGobbler.start();

            int exitVal = process.waitFor();
            errorGobbler.join();
            outputGobbler.join();
            log(verbose, "EXEC result =====> " + exitVal);
            if (exitVal != 0) {
                throw new BuildException("Process.exec("+cmd+") returned "+exitVal, exitVal);
            }
        } catch (IOException ioe) {
            throw new BuildException("IO error during Process.exec("+cmd+")", ioe);
        } catch (InterruptedException ie) {
            throw new BuildException("Process.exec("+cmd+") was interuppted ", ie);
        } finally {
            if (process != null) {
                process.destroy();
                process = null;
            }
        }
    }

    /**
     * Return true if Java5 syntax should be supported.
     * 
     * @return
     */
    public boolean isJava5SyntaxSupported() {
        return isJava5SyntaxSupported;
    }
    
    public boolean isWantingPpcCompilerOnMac() {
        return isWantingPpcCompilerOnMac;
    }
    
}