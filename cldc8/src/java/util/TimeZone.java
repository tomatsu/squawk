/*
 *   
 *
 * Portions Copyright  2000-2007 Sun Microsystems, Inc. All Rights
 * Reserved.  Use is subject to license terms.
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

/*
 * (C) Copyright Taligent, Inc. 1996-1998 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996-1998 - All Rights Reserved
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 */

package java.util;

import com.sun.cldc.util.TimeZoneImplementation;

/**
 * <code>TimeZone</code> represents a time zone offset, and also figures 
 * out daylight savings.
 * <p>
 * Typically, you get a <code>TimeZone</code> using <code>getDefault</code>
 * which creates a <code>TimeZone</code> based on the time zone where the program
 * is running. For example, for a program running in Japan, <code>getDefault</code>
 * creates a <code>TimeZone</code> object based on Japanese Standard Time.
 * <p>
 * You can also get a <code>TimeZone</code> using <code>getTimeZone</code> along
 * with a time zone ID. For instance, the time zone ID for the Pacific
 * Standard Time zone is "PST". So, you can get a PST <code>TimeZone</code> object
 * with:
 * <blockquote>
 * <pre>
 * TimeZone tz = TimeZone.getTimeZone("PST");
 * </pre>
 * </blockquote>
 *
 * <p> This class is a pure subset of the java.util.TimeZone class in JDK 1.3.
 * <p> The only time zone ID that is required to be supported is "GMT".
 * <p>
 * Apart from the methods and variables being subset, the semantics of the
 * getTimeZone() method may also be subset: custom IDs such as "GMT-8:00"
 * are not required to be supported.
 *
 * @see     java.util.Calendar
 * @see     java.util.Date
 * @version CLDC 1.1 02/01/2002 (Based on JDK 1.3)
 */
public abstract class TimeZone implements Cloneable {

    private static TimeZone defaultZone = null;
    private String           ID;

    public TimeZone() {
    }

    /**
     * Gets offset, for current date, modified in case of
     * daylight savings. This is the offset to add *to* GMT to get local time.
     * Gets the time zone offset, for current date, modified in case of daylight
     * savings. This is the offset to add *to* GMT to get local time. Assume
     * that the start and end month are distinct. This method may return incorrect
     * results for rules that start at the end of February (e.g., last Sunday in
     * February) or the beginning of March (e.g., March 1).
     *
     * @param era           The era of the given date (0 = BC, 1 = AD).
     * @param year          The year in the given date.
     * @param month         The month in the given date. Month is 0-based. e.g.,
     *                      0 for January.
     * @param day           The day-in-month of the given date.
     * @param dayOfWeek     The day-of-week of the given date.
     * @param millis        The milliseconds in day in <em>standard</em> local time.
     * @return              The offset to add *to* GMT to get local time.
     * @exception IllegalArgumentException the era, month, day,
     * dayOfWeek, or millis parameters are out of range
     */
    public abstract int getOffset(int era, int year, int month, int day,
                                  int dayOfWeek, int millis);

    /**
     * Gets the GMT offset for this time zone.
     *
     * @return the GMT offset for this time zone.
     */
    public abstract int getRawOffset();

    /**
     * Queries if this time zone uses Daylight Savings Time.
     *
     * @return if this time zone uses Daylight Savings Time.
     */
    public abstract boolean useDaylightTime();

    /**
     * Gets the ID of this time zone.
     * @return the ID of this time zone.
     */
    public String getID() {
        return ID;
    }

    /**
     * Sets the time zone ID. This does not change any other data in
     * the time zone object.
     * @param ID the new time zone ID.
     */
    public void setID(String ID)
    {
        if (ID == null) {
            throw new NullPointerException();
        }
		this.ID = ID;
    }

    /**
     * Gets the <code>TimeZone</code> for the given ID.
     * @param ID the ID for a <code>TimeZone</code>, either an abbreviation such as
     * "GMT", or a full name such as "America/Los_Angeles".
     * <p> The only time zone ID that is required to be supported is "GMT".
     *
     * @return the specified TimeZone, or the GMT zone if the given ID cannot be
     * understood.
     */
    public static synchronized TimeZone getTimeZone(String ID) {
        if (ID == null) {
            throw new NullPointerException();
        }
        getDefault();
        TimeZone tz = ((TimeZoneImplementation)defaultZone).getInstance(ID);
        if (tz == null) {
            tz = ((TimeZoneImplementation)defaultZone).getInstance("GMT");
        }
        return tz;
    }

    /**
     * Gets the default <code>TimeZone</code> for this host.
     * The source of the default <code>TimeZone</code>
     * may vary with implementation.
     * @return a default <code>TimeZone</code>.
     */

