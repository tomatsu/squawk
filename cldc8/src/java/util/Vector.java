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

package java.util;

import com.sun.squawk.*;
import com.sun.cldchi.jvm.JVM;
/*if[JAVA5SYNTAX]*/
import com.sun.squawk.Java5Marker;
/*end[JAVA5SYNTAX]*/

/**
 * The <code>Vector</code> class implements a growable array of
 * objects. Like an array, it contains components that can be
 * accessed using an integer index. However, the size of a
 * <code>Vector</code> can grow or shrink as needed to accommodate
 * adding and removing items after the <code>Vector</code> has been created.
 * <p>
 * Each vector tries to optimize storage management by maintaining a
 * <code>capacity</code> and a <code>capacityIncrement</code>. The
 * <code>capacity</code> is always at least as large as the vector
 * size; it is usually larger because as components are added to the
 * vector, the vector's storage increases in chunks the size of
 * <code>capacityIncrement</code>. An application can increase the
 * capacity of a vector before inserting a large number of
 * components; this reduces the amount of incremental reallocation.
 *
 * @version 12/17/01 (CLDC 1.1)
 * @since   JDK1.0, CLDC 1.0
 */
/*if[JAVA5SYNTAX]*/
@Java5Marker
public class Vector<E> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable {
/*else[JAVA5SYNTAX]*/
//public class Vector {
/*end[JAVA5SYNTAX]*/

    /**
     * The array buffer into which the components of the vector are
     * stored. The capacity of the vector is the length of this array buffer.
     *
     * @since   JDK1.0
     */
    protected Object elementData[];

    /**
     * The number of valid components in the vector.
     *
     * @since   JDK1.0
     */
    protected int elementCount;

    /**
     * The amount by which the capacity of the vector is automatically
     * incremented when its size becomes greater than its capacity. If
     * the capacity increment is <code>0</code>, the capacity of the
     * vector is doubled each time it needs to grow.
     *
     * @since   JDK1.0
     */
    protected int capacityIncrement;

    /**
     * Constructs an empty vector with the specified initial capacity and
     * capacity increment.
     *
     * @param   initialCapacity     the initial capacity of the vector.
     * @param   capacityIncrement   the amount by which the capacity is
     *                              increased when the vector overflows.
     * @exception IllegalArgumentException if the specified initial capacity
     *            is negative
     */
    public Vector(int initialCapacity, int capacityIncrement) {
        super();
        if (initialCapacity < 0) {
            throw new IllegalArgumentException(
/* #ifdef VERBOSE_EXCEPTIONS */
/// skipped                       "Illegal Capacity: "+ initialCapacity
/* #endif */
            );
        }
        this.elementData = new Object[initialCapacity];
        this.capacityIncrement = capacityIncrement;
    }

    /**
     * Constructs an empty vector with the specified initial capacity.
     *
     * @param   initialCapacity   the initial capacity of the vector.
     * @since   JDK1.0
     */
    public Vector(int initialCapacity) {
        this(initialCapacity, 0);
    }

    /**
     * Constructs an empty vector.
     *
     * @since   JDK1.0
     */
    public Vector() {
        this(10);
    }

	public Vector(Collection<? extends E> c) {
        elementData = c.toArray(new Object[0]);
        elementCount = elementData.length;
	}

    /**
     * Copies the components of this vector into the specified array.
     * The array must be big enough to hold all the objects in this  vector.
     *
     * @param   anArray   the array into which the components get copied.
     * @since   JDK1.0
     */
    public synchronized void copyInto(Object anArray[]) {
        int i = elementCount;
        while (i-- > 0) {
            anArray[i] = elementData[i];
        }
    }

    /**
     * Trims the capacity of this vector to be the vector's current
     * size. An application can use this operation to minimize the
     * storage of a vector.
     *
     * @since   JDK1.0
     */
    public synchronized void trimToSize() {
        modCount++;		
        if (elementCount < elementData.length) {
            Object[] newData = new Object[elementCount];
            System.arraycopy(elementData, 0, newData, 0, elementCount);
            elementData = newData;
        }
    }

    /**
     * Increases the capacity of this vector, if necessary, to ensure
     * that it can hold at least the number of components specified by
     * the minimum capacity argument.
     *
     * @param   minCapacity   the desired minimum capacity.
     * @since   JDK1.0
     */
    public synchronized void ensureCapacity(int minCapacity) {
        modCount++;
        ensureCapacityHelper(minCapacity);		
    }

