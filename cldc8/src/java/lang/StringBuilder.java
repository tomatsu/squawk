//if[JAVA5SYNTAX]*/
package java.lang;

import com.sun.squawk.Java5Marker;

/**
 * A mutable sequence of characters.  This class provides an API compatible
 * with <code>StringBuffer</code>, but with no guarantee of synchronization.
 * This class is designed for use as a drop-in replacement for
 * <code>StringBuffer</code> in places where the string buffer was being
 * used by a single thread (as is generally the case).   Where possible,
 * it is recommended that this class be used in preference to
 * <code>StringBuffer</code> as it will be faster under most implementations.
 * 
 * <p>The principal operations on a <code>StringBuilder</code> are the 
 * <code>append</code> and <code>insert</code> methods, which are 
 * overloaded so as to accept data of any type. Each effectively 
 * converts a given datum to a string and then appends or inserts the 
 * characters of that string to the string builder. The 
 * <code>append</code> method always adds these characters at the end 
 * of the builder; the <code>insert</code> method adds the characters at 
 * a specified point. 
 * <p>
 * For example, if <code>z</code> refers to a string builder object 
 * whose current contents are "<code>start</code>", then 
 * the method call <code>z.append("le")</code> would cause the string 
 * builder to contain "<code>startle</code>", whereas 
 * <code>z.insert(4, "le")</code> would alter the string builder to 
 * contain "<code>starlet</code>". 
 * <p>
 * In general, if sb refers to an instance of a <code>StringBuilder</code>, 
 * then <code>sb.append(x)</code> has the same effect as 
 * <code>sb.insert(sb.length(),&nbsp;x)</code>.
 *
 * Every string builder has a capacity. As long as the length of the 
 * character sequence contained in the string builder does not exceed 
 * the capacity, it is not necessary to allocate a new internal 
 * buffer. If the internal buffer overflows, it is automatically made larger.
 *
 * <p>Instances of <code>StringBuilder</code> are not safe for
 * use by multiple threads. If such synchronization is required then it is
 * recommended that {@link java.lang.StringBuffer} be used. 
 * <p>
 * This Java Card class is a subset of the JDK 1.5 StringBuilder class. Some
 * interfaces, methods and/or variables have been pruned, and/or other methods
 * simplified, in an effort to reduce the size of this class and/or eliminate
 * dependencies on unsupported features.
 * 
 * @author  Michael McCloskey
 * @version     1.9, 07/16/04
 * @see         java.lang.StringBuffer
 * @see         java.lang.String
 * @since   JDK 1.5, Java Card 3.0
 */
@Java5Marker
public final class StringBuilder extends AbstractStringBuilder implements Appendable, CharSequence {
    /**
     * Constructs a string builder with no characters in it and an 
     * initial capacity of 16 characters. 
     */
    public StringBuilder() {
        super(16);
    }

    /**
     * Constructs a string builder with no characters in it and an 
     * initial capacity specified by the <code>capacity</code> argument. 
     *
     * @param      capacity  the initial capacity.
     * @throws     NegativeArraySizeException  if the <code>capacity</code>
     *               argument is less than <code>0</code>.
     */
    public StringBuilder(int capacity) {
        super(capacity);
    }

    /**
     * Constructs a string builder initialized to the contents of the 
     * specified string. The initial capacity of the string builder is 
     * <code>16</code> plus the length of the string argument.  
     *
     * @param   str   the initial contents of the buffer.
     * @throws    NullPointerException if <code>str</code> is <code>null</code>
     */
    public StringBuilder(String str) {
        super(str.length() + 16);
        append(str);
    }

    public StringBuilder(CharSequence seq) {
        super(seq.length() + 16);
        append(seq);
	}
	
    /**
     * @see     java.lang.String#valueOf(java.lang.Object)
     * @see     #append(java.lang.String)
     */
    public StringBuilder append(Object obj) {
        return append(String.valueOf(obj));
    }

