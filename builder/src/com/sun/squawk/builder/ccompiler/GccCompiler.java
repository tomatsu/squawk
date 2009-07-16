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

package com.sun.squawk.builder.ccompiler;

import com.sun.squawk.builder.*;
import com.sun.squawk.builder.platform.*;
import java.io.*;

/**
 * The interface for the "gcc" compiler.
 */
public class GccCompiler extends CCompiler {

    public GccCompiler(String name, Build env, Platform platform) {
        super(name, env, platform);
    }

    public GccCompiler(Build env, Platform platform) {
        this("gcc", env, platform);
    }

    /**
     * {@inheritDoc}
     */
    public String options(boolean disableOpts) {
        StringBuffer buf = new StringBuffer();
        if (!disableOpts) {
            if (options.o1)                 { buf.append("-O1 ");               }
            if (options.o2)              { buf.append("-O2 ");               }
            // if (options.o2)                 { buf.append(" -Os  -finline-functions -finline-limit=55 --param max-inline-insns-single=55 -Winline  ");               }
            // think about -frtl-abstract-sequences, not in gcc 4.0.1 though.
            if (options.o3)                 { buf.append("-DMAXINLINE -O3 ");   }
//          if (options.o3)                 { buf.append("-DMAXINLINE -O3 -Winline ");   }
        }
        if (options.tracing)            { buf.append("-DTRACE ");           }
        if (options.profiling)          { buf.append("-DPROFILING ");       }
        if (options.macroize)           { buf.append("-DMACROIZE ");        }
        if (options.assume)             { buf.append("-DASSUME ");          }
        if (options.typemap)            { buf.append("-DTYPEMAP ");         }
        if (options.ioport)             { buf.append("-DIOPORT ");          }
        if (options.kernel)             { buf.append("-DKERNEL_SQUAWK=true ");     }
        
        if (options.nativeVerification) { buf.append("-DNATIVE_VERIFICATION=true ");          }
 
        buf.append("  -ffunction-sections -fdata-sections ");
        // Required for definition of RTLD_DEFAULT handle sent to dlsym
        buf.append("-D_GNU_SOURCE ");

        // Only enable debug switch if not optimizing
        if (!options.o1 &&
            !options.o2 &&
            !options.o3)                { buf.append("-g ");               }

        buf.append("-DSQUAWK_64=" + options.is64).
            append(' ').
            append(get64BitOption()).append(' ');

        if (Platform.isX86Architecture()) {
            buf.append("-ffloat-store ");
        }

        buf.append("-DPLATFORM_BIG_ENDIAN=" + platform.isBigEndian()).append(' ');
        buf.append("-DPLATFORM_UNALIGNED_LOADS=" + platform.allowUnalignedLoads()).append(' ');

        return buf.append(options.cflags).append(' ').toString();
    }

    private int defaultSizeofPointer = -1;

    /**
     * Compiles a small C program to determine the default pointer size of this version of gcc.
     *
     * @return  the size (in bytes) of a pointer compiled by this version of gcc
     */
    private int getDefaultSizeofPointer() {
        if (defaultSizeofPointer == -1) {
            try {
                File cFile = File.createTempFile("sizeofpointer", ".c");
                PrintStream out = new PrintStream(new FileOutputStream(cFile));
                out.println("#include <stdlib.h>");
                out.println("int main (int argc, char **argv) {");
                out.println("    exit(sizeof(char *));");
                out.println("}");
                out.close();

                String exePath = cFile.getPath();
                File exe = new File(exePath.substring(0, exePath.length() - 2));

                env.exec("gcc -o " + exe.getPath() + " " + cFile.getPath());
                cFile.delete();

                try {
                    env.exec(exe.getPath());
                } catch (BuildException e) {
                    exe.delete();
                    return defaultSizeofPointer = e.exitValue;
                }
                throw new BuildException("gcc pointer size test returned 0");
            } catch (IOException ioe) {
                throw new BuildException("could run pointer size gcc test", ioe);
            }
        }
        return defaultSizeofPointer;
    }

    /**
     * Gets the compiler option for specifying the word size of the target architecture.
     *
     * @return word size compiler option
     */
    public String get64BitOption() {
        int pointerSize = getDefaultSizeofPointer();
        if (options.is64) {
            return pointerSize == 8 ? "" : "-m64 ";
        } else {
            return pointerSize == 4 ? "" : "-m32 ";
        }
    }

    /**
     * Gets the linkage options that must come after the input object files.
     *
     * @return the linkage options that must come after the input object files
     */
    public String getLinkSuffix() {
        String jvmLib = env.getPlatform().getJVMLibraryPath();
        String suffix = " " + get64BitOption() + " -L" + jvmLib.replaceAll(File.pathSeparator, " -L") + " -ljvm";

        if (options.kernel && options.hosted) {
            /* Hosted by HotSpot and so need to interpose on signal handling. */
            suffix = suffix + " -ljsig";
        }

        if (options.floatsSupported) {
            return " -ldl -lm" + suffix;
        } else {
            return " -ldl" + suffix;
        }
    }

    /**
     * Gets the platform-dependant gcc switch used to produce a shared library.
     *
     * @return the platform-dependant gcc switch used to produce a shared library
     */
    public String getSharedLibrarySwitch() {
        return "-shared";
    }

    /**
     * {@inheritDoc}
     */
    public File compile(File[] includeDirs, File source, File dir, boolean disableOpts) {
        File object = new File(dir, source.getName().replaceAll("\\.c", "\\.o"));
        env.exec("gcc -c " +
                 options(disableOpts) + " " +
                 include(includeDirs, "-I") +
                 " -o " + object + " " + source);
        return object;
    }

    /**
     * {@inheritDoc}
     */
    public File link(File[] objects, String out, boolean dll) {
        String output;
        String exec;

        if (dll) {
            output = System.mapLibraryName(out);
            exec = "-o " + output + " " + getSharedLibrarySwitch() + " " + Build.join(objects) + " " + getLinkSuffix();
        } else {
            output = out + platform.getExecutableExtension();
            exec = "--gc-sections -o " + output + " " + Build.join(objects) + " " + getLinkSuffix();
        }
        env.exec("gcc " + exec);
        return new File(output);
    }

    /**
     * {@inheritDoc}
     */
    public String getArchitecture() {
        return "X86";
    }
}
