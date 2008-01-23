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

import com.sun.squawk.builder.Build;
import com.sun.squawk.builder.BuildException;
import com.sun.squawk.builder.Command;
import com.sun.squawk.builder.Target;
import com.sun.squawk.builder.util.FileSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;


/**
 * Command to build the UEI.
 *
 *
 * @since Apr 12, 2007 12:10:19 PM
 */
public class UEICommand extends Command {
    // UEI output dirs
    private static final String UEI_BUILD_DIR = "uei-built";
    private static final String UEI_BUILD_DIR_BIN = UEI_BUILD_DIR + File.separator + "bin";
    private static final String UEI_BUILD_DIR_LIB = UEI_BUILD_DIR + File.separator + "lib";

    // Common UEI paths
    private static final String UEI_COMMON_JAR_FINALNAME = "uei-common.jar";
    private static final String UEI_COMMON_JAR_FINALPATH = UEI_BUILD_DIR_LIB + File.separator +
                                                           UEI_COMMON_JAR_FINALNAME;
    private static final String UEI_COMMON_PATH = "uei" + File.separator + "uei-common";
    private static final String UEI_COMMON_JAR_OUTPUT = UEI_COMMON_PATH + File.separator + "classes.jar";
    private static final String UEI_COMMON_SRC_PATH = UEI_COMMON_PATH + File.separator + "src" + File.separator +
                                                      "main" + File.separator + "java";

    // Squawk-specific UEI paths
    private static final String UEI_SQUAWK_JAR_FINALNAME = "uei-squawk.jar";
    private static final String UEI_SQUAWK_JAR_FINALPATH = UEI_BUILD_DIR_LIB + File.separator +
                                                           UEI_SQUAWK_JAR_FINALNAME;
    private static final String UEI_SQUAWK_PATH = "uei" + File.separator + "uei-squawk";
    private static final String UEI_SQUAWK_JAVAC_OUTPUT = UEI_SQUAWK_PATH + File.separator + "classes";
    private static final String UEI_SQUAWK_JAR_OUTPUT = UEI_SQUAWK_PATH + File.separator + "classes.jar";
    private static final String UEI_SQUAWK_RESOURCE_PATH = UEI_SQUAWK_PATH + File.separator + "src" + File.separator +
                                                           "main" + File.separator + "resources";
    private static final String UEI_SQUAWK_SRC_PATH = UEI_SQUAWK_PATH + File.separator + "src" + File.separator +
                                                      "main" + File.separator + "java";

    // Emulator binaries
    private static final String UEI_EMULATOR_BINARY_FILENAME_UNIX = "emulator";
    private static final String UEI_EMULATOR_BINARY_FILENAME_WINDOWS = "emulator.exe";
    private static final String UEI_SQUAWK_EMULATOR_BINARY_UNIX = UEI_SQUAWK_PATH + File.separator + "uei-bins" +
                                                                  File.separator + UEI_EMULATOR_BINARY_FILENAME_UNIX;
    private static final String UEI_SQUAWK_EMULATOR_BINARY_WINDOWS = UEI_SQUAWK_PATH + File.separator + "uei-bins" +
                                                                     File.separator +
                                                                     UEI_EMULATOR_BINARY_FILENAME_WINDOWS;
    private static final String UEI_EMULATOR_FINALPATH_UNIX = UEI_BUILD_DIR_BIN + File.separator +
                                                              UEI_EMULATOR_BINARY_FILENAME_UNIX;
    private static final String UEI_EMULATOR_FINALPATH_WINDOWS = UEI_BUILD_DIR_BIN + File.separator +
                                                                 UEI_EMULATOR_BINARY_FILENAME_WINDOWS;
    private static final String SQUAWK_EXECUTABLE = "squawk";
    private static final String SQUAWK_EXECUTABLE_FINALPATH = UEI_BUILD_DIR_BIN + File.separator + SQUAWK_EXECUTABLE;

    // Build environment
    private Build env;

    public UEICommand(final Build env) {
        super(env, "uei");
        this.env = env;
    }

    /**
     * Removes all the files generated by running this command.
     */
    public void clean() {
        // Clean common UEI stuff
        File srcPaths[] = new File[1];
        srcPaths[0] = new File("uei" + File.separator + "uei-common" + File.separator + "src" + File.separator +
                               "main" + File.separator + "java");

        Target command = new Target(null /*classPath*/, false, "uei" + File.separator + "uei-common", srcPaths, false,
                                    env);
        command.clean();

        // Clean Squawk-specific stuff
        srcPaths[0] = new File("uei" + File.separator + "uei-squawk" + File.separator + "src" + File.separator +
                               "main" + File.separator + "java");
        command = new Target(null /*classPath*/, false, "uei" + File.separator + "uei-squawk", srcPaths, false, env);
        command.clean();

        // Delete the build directory
        Build.clear(new File(UEI_BUILD_DIR), true);
    }

    /**
     * Gets a brief one-line description of what this command does.
     *
     * @return a brief one-line description of what this command does
     */
    public String getDescription() {
        return "Builds the Unified Emulator Interface(UEI) module";
    }

    /**
     * Gets the name of this command.
     *
     * @return the name of this command
     */
    public String getName() {
        return "uei";
    }