    /**
     * Appends the specified string to this character sequence.
     * <p>
     * The characters of the <code>String</code> argument are appended, in 
     * order, increasing the length of this sequence by the length of the 
     * argument. If <code>str</code> is <code>null</code>, then the four 
     * characters <code>"null"</code> are appended.
     * <p>
     * Let <i>n</i> be the length of this character sequence just prior to 
     * execution of the <code>append</code> method. Then the character at 
     * index <i>k</i> in the new character sequence is equal to the character 
     * at index <i>k</i> in the old character sequence, if <i>k</i> is less 
     * than <i>n</i>; otherwise, it is equal to the character at index 
     * <i>k-n</i> in the argument <code>str</code>.
     *
     * @param   str   a string.
     * @return  a reference to this object.
     */
    public StringBuilder append(String str) {
        super.append(str);
        return this;
    }

    /**
     * Appends the specified <tt>StringBuffer</tt> to this sequence.
     * <p>
     * The characters of the <tt>StringBuffer</tt> argument are appended, 
     * in order, to this sequence, increasing the 
     * length of this sequence by the length of the argument. 
     * If <tt>sb</tt> is <tt>null</tt>, then the four characters 
     * <tt>"null"</tt> are appended to this sequence.
     * <p>
     * Let <i>n</i> be the length of this character sequence just prior to 
     * execution of the <tt>append</tt> method. Then the character at index 
     * <i>k</i> in the new character sequence is equal to the character at 
     * index <i>k</i> in the old character sequence, if <i>k</i> is less than 
     * <i>n</i>; otherwise, it is equal to the character at index <i>k-n</i> 
     * in the argument <code>sb</code>.
     *
     * @param   sb   the <tt>StringBuffer</tt> to append.
     * @return  a reference to this object.
     */
    public StringBuilder append(StringBuffer sb) { 
        super.append(sb);
        return this;
    }

    public StringBuilder append(CharSequence s) {
        super.append(s);
        return this;
	}
	
    public StringBuilder append(CharSequence s, int start, int end) {
        super.append(s, start, end);
        return this;
	}
	
    /**
     * Appends the string representation of the <code>char</code> array 
     * argument to this sequence. 
     * <p>
     * The characters of the array argument are appended, in order, to 
     * the contents of this sequence. The length of this sequence
     * increases by the length of the argument. 
     * <p>
     * The overall effect is exactly as if the argument were converted to 
     * a string by the method {@link String#valueOf(char[])} and the 
     * characters of that string were then {@link #append(String) appended} 
     * to this character sequence.
     *
     * @param   str   the characters to be appended.
     * @return  a reference to this object.
     */
    public StringBuilder append(char str[]) {
        super.append(str);
        return this;
    }

    /**
     * Appends the string representation of a subarray of the
     * <code>char</code> array argument to this sequence.
     * <p>
     * Characters of the <code>char</code> array <code>str</code>, starting at
     * index <code>offset</code>, are appended, in order, to the contents
     * of this sequence. The length of this sequence increases
     * by the value of <code>len</code>.
     * <p>
     * The overall effect is exactly as if the arguments were converted to
     * a string by the method {@link String#valueOf(char[],int,int)} and the
     * characters of that string were then {@link #append(String) appended}
     * to this character sequence.
     *
     * @param   str      the characters to be appended.
     * @param   offset   the index of the first <code>char</code> to append.
     * @param   len      the number of <code>char</code>s to append.
     * @return  a reference to this object.
     */
    public StringBuilder append(char str[], int offset, int len) {
        super.append(str, offset, len);
        return this;
    }

    /**
     * Appends the string representation of the <code>boolean</code> 
     * argument to the sequence.
     * <p>
     * The argument is converted to a string as if by the method 
     * <code>String.valueOf</code>, and the characters of that 
     * string are then appended to this sequence. 
     *
     * @param   b   a <code>boolean</code>.
     * @return  a reference to this object.
     */
    public StringBuilder append(boolean b) {
        super.append(b);
        return this;
    }

    /**
     * Appends the string representation of the <code>char</code> 
     * argument to this sequence. 
     * <p>
     * The argument is appended to the contents of this sequence. 
     * The length of this sequence increases by <code>1</code>. 
     * <p>
     * The overall effect is exactly as if the argument were converted to 
     * a string by the method {@link String#valueOf(char)} and the character 
     * in that string were then {@link #append(String) appended} to this 
     * character sequence.
     *
     * @param   c   a <code>char</code>.
     * @return  a reference to this object.
     */
    public StringBuilder append(char c) {
        super.append(c);
        return this;
    }