    /* <p>
     * The following is information for implementers. Applications
     * should not need to be aware of this or rely on it, because 
     * each implementation may do it differently:
     * <p>
     * The TimeZone class will look up a time zone implementation
     * class at runtime. The class name will take the form:
     * <p>
     * <code>{classRoot}.util.{platform}.TimeZoneImpl</code>
     * <p>
     * To simplify things, we use a hard-coded path name here.
     * Actual location of the implementation class may vary 
     * from one implementation to another.
     */
    public static synchronized TimeZone getDefault() {
        if ( defaultZone == null ) {
            try {
/*if[GMT_ONLY]*/
                defaultZone = new com.sun.cldc.util.j2me.GMTImpl();
/*else[GMT_ONLY]*/
//              defaultZone = new com.sun.cldc.util.j2me.TimeZoneImpl();
/*end[GMT_ONLY]*/
                // Construct a new TimeZoneImplementation instance
                defaultZone = ((TimeZoneImplementation)defaultZone).getInstance(null);
            }
            catch (Exception x) {}
        }
        return defaultZone;
    }

    public static synchronized void setDefault(TimeZone zone) {
        defaultZone = zone;
    }

    /** 
     * Gets all the available IDs supported.
     * @return  an array of IDs.
     */
    public static String[] getAvailableIDs() {
        getDefault();
        return ((TimeZoneImplementation)defaultZone).getIDs();
    }

    /**
     * Gets the available IDs according to the given time zone offset in milliseconds.
     *
     * @param rawOffset the given time zone GMT offset in milliseconds.
     * @return an array of IDs, where the time zone for that ID has
     * the specified GMT offset. For example, "America/Phoenix" and "America/Denver"
     * both have GMT-07:00, but differ in daylight savings behavior.
     * @see #getRawOffset()
     */
    public static synchronized String[] getAvailableIDs(int rawOffset) {
		throw new RuntimeException("TODO");
	}

    /**
     * Returns a long standard time name of this {@code TimeZone} suitable for
     * presentation to the user in the default locale.
     * This method returns the long name, not including daylight savings.
     * If the display name is not available for the locale,
     * then this method returns a string in the
     * <a href="#NormalizedCustomID">normalized custom ID format</a>.
     * @return the human-readable name of this time zone in the default locale.
     * @since 1.2
     */
    public final String getDisplayName() {
		return "GMT+00:00";
    }

    /**
     * Returns a name in the specified {@code style} of this {@code TimeZone}
     * suitable for presentation to the user in the default locale. If the
     * specified {@code daylight} is {@code true}, a Daylight Saving Time name
     * is returned (even if this {@code TimeZone} doesn't observe Daylight Saving
     * Time). Otherwise, a Standard Time name is returned.
     *
     * If the display name is not available for the locale, then this
     * method returns a string in the
     * <a href="#NormalizedCustomID">normalized custom ID format</a>.
     * @param daylight if true, return the daylight savings name.
     * @param style either <code>LONG</code> or <code>SHORT</code>
     * @return the human-readable name of this time zone in the default locale.
     * @since 1.2
     */
    public final String getDisplayName(boolean daylight, int style) {
		return "GMT+00:00";
    }

    /**
     * Returns the amount of time to be added to local standard time
     * to get local wall clock time.
     * <p>
     * The default implementation always returns 3600000 milliseconds
     * (i.e., one hour) if this time zone observes Daylight Saving
     * Time. Otherwise, 0 (zero) is returned.
     * <p>
     * If an underlying TimeZone implementation subclass supports
     * historical Daylight Saving Time changes, this method returns
     * the known latest daylight saving value.
     *
     * @return the amount of saving time in milliseconds
     * @since 1.4
     */
    public int getDSTSavings() {
        return 0;
    }
	
    /**
     * Returns true if this zone has the same rule and offset as another zone.
     * That is, if this zone differs only in ID, if at all.  Returns false
     * if the other zone is null.
     * @param other the <code>TimeZone</code> object to be compared with
     * @return true if the other zone is not null and is the same as this one,
     * with the possible exception of the ID
     * @since 1.2
     */
    public boolean hasSameRules(TimeZone other) {
        return other != null && getRawOffset() == other.getRawOffset() &&
            useDaylightTime() == other.useDaylightTime();
    }

    /**
     * Creates a copy of this <code>TimeZone</code>.
     *
     * @return a clone of this <code>TimeZone</code>
     */
    public Object clone() {
		try {
			TimeZone other = (TimeZone) super.clone();
			other.ID = ID;
			return other;
		} catch (CloneNotSupportedException e) {
			throw new Error(e);
		}
    }

}
