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

package java.io;
import com.sun.cldc.i18n.*;
// TODO
//import java.util.Formatter;

/**
 * A <code>PrintStream</code> adds functionality to another output stream,
 * namely the ability to print representations of various data values
 * conveniently.  Two other features are provided as well.  Unlike other output
 * streams, a <code>PrintStream</code> never throws an
 * <code>IOException</code>; instead, exceptional situations merely set an
 * internal flag that can be tested via the <code>checkError</code> method.
 *
 * <p> All characters printed by a <code>PrintStream</code> are converted into
 * bytes using the platform's default character encoding.
 *
 * @version    12/17/01 (CLDC 1.1)
 * @since      JDK1.0, CLDC 1.0
 */

public class PrintStream extends OutputStream
		implements Appendable, Closeable
{

    private boolean trouble = false;
//	TODO
//    private Formatter formatter;

    /**
     * Track both the text- and character-output streams, so that their buffers
     * can be flushed without flushing the entire stream.
     */
    private OutputStreamWriter charOut;
    private OutputStream       byteOut;

    /**
     * Create a new print stream.  This stream will not flush automatically.
     *
     * @param  out  The output stream to which values and objects will be
     *              printed
     */
    public PrintStream(OutputStream out) {
        if (out == null) {
            throw new NullPointerException(
/* #ifdef VERBOSE_EXCEPTIONS */
/// skipped                       "Null output stream"
/* #endif */
            );
        }
        byteOut = out;
	if (!Helper.ISO8859_1_ONLY_SUPPORTED) {
	    this.charOut = new OutputStreamWriter(out);
	}
    }

    /**
     * Creates a new print stream.
     *
     * @param  out        The output stream to which values and objects will be
     *                    printed
     * @param  autoFlush  A boolean; if true, the output buffer will be flushed
     *                    whenever a byte array is written, one of the
     *                    <code>println</code> methods is invoked, or a newline
     *                    character or byte (<code>'\n'</code>) is written
     *
     * @see java.io.PrintWriter#PrintWriter(java.io.OutputStream, boolean)
     */
    public PrintStream(OutputStream out, boolean autoFlush/*unused*/) {
        this(out);
    }

    /**
     * Creates a new print stream.
     *
     * @param  out        The output stream to which values and objects will be
     *                    printed
     * @param  autoFlush  A boolean; if true, the output buffer will be flushed
     *                    whenever a byte array is written, one of the
     *                    <code>println</code> methods is invoked, or a newline
     *                    character or byte (<code>'\n'</code>) is written
     * @param  encoding   The name of a supported
     *                    <a href="../lang/package-summary.html#charenc">
     *                    character encoding</a>
     *
     * @throws  UnsupportedEncodingException
     *          If the named encoding is not supported
     *
     * @since  1.4
     */
    public PrintStream(OutputStream out, boolean autoFlush/*unused*/, String encoding)
        throws UnsupportedEncodingException
    {
        if (out == null) 
            throw new NullPointerException();
        this.byteOut = out;
	if (!Helper.ISO8859_1_ONLY_SUPPORTED) {
	    this.charOut = new OutputStreamWriter(this, encoding);
	}
    }

    /**
     * Check to make sure that the stream has not been closed
     */
    private void ensureOpen() throws IOException {
	if (!Helper.ISO8859_1_ONLY_SUPPORTED) {
	    if (charOut == null) {
		throw new IOException(
/* #ifdef VERBOSE_EXCEPTIONS */
/// skipped                       "Stream closed"
/* #endif */
            );
	    }
	} else {
	    if (byteOut == null) {
		throw new IOException(
/* #ifdef VERBOSE_EXCEPTIONS */
/// skipped                       "Stream closed"
/* #endif */
            );
	    }
	}
    }

    /**
     * Flush the stream.  This is done by writing any buffered output bytes to
     * the underlying output stream and then flushing that stream.
     *
     * @see        java.io.OutputStream#flush()
     */
    public void flush() {
	if (!Helper.ISO8859_1_ONLY_SUPPORTED) {
	    synchronized (this) {
		try {
		    ensureOpen();
		    charOut.flush();
		}
		catch (IOException x) {
		    trouble = true;
		}
	    }
	}
    }

    private boolean closing = false; /* To avoid recursive closing */

    /**
     * Close the stream.  This is done by flushing the stream and then closing
     * the underlying output stream.
     *
     * @see        java.io.OutputStream#close()
     */
    public void close() {
	if (!Helper.ISO8859_1_ONLY_SUPPORTED) {
	    synchronized (this) {
		if (! closing) {
		    closing = true;
		    try {
			charOut.close();
		    }
		    catch (IOException x) {
			trouble = true;
		    }
		    charOut = null;
		    byteOut = null;
		}
	    }
	} else {
	    byteOut = null;
	}
    }

    /**
     * Flush the stream and check its error state.  The internal error state
     * is set to <code>true</code> when the underlying output stream throws an
     * <code>IOException</code>,
     * and when the <code>setError</code> method is invoked.
     *
     * @return True if and only if this stream has encountered an
     *         <code>IOException</code>, or the
     *         <code>setError</code> method has been invoked
     */
    public boolean checkError() {
	if (!Helper.ISO8859_1_ONLY_SUPPORTED) {
	    if (charOut != null)
		flush();
	}
        return trouble;
    }

    /**
     * Set the error state of the stream to <code>true</code>.
     *
     * @since JDK1.1
     */
    protected void setError() {
        trouble = true;
    }

    /**
     * Clears the internal error state of this stream.
     *
     * <p> This method will cause subsequent invocations of {@link
     * #checkError()} to return <tt>false</tt> until another write
     * operation fails and invokes {@link #setError()}.
     *
     * @since 1.6
     */
    protected void clearError() {
        trouble = false;
    }


    /*
     * Exception-catching, synchronized output operations,
     * which also implement the write() methods of OutputStream
     */

    /**
     * Write the specified byte to this stream.
     *
     * <p> Note that the byte is written as given; to write a character that
     * will be translated according to the platform's default character
     * encoding, use the <code>print(char)</code> or <code>println(char)</code>
     * methods.
     *
     * @param  b  The byte to be written
     * @see #print(char)
     * @see #println(char)
     */
    public void write(int b) {
	if (!Helper.ISO8859_1_ONLY_SUPPORTED) {
	    try {
		synchronized (this) {
		    ensureOpen();
		    byteOut.write(b);
		}
	    } catch (IOException x) {
		trouble = true;
	    }
	} else {
	    try {
		byteOut.write(b);
	    } catch (IOException x) {
		trouble = true;
	    }
	}
    }

    /**
     * Write <code>len</code> bytes from the specified byte array starting at
     * offset <code>off</code> to this stream.
     *
     * <p> Note that the bytes will be written as given; to write characters
     * that will be translated according to the platform's default character
     * encoding, use the <code>print(char)</code> or <code>println(char)</code>
     * methods.
     *
     * @param  buf   A byte array
     * @param  off   Offset from which to start taking bytes
     * @param  len   Number of bytes to write
     */
    public void write(byte buf[], int off, int len) {
	if (!Helper.ISO8859_1_ONLY_SUPPORTED) {
	    try {
		synchronized (this) {
		    ensureOpen();
		    byteOut.write(buf, off, len);
		}
	    } catch (IOException x) {
		trouble = true;
	    }
	} else {
	    try {
		byteOut.write(buf, off, len);
	    } catch (IOException x) {
		trouble = true;
	    }
	}
    }

    /*
     * The following private methods on the text- and character-output streams
     * always flush the stream buffers, so that writes to the underlying byte
     * stream occur as promptly as with the original PrintStream.
     */

    private void write(char buf[]) {
	if (!Helper.ISO8859_1_ONLY_SUPPORTED) {
	    try {
		synchronized (this) {
		    ensureOpen();
		    charOut.write(buf);
		}
	    } catch (IOException x) {
		trouble = true;
	    }
	} else {
	    for (int i = 0; i < buf.length; i++) {
		write(buf[i]);
	    }
	}
    }

    private void write(String s) {
	if (!Helper.ISO8859_1_ONLY_SUPPORTED) {
	    try {
		synchronized (this) {
		    ensureOpen();
		    charOut.write(s);
		}
	    } catch (IOException x) {
		trouble = true;
	    }
	} else {
	    int len = s.length();
	    try {
		for (int i = 0; i < len; i++) {
		    byteOut.write(s.charAt(i));
		}
	    } catch (IOException e) {
		trouble = true;
	    }
	}
    }

    private void newLine() {
	if (!Helper.ISO8859_1_ONLY_SUPPORTED) {
	    try {
		synchronized (this) {
		    ensureOpen();
		    charOut.write('\n');
		}
	    } catch (IOException x) {
		trouble = true;
	    }
	} else {
	    try {
		byteOut.write('\n');
	    } catch (IOException x) {
		trouble = true;
	    }
	}
    }


    /* Methods that do not terminate lines */

    /**
     * Print a boolean value.  The string produced by <code>{@link
     * java.lang.String#valueOf(boolean)}</code> is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param      b   The <code>boolean</code> to be printed
     */
    public void print(boolean b) {
        write(b ? "true" : "false");
    }

    /**
     * Print a character.  The character is translated into one or more bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param      c   The <code>char</code> to be printed
     */
    public void print(char c) {
        write(String.valueOf(c));
    }

    /**
     * Print an integer.  The string produced by <code>{@link
     * java.lang.String#valueOf(int)}</code> is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param      i   The <code>int</code> to be printed
     * @see        java.lang.Integer#toString(int)
     */
    public void print(int i) {
        write(String.valueOf(i));
    }

    /**
     * Print a long integer.  The string produced by <code>{@link
     * java.lang.String#valueOf(long)}</code> is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param      l   The <code>long</code> to be printed
     * @see        java.lang.Long#toString(long)
     */
    public void print(long l) {
        write(String.valueOf(l));
    }

/*if[FLOATS]*/
    /**
     * Print a floating point number.  The string produced by 
     * <code>{@link java.lang.String#valueOf(float)}</code> is translated 
     * into bytes according to the platform's default character encoding, 
     * and these bytes are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param      f   The <code>float</code> to be printed
     * @see        java.lang.Float#toString(float)
     * @since      CLDC 1.1
     */
    public void print(float f) {
        write(String.valueOf(f));
    }

    /**
     * Print a double-precision floating point number.  The string produced by 
     * <code>{@link java.lang.String#valueOf(double)}</code> is translated 
     * into bytes according to the platform's default character encoding, 
     * and these bytes are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param      d   The <code>double</code> to be printed
     * @see        java.lang.Double#toString(double)
     * @since      CLDC 1.1
     */
    public void print(double d) {
        write(String.valueOf(d));
    }
/*end[FLOATS]*/

    /**
     * Print an array of characters.  The characters are converted into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param      s   The array of chars to be printed
     *
     * @throws  NullPointerException  If <code>s</code> is <code>null</code>
     */
    public void print(char s[]) {
        write(s);
    }

    /**
     * Print a string.  If the argument is <code>null</code> then the string
     * <code>"null"</code> is printed.  Otherwise, the string's characters are
     * converted into bytes according to the platform's default character
     * encoding, and these bytes are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param      s   The <code>String</code> to be printed
     */
    public void print(String s) {
        if (s == null) {
            s = "null";
        }
        write(s);
    }

    /**
     * Print an object.  The string produced by the <code>{@link
     * java.lang.String#valueOf(Object)}</code> method is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param      obj   The <code>Object</code> to be printed
     * @see        java.lang.Object#toString()
     */
    public void print(Object obj) {
        write(String.valueOf(obj));
    }


    /* Methods that do terminate lines */

    /**
     * Terminate the current line by writing the line separator string.  The
     * line separator string is defined by the system property
     * <code>line.separator</code>, and is not necessarily a single newline
     * character (<code>'\n'</code>).
     */
    public void println() {
        newLine();
    }

    /**
     * Print a boolean and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(boolean)}</code> and then
     * <code>{@link #println()}</code>.
     *
     * @param x  The <code>boolean</code> to be printed
     */
    public void println(boolean x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    /**
     * Print a character and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(char)}</code> and then
     * <code>{@link #println()}</code>.
     *
     * @param x  The <code>char</code> to be printed.
     */
    public void println(char x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    /**
     * Print an integer and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(int)}</code> and then
     * <code>{@link #println()}</code>.
     *
     * @param x  The <code>int</code> to be printed.
     */
    public void println(int x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    /**
     * Print a long and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(long)}</code> and then
     * <code>{@link #println()}</code>.
     *
     * @param x  The <code>long</code> to be printed.
     */
    public void println(long x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

/*if[FLOATS]*/
    /**
     * Print a float and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(float)}</code> and then
     * <code>{@link #println()}</code>.
     *
     * @param x  The <code>float</code> to be printed.
     * @since CLDC 1.1
     */
    public void println(float x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    /**
     * Print a double and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(double)}</code> and then
     * <code>{@link #println()}</code>.
     *
     * @param x  The <code>double</code> to be printed.
     * @since CLDC 1.1
     */
    public void println(double x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }
/*end[FLOATS]*/

    /**
     * Print an array of characters and then terminate the line.  This method
     * behaves as though it invokes <code>{@link #print(char[])}</code> and
     * then <code>{@link #println()}</code>.
     *
     * @param x  an array of chars to print.
     */
    public void println(char x[]) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    /**
     * Print a String and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(String)}</code> and then
     * <code>{@link #println()}</code>.
     *
     * @param x  The <code>String</code> to be printed.
     */
    public void println(String x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    /**
     * Print an Object and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(Object)}</code> and then
     * <code>{@link #println()}</code>.
     *
     * @param x  The <code>Object</code> to be printed.
     */
    public void println(Object x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    /**
     * A convenience method to write a formatted string to this output stream
     * using the specified format string and arguments.
     *
     * <p> An invocation of this method of the form <tt>out.printf(format,
     * args)</tt> behaves in exactly the same way as the invocation
     *
     * <pre>
     *     out.format(format, args) </pre>
     *
     * @param  format
     *         A format string as described in <a
     *         href="../util/Formatter.html#syntax">Format string syntax</a>
     *
     * @param  args
     *         Arguments referenced by the format specifiers in the format
     *         string.  If there are more arguments than format specifiers, the
     *         extra arguments are ignored.  The number of arguments is
     *         variable and may be zero.  The maximum number of arguments is
     *         limited by the maximum dimension of a Java array as defined by
     *         the <a href="http://java.sun.com/docs/books/vmspec/">Java
     *         Virtual Machine Specification</a>.  The behaviour on a
     *         <tt>null</tt> argument depends on the <a
     *         href="../util/Formatter.html#syntax">conversion</a>.
     *
     * @throws  IllegalFormatException
     *          If a format string contains an illegal syntax, a format
     *          specifier that is incompatible with the given arguments,
     *          insufficient arguments given the format string, or other
     *          illegal conditions.  For specification of all possible
     *          formatting errors, see the <a
     *          href="../util/Formatter.html#detail">Details</a> section of the
     *          formatter class specification.
     *
     * @throws  NullPointerException
     *          If the <tt>format</tt> is <tt>null</tt>
     *
     * @return  This output stream
     *
     * @since  1.5
     */
    public PrintStream printf(String format, Object ... args) {
        return format(format, args);
    }

    /**
     * Writes a formatted string to this output stream using the specified
     * format string and arguments.
     *
     * <p> The locale always used is the one returned by {@link
     * java.util.Locale#getDefault() Locale.getDefault()}, regardless of any
     * previous invocations of other formatting methods on this object.
     *
     * @param  format
     *         A format string as described in <a
     *         href="../util/Formatter.html#syntax">Format string syntax</a>
     *
     * @param  args
     *         Arguments referenced by the format specifiers in the format
     *         string.  If there are more arguments than format specifiers, the
     *         extra arguments are ignored.  The number of arguments is
     *         variable and may be zero.  The maximum number of arguments is
     *         limited by the maximum dimension of a Java array as defined by
     *         the <a href="http://java.sun.com/docs/books/vmspec/">Java
     *         Virtual Machine Specification</a>.  The behaviour on a
     *         <tt>null</tt> argument depends on the <a
     *         href="../util/Formatter.html#syntax">conversion</a>.
     *
     * @throws  IllegalFormatException
     *          If a format string contains an illegal syntax, a format
     *          specifier that is incompatible with the given arguments,
     *          insufficient arguments given the format string, or other
     *          illegal conditions.  For specification of all possible
     *          formatting errors, see the <a
     *          href="../util/Formatter.html#detail">Details</a> section of the
     *          formatter class specification.
     *
     * @throws  NullPointerException
     *          If the <tt>format</tt> is <tt>null</tt>
     *
     * @return  This output stream
     *
     * @since  1.5
     */
    public PrintStream format(String format, Object ... args) {
		/*
        try {
            synchronized (this) {
                ensureOpen();
                if (formatter == null)
                    formatter = new Formatter((Appendable) this);
                formatter.format(format, args);
            }
        } catch (InterruptedIOException x) {
            Thread.currentThread().interrupt();
        } catch (IOException x) {
            trouble = true;
        }
        return this;
		*/
		throw new RuntimeException("TODO");
    }
		
	/**
     * Appends the specified character sequence to this output stream.
     *
     * <p> An invocation of this method of the form <tt>out.append(csq)</tt>
     * behaves in exactly the same way as the invocation
     *
     * <pre>
     *     out.print(csq.toString()) </pre>
     *
     * <p> Depending on the specification of <tt>toString</tt> for the
     * character sequence <tt>csq</tt>, the entire sequence may not be
     * appended.  For instance, invoking then <tt>toString</tt> method of a
     * character buffer will return a subsequence whose content depends upon
     * the buffer's position and limit.
     *
     * @param  csq
     *         The character sequence to append.  If <tt>csq</tt> is
     *         <tt>null</tt>, then the four characters <tt>"null"</tt> are
     *         appended to this output stream.
     *
     * @return  This output stream
     *
     * @since  1.5
     */
    public PrintStream append(CharSequence csq) {
        if (csq == null)
            print("null");
        else
            print(csq.toString());
        return this;
    }

    /**
     * Appends a subsequence of the specified character sequence to this output
     * stream.
     *
     * <p> An invocation of this method of the form <tt>out.append(csq, start,
     * end)</tt> when <tt>csq</tt> is not <tt>null</tt>, behaves in
     * exactly the same way as the invocation
     *
     * <pre>
     *     out.print(csq.subSequence(start, end).toString()) </pre>
     *
     * @param  csq
     *         The character sequence from which a subsequence will be
     *         appended.  If <tt>csq</tt> is <tt>null</tt>, then characters
     *         will be appended as if <tt>csq</tt> contained the four
     *         characters <tt>"null"</tt>.
     *
     * @param  start
     *         The index of the first character in the subsequence
     *
     * @param  end
     *         The index of the character following the last character in the
     *         subsequence
     *
     * @return  This output stream
     *
     * @throws  IndexOutOfBoundsException
     *          If <tt>start</tt> or <tt>end</tt> are negative, <tt>start</tt>
     *          is greater than <tt>end</tt>, or <tt>end</tt> is greater than
     *          <tt>csq.length()</tt>
     *
     * @since  1.5
     */
    public PrintStream append(CharSequence csq, int start, int end) {
        CharSequence cs = (csq == null ? "null" : csq);
        write(cs.subSequence(start, end).toString());
        return this;
    }

    /**
     * Appends the specified character to this output stream.
     *
     * <p> An invocation of this method of the form <tt>out.append(c)</tt>
     * behaves in exactly the same way as the invocation
     *
     * <pre>
     *     out.print(c) </pre>
     *
     * @param  c
     *         The 16-bit character to append
     *
     * @return  This output stream
     *
     * @since  1.5
     */
    public PrintStream append(char c) {
        print(c);
        return this;
    }

}
