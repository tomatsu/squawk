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

package com.sun.squawk.util;

import com.sun.squawk.*;
import com.sun.squawk.pragma.*;

/**
 * Provides support for assertions that can be removed on demand in order for
 * building a release version.
 *
 * @version  1.00
 */
public class Assert {

    /**
     * Whether assertions are included in the bytecodes or not.
     */
    public static final boolean ASSERTS_ENABLED = Klass.ASSERTIONS_ENABLED;

    /**
     * Flag to always enable shouldNotReachHere().
     */
    public static final boolean SHOULD_NOT_REACH_HERE_ALWAYS_ENABLED = true;

    /**
     * Don't let anyone instantiate this class.
     */
    private Assert() {}

    /**
     * Create one centralized place where an exception is thrown in case of Assert failure.
     * Makes it easier to place a single breakpoint and debug.
     * 
     * @param message
     */
    protected static void throwAssertFailedException(String message)  throws NotInlinedPragma {
        throw new RuntimeException(message);
    }
    
    /**
     * Asserts that the specified condition is true. If the condition is false,
     * a RuntimeException is thrown with the specified message.
     *
     * @param   cond  condition to be tested
     * @param   msg   message that explains the failure
     *
     * @vm2c macro( if (!(cond)) { fprintf(stderr, "Assertion failed: \"%s\", at %s:%d\n", msg, __FILE__, __LINE__); fatalVMError(""); } )
     */
    public static void that(boolean cond, String msg) {
        if (ASSERTS_ENABLED && !cond) {
            System.err.flush();
            System.out.flush();
            throwAssertFailedException("Assertion failed: " + msg);
        }
    }

    /**
     * Asserts that the specified condition is true. If the condition is false,
     * a RuntimeException is thrown.
     *
     * @param   cond  condition to be tested
     *
     * @vm2c macro( if (!(cond)) { fprintf(stderr, "Assertion failed: \"%s\", at %s:%d\n", #cond, __FILE__, __LINE__); fatalVMError(""); } )
     */
    public static void that(boolean cond) {
        if (ASSERTS_ENABLED && !cond) {
            throwAssertFailedException("Assertion failed");
        }
    }

    /**
     * Asserts that the compiler should never reach this point.
     *
     * @param   msg   message that explains the failure
     * @return a null RuntimeException so that constructions such
     *         as <code>throw Assert.shouldNotReachHere()</code> will
     *         be legal and thus avoid the need to return meaningless
     *         values from functions that have failed.
     *
     * @vm2c macro( { fprintf(stderr, "shouldNotReachHere: %s -- %s:%d\n", msg, __FILE__, __LINE__); fatalVMError(""); } )
     */
    public static RuntimeException shouldNotReachHere(String msg) throws NotInlinedPragma {
        throwAssertFailedException("Assertion failed: should not reach here: " + msg);
        // NO-OP
        return null;
    }

    /**
     * Asserts that the compiler should never reach this point.
     *
     * @return a null RuntimeException so that constructions such
     *         as <code>throw Assert.shouldNotReachHere()</code> will
     *         be legal and thus avoid the need to return meaningless
     *         values from functions that have failed.
     *
     * @vm2c macro( { fprintf(stderr, "shouldNotReachHere -- %s:%d\n", __FILE__, __LINE__); fatalVMError(""); } )
     */
    public static RuntimeException shouldNotReachHere() {
        throwAssertFailedException("Assertion failed: should not reach here");
        // NO-OP
        return null;
    }


    /*---------------------------------------------------------------------------*\
     *                      Fatal versions of the above methods                  *
    \*---------------------------------------------------------------------------*/

    /**
     * Asserts that the specified condition is true. If the condition is false
     * the specified message is displayed and the VM is halted.
     *
     * @param   cond  condition to be tested
     * @param   msg   message that explains the failure
     *
     * @vm2c macro( if (!(cond)) { fprintf(stderr, "Assertion failed: \"%s\", at %s:%d\n", msg, __FILE__, __LINE__); fatalVMError(""); } )
     */
    public static void thatFatal(boolean cond, String msg) {
        if (!cond) {
            VM.print("Assertion failed: ");
            VM.println(msg);
            VM.fatalVMError();
        }
    }

    /**
     * Asserts that the specified condition is true. If the condition is false
     * the VM is halted.
     *
     * @param   cond  condition to be tested
     *
     * @vm2c macro( if (!(cond)) { fprintf(stderr, "Assertion failed: \"%s\", at %s:%d\n", #cond, __FILE__, __LINE__); fatalVMError(""); } )
     */
    public static void thatFatal(boolean cond) {
        if (!cond) {
            VM.println("Assertion failed");
            VM.fatalVMError();
        }
    }

    /**
     * Asserts that the compiler should never reach this point.
     *
     * @param   msg   message that explains the failure
     * @return a null RuntimeException so that constructions such
     *         as <code>throw Assert.shouldNotReachHere()</code> will
     *         be legal and thus avoid the need to return meaningless
     *         values from functions that have failed.
     *
     * @vm2c macro( { fprintf(stderr, "shouldNotReachHere: %s -- %s:%d\n", msg, __FILE__, __LINE__); fatalVMError(""); } )
     */
    public static RuntimeException shouldNotReachHereFatal(String msg) {
        VM.print("Assertion failed: should not reach here: ");
        VM.println(msg);
        VM.fatalVMError();
        return null;
    }

    /**
     * Asserts that the compiler should never reach this point.
     *
     * @return a null RuntimeException so that constructions such
     *         as <code>throw Assert.shouldNotReachHere()</code> will
     *         be legal and thus avoid the need to return meaningless
     *         values from functions that have failed.
     *
     * @vm2c macro( { fprintf(stderr, "shouldNotReachHere -- %s:%d\n", __FILE__, __LINE__); fatalVMError(""); } )
     */
    public static RuntimeException shouldNotReachHereFatal() {
        VM.println("Assertion failed: should not reach here");
        VM.fatalVMError();
        return null;
    }

    /*---------------------------------------------------------------------------*\
     *        Fatal VM assertions that won't be removed by the pre-processor     *
    \*---------------------------------------------------------------------------*/

    /**
     * Asserts that the specified condition is true. If the condition is false
     * the specified message is displayed and the VM is halted.
     *
     * Calls to this method are never removed by the Squawk pre-processor and as
     * such should only be placed in frequent execution paths absolutely necessary.
     *
     * @param   cond  condition to be tested
     * @param   msg   message that explains the failure
     *
     * @vm2c macro( if (!(cond)) { fprintf(stderr, "Assertion failed: \"%s\", at %s:%d\n", msg, __FILE__, __LINE__); fatalVMError(""); } )
     */
    public static void always(boolean cond, String msg) {
        if (!cond) {
            VM.print("Assertion failed: ");
            VM.println(msg);
            VM.fatalVMError();
        }
    }

    /**
     * Asserts that the specified condition is true. If the condition is false
     * the VM is halted.
     *
     * Calls to this method are never removed by the Squawk pre-processor and as
     * such should only be placed in frequent execution paths absolutely necessary.
     *
     * @param   cond  condition to be tested
     *
     * @vm2c macro( if (!(cond)) { fprintf(stderr, "Assertion failed: \"%s\", at %s:%d\n", #cond, __FILE__, __LINE__); fatalVMError(""); } )
     */
    public static void always(boolean cond) {
        if (!cond) {
            VM.println("Assertion failed");
            VM.fatalVMError();
        }
    }
}

