/*
 *   
 *
 * Copyright  1990-2007 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version
 * 2 only, as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included at /legal/license.txt).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, CA 95054 or visit www.sun.com if you need additional
 * information or have any questions.
 */

package java.lang;

/*if[JAVA5SYNTAX]*/
import com.sun.squawk.Java5Marker;
/*end[JAVA5SYNTAX]*/
import com.sun.squawk.Klass;


/**
 *
 * The Byte class is the standard wrapper for byte values.
 *
 * @version 12/17/01 (CLDC 1.1)
 * @since   JDK1.1, CLDC 1.0
 */
public final class Byte extends Number implements Comparable<Byte> {

    /**
     * The minimum value a Byte can have.
     */
    public static final byte MIN_VALUE = -128;

    /**
     * The maximum value a Byte can have.
     */
    public static final byte MAX_VALUE = 127;

    public static final Class<Byte>     TYPE = Klass.asClass(Klass.BYTE);
	
    public static final int SIZE = 8;
	
    /**
     * Assuming the specified String represents a byte, returns
     * that byte's value. Throws an exception if the String cannot
     * be parsed as a byte.  The radix is assumed to be 10.
     *
     * @param s       the String containing the byte
     * @return        the parsed value of the byte
     * @exception     NumberFormatException If the string does not
     *                contain a parsable byte.
     */
    public static byte parseByte(String s) throws NumberFormatException {
        return parseByte(s, 10);
    }

    /**
     * Assuming the specified String represents a byte, returns
     * that byte's value. Throws an exception if the String cannot
     * be parsed as a byte.
     *
     * @param s       the String containing the byte
     * @param radix   the radix to be used
     * @return        the parsed value of the byte
     * @exception     NumberFormatException If the String does not
     *                contain a parsable byte.
     */
    public static byte parseByte(String s, int radix)
        throws NumberFormatException {
        int i = Integer.parseInt(s, radix);
        if (i < MIN_VALUE || i > MAX_VALUE)
            throw new NumberFormatException();
        return (byte)i;
    }

    /**
     * The value of the Byte.
     */
    private byte value;

    /**
     * Constructs a Byte object initialized to the specified byte value.
     *
     * @param value     the initial value of the Byte
     */
    public Byte(byte value) {
        this.value = value;
    }

    public Byte(String s) {
		this.value = parseByte(s);
	}
	
    /**
     * Returns the value of this Byte as a byte.
     *
     * @return the value of this Byte as a byte.
     */
    public byte byteValue() {
        return value;
    }

    public short shortValue() {
        return (short)value;
    }

	public int intValue() {
        return (int)value;
	}

    public long longValue() {
        return (long)value;
    }

    public float floatValue() {
        return (float)value;
	}

    public double doubleValue() {
        return (double)value;
	}
	
    /**
     * Returns a String object representing this Byte's value.
     */
    public String toString() {
      return String.valueOf((int)value);
    }

    public static String toString(byte b) {
        return Integer.toString((int)b, 10);
    }
	
    /**
     * Returns a hashcode for this Byte.
     */
    public int hashCode() {
        return (int)value;
    }

    /**
     * Compares this object to the specified object.
     *
     * @param obj       the object to compare with
     * @return          true if the objects are the same; false otherwise.
     */
    public boolean equals(Object obj) {
        if (obj instanceof Byte) {
            return value == ((Byte)obj).byteValue();
        }
        return false;
    }

/*if[JAVA5SYNTAX]*/
    @Java5Marker
/*end[JAVA5SYNTAX]*/
    public static Byte valueOf(final byte val) {
        return new Byte(val);
    }

    public static Byte valueOf(String s) throws NumberFormatException {
        return valueOf(s, 10);
    }

    public static Byte valueOf(String s, int radix)
        throws NumberFormatException {
        return valueOf(parseByte(s, radix));
    }

    public int compareTo(Byte anotherByte) {
        return this.value - anotherByte.value;
    }
	
    public static Byte decode(String nm) throws NumberFormatException {
        int i = Integer.decode(nm);
        if (i < MIN_VALUE || i > MAX_VALUE)
            throw new NumberFormatException(
                    "Value " + i + " out of range from input " + nm);
        return valueOf((byte)i);
	}
}