    /**
     * This implements the unsynchronized semantics of ensureCapacity.
     * Synchronized methods in this class can internally call this
     * method for ensuring capacity without incurring the cost of an
     * extra synchronization.
     *
     * @see java.util.Vector#ensureCapacity(int)
     */
    private void ensureCapacityHelper(int minCapacity) {
        int oldCapacity = elementData.length;
        if (minCapacity > oldCapacity) {
            int newCapacity = (capacityIncrement > 0) ? (oldCapacity + capacityIncrement)
                    : (oldCapacity * 2);
            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }
            Object[] newData = new Object[newCapacity];
            JVM.unchecked_obj_arraycopy(elementData, 0, newData, 0, oldCapacity);
            elementData = newData;
        }
    }

    /**
     * Sets the size of this vector. If the new size is greater than the
     * current size, new <code>null</code> items are added to the end of
     * the vector. If the new size is less than the current size, all
     * components at index <code>newSize</code> and greater are discarded.
     *
     * @param   newSize   the new size of this vector.
     * @throws  ArrayIndexOutOfBoundsException if new size is negative.
     * @since   JDK1.0
     */
    public synchronized void setSize(int newSize) {
        modCount++;		
        if ((newSize > elementCount) && (newSize > elementData.length)) {
            ensureCapacityHelper(newSize);
        } else {
            for (int i = newSize ; i < elementCount ; i++) {
                elementData[i] = null;
            }
        }
        elementCount = newSize;
    }

    /**
     * Returns the current capacity of this vector.
     *
     * @return  the current capacity of this vector.
     * @since   JDK1.0
     */
    public int capacity() {
        return elementData.length;
    }

    /**
     * Returns the number of components in this vector.
     *
     * @return  the number of components in this vector.
     * @since   JDK1.0
     */
    public int size() {
        return elementCount;
    }

    /**
     * Tests if this vector has no components.
     *
     * @return  <code>true</code> if this vector has no components;
     *          <code>false</code> otherwise.
     * @since   JDK1.0
     */
    public boolean isEmpty() {
        return elementCount == 0;
    }

///*if[JAVA5SYNTAX]*/
//    public Iterator<E> iterator() {
//        return new VectorEnumerator<E>(this);
//    }
///*end[JAVA5SYNTAX]*/
    
    /**
     * Returns an enumeration of the components of this vector.
     *
     * @return  an enumeration of the components of this vector.
     * @see     java.util.Enumeration
     * @since   JDK1.0
     */
/*if[JAVA5SYNTAX]*/
    public synchronized Enumeration<E> elements() {
        return new VectorEnumerator<E>(this);
    }
