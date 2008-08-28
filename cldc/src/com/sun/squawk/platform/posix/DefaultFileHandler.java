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

package com.sun.squawk.platform.posix;

import com.sun.squawk.VM;
import java.io.IOException;
import java.util.Vector;
import com.sun.squawk.platform.BaseFileHandler;
import com.sun.squawk.platform.callouts.Pointer;
import com.sun.squawk.platform.posix.callouts.LibC;
import com.sun.squawk.platform.posix.callouts.Stat;

/**
 * Default file handler.
 */
public class DefaultFileHandler implements BaseFileHandler {

    /*
     * Placeholder for the file handle from the native layer.
     * Value of <code>0</code> indicates that file is either closed or
     * does not exists
     */
    /** Read handle. */
    private int readHandle  = 0;
    /** Write handle. */
    private int writeHandle = 0;

    /** File name. */
    private String nativeFileName;
    
//    private Pointer fileName;
//    // The dir name is required when checking the fileSystem sizes
//    /** Root directory. */
//    private long rootDir    = 0;

    /** Illegal file name characters. */
    private final static String illegalChars = ":\\";

//    /**
//     * The flag is used for performance optimization.
//     * <code>true</code> stands for it was detected
//     * what private directory exists.
//     */
//    private static boolean privateDirExists = false;

//    /**
//     * Init class info in the native code
//     */
//    static {
////        initialize();
//
//        illegalChars = illegalFileNameChars0();
//        if (illegalChars == null) {
//            illegalChars = "";
//        }
//    }
    
    /**
     * Connect file handler to the abstract file target. This operation should
     * not trigger any access to the native filesystem.
     *
     * @param rootName The name of the root directory.
     *
     * @param fileName Full path to the file to be handled by this handler.
     *
     * @throws IllegalArgumentException if filename contains characters
     *         not allowed by the file system. This check should not involve
     *         any actual access to the filesystem.
     */
    public void connect(String rootName, String fileName) {
// System.err.println("connect(rootName: " + rootName + ", fileName: " + fileName + ")");
            StringBuffer name = new StringBuffer(fileName);
//System.err.println("connect: name before convert " + name);
            pathToNativeSeparator(name, 0, name.length());
            nativeFileName = name.toString();
//System.err.println("connect: name after convert " + nativeFileName);
    }

//    /**
//     * Creates dedicated private working directory for the MIDlet suite.
//     * Does nothing if specified root is not private root or the directory
//     * already exists.
//     *
//     * @param rootName the name of file root
//     */
//    public void createPrivateDir(String rootName) throws IOException {
//        // create private directory if necessary
//        String privateDir = System.getProperty("fileconn.dir.private");
//        if (privateDir != null) {
//            privateDir = privateDir.substring(7);
//            if (!privateDirExists && rootName.regionMatches(true, 0, privateDir,
//                    1, rootName.length())) {
//                BaseFileHandler fh = new DefaultFileHandler();
//                fh.connect(rootName, privateDir);
//                if (!fh.exists()) {
//                    fh.mkdir();
//                }
//                privateDirExists = true;
//            }
//        }
//    }

//    /**
//     * Gets a filtered list of files and directories contained in a directory.
//     * The directory is the handler's target as specified in
//     * <code>create()</code>.
//     *
//     * @param   filter String against which all files and directories are
//     *          matched for retrieval.  An asterisk ("*") can be used as a
//     *          wildcard to represent 0 or more occurrences of any character.
//     *          If null no filtering is performed
//     * @param   includeHidden boolean indicating whether files marked as hidden
//     *          should be included or not in the list of files and directories
//     *          returned.
//     * @return  An Enumeration of strings, denoting the files and directories
//     *          in the directory matching the filter. Directories are denoted
//     *          with a trailing slash "/" in their returned name.  The
//     *          Enumeration has zero length if the directory is empty or no
//     *          files and/or directories are found matching the given filter.
//     *          Any current directory indication (".") and any parent directory
//     *          indication ("..") is not included in the list of files and
//     *          directories returned.
//     * @throws  IOException if invoked on a file, the directory does not exist,
//     *          the directory is not accessible, or an I/O error occurs.
//     * @throws  IllegalArgumentException if filter contains any path
//     *          specification or is an invalid filename for the platform
//     *          (e.g. contains characters invalid for a filename on their
//     *          platform).
//     */
//    public Vector list(String filter, boolean includeHidden)
//            throws IOException {
//
//        Vector list = new Vector();
//
//        long dirHandle = openDir();
//
//        String fname = dirGetNextFile(dirHandle, includeHidden);
//        while (fname != null) {
//            // cleanname is passed to the filter and does not contain trailing
//            // slash denoting directory
//            String cleanname;
//            if (fname.charAt(fname.length() - 1) == '/') {
//                cleanname = fname.substring(0, fname.length() - 1);
//            } else {
//                cleanname = fname;
//            }
//
//            if (filterAccept(filter, cleanname)) {
//                list.addElement(fname);
//            }
//            fname = dirGetNextFile(dirHandle, includeHidden);
//        }
//
//        closeDir(dirHandle);
//        return list;
//    }
//
//    /**
//     * List filesystem roots available on the device. For the description of
//     * the correct root format see <code>FileConnection</code> documentation.
//     * @return array of roots;
//     *         empty array is returned if there are no roots available.
//     */
//    public Vector listRoots() {
//        Vector roots = new Vector();
//        String s = getMountedRoots();
//        if (s != null) {
//            String[] rs = com.sun.kvem.midp.pim.formats.FormatSupport
//                .split(s, '\n', 0);
//            for (int i = 0; i < rs.length; i++) {
//                roots.addElement(rs[i]);
//            }
//        }
//        return roots;
//    }
//
//    /**
//     * Create file corresponding to this file handler. The
//     * file is created immediately on the actual file system upon invocation of
//     * this method.  Files are created with zero length and data can be put
//     * into the file through write method after opening the file. This method
//     * does not create any directories specified in the file's path.
//     *
//     * @throws IOException if invoked on the existing file or unexpected error
//     *                     occurs.
//     */
//    public native void create() throws IOException;

