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

package com.sun.squawk;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Vector;

import com.sun.squawk.pragma.HostedPragma;
import com.sun.squawk.util.Arrays;
import com.sun.squawk.util.Assert;
import com.sun.squawk.util.LineReader;
import com.sun.squawk.vm.CID;

/**
 * A suite is the unit of deployment/compilation in the Squawk system.
 */
public final class Suite {

    /*---------------------------------------------------------------------------*\
     *                            Fields and constructor                         *
    \*---------------------------------------------------------------------------*/

    /**
     * The classes in the suite.
     */
    private Klass[] classes;

    /**
     * The name of the suite.
     */
    private final String name;

    /**
     * The array of metadata objects for the classes in this suite. The
     * metadata object for the class at index <i>idx</i> in the
     * <code>classes</code> array is at index <i>idx</i> in the
     * <code>metadata</code> array.
     */
    private KlassMetadata[] metadatas;

    private final int type;

    /**
     * The suite that this suite is bound against. That is, the classes of this
     * suite reference classes in the parent suite and its parents.
     */
    private final Suite parent;

    /**
     * Specifies whether or not this suite is open. Only an open suite can have
     * classes installed in it.
     */
    private boolean closed;

	/**
	 * Resource files embedded in the suite.
	 */
    private ResourceFile[] resourceFiles;
	
	/**
	 * Manifest properties embedded in the suite.
	 */
	private ManifestProperty [] manifestProperties;
    
    /**
     * PROPERTIES_MANIFEST_RESOURCE_NAME has already been looked for or found.
     */
    private boolean isPropertiesManifestResourceInstalled;
    
    /**
     * List of classes that should throw a NoClassDefFoundError instead of a ClassNotFoundException.
     * See implementation of {@link Klass#forName(String)} for more information.
     */
    private String[] noClassDefFoundErrorClassNames;

    /**
     * List of classes that are unused in the suite.
     * They will be deleted in the stripped version of the suite.
     * This field not saved in the suite file.
     */
    private Klass[] stripClassesLater;
	
    /**
     * Creates a new <code>Suite</code> instance.
     *
     * @param  name        the name of the suite
     * @param  parent      suite whose classes are linked to by the classes of this suite
     */
    Suite(String name, Suite parent, int type) {
        this.name = name;
        this.parent = parent;
        int count = (isBootstrap() ? CID.LAST_SYSTEM_ID + 1 : 0);
        classes = new Klass[count];
        metadatas = new KlassMetadata[count];
        resourceFiles = new ResourceFile[0];
		manifestProperties = new ManifestProperty [0];
        if (type < 0 || type > METADATA) {
            throw new IllegalArgumentException("type: " + type);
        }
        this.type = type;
    }

    /*---------------------------------------------------------------------------*\
     *                                  Getters                                  *
    \*---------------------------------------------------------------------------*/

    /**
     * Gets this suite's name.
     *
     * @return  this suite's name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the parent suite of this suite.
     *
     * @return the parent suite of this suite
     */
    public Suite getParent() {
        return parent;
    }

    /**
     * Gets the URI identifier of the serialized form of this suite.
     *
     * @return the URI from which this suite was loaded or null if the suite was dynamically created
     */
    public String getURI() {
        ObjectMemory om = getReadOnlyObjectMemory();
        if (om != null) {
            return om.getURI();
        } else {
            return null;
        }
    }

    /**
     * Gets the number of classes in this suite.
     *
     * @return the number of classes in this suite
     */
    public int getClassCount() {
        return classes.length;
    }

    /**
     * Determines if this suite is closed. Open an open suite can have classes installed in it.
     *
     * @return boolean
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Get the suite's type (APPLICATION, LIBRARY, etc.
     * @return int representing suite type
     */
    public int getType() {
        return type;
    }

    /**
     * Determines if this is the bootstrap suite containing the system classes.
     *
     * @return true if this suite has no parent
     */
    public boolean isBootstrap() {
        return parent == null;
    }

    /**
     * Gets the next available number for a class that will be installed in this suite.
     * The value returned by this method will never be the same for this suite.
     *
     * @return the next available number for a class that will be installed in this suite
     */
    int getNextAvailableClassNumber() {
        return getClassCount();
    }

    /**
     * Gets the class in this suite corresponding to a given class number.
     *
     * @param   suiteID  the class number of the class to retrieve
     * @return  the class corresponding to <code>suiteID</code>
     */
    public Klass getKlass(int suiteID) {
        Assert.that(suiteID < classes.length);
        return classes[suiteID];
    }