/*else[JAVA5SYNTAX]*/
//    public synchronized Enumeration elements() {
//        return new VectorEnumerator(this);
//    }
/*end[JAVA5SYNTAX]*/

    /**
     * Tests if the specified object is a component in this vector.
     *
     * @param   elem   an object.
     * @return  <code>true</code> if the specified object is a component in
     *          this vector; <code>false</code> otherwise.
     * @since   JDK1.0
     */
    public boolean contains(Object elem) {
        return indexOf(elem, 0) >= 0;
    }

    /**
     * Searches for the first occurrence of the given argument, testing
     * for equality using the <code>equals</code> method.
     *
     * @param   elem   an object.
     * @return  the index of the first occurrence of the argument in this
     *          vector; returns <code>-1</code> if the object is not found.
     * @see     java.lang.Object#equals(java.lang.Object)
     * @since   JDK1.0
     */
    public int indexOf(Object elem) {
        return indexOf(elem, 0);
    }

    /**
     * Searches for the first occurrence of the given argument, beginning
     * the search at <code>index</code>, and testing for equality using
     * the <code>equals</code> method.
     *
     * @param   elem    an object.
     * @param   index   the index to start searching from.
     * @return  the index of the first occurrence of the object argument in
     *          this vector at position <code>index</code> or later in the
     *          vector; returns <code>-1</code> if the object is not found.
     * @see     java.lang.Object#equals(java.lang.Object)
     * @since   JDK1.0
     */
    public synchronized int indexOf(Object elem, int index) {
        if (elem == null) {
            for (int i = index ; i < elementCount ; i++)
                if (elementData[i]==null)
                    return i;
        } else {
            for (int i = index ; i < elementCount ; i++)
                if (elem.equals(elementData[i]))
                    return i;
        }
        return -1;
    }

    /**
     * Returns the index of the last occurrence of the specified object in
     * this vector.
     *
     * @param   elem   the desired component.
     * @return  the index of the last occurrence of the specified object in
     *          this vector; returns <code>-1</code> if the object is not found.
     * @since   JDK1.0
     */
    public int lastIndexOf(Object elem) {
        return lastIndexOf(elem, elementCount-1);
    }

    /**
     * Searches backwards for the specified object, starting from the
     * specified index, and returns an index to it.
     *
     * @param   elem    the desired component.
     * @param   index   the index to start searching from.
     * @return  the index of the last occurrence of the specified object in this
     *          vector at position less than <code>index</code> in the vector;
     *          <code>-1</code> if the object is not found.
     * @exception  IndexOutOfBoundsException  if <tt>index</tt> is greater
     *             than or equal to the current size of this vector.
     * @since   JDK1.0
     */
    public synchronized int lastIndexOf(Object elem, int index) {
        if (index >= elementCount) {
            throw new IndexOutOfBoundsException(
/* #ifdef VERBOSE_EXCEPTIONS */
/// skipped                       index + " >= " + elementCount
/* #endif */
            );
        }

        if (elem == null) {
            for (int i = index; i >= 0; i--)
                if (elementData[i]==null)
                    return i;
        } else {
            for (int i = index; i >= 0; i--)
                if (elem.equals(elementData[i]))
                    return i;
        }
        return -1;
    }

    /**
     * Returns the component at the specified index.
     *
     * @param      index   an index into this vector.
     * @return     the component at the specified index.
     * @exception  ArrayIndexOutOfBoundsException  if an invalid index was
     *             given.
     * @since      JDK1.0
     */
/*if[JAVA5SYNTAX]*/
    @SuppressWarnings("unchecked")
    public synchronized E elementAt(int index) {
        if (index >= elementCount) {
            throw new ArrayIndexOutOfBoundsException(
/* #ifdef VERBOSE_EXCEPTIONS */
/// skipped                       index + " >= " + elementCount
/* #endif */
            );
        }
        return (E) elementData[index];
    }
/*else[JAVA5SYNTAX]*/
//    public synchronized Object elementAt(int index) {
//        if (index >= elementCount) {
//            throw new ArrayIndexOutOfBoundsException(
///* #ifdef VERBOSE_EXCEPTIONS */
///// skipped                       index + " >= " + elementCount
///* #endif */
//            );
//        }
//        return elementData[index];
//    }
/*end[JAVA5SYNTAX]*/

    /**
     * Returns the first component of this vector.
     *
     * @return     the first component of this vector.
     * @exception  NoSuchElementException  if this vector has no components.
     * @since      JDK1.0
     */
/*if[JAVA5SYNTAX]*/
    @SuppressWarnings("unchecked")
    public synchronized E firstElement() {
        if (elementCount == 0) {
            throw new NoSuchElementException();
        }
        return (E) elementData[0];
    }
/*else[JAVA5SYNTAX]*/
//    public synchronized Object firstElement() {
//        if (elementCount == 0) {
//            throw new NoSuchElementException();
//        }
//        return elementData[0];
//    }
/*end[JAVA5SYNTAX]*/

    /**
     * Returns the last component of the vector.
     *
     * @return  the last component of the vector, i.e., the component at index
     *          <code>size()&nbsp;-&nbsp;1</code>.
     * @exception  NoSuchElementException  if this vector is empty.
     * @since   JDK1.0
     */
/*if[JAVA5SYNTAX]*/
    @SuppressWarnings("unchecked")
    public synchronized E lastElement() {
        if (elementCount == 0) {
            throw new NoSuchElementException();
        }
        return (E) elementData[elementCount - 1];
    }
/*else[JAVA5SYNTAX]*/
//    public synchronized Object lastElement() {
//        if (elementCount == 0) {
//            throw new NoSuchElementException();
//        }
//        return elementData[elementCount - 1];
//    }
/*end[JAVA5SYNTAX]*/

    /**
     * Sets the component at the specified <code>index</code> of this
     * vector to be the specified object. The previous component at that
     * position is discarded.
     * <p>
     * The index must be a value greater than or equal to <code>0</code>
     * and less than the current size of the vector.
     *
     * @param      obj     what the component is to be set to.
     * @param      index   the specified index.
     * @exception  ArrayIndexOutOfBoundsException  if the index was invalid.
     * @see        java.util.Vector#size()
     * @since      JDK1.0
     */
