package com.sun.squawk.io.j2me.datagram;

import javax.microedition.io.*;
import java.io.*;
import esp8266.NetUtil;

public class DatagramObject implements Datagram {

	private String host;
	int remoteaddr;
	int remoteport;
	
	byte[] data;
	int offset;
	int len;
	
	int pointer;
	
    DatagramObject(byte[] data, int len) throws IOException {
		this(data, len, null);
	}
	
    DatagramObject(byte[] data, int len, String addr) throws IOException {
		this.data = data;
		this.offset = 0;
		this.len = len;
		setAddress(addr);
	}
	
    public String getAddress() {
		return "datagram://" + host + ":" + remoteport;
    }

    public byte[] getData() {
		return data;
    }

    public int getOffset() {
		return offset;
    }

    public int getLength() {
		return len;
    }

    public void setAddress(String name) throws IOException {
		if (!name.startsWith("datagram://")) {
			throw new IOException();
		}
		int idx = name.indexOf(':', 11);
		if (idx < 0) {
			throw new IllegalArgumentException();
		} else if (idx == offset) { // no host
			this.host = null;
			this.remoteaddr = 0;
		} else {
			this.host = name.substring(12, idx);
			this.remoteaddr = NetUtil.gethostbyname(host);
		}
		String port = name.substring(idx + 1);
		if ("".equals(port)) {
			this.remoteport = 0;
		} else {
			this.remoteport = Integer.parseInt(port);
		}
    }

    public void setAddress(Datagram reference) {
		try {
			setAddress(reference.getAddress());
		} catch (IOException e) {
			throw new Error();
		}
    }

    public void setData(byte[] buffer, int offset, int len)  {
		this.data = buffer;
		this.offset = offset;
		this.len = len;
    }

    public void setLength(int len) {
		if (len < 0 || len > data.length - offset) {
			throw new IllegalArgumentException();
		}
		this.len = len;
    }

//
// Read / write functions.
//

    public void reset() {
        pointer = 0;
		offset = 0;
		len = 0;
    }

    public long skip(long n) throws EOFException {
		if (n > (this.len - pointer)) {
			throw new EOFException();
		}
        pointer += n;
        return n;
    }

    public int read() throws IOException {
        if (pointer >= this.len) {
            return -1;
        }
        return data[offset + (pointer++)] & 0xFF;
    }

    /**
     * See the general contract of the <code>readFully</code>
     * method of <code>DataInput</code>.
     * <p>
     * Bytes
     * for this operation are read from the contained
     * input stream.
     *
     * @param      b   the buffer into which the data is read.
     * @exception  EOFException  if this input stream reaches the end before
     *               reading all the bytes.
     * @exception  IOException   if an I/O error occurs.
     */
    public final void readFully(byte b[]) throws IOException {
        readFully(b, 0, b.length);
    }

    /**
     * See the general contract of the <code>readFully</code>
     * method of <code>DataInput</code>.
     * <p>
     * Bytes
     * for this operation are read from the contained
     * input stream.
     *
     * @param      b     the buffer into which the data is read.
     * @param      off   the start offset of the data.
     * @param      len   the number of bytes to read.
     * @exception  EOFException  if this input stream reaches the end before
     *               reading all the bytes.
     * @exception  IOException   if an I/O error occurs.
     */
    public final void readFully(byte b[], int off, int len) throws IOException {
        if ((len < 0) || (off < 0) || ((off+len) > b.length))
            throw new IndexOutOfBoundsException();

        int n = 0;
        while (n < len) {
            int ch = read();
            if (ch < 0) {
                throw new EOFException();
            }
            b[off+(n++)] = (byte)ch;
        }
    }

    /**
     * See the general contract of the <code>skipBytes</code>
     * method of <code>DataInput</code>.
     * <p>
     * Bytes
     * for this operation are read from the contained
     * input stream.
     *
     * @param      n   the number of bytes to be skipped.
     * @return     the actual number of bytes skipped.
     * @exception  IOException   if an I/O error occurs.
     */
    public final int skipBytes(int n) throws IOException {
		// Do not skip past end of buffer
		if ((n > 0) && (n >= (len-pointer))) {
			// Return the last position if trying to go past end of
			//  buffer
			n = len-pointer;
		}
        pointer += n;

		// Return the pointer position of next byte
        return n;
    }