    /**
     * Appends the string representation of the <code>int</code> 
     * argument to this sequence. 
     * <p>
     * The argument is converted to a string as if by the method 
     * <code>String.valueOf</code>, and the characters of that 
     * string are then appended to this sequence. 
     *
     * @param   i   an <code>int</code>.
     * @return  a reference to this object.
     */
    public StringBuilder append(int i) {
        super.append(i);
        return this;
    }

    final static int[] sizeTable = { 9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999, Integer.MAX_VALUE };

    // Requires positive x
    static int stringSizeOfInt(int x) {
        for (int i = 0;; i++)
            if (x <= sizeTable[i])
                return i + 1;
    }

    /**
     * Appends the string representation of the <code>long</code> 
     * argument to this sequence.
     * <p>
     * The argument is converted to a string as if by the method 
     * <code>String.valueOf</code>, and the characters of that 
     * string are then appended to this sequence.
     *
     * @param   l   a <code>long</code>.
     * @return  a reference to this object.
     */
    public StringBuilder append(long l) {
        super.append(l);
        return this;
    }

    public StringBuilder append(float f) {
        super.append(f);
        return this;
    }

    public StringBuilder append(double d) {
        super.append(d);
        return this;
	}

    /**
     * Removes the characters in a substring of this sequence.
     * The substring begins at the specified <code>start</code> and extends to
     * the character at index <code>end - 1</code> or to the end of the
     * sequence if no such character exists. If
     * <code>start</code> is equal to <code>end</code>, no changes are made.
     *
     * @param      start  The beginning index, inclusive.
     * @param      end    The ending index, exclusive.
     * @return     This object.
     * @throws     StringIndexOutOfBoundsException  if <code>start</code>
     *             is negative, greater than <code>length()</code>, or
     *         greater than <code>end</code>.
     */
    @Override
    public StringBuilder delete(int start, int end) {
        super.delete(start, end);
        return this;
    }

    /**
     * Removes the <code>char</code> at the specified position in this
     * sequence. This sequence is shortened by one <code>char</code>.
     *
     * <p>Note: If the character at the given index is a supplementary
     * character, this method does not remove the entire character. If
     * correct handling of supplementary characters is required,
     * determine the number of <code>char</code>s to remove by calling
     * <code>Character.charCount(thisSequence.codePointAt(index))</code>,
     * where <code>thisSequence</code> is this sequence.
     *
     * @param       index  Index of <code>char</code> to remove
     * @return      This object.
     * @throws      StringIndexOutOfBoundsException  if the <code>index</code>
     *          is negative or greater than or equal to
     *          <code>length()</code>.
     */
    @Override
    public StringBuilder deleteCharAt(int index) {
        super.deleteCharAt(index);
        return this;
    }

    /**
     * Replaces the characters in a substring of this sequence
     * with characters in the specified <code>String</code>. The substring
     * begins at the specified <code>start</code> and extends to the character
     * at index <code>end - 1</code> or to the end of the
     * sequence if no such character exists. First the
     * characters in the substring are removed and then the specified
     * <code>String</code> is inserted at <code>start</code>. (This 
     * sequence will be lengthened to accommodate the
     * specified String if necessary.)
     * 
     * @param      start    The beginning index, inclusive.
     * @param      end      The ending index, exclusive.
     * @param      str   String that will replace previous contents.
     * @return     This object.
     * @throws     StringIndexOutOfBoundsException  if <code>start</code>
     *             is negative, greater than <code>length()</code>, or
     *         greater than <code>end</code>.
     */
    @Override
    public StringBuilder replace(int start, int end, String str) { 
        super.replace(start, end, str);
        return this;
    }

    /**
     * Inserts the string representation of a subarray of the <code>str</code>
     * array argument into this sequence. The subarray begins at the
     * specified <code>offset</code> and extends <code>len</code> <code>char</code>s.
     * The characters of the subarray are inserted into this sequence at
     * the position indicated by <code>index</code>. The length of this
     * sequence increases by <code>len</code> <code>char</code>s.
     *
     * @param      index    position at which to insert subarray.
     * @param      str       A <code>char</code> array.
     * @param      offset   the index of the first <code>char</code> in subarray to
     *             be inserted.
     * @param      len      the number of <code>char</code>s in the subarray to
     *             be inserted.
     * @return     This object
     * @throws     StringIndexOutOfBoundsException  if <code>index</code>
     *             is negative or greater than <code>length()</code>, or
     *             <code>offset</code> or <code>len</code> are negative, or
     *             <code>(offset+len)</code> is greater than
     *             <code>str.length</code>.
     */
    @Override
    public StringBuilder insert(int index, char str[], int offset, int len) { 
        super.insert(index, str, offset, len);
        return this;
    }

