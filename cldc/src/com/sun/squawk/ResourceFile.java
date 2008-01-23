/*
 * Copyright 2005-2008 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.squawk;

import com.sun.squawk.util.*;

/**
 * Stores a resource file (name and contents) in the suite file.
 *
 */
public final class ResourceFile {
	/**
	 * Creates a resource file object.
	 */
	public ResourceFile(String name, byte [] data) {
		this.name = name;
		this.data = data;
	}

	/**
	 * The name of the resource file, for example "example/chess/br.bishop.gif".
	 */
	public final String name;
	
	/**
	 * The contents of the resource file.
	 */
	public final byte [] data;
	
	/**
	 * Comparator for ResourceFile objects (which are sorted by the resource files).
	 */
	public final static Comparer comparer = new Comparer() {
		/**
		 * Compares either two ResourceFile objects, or a ResourceFile object and a String object for their relative ordering based on their file names.
		 *
		 * @param o1 a ResourceFile object
		 * @param o2 either a ResourceFile object or a String object
		 */
		public int compare(Object o1, Object o2) {
			if (o1 == o2 || !(o1 instanceof ResourceFile)) {
				return 0;
			}
			
			if (o2 instanceof ResourceFile) {
				return ((ResourceFile) o1).name.compareTo(((ResourceFile) o2).name);
			} else if (o2 instanceof String) {
				return ((ResourceFile) o1).name.compareTo((String) o2);
			} 
			
			return 0;
		}
	};
}