    /**
     * Check is file or directory corresponding to this filehandler exists.
     * @return <code>true</code> if the file/directory exists,
     *         <code>false</code> otherwise.
     */
    public boolean exists() {
        if (Stat.stat(nativeFileName, new Stat.Struct_Stat()) != -1) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isModeDir(int mode) {
        return (((mode) & Stat.S_IFMT) == Stat.S_IFDIR);
    }
            
    /**
     * Check is file corresponding to this filehandler exists and is a
     * directory.
     * @return <code>true</code> if pathname is a directory
     */
    public boolean isDirectory() {
        Stat.Struct_Stat stats = new Stat.Struct_Stat();
        if (Stat.stat(nativeFileName, stats) != -1) {
            return isModeDir(stats.st_mode);
        } else {
            return false;
        }
    }

//    /**
//     * Deletes the file or directory associated with this handler.
//     * The file or directory is deleted immediately on
//     * the actual file system upon invocation of this method. Previously open
//     * native file should be closed.The
//     * handler instance object remains connected and available for use.
//     *
//     * @throws  IOException If the target is a directory and it is not empty,
//     *      the connection target does not exist or is unaccessible, or
//     *      an unspecified error occurs preventing deletion of the target.
//     */
//    public native void delete() throws IOException;
//
//    /**
//     * Renames the selected file or directory to a new name in the same
//     * directory.  The file or directory is renamed immediately on the actual
//     * file system upon invocation of this method. No file or directory by the
//     * original name exists after this method call. Previously open native file
//     * should be closed. The handler
//     * instance object remains connected and available for use,
//     * referring now to the file or directory by its new name.
//     *
//     * @param   newName The new name of the file or directory. The name must
//     *          be the full qualified name of the new file
//     * @throws  IOException if the connection's target does not exist, the
//     *          connection's target is not accessible, a file or directory
//     *          already exists by the <code>newName</code>, or
//     *          <code>newName</code> is an invalid filename for the platform
//     *          (e.g. contains characters invalid in a filename
//     *           on the platform).
//     */
//    public void rename(String newName) throws IOException {
//        // we start search of '/' from '1' to skip leading '/'
//        // that means local machine in URL specification
//        int rootEnd = newName.indexOf('/', 1);
//        String rootName = newName.substring(1, rootEnd + 1);
//
//        if (rootName.length() == 0) {
//            return;
//        }
//        String rootPath = getNativePathForRoot(rootName);
//        if (rootPath != null)
//        {
//            StringBuffer name = new StringBuffer(rootPath);
//            int curr = name.length();
//
//            String newNameWORoot = newName.substring(rootEnd + 1);
//            name.append(newNameWORoot);
//
////            String privateDirURL = System.getProperty("fileconn.dir.private");
////            if (privateDirURL != null) {
////                int privateDirURLLen = privateDirURL.length();
////                if (newName.regionMatches(true, 0, privateDirURL, 7,
////                        privateDirURLLen - 7)) {
////                    name.insert(curr + privateDirURLLen - rootName.length() - 8,
////                        getSuiteID() + getFileSeparator());
////                }
////            }
//
//            pathToNativeSeparator(name, curr, name.length() - curr);
//            rename0(name.toString());
//        }
//    }
//
//    /**
//     * Helper method that renames the file.
//     * @param newName new name for the file
//     * @throws IOException if any error occurs
//     */
//    private native void rename0(String newName) throws IOException;
//
//    /**
//     * Truncates the file, discarding all data from the given byte offset to
//     * the current end of the file.  If the byte offset provided is greater
//     * than or equal to the file's current byte count, the method returns
//     * without changing the file.
//     *
//     * @param   byteOffset the offset into the file from which truncation
//     *          occurs.
//     * @throws  IOException if invoked on a directory or the file does not
//     *          exist or is not accessible.
//     */
//    public native void truncate(long byteOffset) throws IOException;
//
//    /**
//     * Determines the size of a file on the file system. The size of a file
//     * always represents the number of bytes contained in the file; there is
//     * no pre-allocated but empty space in a file. Users should perform an
//     * explicit <code>flush()</code> on any open output streams to the file
//     * prior to invoking this method to ensure accurate results.
//     *
//     * @return  The size in bytes of the selected file, or -1 if the
//     *          file does not exist or is not accessible.
//     * @throws  IOException if the method is invoked on a directory.
//     */
//    public native long fileSize() throws IOException;

//    /**
//     * Determines the size in bytes on a file system of all of the files
//     * that are contained in a directory.
//     *
//     * @param   includeSubDirs if <code>true</code>, size calculation will
//     *          include all subdirectories' size.
//     * @return  The size in bytes occupied by the files included in the
//     *          directory, or -1 if the directory does not exist or is
//     *          not accessible.
//     * @throws  IOException if some error occures while accessing the directory.
//     */
//    public native long directorySize(boolean includeSubDirs) throws IOException;

//    /**
//     * Check is file corresponding to this filehandler exists and has a
//     * read permission.
//     * @return <code>true</code> if the file can be read
//     */
//    public boolean canRead();

//    /**
//     * Check is file corresponding to this filehandler exists and has a
//     * write permission.
//     * @return <code>true</code> if the file can be written
//     */
//    public boolean canWrite();

//    /**
//     * Check is file corresponding to this filehandler exists and is
//     * hidden.
//     * @return <code>true</code> if the file is not visible
//     */
//    public boolean isHidden() {
//        // Note: ANSI C does not define hidden files.
//        // Sure, on UNIX systems a file is considered to be hidden
//        // if its name begins with a period character ('.'), but we can not
//        // rename files during setHidden() method, so we consider
//        // what hidden files are not supported on UNIX systems, and this method
//        // always returns false on UNIX as it's required by JSR 75 spec.
//        return isHidden0();
//    }
//
//    /**
//     * Helper method that checks if the file is visible.
//     * @return <code>true</code> if the file is not visible
//     */
//    private native boolean isHidden0();
//
//    /**
//     * Sets the file or directory readable attribute to the
//     * indicated value.  The readable attribute for the file on the actual
//     * file system is set immediately upon invocation of this method. If the
//     * file system doesn't support a settable read attribute, this method is
//     * ignored and <code>canRead()</code> always returns true.
//     *
//     * @param   readable The new state of the readable flag of
//     *          the selected file.
//     * @throws  IOException of the connection's target does not exist or is not
//     *          accessible.
//     * @see     #canRead
//     */
//    public native void setReadable(boolean readable) throws IOException;

//    /**
//     * Sets the file or directory associated with this file handler writable
//     * attribute to the
//     * indicated value.  The writable attribute for the file on the actual
//     * file system is set immediately upon invocation of the method. If the
//     * file system doesn't support a settable write attribute, this method is
//     * ignored and <code>canWrite()</code> always returns true.
//     *
//     * @param   writable The new state of the writable flag of the selected
//     *                   file.
//     * @throws  IOException if the connection's target does not exist or is not
//     *          accessible.
//     * @see     #canWrite
//     */
//    public native void setWritable(boolean writable) throws IOException;
//
//    /**
//     * Sets the hidden attribute of the file associated with this file handler
//     * to the value provided.  The attribute is applied to the file on the
//     * actual file system immediately upon invocation of this method if the
//     * file system
//     * and platform support it. If the file system doesn't support a hidden
//     * attribute, this method is ignored and <code>isHidden()</code> always
//     * returns false.  Since the exact definition of hidden is
//     * system-dependent,
//     * this method only works on file systems that support a settable file
//     * attribute. For example, on Win32 and FAT file systems, a file may be
//     * considered hidden if it has been marked as such in the file's
//     * attributes; therefore this method is applicable.  However on UNIX
//     * systems a file may be considered to be hidden if its name begins with a
//     * period character ('.'). In the UNIX case, this method may be ignored and
//     * the method to make a file hidden may be the <code>rename()</code>
//     * method.
//     *
//     * @param   hidden The new state of the hidden flag of the selected file.
//     * @throws  IOException if the connection's target does not exist or is not
//     *          accessible.
//     * @see     #isHidden
//     */
//    public void setHidden(boolean hidden) throws IOException {
//        setHidden0(hidden);
//    }
//
//    /**
//     * Helper method that marks the file hidden flag.
//     * @param hidden <code>true</code> to make file as not visible
//     * @throws IOException if any error occurs
//     */
//    private native void setHidden0(boolean hidden) throws IOException;
//
//    /**
//     * Creates a directory corresponding to the directory
//     * string provided in the connect() method.
//     * The directory is created immediately on the actual
//     * file system upon invocation of this method.  Directories in the
//     * specified path are not recursively created and must be explicitly
//     * created before subdirectories can be created.
//     *
//     * @throws  IOException if invoked on an existing directory or on any file
//     *          (<code>create()</code> is used to create files), the target
//     *          file system is not accessible, or an unspecified error occurs
//     *          preventing creation of the directory.
//     */
//    public native void mkdir() throws IOException;
//
//    /**
//     * Determines the free memory that is available on the file system the file
//     * or directory resides on. This may only be an estimate and may vary based
//     * on platform-specific file system blocking and metadata information.
//     *
//     * @return  The available size in bytes on a file system, or -1 if an
//     *          error occurs.
//     */
//    public native long availableSize();

//    /**
//     * Determines the total size of the file system the connection's target
//     * resides on.
//     *
//     * @return  The total size of the file system in bytes, or -1 if an
//     *          error occurs.
//     */
//    public native long totalSize();
//
//    /**
//     * Determines the used memory of a file system the connection's target
//     * resides on.  This may only be an estimate and may vary based
//     * on platform-specific file system blocking and metadata information.
//     *
//     * @return  The used size of bytes on a file system, or -1 if an
//     *          error occurs.
//     */
//    public long usedSize() {
//        return totalSize() - availableSize();
//    }

    /**
     * Returns a string that contains all characters forbidden for the use on
     * the given platform except "/" (forward slash) which is always considered
     * illegal. If there are no such characters an empty string is returned.
     * @return string of illegal file name characters
     */
    public String illegalFileNameChars() {
        return illegalChars;
    }

//    /**
//     * Returns the time that the file denoted by this file handler
//     * was last modified.
//     * @return The time then last modification of the file took place.
//     */
//    public native long lastModified();

    /**
     * Opens the file for reading.
     * @throws IOException if any error occurs during input/output operations.
     */
    public void openForRead() throws IOException {
        // @TODO: Check that file isn't dir first?
        
        readHandle = LibC.open(nativeFileName, LibC.O_RDONLY);
//System.err.println("openForRead: " + readHandle);
        LibC.errCheckNeg(readHandle);
    }

    /**
     * Closes the file for reading.
     * @throws IOException if any error occurs during input/output operations.
     */
    public void closeForRead() throws IOException {
        LibC.errCheckNeg(LibC.close(readHandle));
//System.err.println("closeForRead: " + readHandle);

        readHandle = -1;
    }

    /**
     * Opens the file for writing.
     * @throws IOException if any error occurs during input/output operations.
     */
    public void openForWrite() throws IOException {
        // @TODO: Check that file isn't dir first?
        writeHandle = LibC.open(nativeFileName, LibC.O_WRONLY);
//System.err.println("openForWrite: " + writeHandle);

        LibC.errCheckNeg(writeHandle);
    }

    /**
     * Closes the file for writing.
     * @throws IOException if any error occurs during input/output operations.
     */
    public void closeForWrite() throws IOException {
        LibC.errCheckNeg(LibC.close(writeHandle));
//   System.err.println("closeForWrite: " + writeHandle);
        writeHandle = -1;
    }

    /**
     * Closes the file for both reading and writing.
     * @throws IOException if any error occurs during input/output operations.
     */    
    public void closeForReadWrite() throws IOException {
        LibC.errCheckNeg(LibC.close(readHandle));
        readHandle = -1;
        LibC.errCheckNeg(LibC.close(writeHandle));
        writeHandle = -1;
    }

    /**
     * Reads data from the file to an array.
     * @param b array for input data
     * @param off index in the input array
     * @param len length of data to read
     * @return length of data really read
     * @throws IOException if any error occurs.
     */
    public int read(byte b[], int off, int len) throws IOException {
        if (readHandle < 0) {
            throw new IOException("file closed");
        }
        // @TODO: THIS BLOCKS!!!!
        int result = LibC.read(readHandle, b, off, len);

        LibC.errCheckNeg(result);
        return result;
    }

    /**
     * Write data from an array to the file.
     * @param b array of output data
     * @param off index in the output array
     * @param len length of data to write
     * @return length of data really written
     * @throws IOException if any error occurs.
     */
    public int write(byte b[], int off, int len) throws IOException {
        if (writeHandle < 0) {
            throw new IOException("file closed");
        }
        // @TODO: THIS BLOCKS!!!!
         int result = LibC.write(writeHandle, b, off, len);
         LibC.errCheckNeg(result);
         return result;
    }

    /**
     * Sets the next write location.
     * @param offset seek position for next write
     * @throws IOException if any error occurs.
     */
    public void positionForWrite(long offset) throws IOException { }

    /**
     * Flushes any output to the file.
     * @throws IOException if any error occurs.
     */
    public void flush() throws IOException {
        if (writeHandle > 0) {
            LibC.errCheckNeg(LibC.fsync(writeHandle));
        }
    }

    /**
     * Close file associated with this handler. Open file and all system
     * resources should be released by this call. Handler object can be
     * reused by subsequent call to connect().
     *
     * @throws IOException if I/O error occurs
     */
    public void close() throws IOException {
        closeForReadWrite();
    }

//    /**
//     * Returns ID of the current MIDlet suite in 8-digit reverse hexadecimal
//     * form (the same form is used in native code).
//     *
//     * @return resulting String representation of the MIDlet suite ID
//     */
//    private String getSuiteID() {
//        MIDletSuite midletSuite =
//            MIDletStateHandler.getMidletStateHandler().getMIDletSuite();
//        StringBuffer id = new StringBuffer(Integer.toHexString(midletSuite.getID()));
//        int len = id.length();
//        while (len++ < 8) {
//            id.insert(0, '0');
//        }
//        return id.reverse().toString();
//    }

    /**
     * Replace all entries of the "//" with "/" (multiple separators
     * with single separator) and all "/" with the native separator.
     *
     * @param name StringBuffer to process
     * @param off offset from where to start the conversion
     * @param len length to convert
     *
     * @return the same StringBuffer after the process
     */
    private StringBuffer pathToNativeSeparator(StringBuffer name,
                            int off, int len) {

        int  length = off + len;
        int  curr   = off;
        char sep    = getFileSeparator();

        while ((curr + 1) < length) {
            if (name.charAt(curr) == '/' && name.charAt(curr+1) == '/') {
                name.deleteCharAt(curr);
                length--;
                continue;
            } else if (name.charAt(curr) == '/') {
                name.setCharAt(curr, sep);
            }
            curr++;
        }

        // trim trailing slash if it exists
        if (length != 1 && name.charAt(length - 1) == '/') {
            name.deleteCharAt(length - 1);
        }

        return name;
    }

    /**
     * Check that given string matches the given filter. Filter is defined by
     * the FileConnection spec as follows:
     * <p>String against which all files and directories are
     *    matched for retrieval.  An asterisk ("*") can be used as a
     *    wildcard to represent 0 or more occurrences of any character.
     * <p>Filter value of null matches any string.
     *
     * @param filter string against witch to match
     * @param str    string to be matched
     *
     * @return true is str matches the filter of if filter is null,
     *         otherwise false
     */
    private boolean filterAccept(String filter, String str) {

        if (filter == null) {
            return true;
        }

        if (filter.length() == 0) {
            return false;
        }

        int  currPos = 0;
        int currComp = 0, firstSigComp = 0;
        int idx;

        // Splitted string does not contain separators themselves
        String components[] = split(filter, '*', 0);

        // if filter does not begin with '*' check that string begins with
        // filter's first component
        if (filter.charAt(0) != '*') {
            if (!str.startsWith(components[0])) {
                return false;
            } else {
                currPos += components[0].length();
                currComp++;
                firstSigComp = currComp;
            }
        }

        // Run on the string and check that it contains all filter
        // components sequentially
        for (; currComp < components.length; currComp++) {
            if ((idx = str.indexOf(components[currComp], currPos)) != -1) {
                currPos = idx + components[currComp].length();
            } else {
                // run out of the string while filter components remain
                return false;
            }
        }

        // At this point we run out of filter. First option is that
        // filter ends with '*', or string is finished,
        // we are fine then, and accept the string.
        //
        // In the other case we check that string ends with the last component
        // of the filter (given that there was an asterisk before the last
        // component
        if (!(filter.charAt(filter.length() - 1) == '*'
                || currPos == str.length())) {
            if (components.length > firstSigComp) {
                // does string end with the last filter component?
                if (!str.endsWith(components[components.length - 1])) {
                    return false;
                }
            } else {
                // there was no asteric before last filter component
                return false;
            }
        }

        // If we got here string is accepted
        return true;
    }

    /**
     * Parses a separated list of strings into a string array.
     * An escaped separator (backslash followed by separatorChar) is not
     * treated as a separator.
     * @param data string to be processed
     * @param separatorChar the character used to separate items
     * @param startingPoint Only use the part of the string that follows this
     * index
     *
     * @return a non-null string array containing string elements
     */
    private static String[] split(String data, char separatorChar,
            int startingPoint) {

        if (startingPoint == data.length()) {
            return new String[0];
        }
        Vector elementList = new Vector();

        if (data.charAt(startingPoint) == separatorChar) {
            startingPoint++;
        }

        int startSearchAt = startingPoint;
        int startOfElement = startingPoint;

        for (int i; (i = data.indexOf(separatorChar, startSearchAt)) != -1; ) {
            if (i != 0 && data.charAt(i - 1) == '\\') {
                // escaped semicolon. don't treat it as a separator
                startSearchAt = i + 1;
            } else {
                String element = data.substring(startOfElement, i);
                elementList.addElement(element);
                startSearchAt = startOfElement = i + 1;
            }
        }

        if (data.length() > startOfElement) {
            if (elementList.size() == 0) {
                return new String[] { data.substring(startOfElement) };
            }
            elementList.addElement(data.substring(startOfElement));
        }

        String[] elements = new String[elementList.size()];
        for (int i = 0; i < elements.length; i++) {
            elements[i] = (String) elementList.elementAt(i);
        }
        return elements;
    }

    /**
     * Return pointer to the system-dependent file name stored in the native
     * code.
     *
     * @param name a string representing the filename to convert to native
     *             the form
     * @return A pointer to the system-dependent file name
     */
    private static Pointer getNativeName(String name, Pointer oldName) {
        if (!oldName.address().isZero()) {
            oldName.free();
        }
        return Pointer.createStringBuffer(name);
    }

    /**
     * Gets the system-dependent file separator character.
     * @return The file separator character.
     */
    public static char getFileSeparator() {
        return VM.getFileSeparatorChar();
    }

//    /**
//     * Opens the directory.
//     * @return native pointer to an opaque filelist structure used by
//     *         methods iterating over file list.
//     */
//    private native long openDir();
//
//    /**
//     * Closes the directory.
//     * @param dirHandle native pointer to an opaque filelist structure
//     *         returned by openDir method.
//     */
//    private native void closeDir(long dirHandle);
//
//    /**
//     * Gets the next file in directory.
//     * @param dirHandle native pointer to a filelist structure
//     *                  returned by <code>openDir</code>
//     * @param includeHidden determines whether it's necessary
//     *                      to include hidden files and directories
//     * @return the name of the file.
//     */
//    private native String dirGetNextFile(long dirHandle, boolean includeHidden);
//
//    /**
//     * Gets the mounted root file systems.
//     * @return A string containing currently mounted roots
//     *          separated by '\n' character
//     */
//    private native String getMountedRoots();

    /**
     * Gets OS path for the specified file system root.
     * @param root root name
     * @return The path to access the root
     */
    private String getNativePathForRoot(String root) {
        return "/";
    }

//    /**
//     * Gets the list of illegal characters in file names.
//     * @return A string containing the characters
//     *         that are not allowed inside file names.
//     */
//    private static native String illegalFileNameChars0();
//
//    /**
//     * Initializes native part of file handler.
//     */
//    private native static void initialize();
//
//    /**
//     * Cleanup after garbage collected instance
//     */
//    private native void finalize();
}