    /**
     * Inserts the string representation of the <code>Object</code> 
     * argument into this character sequence.
     * <p>
     * The second argument is converted to a string as if by the method 
     * <code>String.valueOf</code>, and the characters of that 
     * string are then inserted into this sequence at the indicated 
     * offset. 
     * <p>
     * The offset argument must be greater than or equal to 
     * <code>0</code>, and less than or equal to the length of this 
     * sequence.
     *
     * @param      offset   the offset.
     * @param      obj      an <code>Object</code>.
     * @return     a reference to this object.
     * @throws     StringIndexOutOfBoundsException  if the offset is invalid.
     */
    @Override
    public StringBuilder insert(int offset, Object obj) {
		super.insert(offset, obj);
		return this;
    }

    /**
     * Inserts the string into this character sequence.
     * <p>
     * The characters of the <code>String</code> argument are inserted, in 
     * order, into this sequence at the indicated offset, moving up any 
     * characters originally above that position and increasing the length 
     * of this sequence by the length of the argument. If 
     * <code>str</code> is <code>null</code>, then the four characters 
     * <code>"null"</code> are inserted into this sequence.
     * <p>
     * The character at index <i>k</i> in the new character sequence is 
     * equal to:
     * <ul>
     * <li>the character at index <i>k</i> in the old character sequence, if 
     * <i>k</i> is less than <code>offset</code> 
     * <li>the character at index <i>k</i><code>-offset</code> in the 
     * argument <code>str</code>, if <i>k</i> is not less than 
     * <code>offset</code> but is less than <code>offset+str.length()</code> 
     * <li>the character at index <i>k</i><code>-str.length()</code> in the 
     * old character sequence, if <i>k</i> is not less than 
     * <code>offset+str.length()</code>
     * </ul><p>
     * The offset argument must be greater than or equal to 
     * <code>0</code>, and less than or equal to the length of this 
     * sequence.
     *
     * @param      offset   the offset.
     * @param      str      a string.
     * @return     a reference to this object.
     * @throws     StringIndexOutOfBoundsException  if the offset is invalid.
     */
    @Override
    public StringBuilder insert(int offset, String str) {
        super.insert(offset, str);
        return this;
    }

    /**
     * Inserts the string representation of the <code>char</code> array 
     * argument into this sequence.
     * <p>
     * The characters of the array argument are inserted into the 
     * contents of this sequence at the position indicated by 
     * <code>offset</code>. The length of this sequence increases by 
     * the length of the argument. 
     * <p>
     * The overall effect is exactly as if the argument were converted to 
     * a string by the method {@link String#valueOf(char[])} and the 
     * characters of that string were then 
     * {@link #insert(int,String) inserted} into this 
     * character sequence at the position indicated by
     * <code>offset</code>.
     *
     * @param      offset   the offset.
     * @param      str      a character array.
     * @return     a reference to this object.
     * @throws     StringIndexOutOfBoundsException  if the offset is invalid.
     */
    @Override
    public StringBuilder insert(int offset, char str[]) {
        super.insert(offset, str);
        return this;
    }

    /**
     * Inserts the string representation of the <code>boolean</code> 
     * argument into this sequence. 
     * <p>
     * The second argument is converted to a string as if by the method 
     * <code>String.valueOf</code>, and the characters of that 
     * string are then inserted into this sequence at the indicated 
     * offset. 
     * <p>
     * The offset argument must be greater than or equal to 
     * <code>0</code>, and less than or equal to the length of this 
     * sequence. 
     *
     * @param      offset   the offset.
     * @param      b        a <code>boolean</code>.
     * @return     a reference to this object.
     * @throws     StringIndexOutOfBoundsException  if the offset is invalid.
     */
    @Override
    public StringBuilder insert(int offset, boolean b) {
        super.insert(offset, b);
        return this;
    }