    /**
     * See the general contract of the <code>readBoolean</code>
     * method of <code>DataInput</code>.
     * <p>
     * Bytes
     * for this operation are read from the contained
     * input stream.
     *
     * @return     the <code>boolean</code> value read.
     * @exception  EOFException  if this input stream has reached the end.
     */
    public final boolean readBoolean() throws IOException {
        int ch = read();
        if (ch < 0)
            throw new EOFException();
        return (ch != 0);
    }

    /**
     * See the general contract of the <code>readByte</code>
     * method of <code>DataInput</code>.
     * <p>
     * Bytes
     * for this operation are read from the contained
     * input stream.
     *
     * @return     the next byte of this input stream as a signed 8-bit
     *             <code>byte</code>.
     * @exception  EOFException  if this input stream has reached the end.
     * @exception  IOException   if an I/O error occurs.
     */
    public final byte readByte() throws IOException {
        int ch = read();
        if (ch < 0)
            throw new EOFException();
        return (byte)(ch);
    }

    /**
     * See the general contract of the <code>readUnsignedByte</code>
     * method of <code>DataInput</code>.
     * <p>
     * Bytes
     * for this operation are read from the contained
     * input stream.
     *
     * @return     the next byte of this input stream, interpreted as an
     *             unsigned 8-bit number.
     * @exception  EOFException  if this input stream has reached the end.
     * @exception  IOException   if an I/O error occurs.
     */
    public final int readUnsignedByte() throws IOException {
        int ch = read();
        if (ch < 0)
            throw new EOFException();
        return ch;
    }

    /**
     * See the general contract of the <code>readShort</code>
     * method of <code>DataInput</code>.
     * <p>
     * Bytes
     * for this operation are read from the contained
     * input stream.
     *
     * @return     the next two bytes of this input stream, interpreted as a
     *             signed 16-bit number.
     * @exception  EOFException  if this input stream reaches the end before
     *               reading two bytes.
     * @exception  IOException   if an I/O error occurs.
     */
    public final short readShort() throws IOException {
        int ch1 = read();
        int ch2 = read();
        if ((ch1 | ch2) < 0)
             throw new EOFException();
        return (short)((ch1 << 8) + (ch2 << 0));
    }

    /**
     * See the general contract of the <code>readUnsignedShort</code>
     * method of <code>DataInput</code>.
     * <p>
     * Bytes
     * for this operation are read from the contained
     * input stream.
     *
     * @return     the next two bytes of this input stream, interpreted as an
     *             unsigned 16-bit integer.
     * @exception  EOFException  if this input stream reaches the end before
     *               reading two bytes.
     * @exception  IOException   if an I/O error occurs.
     */
    public final int readUnsignedShort() throws IOException {
        int ch1 = read();
        int ch2 = read();
        if ((ch1 | ch2) < 0)
             throw new EOFException();
        return (ch1 << 8) + (ch2 << 0);
    }

    /**
     * See the general contract of the <code>readChar</code>
     * method of <code>DataInput</code>.
     * <p>
     * Bytes
     * for this operation are read from the contained
     * input stream.
     *
     * @return     the next two bytes of this input stream as a Unicode
     *             character.
     * @exception  EOFException  if this input stream reaches the end before
     *               reading two bytes.
     * @exception  IOException   if an I/O error occurs.
     */
    public final char readChar() throws IOException {
        int ch1 = read();
        int ch2 = read();
        if ((ch1 | ch2) < 0)
             throw new EOFException();
        return (char)((ch1 << 8) + (ch2 << 0));
    }

