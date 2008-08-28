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

#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <jni.h>
#include <signal.h>


#define WIN32_LEAN_AND_MEAN
#define NOMSG
#include <windows.h>
#include <process.h>
#include <winsock2.h>

#define jlong  __int64

#define FT2INT64(ft) ((jlong)(ft).dwHighDateTime << 32 | (jlong)(ft).dwLowDateTime)


/* This standard C function is not provided on Windows */
char* strsignal(int signum) {
    switch (signum) {
        case SIGABRT:     return "SIGABRT: Abnormal termination";
        case SIGFPE:      return "SIGFPE: Floating-point error";
        case SIGILL:      return "SIGILL: Illegal instruction";
        case SIGINT:      return "SIGINT: CTRL+C signal";
        case SIGSEGV:     return "SIGSEGV: Illegal storage access";
        case SIGTERM:     return "SIGTERM: Termination request";
        default:          return "<unknown signal>";
    }
}


jlong sysTimeMicros(void) {
    static jlong fileTime_1_1_70 = 0;
    SYSTEMTIME st0;
    FILETIME   ft0;

    if (fileTime_1_1_70 == 0) {
        /*
         * Initialize fileTime_1_1_70 -- the Win32 file time of midnight
         * 1/1/70.
         */
        memset(&st0, 0, sizeof(st0));
        st0.wYear  = 1970;
        st0.wMonth = 1;
        st0.wDay   = 1;
        SystemTimeToFileTime(&st0, &ft0);
        fileTime_1_1_70 = FT2INT64(ft0);
    }

    GetSystemTime(&st0);
    SystemTimeToFileTime(&st0, &ft0);

    /* file times are in 100ns increments, i.e. .0001ms */
    return (FT2INT64(ft0) - fileTime_1_1_70) / 10;
}

jlong sysTimeMillis(void) {
    return sysTimeMicros() / 1000;
}

#if PLATFORM_TYPE_DELEGATING
jint createJVM(JavaVM **jvm, void **env, void *args) {
    HINSTANCE handle;
    jint (JNICALL *CreateJavaVM)(JavaVM **jvm, void **env, void *args) = 0;

    char *name = getenv("JVMDLL");
    if (name == 0) {
    	name = getenv("JAVA_HOME");
    	if (name == 0) {
        	name = "jvm.dll";
    	} else {
    		char *append = "\\jre\\bin\\client\\jvm.dll";
    		char *buff = malloc(strlen(name)+strlen(append)+1);
    		// TODO - this memory isn't being freed, but that's probably
    		// ok since the size is small and this is called only once
    		if (buff == 0) {
    			fprintf(stderr, "Cannot malloc space for jvmdll path\n");
    			return false;
    		}
    		if (name[0] == '\'') {
    			strcpy(buff, name+1);
    			buff[strlen(name)-2] = 0;
    		} else {
    			strcpy(buff, name);
    		}
    		strcat(buff, append);
    		name = buff;
    	}
    }


    handle = LoadLibrary(name);
    if (handle == 0) {
        fprintf(stderr, "Cannot load %s\n", name);
        fprintf(stderr, "Please add the directory containing jvm.dll to your PATH\n");
        fprintf(stderr, "environment variable or set the JVMDLL environment variable\n");
        fprintf(stderr, "to the full path of this file.\n");
        return false;
    }

    CreateJavaVM = (jint (JNICALL *)(JavaVM **,void **, void *)) GetProcAddress(handle, "JNI_CreateJavaVM");

    if (CreateJavaVM == 0) {
        fprintf(stderr,"Cannot resolve JNI_CreateJavaVM in %s\n", name);
        return false;
    }

    return CreateJavaVM(jvm, env, args) == 0;
}
#endif



int sleepTime;
int ticks;

static void ticker(void) {
    for(;;) {
        Sleep(sleepTime);
        ticks++;
    }
}

void osprofstart(int interval) {
    sleepTime = interval;
#ifdef _MT
    if (sleepTime > 0) {
        printf("********** Time profiling set to %d ms **********\n", sleepTime);
        _beginthread((void (*))ticker, 0, 0);
    }
#else
    fprintf(stderr, "No MT -- Profiling not implemented");
    exit(0);
#endif
}

#define OSPROF(traceIP, traceFP, lastOpcode) \
{                                            \
    int t = ticks;                           \
    ticks = 0;                               \
    while (t-- > 0) {                        \
        printProfileStackTrace(traceIP, traceFP, lastOpcode); \
    } \
}




#undef VOID

#define osloop()        /**/
#define osbackbranch()  /**/
#define osfinish()      /**/