    /**
     * Inserts the string representation of the <code>char</code> 
     * argument into this sequence. 
     * <p>
     * The second argument is inserted into the contents of this sequence
     * at the position indicated by <code>offset</code>. The length 
     * of this sequence increases by one. 
     * <p>
     * The overall effect is exactly as if the argument were converted to 
     * a string by the method {@link String#valueOf(char)} and the character 
     * in that string were then {@link #insert(int, String) inserted} into 
     * this character sequence at the position indicated by
     * <code>offset</code>.
     * <p>
     * The offset argument must be greater than or equal to 
     * <code>0</code>, and less than or equal to the length of this 
     * sequence. 
     *
     * @param      offset   the offset.
     * @param      c        a <code>char</code>.
     * @return     a reference to this object.
     * @throws     IndexOutOfBoundsException  if the offset is invalid.
     */
    @Override
    public StringBuilder insert(int offset, char c) {
        super.insert(offset, c);
        return this;
    }

    /**
     * Inserts the string representation of the second <code>int</code> 
     * argument into this sequence. 
     * <p>
     * The second argument is converted to a string as if by the method 
     * <code>String.valueOf</code>, and the characters of that 
     * string are then inserted into this sequence at the indicated 
     * offset. 
     * <p>
     * The offset argument must be greater than or equal to 
     * <code>0</code>, and less than or equal to the length of this 
     * sequence. 
     *
     * @param      offset   the offset.
     * @param      i        an <code>int</code>.
     * @return     a reference to this object.
     * @throws     StringIndexOutOfBoundsException  if the offset is invalid.
     */
    @Override
    public StringBuilder insert(int offset, int i) {
        super.insert(offset, i);
        return this;
    }

    /**
     * Inserts the string representation of the <code>long</code> 
     * argument into this sequence. 
     * <p>
     * The second argument is converted to a string as if by the method 
     * <code>String.valueOf</code>, and the characters of that 
     * string are then inserted into this sequence at the position 
     * indicated by <code>offset</code>. 
     * <p>
     * The offset argument must be greater than or equal to 
     * <code>0</code>, and less than or equal to the length of this 
     * sequence. 
     *
     * @param      offset   the offset.
     * @param      l        a <code>long</code>.
     * @return     a reference to this object.
     * @throws     StringIndexOutOfBoundsException  if the offset is invalid.
     */
    @Override
    public StringBuilder insert(int offset, long l) {
        super.insert(offset, l);
        return this;
    }

    @Override
    public StringBuilder insert(int offset, float f) {
        super.insert(offset, f);
        return this;
	}

    @Override
    public StringBuilder insert(int offset, double d) {
        super.insert(offset, d);
        return this;
	}
	
    @Override
    public StringBuilder insert(int offset, CharSequence s) {
		super.insert(offset, s);
		return this;
	}
	
    @Override
    public StringBuilder insert(int offset, CharSequence s, int start, int end) {
        super.insert(offset, s, start, end);
        return this;
	}
	
    @Override
    public int indexOf(String str) {
        return super.indexOf(str);
	}
	
    @Override
    public int indexOf(String str, int fromIndex) {
        return super.indexOf(str, fromIndex);
	}
	
    @Override
	public int lastIndexOf(String str) {
        return super.lastIndexOf(str);
	}
	
    @Override
	public int lastIndexOf(String str, int fromIndex) {
        return super.lastIndexOf(str, fromIndex);
	}
	
    /**
     * Causes this character sequence to be replaced by the reverse of
     * the sequence.
     *
     * Let <i>n</i> be the character length of this character sequence
     * (not the length in <code>char</code> values) just prior to
     * execution of the <code>reverse</code> method. Then the
     * character at index <i>k</i> in the new character sequence is
     * equal to the character at index <i>n-k-1</i> in the old
     * character sequence.
     *
     * @return  a reference to this object.
     */
    @Override
    public StringBuilder reverse() {
        super.reverse();
        return this;
    }

    /**
     * Returns a string representing the data in this sequence.
     * A new <code>String</code> object is allocated and initialized to 
     * contain the character sequence currently represented by this 
     * object. This <code>String</code> is then returned. Subsequent 
     * changes to this sequence do not affect the contents of the 
     * <code>String</code>.
     *
     * @return  a string representation of this sequence of characters.
     */
    @Override
    public String toString() {
        // Create a copy, don't share the array
        return new String(value, 0, count);
    }

}
