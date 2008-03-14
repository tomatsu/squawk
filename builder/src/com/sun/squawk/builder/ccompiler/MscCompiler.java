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

import com.sun.squawk.builder.platform.*;
import com.sun.squawk.builder.*;
import java.io.File;
import java.io.IOException;

/**
 * The interface for the "cl" MS Visual C++ compiler.
 */
public class MscCompiler extends CCompiler {
	public static final String VISUAL_STUDIO_80_TOOLS_ENVIRONMENT_VARIABLE = "VS80COMNTOOLS";
	public static final String VISUAL_STUDIO_90_TOOLS_ENVIRONMENT_VARIABLE = "VS90COMNTOOLS";
    
    protected String clCommandString;

    public MscCompiler(Build env, Platform platform) {
        super("msc", env, platform);
    }

    /**
     * {@inheritDoc}
     */
    public String options(boolean disableOpts) {
        StringBuffer buf = new StringBuffer();
        if (!disableOpts) {
            if (options.o1)             { buf.append("/O1 ");              }
            if (options.o2)             { buf.append("/O2 /Gs "); }
            if (options.o3)             { buf.append("/DMAXINLINE ");      }
        }
        if (options.tracing)            { buf.append("/DTRACE ");          }
        if (options.profiling)          { buf.append("/DPROFILING /MT ");  }
        if (options.macroize)           { buf.append("/DMACROIZE ");       }
        if (options.assume)             { buf.append("/DASSUME ");         }
        if (options.typemap)            { buf.append("/DTYPEMAP ");        }
        if (options.ioport)             { buf.append("/DIOPORT ");         }

        if (options.kernel) {
            throw new BuildException("-kernel option not supported by MscCompiler");
        }
        
        if (options.nativeVerification){ buf.append("/DNATIVE_VERIFICATION=true ");         }
        	

        // Only enable debug switch if not optimizing
        if (!options.o1 &&
            !options.o2 &&
            !options.o3)                { buf.append("/ZI ");              }

        if (options.is64) {
            throw new BuildException("-64 option not supported by MscCompiler");
        }

        buf.append("/DIOPORT ");

        buf.append("/DPLATFORM_BIG_ENDIAN=" + platform.isBigEndian()).append(' ');
        buf.append("/DPLATFORM_UNALIGNED_LOADS=" + platform.allowUnalignedLoads()).append(' ');

        return buf.append(options.cflags).append(' ').toString();
    }

    public String getClCommandString() {
        if (clCommandString == null) {
            clCommandString = "cl";
            String toolsDirectory = System.getProperty(VISUAL_STUDIO_90_TOOLS_ENVIRONMENT_VARIABLE);
            if (toolsDirectory == null) {
            	toolsDirectory = System.getProperty(VISUAL_STUDIO_80_TOOLS_ENVIRONMENT_VARIABLE);
            }
            if (toolsDirectory == null) {
            	toolsDirectory = System.getenv(VISUAL_STUDIO_90_TOOLS_ENVIRONMENT_VARIABLE);
            }
            if (toolsDirectory == null) {
            	toolsDirectory = System.getenv(VISUAL_STUDIO_80_TOOLS_ENVIRONMENT_VARIABLE);
            }
            if (toolsDirectory != null) {
            }
            try {
                String command = "\"" + toolsDirectory + "vsvars32.bat\" && cl";
            	env.log(env.verbose, "Trying to find propert compiler command with: " + command);
                // Try the command to see if it works, if it does work then we want to use it
                Runtime.getRuntime().exec(command);
                clCommandString = command;
            } catch (IOException e) {
            }
        }
        return clCommandString;
    }
    
    /**
     * {@inheritDoc}
     */
    public File compile(File[] includeDirs, File source, File dir, boolean disableOpts) {
        File object = new File(dir, source.getName().replaceAll("\\.c", "\\.obj"));
        env.exec(getClCommandString() + " /c /nologo /wd4996 " +
                 options(disableOpts) + " " +
                 include(includeDirs, "/I") +
                 "/Fo" + object + " " + source);
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
            exec = " /Fe" + output + " /LD " + Build.join(objects) + " /link wsock32.lib /IGNORE:4089";
        } else {
            output = out + platform.getExecutableExtension();
            exec = " /Fe" + output + " " + Build.join(objects) + " /link wsock32.lib /IGNORE:4089";
        }
        env.exec(getClCommandString() + " " + exec);
        return new File(output);
    }

    /**
     * {@inheritDoc}
     */
    public String getArchitecture() {
        return "X86";
    }
}
