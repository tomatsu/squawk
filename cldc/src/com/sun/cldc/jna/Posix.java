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

package com.sun.cldc.jna;

import com.sun.squawk.VM;
import java.util.Hashtable;

/**
 *
 * @author dw29446
 */
public class Posix extends Platform {

    public boolean deleteNativeLibraryAfterVMExit() {
        return false; // TODO: implement for real
    }

    public boolean hasRuntimeExec() {
        return true; // TODO: implement for real
    }

    public boolean isFreeBSD() {
        return platformName().equalsIgnoreCase("freebsd");
    }

    public boolean isLinux() {
        return platformName().equalsIgnoreCase("linux");
    }

    public boolean isMac() {
        return platformName().equalsIgnoreCase("macosx");
    }

    public boolean isOpenBSD() {
        return platformName().equalsIgnoreCase("openbsd");
    }

    public boolean isSolaris() {
        return platformName().equalsIgnoreCase("solaris");
    }

    public boolean isWindows() {
        return false;
    }

    public boolean isWindowsCE() {
        return false;
    }

    public boolean isX11() {
        return false; // TODO: implement for real
    }

    /**
     * Get the name of the package that contains the native implementation for this platform:
     */
    public String getPlatformPackageName() {
        return "com.sun.squawk.platform.posix";
    }

    /**
     * Get the name of the package that contains the native implementation for this platform:
     */
    public String getPlatformNativePackageName() {
        return  "com.sun.squawk.platform.posix." + platformName().toLowerCase() + ".natives";
    }

    public Posix() {
        if (isMac()) {
            commonMappings.put("socket", "");
            commonMappings.put("c", "");
            commonMappings.put("resolv", "");
            commonMappings.put("net", "");
            commonMappings.put("nsl", "");
        }
    }
}