    /**
     * See the general contract of the <code>readInt</code>
     * method of <code>DataInput</code>.
     * <p>
     * Bytes
     * for this operation are read from the contained
     * input stream.
     *
     * @return     the next four bytes of this input stream, interpreted as an
     *             <code>int</code>.
     * @exception  EOFException  if this input stream reaches the end before
     *               reading four bytes.
     * @exception  IOException   if an I/O error occurs.
     */
    public final int readInt() throws IOException {
        int ch1 = read();
        int ch2 = read();
        int ch3 = read();
        int ch4 = read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
             throw new EOFException();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    public final float readFloat() throws IOException {
        int ch = readInt();
		float fl = Float.intBitsToFloat(ch);
		return(fl);
    }

    public final double readDouble() throws IOException {
        long ch = readLong();
		double db = Double.longBitsToDouble(ch);
		return(db);
    }

    /**
     * See the general contract of the <code>readLong</code>
     * method of <code>DataInput</code>.
     * <p>
     * Bytes
     * for this operation are read from the contained
     * input stream.
     *
     * @return     the next eight bytes of this input stream, interpreted as a
     *             <code>long</code>.
     * @exception  EOFException  if this input stream reaches the end before
     *               reading eight bytes.
     * @exception  IOException   if an I/O error occurs.
     */
    public final long readLong() throws IOException {
        return ((long)(readInt()) << 32) + (readInt() & 0xFFFFFFFFL);
    }

    /**
     * See the general contract of the <code>readUTF</code>
     * method of <code>DataInput</code>.
     * <p>
     * Bytes
     * for this operation are read from the contained
     * input stream.
     *
     * @return     a Unicode string.
     * @exception  EOFException  if this input stream reaches the end before
     *               reading all the bytes.
     * @exception  IOException   if an I/O error occurs.
     * @see        java.io.DataInputStream#readUTF(java.io.DataInput)
     */
    public final String readUTF() throws IOException {
        return DataInputStream.readUTF(this);
    }

    /**
     * See the general contract of the <code>readLine</code>
     * method of <code>DataInput</code>.
     * <p>
     * Bytes
     * for this operation are read from the contained
     * input stream.
     *
     * @return     a Unicode string.
     * @exception  EOFException  if this input stream reaches the end before
     *               reading all the bytes.
     * @exception  IOException   if an I/O error occurs.
     * @see        java.io.DataInputStream#readLine()
     */
    public final String readLine() throws IOException {
        return DataInputStream.readUTF(this);
    }

    public void write(int ch) throws IOException {
        byte[] b = this.data;
	    if (len == 0) {
			b = new byte[1];
			setData(b, 0, 1);
			setLength(1);
	    }
		if ((offset + pointer) == b.length) {
			byte new_b[] = new byte[b.length+1];
			System.arraycopy(b, 0, new_b, 0, b.length);
			b = new_b;
			setData(b, 0, b.length);
			setLength(b.length);
		}
        b[offset + pointer++] = (byte)ch;
    }

    /**
     * Writes to the output stream all the bytes in array <code>b</code>.
     * If <code>b</code> is <code>null</code>,
     * a <code>NullPointerException</code> is thrown.
     * If <code>b.length</code> is zero, then
     * no bytes are written. Otherwise, the byte
     * <code>b[0]</code> is written first, then
     * <code>b[1]</code>, and so on; the last byte
     * written is <code>b[b.length-1]</code>.
     *
     * @param      b   the data.
     * @exception  IOException  if an I/O error occurs.
     */
    public void write(byte b[]) throws IOException {
	}

    /**
     * Writes <code>len</code> bytes from array
     * <code>b</code>, in order,  to
     * the output stream.  If <code>b</code>
     * is <code>null</code>, a <code>NullPointerException</code>
     * is thrown.  If <code>off</code> is negative,
     * or <code>len</code> is negative, or <code>off+len</code>
     * is greater than the length of the array
     * <code>b</code>, then an <code>IndexOutOfBoundsException</code>
     * is thrown.  If <code>len</code> is zero,
     * then no bytes are written. Otherwise, the
     * byte <code>b[off]</code> is written first,
     * then <code>b[off+1]</code>, and so on; the
     * last byte written is <code>b[off+len-1]</code>.
     *
     * @param      b     the data.
     * @param      off   the start offset in the data.
     * @param      len   the number of bytes to write.
     * @exception  IOException  if an I/O error occurs.
     */
    public void write(byte b[], int off, int len) throws IOException {
	}

    /**
     * Writes a <code>boolean</code> value to this output stream.
     * If the argument <code>v</code>
     * is <code>true</code>, the value <code>(byte)1</code>
     * is written; if <code>v</code> is <code>false</code>,
     * the  value <code>(byte)0</code> is written.
     * The byte written by this method may
     * be read by the <code>readBoolean</code>
     * method of interface <code>DataInput</code>,
     * which will then return a <code>boolean</code>
     * equal to <code>v</code>.
     *
     * @param      v   the boolean to be written.
     * @exception  IOException  if an I/O error occurs.
     */
    public void writeBoolean(boolean v) throws IOException {
        write(v ? 1 : 0);
	}

    /**
     * Writes to the output stream the eight low-
     * order bits of the argument <code>v</code>.
     * The 24 high-order bits of <code>v</code>
     * are ignored. (This means that <code>writeByte</code>
     * does exactly the same thing as <code>write</code>
     * for an integer argument.) The byte written
     * by this method may be read by the <code>readByte</code>
     * method of interface <code>DataInput</code>,
     * which will then return a <code>byte</code>
     * equal to <code>(byte)v</code>.
     *
     * @param      v   the byte value to be written.
     * @exception  IOException  if an I/O error occurs.
     */
    public void writeByte(int v) throws IOException {
		write(v);
	}

    /**
     * Writes two bytes to the output
     * stream to represent the value of the argument.
     * The byte values to be written, in the order
     * shown, are: <p>
     * <pre><code>
     * (byte)(0xff &amp; (v &gt;&gt; 8))
     * (byte)(0xff &amp; v)
     * </code> </pre> <p>
     * The bytes written by this method may be
     * read by the <code>readShort</code> method
     * of interface <code>DataInput</code>, which
     * will then return a <code>short</code> equal
     * to <code>(short)v</code>.
     *
     * @param      v   the <code>short</code> value to be written.
     * @exception  IOException  if an I/O error occurs.
     */
    public void writeShort(int v) throws IOException {
        write((v >>> 8) & 0xFF);
        write((v >>> 0) & 0xFF);
	}

    /**
     * Writes a <code>char</code> value, which
     * is comprised of two bytes, to the
     * output stream.
     * The byte values to be written, in the order
     * shown, are:
     * <p><pre><code>
     * (byte)(0xff &amp; (v &gt;&gt; 8))
     * (byte)(0xff &amp; v)
     * </code></pre><p>
     * The bytes written by this method may be
     * read by the <code>readChar</code> method
     * of interface <code>DataInput</code>, which
     * will then return a <code>char</code> equal
     * to <code>(char)v</code>.
     *
     * @param      v   the <code>char</code> value to be written.
     * @exception  IOException  if an I/O error occurs.
     */
    public void writeChar(int v) throws IOException {
        write((v >>> 8) & 0xFF);
        write((v >>> 0) & 0xFF);
	}
	
    /**
     * Writes an <code>int</code> value, which is
     * comprised of four bytes, to the output stream.
     * The byte values to be written, in the order
     * shown, are:
     * <p><pre><code>
     * (byte)(0xff &amp; (v &gt;&gt; 24))
     * (byte)(0xff &amp; (v &gt;&gt; 16))
     * (byte)(0xff &amp; (v &gt;&gt; &#32; &#32;8))
     * (byte)(0xff &amp; v)
     * </code></pre><p>
     * The bytes written by this method may be read
     * by the <code>readInt</code> method of interface
     * <code>DataInput</code>, which will then
     * return an <code>int</code> equal to <code>v</code>.
     *
     * @param      v   the <code>int</code> value to be written.
     * @exception  IOException  if an I/O error occurs.
     */
    public void writeInt(int v) throws IOException {
        write((v >>> 24) & 0xFF);
        write((v >>> 16) & 0xFF);
        write((v >>>  8) & 0xFF);
        write((v >>>  0) & 0xFF);
	}

    /**
     * Writes an <code>long</code> value, which is
     * comprised of four bytes, to the output stream.
     * The byte values to be written, in the order
     * shown, are:
     * <p><pre><code>
     * (byte)(0xff &amp; (v &gt;&gt; 56))
     * (byte)(0xff &amp; (v &gt;&gt; 48))
     * (byte)(0xff &amp; (v &gt;&gt; 40))
     * (byte)(0xff &amp; (v &gt;&gt; 32))
     * (byte)(0xff &amp; (v &gt;&gt; 24))
     * (byte)(0xff &amp; (v &gt;&gt; 16))
     * (byte)(0xff &amp; (v &gt;&gt;  8))
     * (byte)(0xff &amp; v)
     * </code></pre><p>
     * The bytes written by this method may be
     * read by the <code>readLong</code> method
     * of interface <code>DataInput</code>, which
     * will then return a <code>long</code> equal
     * to <code>v</code>.
     *
     * @param      v   the <code>long</code> value to be written.
     * @exception  IOException  if an I/O error occurs.
     */
    public void writeLong(long v) throws IOException {
        write((int)(v >>> 56) & 0xFF);
        write((int)(v >>> 48) & 0xFF);
        write((int)(v >>> 40) & 0xFF);
        write((int)(v >>> 32) & 0xFF);
        write((int)(v >>> 24) & 0xFF);
        write((int)(v >>> 16) & 0xFF);
        write((int)(v >>>  8) & 0xFF);
        write((int)(v >>>  0) & 0xFF);
	}

/*if[FLOATS]*/
    /**
     * Writes a <code>float</code> value,
     * which is comprised of four bytes, to the output stream.
     * It does this as if it first converts this
     * <code>float</code> value to an <code>int</code>
     * in exactly the manner of the <code>Float.floatToIntBits</code>
     * method  and then writes the <code>int</code>
     * value in exactly the manner of the <code>writeInt</code>
     * method.  The bytes written by this method
     * may be read by the <code>readFloat</code>
     * method of interface <code>DataInput</code>,
     * which will then return a <code>float</code>
     * equal to <code>v</code>.
     *
     * @param      v   the <code>float</code> value to be written.
     * @exception  IOException  if an I/O error occurs.
     * @since      CLDC 1.1
     */
    public void writeFloat(float v) throws IOException {
        writeInt((int)(Float.floatToIntBits(v)));
	}

    /**
     * Writes a <code>double</code> value,
     * which is comprised of eight bytes, to the output stream.
     * It does this as if it first converts this
     * <code>double</code> value to a <code>long</code>
     * in exactly the manner of the <code>Double.doubleToLongBits</code>
     * method  and then writes the <code>long</code>
     * value in exactly the manner of the <code>writeLong</code>
     * method. The bytes written by this method
     * may be read by the <code>readDouble</code>
     * method of interface <code>DataInput</code>,
     * which will then return a <code>double</code>
     * equal to <code>v</code>.
     *
     * @param      v   the <code>double</code> value to be written.
     * @exception  IOException  if an I/O error occurs.
     * @since      CLDC 1.1
     */
    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
	}
/*end[FLOATS]*/