	/**
	 * Gets the contents of a resource file embedded in the suite.
	 *
	 * @param name the name of the resource file whose contents is to be retrieved
	 * @return the resource data, or null if the resource file doesn't exist
	 */
	public byte [] getResourceData(String name) {
        // Look in parents first
        if (!isBootstrap()) {
            byte[] bytes = parent.getResourceData(name);
            if (bytes != null) {
                return bytes;
            }
        }
        int index = -1;
        for (int i=0; i < resourceFiles.length; i++) {
            if (resourceFiles[i].name.equals(name)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return null;
        }
		return resourceFiles[index].data;
	}

	/**
	 * Return all of the resource files defined for this suite.
	 * 
	 * @return
	 */
	public ResourceFile[] getResourceFiles() {
	    return resourceFiles;
	}

	/**
	 * Gets the names of all manifest properties embedded in this suite.
	 * 
     * @return enumeration over the names
	 */
    Enumeration getManifestPropertyNames() {
		Vector names = new Vector(manifestProperties.length);
		for (int i = 0; i < manifestProperties.length; i++) {
			names.addElement(manifestProperties[i].name);
		}
		return names.elements();
	}
	
	/**
	 * Gets the value of an {@link Suite#PROPERTIES_MANIFEST_RESOURCE_NAME} property embedded in the suite.
	 *
	 * @param name the name of the property whose value is to be retrieved
	 * @return the property value
	 */
	String getManifestProperty(String name) {
		int index = Arrays.binarySearch(manifestProperties, name, ManifestProperty.comparer);
        if (index < 0) {
            // To support dynamic class loading we need to follow the same semantics as Klass.forName
            // which is to look to see if we can't dynamically load a property if its not found
            if (isClosed() || isPropertiesManifestResourceInstalled) {
                return null;
            }
            // The following should automatically install the properties if there is a manifest
            InputStream input = getResourceAsStream(PROPERTIES_MANIFEST_RESOURCE_NAME, null);
            if (input != null) {
                try {input.close();} catch (IOException e) {};
            }
            isPropertiesManifestResourceInstalled = true;
            index = Arrays.binarySearch(manifestProperties, name, ManifestProperty.comparer);
            if (index < 0) {
                return null;
            }
        }
		return manifestProperties [index].value;
	}
	
    /**
     * Finds a resource with a given name.  This method returns null if no
     * resource with this name is found.  The rules for searching
     * resources associated with a given class are profile
     * specific.
     *
     * @param name  name of the desired resource
     * @param klass Used to get the absolute path to resource if name is not absolute, if null, then assume resource name is absolute
     * @return      a <code>java.io.InputStream</code> object.
     * @since JDK1.1
     */
    final java.io.InputStream getResourceAsStream(String name, Klass klass) {
        if ((name.length() > 0 && name.charAt(0) == '/')) {
            name = name.substring(1);
        } else if (klass != null) {
            String className = klass.getName();
            int dotIndex = className.lastIndexOf('.');
            if (dotIndex >= 0) {
                name = className.substring(0, dotIndex + 1).replace('.', '/') + name;
            }
        }
        byte[] bytes = getResourceData(name);
        if (bytes == null) {
/*if[ENABLE_DYNAMIC_CLASSLOADING]*/
            // TODO Should we throw exceptions here like forName ?, I do not think so, since getting resources is not
            // as hard a requirement as being able to find a class ?
            if (isClosed()) {
                return null;
            }
            Isolate isolate = VM.getCurrentIsolate();
            TranslatorInterface translator = isolate.getTranslator();
            if (translator == null) {
                return null;
            }
            translator.open(isolate.getLeafSuite(), isolate.getClassPath());
            bytes = translator.getResourceData(name);
            if (bytes == null) {
                return null;
            }
/*else[ENABLE_DYNAMIC_CLASSLOADING]*/
//          return null;
/*end[ENABLE_DYNAMIC_CLASSLOADING]*/
        }
        return new ByteArrayInputStream(bytes);
    }

    /**
     * Gets a string representation of this suite. The string returned is
     * name of this suite with "suite " prepended.
     *
     * @return  the name of this suite with "suite " prepended
     */
    public String toString() {
        return "suite " + name + " [type: " + typeToString(type) + ", closed: " + closed + ", parent: " + parent + "]";
    }

    public void printSuiteInfo() {
        System.out.println(this);

        int classcount = 0;
        int metadatacount = 0;
        for (int i = 0; i < classes.length; i++) {
            if (classes[i] != null) {
                classcount++;
            }
        }
        for (int i = 0; i < metadatas.length; i++) {
            if (metadatas[i] != null) {
                metadatacount++;
            }
        }
        System.out.println("    classes length: " + classes.length + " classes count: " + classcount);
        System.out.println("    metadata length: " + metadatas.length + " metadatas count: " + metadatacount);
        if (getParent() != null) {
            getParent().printSuiteInfo();
        }
    }

    /**
     * Gets a reference to the ObjectMemory containing this suite in read-only memory.
     *
     * @return  the ObjectMemory containing this suite if it is in read-only memory or null
     */
    private ObjectMemory getReadOnlyObjectMemoryHosted() throws HostedPragma {
        ObjectMemory result = GC.lookupReadOnlyObjectMemoryByRoot(this);
        if (result != null) {
            return result;
        }
        String uri = (isBootstrap() ? ObjectMemory.BOOTSTRAP_URI : "file://" + name + FILE_EXTENSION);
        return GC.lookupReadOnlyObjectMemoryBySourceURI(uri);
    }

    /**
     * Gets a reference to the ObjectMemory containing this suite in read-only memory.
     *
     * @return  the ObjectMemory containing this suite if it is in read-only memory or null
     */
    ObjectMemory getReadOnlyObjectMemory() {
        if (VM.isHosted()) {
            return getReadOnlyObjectMemoryHosted();
        } else {
            return GC.lookupReadOnlyObjectMemoryByRoot(this);
        }
    }

    /*---------------------------------------------------------------------------*\
     *                            Class installation                             *
    \*---------------------------------------------------------------------------*/

    /**
     * Installs a given class into this suite.
     *
     * @param klass  the class to install
     */
    private void installClass0(Klass klass, int suiteID) {
        checkWrite();
        if (suiteID < classes.length) {
            Assert.that(classes[suiteID] == null, klass + " already installed");
        } else {
            Klass[] old = classes;
            classes = new Klass[suiteID + 1];
            System.arraycopy(old, 0, classes, 0, old.length);
        }
        classes[suiteID] = klass;
    }

    /**
     * Installs a given class into this suite.
     *
     * @param klass  the class to install
     */
    public void installClass(Klass klass) {
        installClass0(klass, klass.getSuiteID());
    }

    /**
     * DCE can remove classes, but we want to keep IDs constant, so install
     * dummy entries...
     *
     * @param klass  the class to install
     */
    public void installFillerClassAndMetadata() {
        int suiteID = getNextAvailableClassNumber();
        installClass0(null, suiteID);
        installMetadata0(null, null, suiteID);
    }

    /**
     * Installs the metadata for a class into this suite. This class to which
     * the metadata pertains must already have been installed and there must
     * be no metadata currently installed for the class.
     *
     * @param metadata  the metadata to install
     */
    private void installMetadata0(KlassMetadata metadata, Klass klass, int suiteID) {
        checkWrite();
//        Assert.that(suiteID < classes.length && classes[suiteID] == metadata.getDefinedClass(), klass + " not yet installed? suiteID: " + suiteID + ", classes.length: " + classes.length);
        if (suiteID < metadatas.length) {
            Assert.that(metadatas[suiteID] == null, "metadata for " + klass + "already installed");
        } else {
            KlassMetadata[] old = metadatas;
            metadatas = new KlassMetadata[suiteID + 1];
            System.arraycopy(old, 0, metadatas, 0, old.length);
        }
        metadatas[suiteID] = metadata;
    }

    /**
     * Installs the metadata for a class into this suite. This class to which
     * the metadata pertains must already have been installed and there must
     * be no metadata currently installed for the class.
     *
     * @param metadata  the metadata to install
     */
    void installMetadata(KlassMetadata metadata) {
        Klass klass = metadata.getDefinedClass();
        int suiteID = klass.getSuiteID();
        installMetadata0(metadata, klass, suiteID);
    }
 
    private void removeClass(Klass klass) {
        int suiteID = klass.getSuiteID();
        Assert.always(classes[suiteID] == klass);
        Assert.always(metadatas == null || metadatas[suiteID] == null || metadatas[suiteID].getDefinedClass() == klass);
        checkWrite();
        classes[suiteID] = null;
        if (metadatas != null) {
            metadatas[suiteID] = null;
        }
    }

    private void removeMetadata(Klass klass) {
        int suiteID = klass.getSuiteID();
        Assert.always(metadatas[suiteID] == null || metadatas[suiteID].getDefinedClass() == klass);
        checkWrite();
        metadatas[suiteID] = null;
    }

    public void setUnusedClasses(Klass[] klasses) {
        stripClassesLater = klasses;
    }
    
    /**
     * Installs the metadatas found in metadataSuite directly into my metadatas.  This is done by the Romizer
     * as it saves all of the original metadata into a separate suite, but on loading, these need to be put back
     * into the original Suite.  This original suite turns out to be the parent of the suite containing the metadata
     * 
     * @param metadataSuite
     */
    void pushUpMetadatas() {
        Assert.that(parent != null);
        checkSuite();
        parent.checkSuite();

        for (int i = 0; i < metadatas.length; i++) {
            KlassMetadata km = metadatas[i];
            if (km != null) {
                Klass klass = km.getDefinedClass();
                int id = klass.getSuiteID();
                if (id != i) {
                    System.out.println("Metadata index " + i + " going into index " + id);
                }

                if (parent.contains(klass)) {
                    parent.metadatas[id] = km;
                } else {
                    System.out.println("!!! Metadata " + km + " has no klass in parent. klass =" + klass);
                }
            }
        }
        parent.checkSuite();
    }
    
    /**
     * If a {@link Klass#forName(String)} is performed and class requested is not found AND
     * its added to our list of {@link #classesToNoClassDefFoundError} then we will throw a
     * {@link NoClassDefFoundError}.
     * 
     * @param classNames
     */
	public void addNoClassDefFoundErrorClassNames(String[] classNames) {
		if (noClassDefFoundErrorClassNames == null && (classNames == null || classNames.length == 0)) {
			return;
		}
		if (noClassDefFoundErrorClassNames == null) {
			noClassDefFoundErrorClassNames = new String[0];
		}
		String[] newNames = new String[noClassDefFoundErrorClassNames.length + classNames.length];
		System.arraycopy(noClassDefFoundErrorClassNames, 0, newNames, 0, noClassDefFoundErrorClassNames.length);
		System.arraycopy(classNames, 0, newNames, noClassDefFoundErrorClassNames.length, classNames.length);
		noClassDefFoundErrorClassNames = newNames;
	}

    /**
     * If a {@link Klass#forName(String)} is performed and class requested is not found AND
     * its added to our list of {@link #classesToNoClassDefFoundError} then we will throw a
     * {@link NoClassDefFoundError}.
     * 
     * @param className
     */
    String[] getNoClassDefFoundErrorClassNames() {
        return noClassDefFoundErrorClassNames;
    }
    
    boolean shouldThrowNoClassDefFoundErrorFor(String className) {
        if (noClassDefFoundErrorClassNames == null) {
            return false;
        }
        for (int i=0; i < noClassDefFoundErrorClassNames.length; i++) {
        	if (noClassDefFoundErrorClassNames[i].equals(className)) {
        		return true;
        	}
        }
        if (parent == null) {
        	return false;
        }
        return parent.shouldThrowNoClassDefFoundErrorFor(className);
    }

    /*---------------------------------------------------------------------------*\
     *                          MIDlet data installation                          *
    \*---------------------------------------------------------------------------*/

	/**
	 * Installs a collection of resource files into this suite. 
	 *
	 * @param resourceFile file to install
	 */
	public void installResource(ResourceFile resourceFile) {
		checkWrite();
        System.arraycopy(resourceFiles, 0, resourceFiles = new ResourceFile[resourceFiles.length + 1], 0, resourceFiles.length - 1);
        resourceFiles[resourceFiles.length - 1] = resourceFile;
        if (resourceFile.name.toUpperCase().equals(PROPERTIES_MANIFEST_RESOURCE_NAME)) {
            isPropertiesManifestResourceInstalled = true;
            // Add the properties defined in the manifest file
            loadProperties(resourceFile.data);
        }
	}

    /**
     * Return true if character is a tab or space. Note that readLine() strips off '\n' and '\r'.
     */
    static boolean isWhiteSpace(char ch) {
        return (ch == ' ') || (ch == '\t');
    }
    
    /**
     * Strip the leading white space characters from string "Src", starting from index "start".
     */
    static String stripLeadingWS(String src, int start) {
        int len = src.length();
        while ((start < len) && isWhiteSpace(src.charAt(start))) {
            start++;
        }
        return src.substring(start);
    }
    
    /** 
     * Parse properties from jar manifest file. Based on manifest spec:
     *     http://java.sun.com/j2se/1.4.2/docs/guide/jar/jar.html
     *
     * ABOUT "application descriptors", WHICH ARE NOT SUPPORTED BY THIS METHOD:
     * Note that this syntax is slightly different than the "application descriptor" syntax in the IMP and MIDP specs.
     * An "application descriptor" does not support "continuation lines", or trailing spaces in a value. This is
     * an known annoyance of the MIDP spec.  In addition, the MIDP 1.0 and IMP 1.0 specs have in a bug in the BNF,
     * such that white space is REQUIRED before and after the value. The MIDP 2.0 specs correctly show that such 
     * white space is optional.
     */
    protected void loadProperties(byte[] bytes) {
        LineReader reader = new LineReader(new InputStreamReader(new ByteArrayInputStream(bytes)));
        String line;
        try {
            String key = null;
            String value = null;
            while ((line = reader.readLine()) != null) {
                if (line.length() == 0) {
                    // empty line, just ignore and go on
                    // NOTE - spec says that this ends the main section. Is this right?
                    continue;
                }
                
                int keyEnd = line.indexOf(':');
                boolean continuationLine = isWhiteSpace(line.charAt(0));
                if (continuationLine) {
                    if ((key == null || value == null)) {
                        throw new IOException("Illformed continuation line :" + line);
                    }
                    value = value + stripLeadingWS(line, 0);
                } else if (keyEnd > 0) {
                    if (key != null) {
                        setProperty(key, value);
                    }
                    key = line.substring(0, keyEnd);
                    value = stripLeadingWS(line, keyEnd+1);
                    // leave this data until next time around.
                } else {
                    throw new IOException("Illformed property line :" + line);
                }
            }
            
            if (key != null) {
                setProperty(key, value);
            }
        } catch (IOException e) {
            if (VM.isVerbose()) {
                System.out.println("Error while loading properties: " + e.getMessage());
            }
        }
    }
    
    public void setProperty(String key, String value) {
        Assert.that(value != null);
        ManifestProperty property = new ManifestProperty(key, value);
        installProperty(property);
    }
    
    /**
	 * Installs a collection of IMlet property values into this suite.
	 *
	 * @param property IMlet property to install
	 */
	public void installProperty(ManifestProperty property) {
		checkWrite();
        // There could be more than one manifest property for a given key,
        // as is the case if JAD properties are added, so take this into account
        int index = Arrays.binarySearch(manifestProperties, property.name, ManifestProperty.comparer);
        if (index < 0) {
            if (VM.isVerbose()) {
                System.out.println("[Adding property key: |" + property.name + "| value: |" + property.value + "|]");
            }
            System.arraycopy(manifestProperties, 0, manifestProperties = new ManifestProperty[manifestProperties.length + 1], 0, manifestProperties.length - 1);
            manifestProperties[manifestProperties.length - 1] = property;
            Arrays.sort(manifestProperties, ManifestProperty.comparer);
        } else {
            if (VM.isVerbose()) {
                System.out.println("[Overwriting property key: |" + property.name + "| value: |" + property.value + "|]");
            }
            manifestProperties[index] = property;
        }
	}

    /*---------------------------------------------------------------------------*\
     *                              Class lookup                                 *
    \*---------------------------------------------------------------------------*/

    /**
     * Gets the <code>KlassMetadata</code> instance from this suite and its parents
     * corresponding to a specified class.
     *
     * @param    klass  a class
     * @return   the <code>KlassMetadata</code> instance corresponding to
     *                <code>klass</code> or <code>null</code> if there isn't one
     */
    KlassMetadata getMetadata(Klass klass) {
        // Look in parents first
        if (!isBootstrap()) {
            KlassMetadata metadata = parent.getMetadata(klass);
            if (metadata != null) {
                return metadata;
            }
        }

        if (metadatas != null && contains(klass)) {
            int suiteID = klass.getSuiteID();
            if (suiteID < metadatas.length) {
                KlassMetadata metadata = metadatas[suiteID];
                if (metadata != null && metadata.getDefinedClass() == klass) {
                    return metadata;
                }
            }
        }
        return null;
    }

    /**
     * Gets the <code>Klass</code> instance from this suite corresponding
     * to a specified class name in internal form.
     *
     * @param   name     the name (in internal form) of the class to lookup
     * @return  the <code>Klass</code> instance corresponding to
     *                   <code>internalName</code> or <code>null</code> if there
     *                   isn't one.
     */
    public Klass lookup(String name) {
        // Look in parents first
        if (!isBootstrap()) {
            Klass klass = parent.lookup(name);
            if (klass != null) {
                return klass;
            }
        }

        for (int i = 0 ; i < classes.length ; i++) {
            Klass klass = classes[i];
            if (klass != null) {
                if (klass.getInternalName().compareTo(name) == 0) { // bootstrapping issues prevent the use of equals()
                    return klass;
                }
            }
        }
        return null;
    }

    /**
     * Returns true if this suite contains the given klass.
     *
     * @param   klass     the klass
     * @return  true if klass belongs to this suite
     */
    public boolean contains(Klass klass) {
        int id = klass.getSuiteID();
        if (id < classes.length && classes[id] != null) {
            if (classes[id] == klass) {
                return true;
            } else if (klass.getInternalName().equals(classes[id].getInternalName())) {
                System.out.println("!!! KLASSES NOT EQUAL, BUT SAME NAME: " + klass + " != " + classes[id]);
            }
        }
        return false;
    }

    /**
     * Ensures that this suite is not in read-only memory before being updated.
     *
     * @throws IllegalStateException if this suite is closed
     * @throws IllegalStoreException if this suite is in read-only memory
     */
    private void checkWrite() {
        if (closed) {
            throw new IllegalStateException(this + " is closed");
        }
        if (!VM.isHosted() && !GC.inRam(this)) {
            throw new IllegalStoreException("trying to update read-only object: " + this);
        }
    }

    /*---------------------------------------------------------------------------*\
     *                            hashcode & equals                              *
    \*---------------------------------------------------------------------------*/

    /**
     * Compares this suite with another object for equality. The result is true
     * if and only if <code>other</code> is a <code>Suite</code> instance
     * and its name is equal to this suite's name.
     *
     * @param   other   the object to compare this suite against
     * @return  true if <code>other</code> is a <code>Suite</code> instance
     *                  and its name is equal to this suite's name
     */
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Suite) {
            return name.equals(((Suite)other).name);
        }
        return false;
    }

    /**
     * Returns a hashcode for this suite which is derived solely from the
     * suite's name.
     *
     * @return  the hashcode of this suite's name
     */
    public final int hashCode() {
        return name.hashCode();
    }

    /**
     * Gets the Suite corresponding to a given URI, loading it if necessary.
     *
     * @param uri   the URI identifying the object memory
     * @param errorOnIOException if true, throw an Error if an IOException occurs, otherwise return null.
     * @return the Suite inside the object memory identified by <code>uri</code>
     * @throws Error if the suite denoted by URI is not available or there was
     *         a problem while loading it
     */
    static Suite getSuite(String uri, boolean errorOnIOException) throws Error {
        ObjectMemory om = GC.lookupReadOnlyObjectMemoryBySourceURI(uri);
        if (om == null) {
            try {
                om = ObjectMemoryLoader.load(uri, true).objectMemory;
            } catch (IOException e) {
                if (errorOnIOException) {
                    e.printStackTrace();
                    throw new Error("IO error while loading suite from '" + uri + "': " + e);
                } else {
                    return null;
                }
            }
        }
        Object root = om.getRoot();
        if (!(root instanceof Suite)) {
            throw new Error("object memory in '" + om.getURI() + "' does not contain a suite");
        }
        return (Suite)root;
    }
    
    /**
     * Gets the Suite corresponding to a given URI, loading it if necessary.
     *
     * @param uri   the URI identifying the object memory
     * @return the Suite inside the object memory identified by <code>uri</code>
     * @throws Error if the suite denoted by URI is not available or there was
     *         a problem while loading it
     */
     static Suite getSuite(String uri) throws Error {
         return getSuite(uri, true);
     }

    /**
     * Describes the configuration of the suite.
     */
    private String configuration;

    /**
     * Gets the configuration of the suite.
     *
     * @return the configuration of the suite
     */
    public String getConfiguration() {
        if (configuration == null) {
            return "complete symbolic information available";
        }
        return configuration;
    }

    /**
     * Serializes the object graph rooted by this suite and writes it to a given stream.
     *
     * @param  dos       the DataOutputStream to which the serialized suite should be written
     * @param  uri       the URI identifier of the serialized suite
     * @param  bigEndian the endianess to be used when serializing this suite
     *
     * @return if hosted, returns the objectMemory that suite was saved to
     * @throws OutOfMemoryError if there was insufficient memory to do the save
     * @throws IOException if there was some IO problem while writing the output
     */
    public ObjectMemory save(DataOutputStream dos, String uri, boolean bigEndian) throws HostedPragma, java.io.IOException, OutOfMemoryError {
        stripClassesLater = null; // don't save this...
        ObjectMemorySerializer.ControlBlock cb = VM.copyObjectGraph(this);
        ObjectMemory parentMemory = null;
        if (!isBootstrap()) {
            parentMemory = parent.getReadOnlyObjectMemory();
            Assert.always(parentMemory != null); // "parent not found: " + parent
        }
        checkSuite();
        ObjectMemorySerializer.save(dos, uri, cb, parentMemory, bigEndian);

        ObjectMemory objectMemory;
        if (VM.isHosted()) {
            objectMemory = saveHosted(uri, cb, parentMemory);
        } else {
        	objectMemory = null;
        }
        return objectMemory;
    }

