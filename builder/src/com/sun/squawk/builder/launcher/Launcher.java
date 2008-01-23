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

package com.sun.squawk.builder.launcher;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Launcher {

    public static void main(String[] args) {
        ClassLoader loader;
		List<URL> urls = new ArrayList<URL>();
    	try {
            URL toolsJar = getToolsJar();
	        if (toolsJar != null) {
	        	urls.add(toolsJar);
	        }
	        URL buildCommandsJar = getBuildCommandsJar();
	        urls.add(buildCommandsJar);
    	} catch (MalformedURLException e) {
    		throw new RuntimeException("Problems building class path to launch builder", e);
    	}
		loader = new URLClassLoader(urls.toArray(new URL[urls.size()]));
		Thread.currentThread().setContextClassLoader(loader);
        try {
        	Class<?> buildClass = loader.loadClass("com.sun.squawk.builder.Build");
            Method mainMethod = buildClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) args);
        } catch (ClassNotFoundException e) {
        	throw new RuntimeException("Problems finding builder", e);
		} catch (SecurityException e) {
        	throw new RuntimeException("Problems finding builder", e);
		} catch (NoSuchMethodException e) {
        	throw new RuntimeException("Problems finding builder", e);
		} catch (IllegalArgumentException e) {
        	throw new RuntimeException("Problems finding builder", e);
		} catch (IllegalAccessException e) {
        	throw new RuntimeException("Problems finding builder", e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
    }

    public static URL getBuildCommandsJar() throws MalformedURLException {
		URL launcherJarUrl = Launcher.class.getProtectionDomain().getCodeSource().getLocation();
		File launcherJarFile = new File(launcherJarUrl.getFile());
		File buildCommandsJarFile = new File(launcherJarFile.getParent(), "build-commands.jar");
		if (buildCommandsJarFile.exists()) {
			return buildCommandsJarFile.toURL();
		}
		throw new RuntimeException("Unable to locate build-commands.jar.  Expected to find it at " + buildCommandsJarFile.getPath());
    }
    
    public static URL getToolsJar() throws MalformedURLException {
        // firstly check if the tools jar is already in the classpath
        boolean toolsJarAvailable = false;
        try {
            // just check whether this throws an exception
            Class.forName("com.sun.tools.javac.Main");
            toolsJarAvailable = true;
        } catch (Exception e1) {
            try {
                Class.forName("sun.tools.javac.Main");
                toolsJarAvailable = true;
            } catch (Exception e2) {
                // ignore
            }
        }
        if (toolsJarAvailable) {
            return null;
        }
        String javaHome = System.getProperty("java.home");
        File toolsJar = new File(javaHome + "/lib/tools.jar");
        if (toolsJar.exists()) {
            return toolsJar.toURL();
        }
        String lookFor = File.separator + "jre";
        if (javaHome.toLowerCase(Locale.US).endsWith(lookFor)) {
            javaHome = javaHome.substring(0, javaHome.length() - lookFor.length());
            toolsJar = new File(javaHome + "/lib/tools.jar");
        }
        if (!toolsJar.exists()) {
        	throw new RuntimeException("Unable to locate tools.jar. Expected to find it in " + toolsJar.getPath());
        }
        return toolsJar.toURL();
    }
    
}
