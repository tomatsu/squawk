package com.sun.squawk.builder.ccompiler;

import com.sun.squawk.builder.*;
import com.sun.squawk.builder.platform.*;
import java.io.*;

/**
 * The interface for the "gcc-arm" compiler.
 */
public class YottaGenerator extends CCompiler {
    
    public YottaGenerator(String name, Build env, Platform platform) {
        super(name, env, platform);
    }

    public YottaGenerator(Build env, Platform platform) {
        this("yotta", env, platform);
    }

    /**
     * {@inheritDoc}
     */
    public String options(boolean disableOpts) {
		return "";
    }

    /**
     * Compiles a small C program to determine the default pointer size of this version of gcc.
     *
     * @return  the size (in bytes) of a pointer compiled by this version of gcc
     */
    protected int getDefaultSizeofPointer() {
        return 4;
    }

    /**
     * Gets the compiler option for specifying the word size of the target architecture.
     *
     * @return word size compiler option
     */
    public String get64BitOption() {
		return "";
    }

    /**
     * Gets the linkage options that must come after the input object files.
     *
     * @return the linkage options that must come after the input object files
     */
    public String getLinkSuffix() {
		return "";
    }

    /**
     * Gets the platform-dependant gcc switch used to produce a shared library.
     *
     * @return the platform-dependant gcc switch used to produce a shared library
     */
    public String getSharedLibrarySwitch() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    public File compile(File[] includeDirs, File source, File dir, boolean disableOpts) {
		System.out.print("compile: source="+source + ", dir="+dir + ", includes=");
		for (int i = 0; i < includeDirs.length; i++) {
			System.out.print(includeDirs[i] + ", ");
		}
		System.out.println();
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public File link(File[] objects, String out, boolean dll) {
		System.out.print("link:");
		for (int i = 0; i < objects.length; i++) {
			System.out.print(objects[i] + ", ");
		}
		System.out.println("out="+out);
		return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getArchitecture() {
        return "ARM";
    }

    /**
     * Use more Java-friendly SSE2 FP instructions instead of x87.
     * SSE2 defined for P4 and newer CPUs, including Atom.
     * @return boolean
     */
    public boolean useSSE2Math() {
        return false;
    }

}