/*if[JAVA5SYNTAX]*/
    public synchronized void setElementAt(E obj, int index) {
/*else[JAVA5SYNTAX]*/
//    public synchronized void setElementAt(Object obj, int index) {
/*end[JAVA5SYNTAX]*/
        if (index >= elementCount) {
            throw new ArrayIndexOutOfBoundsException(
/* #ifdef VERBOSE_EXCEPTIONS */
/// skipped                       index + " >= " +
/// skipped                       elementCount
/* #endif */
            );
        }
        elementData[index] = obj;
    }

    /**
     * Deletes the component at the specified index. Each component in
     * this vector with an index greater or equal to the specified
     * <code>index</code> is shifted downward to have an index one
     * smaller than the value it had previously.
     * <p>
     * The index must be a value greater than or equal to <code>0</code>
     * and less than the current size of the vector.
     *
     * @param      index   the index of the object to remove.
     * @exception  ArrayIndexOutOfBoundsException  if the index was invalid.
     * @see        java.util.Vector#size()
     * @since      JDK1.0
     */
    public synchronized void removeElementAt(int index) {
        modCount++;
        int count = elementCount; 
        if (index >= count || index < 0) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        int j = count - index - 1;
        if (j > 0) {
            JVM.unchecked_obj_arraycopy(elementData, index + 1, elementData, index, j);
        }
        elementData[elementCount = count - 1] = null; /* to let gc do its work */
    }

    /**
     * Inserts the specified object as a component in this vector at the
     * specified <code>index</code>. Each component in this vector with
     * an index greater or equal to the specified <code>index</code> is
     * shifted upward to have an index one greater than the value it had
     * previously.
     * <p>
     * The index must be a value greater than or equal to <code>0</code>
     * and less than or equal to the current size of the vector.
     *
     * @param      obj     the component to insert.
     * @param      index   where to insert the new component.
     * @exception  ArrayIndexOutOfBoundsException  if the index was invalid.
     * @see        java.util.Vector#size()
     * @since      JDK1.0
     */
    public synchronized void insertElementAt(E obj, int index) {
        modCount++;
        int count = elementCount;
        if (index < 0 || index > count) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        if (count == elementData.length) {
            ensureCapacityHelper(count + 1);
        }
        Object[] data = elementData;
        JVM.unchecked_obj_arraycopy(data, index, data, index + 1,
                count - index);
        data[index] = obj;
        elementCount = count + 1; 
    }

    /**
     * Adds the specified component to the end of this vector,
     * increasing its size by one. The capacity of this vector is
     * increased if its size becomes greater than its capacity.
     *
     * @param   obj   the component to be added.
     * @since   JDK1.0
     */
    public synchronized void addElement(E obj) {
        modCount++;
        int count = elementCount;
        if (count == elementData.length) {
            ensureCapacityHelper(count + 1);
        }
        elementData[count] = obj;
        elementCount = count + 1; 
    }

    /**
     * Removes the first occurrence of the argument from this vector. If
     * the object is found in this vector, each component in the vector
     * with an index greater or equal to the object's index is shifted
     * downward to have an index one smaller than the value it had previously.
     *
     * @param   obj   the component to be removed.
     * @return  <code>true</code> if the argument was a component of this
     *          vector; <code>false</code> otherwise.
     * @since   JDK1.0
     */
    public synchronized boolean removeElement(Object obj) {
        modCount++;
        int i = indexOf(obj);
        if (i >= 0) {
            removeElementAt(i);
            return true;
        }
        return false;
    }

    /**
     * Removes all components from this vector and sets its size to zero.
     *
     * @since   JDK1.0
     */
    public synchronized void removeAllElements() {
        modCount++;
        for (int i = 0; i < elementCount; i++) {
            elementData[i] = null;
        }
        elementCount = 0;
    }

    /**
     * Compares the specified Object with this Vector for equality.  Returns
     * true if and only if the specified Object is also a List, both Lists
     * have the same size, and all corresponding pairs of elements in the two
     * Lists are <em>equal</em>.  (Two elements {@code e1} and
     * {@code e2} are <em>equal</em> if {@code (e1==null ? e2==null :
     * e1.equals(e2))}.)  In other words, two Lists are defined to be
     * equal if they contain the same elements in the same order.
     *
     * @param o the Object to be compared for equality with this Vector
     * @return true if the specified Object is equal to this Vector
     */
    public synchronized boolean equals(Object o) {
        return super.equals(o);
    }

    /**
     * Returns the hash code value for this Vector.
     */
    public synchronized int hashCode() {
        return super.hashCode();
    }
	
    /**
     * Returns a string representation of this vector.
     *
     * @return  a string representation of this vector.
     * @since   JDK1.0
     */
    public synchronized String toString() {
        return super.toString();
    }
	
    /**
     * Returns a view of the portion of this List between fromIndex,
     * inclusive, and toIndex, exclusive.  (If fromIndex and toIndex are
     * equal, the returned List is empty.)  The returned List is backed by this
     * List, so changes in the returned List are reflected in this List, and
     * vice-versa.  The returned List supports all of the optional List
     * operations supported by this List.
     *
     * <p>This method eliminates the need for explicit range operations (of
     * the sort that commonly exist for arrays).   Any operation that expects
     * a List can be used as a range operation by operating on a subList view
     * instead of a whole List.  For example, the following idiom
     * removes a range of elements from a List:
     * <pre>
     *      list.subList(from, to).clear();
     * </pre>
     * Similar idioms may be constructed for indexOf and lastIndexOf,
     * and all of the algorithms in the Collections class can be applied to
     * a subList.
     *
     * <p>The semantics of the List returned by this method become undefined if
     * the backing list (i.e., this List) is <i>structurally modified</i> in
     * any way other than via the returned List.  (Structural modifications are
     * those that change the size of the List, or otherwise perturb it in such
     * a fashion that iterations in progress may yield incorrect results.)
     *
     * @param fromIndex low endpoint (inclusive) of the subList
     * @param toIndex high endpoint (exclusive) of the subList
     * @return a view of the specified range within this List
     * @throws IndexOutOfBoundsException if an endpoint index value is out of range
     *         {@code (fromIndex < 0 || toIndex > size)}
     * @throws IllegalArgumentException if the endpoint indices are out of order
     *         {@code (fromIndex > toIndex)}
     */
    public synchronized List<E> subList(int fromIndex, int toIndex) {
        return new Collections.SynchronizedRandomAccessList<E>(
                super.subList(fromIndex, toIndex), this);
    }

    /**
     * Removes from this list all of the elements whose index is between
     * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.
     * Shifts any succeeding elements to the left (reduces their index).
     * This call shortens the list by {@code (toIndex - fromIndex)} elements.
     * (If {@code toIndex==fromIndex}, this operation has no effect.)
     *
     * @param fromIndex index of first element to be removed
     * @param toIndex index after last element to be removed
     */
    protected synchronized void removeRange(int fromIndex, int toIndex) {
        modCount++;
        int numMoved = elementCount - toIndex;
            System.arraycopy(elementData, toIndex, elementData, fromIndex,
                             numMoved);

        // Let gc do its work
        int newElementCount = elementCount - (toIndex-fromIndex);
        while (elementCount != newElementCount)
            elementData[--elementCount] = null;
    }

    public synchronized Object[] toArray() {
        Object[] data = new Object[elementCount];
        System.arraycopy(elementData, 0, data, 0, elementCount);
        return data;
    }
	
    /**
     * Returns an array containing all of the elements in this Vector in the
     * correct order; the runtime type of the returned array is that of the
     * specified array.  If the Vector fits in the specified array, it is
     * returned therein.  Otherwise, a new array is allocated with the runtime
     * type of the specified array and the size of this Vector.
     *
     * <p>If the Vector fits in the specified array with room to spare
     * (i.e., the array has more elements than the Vector),
     * the element in the array immediately following the end of the
     * Vector is set to null.  (This is useful in determining the length
     * of the Vector <em>only</em> if the caller knows that the Vector
     * does not contain any null elements.)
     *
     * @param a the array into which the elements of the Vector are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose.
     * @return an array containing the elements of the Vector
     * @throws ArrayStoreException if the runtime type of a is not a supertype
     * of the runtime type of every element in this Vector
     * @throws NullPointerException if the given array is null
     * @since 1.2
     */
    public synchronized <T> T[] toArray(T[] a) {
        if (a.length < elementCount) {
            a = (T[]) VM.newarray(elementCount, Klass.asKlass(a.getClass()).getComponentType());
        }

        System.arraycopy(elementData, 0, a, 0, elementCount);

        if (a.length > elementCount)
            a[elementCount] = null;

        return a;
    }

    public synchronized Object clone() throws CloneNotSupportedException {
        Vector<E> v = (Vector<E>) super.clone();
        v.elementData = toArray();
        v.modCount = 0;
        return v;
    }

    public synchronized E get(int index) {
        if (index >= elementCount)
            throw new ArrayIndexOutOfBoundsException(index);

        return (E) elementData[index];
    }

    /**
     * Replaces the element at the specified position in this Vector with the
     * specified element.
     *
     * @param index index of the element to replace
     * @param element element to be stored at the specified position
     * @return the element previously at the specified position
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index >= size()})
     * @since 1.2
     */
    public synchronized E set(int index, E element) {
        if (index >= elementCount)
            throw new ArrayIndexOutOfBoundsException(index);

        Object oldValue = elementData[index];
        elementData[index] = element;
        return (E) oldValue;
    }

    /**
     * Appends the specified element to the end of this Vector.
     *
     * @param e element to be appended to this Vector
     * @return {@code true} (as specified by {@link Collection#add})
     * @since 1.2
     */
    public boolean add(E e) {
        modCount++;
        ensureCapacityHelper(elementCount + 1);
        elementData[elementCount++] = e;
        return true;
    }

    /**
     * Removes the first occurrence of the specified element in this Vector
     * If the Vector does not contain the element, it is unchanged.  More
     * formally, removes the element with the lowest index i such that
     * {@code (o==null ? get(i)==null : o.equals(get(i)))} (if such
     * an element exists).
     *
     * @param o element to be removed from this Vector, if present
     * @return true if the Vector contained the specified element
     * @since 1.2
     */
    public boolean remove(Object o) {
        return removeElement(o);
    }

    /**
     * Inserts the specified element at the specified position in this Vector.
     * Shifts the element currently at that position (if any) and any
     * subsequent elements to the right (adds one to their indices).
     *
     * @param index index at which the specified element is to be inserted
     * @param element element to be inserted
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index > size()})
     * @since 1.2
     */
    public void add(int index, E element) {
        insertElementAt(element, index);
    }

    /**
     * Removes the element at the specified position in this Vector.
     * Shifts any subsequent elements to the left (subtracts one from their
     * indices).  Returns the element that was removed from the Vector.
     *
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index >= size()})
     * @param index the index of the element to be removed
     * @return element that was removed
     * @since 1.2
     */
    public synchronized E remove(int index) {
        modCount++;
        int count = elementCount; 
        if (index >= count)
            throw new ArrayIndexOutOfBoundsException(index);

        Object oldValue = elementData[index];

        int numMoved = count - index - 1;
        if (numMoved > 0)
            JVM.unchecked_obj_arraycopy(elementData, index + 1, elementData, index,
                    numMoved);
        elementData[elementCount = count - 1] = null; // Let gc do its work
        return (E) oldValue;
    }

    /**
     * Removes all of the elements from this Vector.  The Vector will
     * be empty after this call returns (unless it throws an exception).
     *
     * @since 1.2
     */
    public void clear() {
        removeAllElements();
    }
    // Bulk Operations

    /**
     * Returns true if this Vector contains all of the elements in the
     * specified Collection.
     *
     * @param   c a collection whose elements will be tested for containment
     *          in this Vector
     * @return true if this Vector contains all of the elements in the
     *         specified collection
     * @throws NullPointerException if the specified collection is null
     */
    public synchronized boolean containsAll(Collection<?> c) {
        return super.containsAll(c);
    }

    /**
     * Appends all of the elements in the specified Collection to the end of
     * this Vector, in the order that they are returned by the specified
     * Collection's Iterator.  The behavior of this operation is undefined if
     * the specified Collection is modified while the operation is in progress.
     * (This implies that the behavior of this call is undefined if the
     * specified Collection is this Vector, and this Vector is nonempty.)
     *
     * @param c elements to be inserted into this Vector
     * @return {@code true} if this Vector changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     * @since 1.2
     */
    public synchronized boolean addAll(Collection<? extends E> c) {
        modCount++;
        Object[] a = c.toArray();
        int numNew = a.length;
        ensureCapacityHelper(elementCount + numNew);
        System.arraycopy(a, 0, elementData, elementCount, numNew);
        elementCount += numNew;
        return numNew != 0;
    }

    /**
     * Removes from this Vector all of its elements that are contained in the
     * specified Collection.
     *
     * @param c a collection of elements to be removed from the Vector
     * @return true if this Vector changed as a result of the call
     * @throws ClassCastException if the types of one or more elements
     *         in this vector are incompatible with the specified
     *         collection
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if this vector contains one or more null
     *         elements and the specified collection does not support null
     *         elements
     * (<a href="Collection.html#optional-restrictions">optional</a>),
     * or if the specified collection is null
     * @since 1.2
     */
    public synchronized boolean removeAll(Collection<?> c) {
        return super.removeAll(c);
    }

    /**
     * Retains only the elements in this Vector that are contained in the
     * specified Collection.  In other words, removes from this Vector all
     * of its elements that are not contained in the specified Collection.
     *
     * @param c a collection of elements to be retained in this Vector
     *          (all other elements are removed)
     * @return true if this Vector changed as a result of the call
     * @throws ClassCastException if the types of one or more elements
     *         in this vector are incompatible with the specified
     *         collection
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if this vector contains one or more null
     *         elements and the specified collection does not support null
     *         elements
     *         (<a href="Collection.html#optional-restrictions">optional</a>),
     *         or if the specified collection is null
     *
     * @since 1.2
     */
    public synchronized boolean retainAll(Collection<?> c)  {
        return super.retainAll(c);
    }

    /**
     * Inserts all of the elements in the specified Collection into this
     * Vector at the specified position.  Shifts the element currently at
     * that position (if any) and any subsequent elements to the right
     * (increases their indices).  The new elements will appear in the Vector
     * in the order that they are returned by the specified Collection's
     * iterator.
     *
     * @param index index at which to insert the first element from the
     *              specified collection
     * @param c elements to be inserted into this Vector
     * @return {@code true} if this Vector changed as a result of the call
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index > size()})
     * @throws NullPointerException if the specified collection is null
     * @since 1.2
     */
    public synchronized boolean addAll(int index, Collection<? extends E> c) {
        modCount++;
        if (index < 0 || index > elementCount)
            throw new ArrayIndexOutOfBoundsException(index);

        Object[] a = c.toArray();
        int numNew = a.length;
        ensureCapacityHelper(elementCount + numNew);

        int numMoved = elementCount - index;
        if (numMoved > 0)
            System.arraycopy(elementData, index, elementData, index + numNew,
                    numMoved);

        System.arraycopy(a, 0, elementData, index, numNew);
        elementCount += numNew;
        return numNew != 0;
    }

}