    /**
     * Writes a string to the output stream.
     * For every character in the string
     * <code>s</code>,  taken in order, one byte
     * is written to the output stream.  If
     * <code>s</code> is <code>null</code>, a <code>NullPointerException</code>
     * is thrown.<p>  If <code>s.length</code>
     * is zero, then no bytes are written. Otherwise,
     * the character <code>s[0]</code> is written
     * first, then <code>s[1]</code>, and so on;
     * the last character written is <code>s[s.length-1]</code>.
     * For each character, one byte is written,
     * the low-order byte, in exactly the manner
     * of the <code>writeByte</code> method . The
     * high-order eight bits of each character
     * in the string are ignored.
     *
     * @param      s   the string of bytes to be written.
     * @throws     IOException  if an I/O error occurs.
     */
    public void writeBytes(String s) throws IOException {
		byte strbyte[] = s.getBytes();
        for (int c=0; c < strbyte.length; c++) {
			writeByte((int)strbyte[c]);
		}
	}

    /**
     * Writes every character in the string <code>s</code>,
     * to the output stream, in order,
     * two bytes per character. If <code>s</code>
     * is <code>null</code>, a <code>NullPointerException</code>
     * is thrown.  If <code>s.length</code>
     * is zero, then no characters are written.
     * Otherwise, the character <code>s[0]</code>
     * is written first, then <code>s[1]</code>,
     * and so on; the last character written is
     * <code>s[s.length-1]</code>. For each character,
     * two bytes are actually written, high-order
     * byte first, in exactly the manner of the
     * <code>writeChar</code> method.
     *
     * @param      s   the string value to be written.
     * @exception  IOException  if an I/O error occurs.
     */
    public void writeChars(String s) throws IOException {
        int len = s.length();
        for (int i = 0 ; i < len ; i++) {
            int v = s.charAt(i);
            write((v >>> 8) & 0xFF);
            write((v >>> 0) & 0xFF);
        }
	}