    /**
     * Runs the command.
     *
     * @param args the command line argmuents
     *
     * @throws com.sun.squawk.builder.BuildException if the command failed
     */
    public void run(final String args[]) throws BuildException {
        // Build common UEI stuff
        File srcPaths[] = new File[1];
        srcPaths[0] = new File(UEI_COMMON_SRC_PATH);

        Target command = new Target(null /*classPath*/, false, UEI_COMMON_PATH, srcPaths, false, env);
        command.run(null);

        // Build Squawk-specific stuff
        String classpath = UEI_COMMON_JAR_OUTPUT;
        srcPaths[0] = new File(UEI_SQUAWK_SRC_PATH);
        command = new Target(classpath, false, UEI_SQUAWK_PATH, srcPaths, false, env);
        command.run(null);

        // Make UEI output directory and underlying structure
        Build.mkdir(new File(UEI_BUILD_DIR));
        Build.mkdir(new File(UEI_BUILD_DIR_BIN));
        Build.mkdir(new File(UEI_BUILD_DIR_LIB));

        // Copy common UEI JAR to the appropriate location, with the appropriate manifest
        copyJarAndMutateManifest(new File(UEI_COMMON_JAR_OUTPUT), new File(UEI_COMMON_JAR_FINALPATH), null);

        // Create Squawk UEI JAR
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Class-Path", "uei-common.jar");
        manifest.getMainAttributes().putValue("Main-Class", "com.sun.squawk.uei.SquawkEmulator");

        FileSet jarFileSets[] = new FileSet[2];
        jarFileSets[0] = new FileSet(new File(UEI_SQUAWK_JAVAC_OUTPUT), new FileSet.NamePrefixSelector(""));
        jarFileSets[1] = new FileSet(new File(UEI_SQUAWK_RESOURCE_PATH), new FileSet.NamePrefixSelector(""));

        env.createJar(new File(UEI_SQUAWK_JAR_FINALPATH), jarFileSets, manifest);

        // Copy UEI binaries to the appropriate location
        Build.cp(new File(UEI_SQUAWK_EMULATOR_BINARY_UNIX), new File(UEI_EMULATOR_FINALPATH_UNIX), false);
        env.chmod(new File(UEI_EMULATOR_FINALPATH_UNIX), "755");
        Build.cp(new File(UEI_SQUAWK_EMULATOR_BINARY_WINDOWS), new File(UEI_EMULATOR_FINALPATH_WINDOWS), false);

        // Copy squawk to the appropriate location
        Build.cp(new File(SQUAWK_EXECUTABLE), new File(SQUAWK_EXECUTABLE_FINALPATH), false);
        env.chmod(new File(SQUAWK_EXECUTABLE_FINALPATH), "755");
        Build.cp(new File("squawk.suite"), new File("uei-built/lib/squawk.suite"), false);
        Build.cp(new File("squawk.jar"), new File("uei-built/lib/squawk.jar"), false);
        Build.cp(new File("squawk_classes.jar"), new File("uei-built/lib/squawk_classes.jar"), false);

        // Copy library JARs to the appropriate location
        Map<String, String> cldcJarManifestKeys = new HashMap<String, String>();
        cldcJarManifestKeys.put("API", "CLDC");
        cldcJarManifestKeys.put("API-Name", "Connected Limited Device Configuration");
        cldcJarManifestKeys.put("API-Specification-Version", "1.1");
        cldcJarManifestKeys.put("API-Type", "Configuration");
        copyJarAndMutateManifest(new File("cldc/classes.jar"), new File("uei-built/lib/cldc11.jar"), cldcJarManifestKeys);

        Map<String, String> impJarManifestKeys = new HashMap<String, String>();
        impJarManifestKeys.put("API", "IMP");
        impJarManifestKeys.put("API-Name", "Information Module Profile");
        impJarManifestKeys.put("API-Specification-Version", "1.0");
        impJarManifestKeys.put("API-Type", "Profile");
        impJarManifestKeys.put("API-Dependencies", "CLDC >= 1.0");
        copyJarAndMutateManifest(new File("imp/classes.jar"), new File("uei-built/lib/imp10.jar"), impJarManifestKeys);
    }

    private void copyJarAndMutateManifest(final File in, final File out,
                                          final Map<String, String> keyValuePairsToReplace) {
        try {
            // Open input JAR
            JarInputStream jarInputStream = new JarInputStream(new FileInputStream(in));

            // Grab manifest from input JAR
            Manifest inputManifest = jarInputStream.getManifest();
            Manifest outputManifest;

            if (inputManifest != null) {
                outputManifest = new Manifest(inputManifest);
            } else {
                outputManifest = new Manifest();
            }

            // Mutate manifest
            if (keyValuePairsToReplace != null) {
                for (final String key : keyValuePairsToReplace.keySet()) {
                    outputManifest.getMainAttributes().putValue(key, keyValuePairsToReplace.get(key));
                }
            }

            // Ensure manifest is sane
            if (!outputManifest.getMainAttributes().containsKey("Manifest-Version")) {
                outputManifest.getMainAttributes().putValue("Manifest-Version", "1.0");
            }

            // Open output JAR
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(out), outputManifest);

            // Write all JAR entries into the output JAR
            try {
                JarEntry jarEntry = jarInputStream.getNextJarEntry();
                byte buffer[] = new byte[4096];

                while (jarEntry != null) {
                    jarOutputStream.putNextEntry(jarEntry);

                    // Copy bytes
                    int readBytes = jarInputStream.read(buffer);

                    while (readBytes != -1) {
                        jarOutputStream.write(buffer, 0, readBytes);
                        readBytes = jarInputStream.read(buffer);
                    }

                    // Close entry
                    jarInputStream.closeEntry();
                    jarOutputStream.closeEntry();

                    // Get next entry
                    jarEntry = jarInputStream.getNextJarEntry();
                }
            } catch (final IOException ex) {
                ex.printStackTrace();
            }

            // Close streams
            jarInputStream.close();
            jarOutputStream.close();
        } catch (final IOException e) {
            throw new BuildException("Failed to copy JAR from " + in.getAbsolutePath() + " to " +
                                     out.getAbsolutePath(), e);
        }
    }
}
