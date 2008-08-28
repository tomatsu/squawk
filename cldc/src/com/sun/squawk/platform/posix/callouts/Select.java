/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.squawk.platform.posix.callouts;

import com.sun.squawk.platform.callouts.*;

/**
 * java wrapper around #include <sys/select.h>
 * 
 */
public class Select {
    /** 
     * The size of an fd_set in bytes
     */
    public static final int FD_SIZE = VarPointer.lookup("sysFD_SIZE", 4).getInt();
    
    private static final FunctionPointer selectPtr = FunctionPointer.lookup("select");    
    private static final FunctionPointer sysFD_SETPtr = FunctionPointer.lookup("sysFD_SET");
    private static final FunctionPointer sysFD_CLRPtr = FunctionPointer.lookup("sysFD_CLR");
    private static final FunctionPointer sysFD_ISSETPtr = FunctionPointer.lookup("sysFD_ISSET");

    /* pure static class */
    private Select() {}
    
    /**
     * Select() examines the I/O descriptor sets whose addresses are passed in readfds, writefds,
     * and errorfds to see if some of their descriptors are ready for reading, are ready for writ-
     * ing, or have an exceptional condition pending, respectively.
     * 
     * On return, select() replaces the given descriptor sets
     * with subsets consisting of those descriptors that are ready for the requested operation.
     * 
     * @param nfds The first nfds descriptors are checked in each set
     * @param readfds
     * @param writefds
     * @param errorfds
     * @param timeout if timout is nill, wait forever, if a pointer to a zero'd timeval, then does NOT wait.
     * @return the total number of ready descriptors in all the sets
     */
     public static int select(int nfds, Pointer readfds, Pointer writefds,
                                Pointer errorfds, Pointer timeout) {
         return selectPtr.call5(nfds, 
                    readfds.address(),
                    writefds.address(),
                    errorfds.address(), 
                    timeout.address());
     }

    /**
     * removes fd from fdset
     * 
     * @param fd
     * @param fd_set
     */
    public static void FD_CLR(int fd, Pointer fd_set) {
        sysFD_CLRPtr.call2(fd, fd_set.address());
    }
    
    /**
     * includes a particular descriptor fd in fdset.
     * 
     * @param fd
     * @param fd_set
     */
    public static void FD_SET(int fd, Pointer fd_set) {
        sysFD_SETPtr.call2(fd, fd_set.address());
    }
    
    /**
     * is non-zero if fd is a member of fd_set, zero otherwise.
     * @param fd
     * @param fd_set
     * @return
     */
    public static boolean FD_ISSET(int fd, Pointer fd_set) {
        int result = sysFD_ISSETPtr.call2(fd, fd_set.address());
        return (result == 0) ? false : true;
    }
    
    /**
     * initializes a descriptor set fdset to the null set
     * @param fd_set
     */
    public static void FD_ZERO(Pointer fd_set) {
        fd_set.clear(FD_SIZE);
    }
    
    /**
     * replaces an already allocated fdset_copy file descriptor set with a copy of fdset_orig.
     * 
     * @param fdset_orig
     * @param fdset_copy
     */
    public static void FD_COPY(Pointer fdset_orig, Pointer fdset_copy) {
//        System.err.println("FD_COPY from: " + fdset_orig + " to: " + fdset_copy + " (size = " + FD_SIZE + ")");
        Pointer.copyBytes(fdset_orig, 0, fdset_copy, 0, FD_SIZE);
    }
    
    /**
     * Allocate a new fd_struct in c memory.
     * @return pointer to new memory
     */
    public static Pointer FD_ALLOCATE() {
        return new Pointer(FD_SIZE);
    }
    
    

}