    /**
     * Writes two bytes of length information
     * to the output stream, followed
     * by the Java modified UTF representation
     * of  every character in the string <code>s</code>.
     * If <code>s</code> is <code>null</code>,
     * a <code>NullPointerException</code> is thrown.
     * Each character in the string <code>s</code>
     * is converted to a group of one, two, or
     * three bytes, depending on the value of the
     * character.<p>
     * If a character <code>c</code>
     * is in the range <code>&#92;u0001</code> through
     * <code>&#92;u007f</code>, it is represented
     * by one byte:<p>
     * <pre>(byte)c </pre>  <p>
     * If a character <code>c</code> is <code>&#92;u0000</code>
     * or is in the range <code>&#92;u0080</code>
     * through <code>&#92;u07ff</code>, then it is
     * represented by two bytes, to be written
     * in the order shown:<p> <pre><code>
     * (byte)(0xc0 | (0x1f &amp; (c &gt;&gt; 6)))
     * (byte)(0x80 | (0x3f &amp; c))
     *  </code></pre>  <p> If a character
     * <code>c</code> is in the range <code>&#92;u0800</code>
     * through <code>uffff</code>, then it is
     * represented by three bytes, to be written
     * in the order shown:<p> <pre><code>
     * (byte)(0xe0 | (0x0f &amp; (c &gt;&gt; 12)))
     * (byte)(0x80 | (0x3f &amp; (c &gt;&gt;  6)))
     * (byte)(0x80 | (0x3f &amp; c))
     *  </code></pre>  <p> First,
     * the total number of bytes needed to represent
     * all the characters of <code>s</code> is
     * calculated. If this number is larger than
     * <code>65535</code>, then a <code>UTFDataFormatError</code>
     * is thrown. Otherwise, this length is written
     * to the output stream in exactly the manner
     * of the <code>writeShort</code> method;
     * after this, the one-, two-, or three-byte
     * representation of each character in the
     * string <code>s</code> is written.<p>  The
     * bytes written by this method may be read
     * by the <code>readUTF</code> method of interface
     * <code>DataInput</code>, which will then
     * return a <code>String</code> equal to <code>s</code>.
     *
     * @param      s   the string value to be written.
     * @exception  IOException  if an I/O error occurs.
     */
    public void writeUTF(String str) throws IOException {
        int strlen = str.length();
        int utflen = 0;
        char[] charr = new char[strlen];
        int c, count = 0;

        str.getChars(0, strlen, charr, 0);

        for (int i = 0; i < strlen; i++) {
            c = charr[i];
            if ((c >= 0x0001) && (c <= 0x007F)) {
                utflen++;
            } else if (c > 0x07FF) {
                utflen += 3;
            } else {
                utflen += 2;
            }
        }

        if (utflen > 65535)
            throw new UTFDataFormatException();

        byte[] bytearr = new byte[utflen+2];
        bytearr[count++] = (byte) ((utflen >>> 8) & 0xFF);
        bytearr[count++] = (byte) ((utflen >>> 0) & 0xFF);
        for (int i = 0; i < strlen; i++) {
            c = charr[i];
            if ((c >= 0x0001) && (c <= 0x007F)) {
                bytearr[count++] = (byte) c;
            } else if (c > 0x07FF) {
                bytearr[count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                bytearr[count++] = (byte) (0x80 | ((c >>  6) & 0x3F));
                bytearr[count++] = (byte) (0x80 | ((c >>  0) & 0x3F));
            } else {
                bytearr[count++] = (byte) (0xC0 | ((c >>  6) & 0x1F));
                bytearr[count++] = (byte) (0x80 | ((c >>  0) & 0x3F));
            }
        }
        write(bytearr);
	}
}