final
/*if[JAVA5SYNTAX]*/
class VectorEnumerator<E> implements Enumeration<E> {
    Vector<E> vector;
/*else[JAVA5SYNTAX]*/
//class VectorEnumerator implements Enumeration {
//    Vector vector;
/*end[JAVA5SYNTAX]*/
    int count;

/*if[JAVA5SYNTAX]*/
    VectorEnumerator(Vector<E> v) {
/*else[JAVA5SYNTAX]*/
//    VectorEnumerator(Vector v) {
/*end[JAVA5SYNTAX]*/
        vector = v;
        count = 0;
    }

    public boolean hasMoreElements() {
        return count < vector.elementCount;
    }

/*if[JAVA5SYNTAX]*/
    @SuppressWarnings("unchecked")
    public E nextElement() {
/*else[JAVA5SYNTAX]*/
//    public Object nextElement() {
/*end[JAVA5SYNTAX]*/
        synchronized (vector) {
            if (count < vector.elementCount) {
/*if[JAVA5SYNTAX]*/
                return (E) vector.elementData[count++];
/*else[JAVA5SYNTAX]*/
//                return vector.elementData[count++];
/*end[JAVA5SYNTAX]*/
            }
        }
        throw new NoSuchElementException(
/* #ifdef VERBOSE_EXCEPTIONS */
/// skipped                   "VectorEnumerator"
/* #endif */
        );
    }
}