//    /**
//     * Serializes the object graph rooted by this suite and writes it to a given stream.
//     *
//     * @param  dos       the DataOutputStream to which the serialized suite should be written
//     * @param  uri       the URI identifier of the serialized suite
//     * @param  bigEndian the endianess to be used when serializing this suite
//     *
//     * @throws OutOfMemoryError if there was insufficient memory to do the save
//     * @throws IOException if there was some IO problem while writing the output
//     */
//    public void saveKlassMetadatas(DataOutputStream dos, String uri, boolean bigEndian) throws HostedPragma, java.io.IOException, OutOfMemoryError {
//        int originalMemorySize = NativeUnsafe.getMemorySize();
//        ObjectMemorySerializer.ControlBlock cb = VM.copyObjectGraph(metadatas);
//        ObjectMemorySerializer.save(dos, uri, cb, getReadOnlyObjectMemory(), bigEndian);
//        if (VM.isHosted()) {
//            saveHosted(uri, cb, null);
//        }
//        NativeUnsafe.setMemorySize(originalMemorySize);
//    }

    /**
     * Serializes the object graph rooted by this suite and writes it to a given stream.
     * FIXME: what does this method REALLY do?
     *
     * @param  uri       the URI identifier of the serialized suite
     */
    private ObjectMemory saveHosted(String uri, ObjectMemorySerializer.ControlBlock cb, ObjectMemory parentMemory) throws HostedPragma {
        Address start = parentMemory == null ? Address.zero() : parentMemory.getEnd();
        int hash = ObjectMemoryLoader.hash(cb.memory);
        ObjectMemory om = new ObjectMemory(start, cb.memory.length, uri, this, hash, parentMemory);
        GC.registerReadOnlyObjectMemory(om);
        return om;
    }

    /**
     * Denotes a suite that encapsulates an application. The classes of an application
     * can not be linked against.
     */
    public static final int APPLICATION = 0;

    /**
     * Denotes a suite that encapsulates a library. The classes of a library
     * can be linked against but the library itself cannot be extended by virtue
     * of other classes linking against it's package private components.
     */
    public static final int LIBRARY = 1;

    /**
     * Denotes a suite that encapsulates an open library. The classes of an open library
     * can be linked against and the library itself can be extended by virtue
     * of other classes linking against it's package private components.
     */
    public static final int EXTENDABLE_LIBRARY = 2;

    /**
     * Denotes a suite that is being debugged. This suite retains all its symbolic information
     * when closed.
     */
    public static final int DEBUG = 3;
    
    /**
     * Denotes a suite that contains all the KlassMetadata for its parent suite.  This is to allow
     * the parent suite to have zero symbolic information, but still have the information
     * available.
     */
    public static final int METADATA = 4;

    /**
     * File name extension that identifies a Suite, includes the '.'.
     * 
     * Duplicated in builder, com.sun.squawk.builder.commands.MakeAPI
     */
    public static final String FILE_EXTENSION = ".suite";

    /**
     * File name extension that identifies a Suite's api, includes the '.'.
     * 
     * Duplicated in builder, com.sun.squawk.builder.commands.MakeAPI
     */
    public static final String FILE_EXTENSION_API = ".api";

    /**
     * File name extension that identifies a Suite's metadata, includes the '.'.
     * 
     * Duplicated in builder, com.sun.squawk.builder.commands.MakeAPI
     */
    public static final String FILE_EXTENSION_METADATA = ".metadata";
    
    /**
     * Given one of the defined suite types, return an English string describing the 
     * suite type.
     *
     * @param suiteType One of APPLICATION, LIBRARY, EXTENDABLE_LIBRARY, or DEBUG.
     * @return a string describing the suite type.
     */
    public static String typeToString(int suiteType) {
        final String[] names = {"application", "library", "extendable library", "debug", "metadata"};
        return names[suiteType];
    }

    /**
     * Denotes the name of the resource that represents the resource name from which I extract
     * properties from when an {@link #installResource(ResourceFile)} is done.
     */
    public static final String PROPERTIES_MANIFEST_RESOURCE_NAME = "META-INF/MANIFEST.MF";
    
    /**
     * Closes this suite. Once closed, a suite is immutable (and may well reside in
     * read-only memory) and cannot have any more classes installed in it
     */
    public void close() {
        closed = true;
    }

    /**
     * Creates a copy of this suite with its symbolic information stripped according to
     * the given parameters.
     *
     * @param type  specifies the type of the suite after closing. Must be
     *              {@link #APPLICATION}, {@link #LIBRARY}, {@link #EXTENDABLE_LIBRARY} or {@link #DEBUG}.
     * @param name new suite name
     * @param parent
     * @return stripped copy of this suite
     */
    public Suite strip(int type, String name, Suite parent) {
        if (type < APPLICATION || type > METADATA) {
            throw new IllegalArgumentException();
        }

        Suite copy = new Suite(name, parent, type);

        if (type == METADATA) {
        	copy.classes = new Klass[0];
            copy.metadatas = new KlassMetadata[metadatas.length];
            System.arraycopy(metadatas, 0, copy.metadatas, 0, metadatas.length);
            // it's finally "later":
            if (stripClassesLater != null) {
                 if (VM.isVerbose()) {
                 	System.out.println("Removing " + stripClassesLater.length + " classes from " + copy);
                 }
                for (int i = 0; i < stripClassesLater.length; i++) {
                	if (VM.isVerbose()) {
                        System.out.println("Removing from metadata suite: " + stripClassesLater[i]);
                    }
                    copy.removeMetadata(stripClassesLater[i]);
                }
            }
        } else {
            copy.classes = new Klass[classes.length];
            System.arraycopy(classes, 0, copy.classes, 0, classes.length);

            if (noClassDefFoundErrorClassNames != null) {
	            copy.noClassDefFoundErrorClassNames = new String[noClassDefFoundErrorClassNames.length];
	            System.arraycopy(noClassDefFoundErrorClassNames, 0, copy.noClassDefFoundErrorClassNames, 0, noClassDefFoundErrorClassNames.length);
            }

            copy.resourceFiles = new ResourceFile[resourceFiles.length];
            System.arraycopy(resourceFiles, 0, copy.resourceFiles, 0, resourceFiles.length);

    		copy.manifestProperties = new ManifestProperty [manifestProperties.length];
    		System.arraycopy(manifestProperties, 0, copy.manifestProperties, 0, manifestProperties.length);

    		copy.metadatas = KlassMetadata.strip(this, metadatas, type);

            // it's finally "later":
            if (stripClassesLater != null) {
                 if (VM.isVerbose()) {
                 	System.out.println("Removing " + stripClassesLater.length + " classes from " + copy);
                 }
                for (int i = 0; i < stripClassesLater.length; i++) {
                	if (VM.isVerbose()) {
                        System.out.println("Removing from suite: " + stripClassesLater[i]);
                    }
                    copy.removeClass(stripClassesLater[i]);
                }
            }
        }

        copy.updateConfiguration(type);
		copy.checkSuite();
        return copy;
    }

    /**
     * Updates the configuration description of this suite based on the parameters that it
     * is {@link #strip stripped} with.
     */
    private void updateConfiguration(int type) {
        if (type == DEBUG) {
            configuration = "symbols not stripped";
        } else {
            configuration = "symbols stripped in " + typeToString(type) + " mode";
        }
    }

    void checkSuite() {
        if (metadatas == null) {
            //System.out.println("<><><><><><><><> metadatas for " + this + " is null");
            return;
        }

        for (int i = 0; i < classes.length; i++) {
            Klass klass = classes[i];
            if (klass != null) {
                if (i < metadatas.length) {
                    KlassMetadata km = metadatas[i];
                    if (km != null) {
                        if (km.getDefinedClass() != klass) {
                            System.out.println("<><><><><><><><> klass is " + klass + " metadata is for " + km.getDefinedClass());
                            System.out.println("<><><><><><><><> klass ID is " + klass.getSuiteID() + " metadata is for ID " + km.getDefinedClass().getSuiteID());
                            System.out.println("<><><><><><><><> i is " + i);
                            System.out.println("<><><><><><><><> suite is " + this);
                        }
                        Assert.always(km.getDefinedClass() == klass);
                    }
                } else {
                    Assert.always(klass.isSynthetic() || klass.isArray());
                }
            } else {
                Assert.always(metadatas[i] == null);
            }
        }
    }
}
