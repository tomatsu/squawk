/*
 * Copyright 2004-2010 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright 2011 Oracle Corporation. All Rights Reserved.
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
 * Please contact Oracle Corporation, 500 Oracle Parkway, Redwood
 * Shores, CA 94065 or visit www.oracle.com if you need additional
 * information or have any questions.
 */

//#define UNICODE true

/**
 * Sets up the global used to pass an int result from an INTERNAL IO operation back to the VM.
 * The value is accessed in the VM by calling VM.serviceResult().
 *
 * @param value the int value to return to the VM
 */
INLINE void returnIntResult(int value) {
    com_sun_squawk_ServiceOperation_result = value;
}

/**
 * Sets up the 2 globals used to pass a long result from an INTERNAL IO operation back to the VM.
 * The value is accessed in the VM as '(high << 32) | (low & 0x00000000FFFFFFFFL)' where
 * 'high' is the result of the next call to VM.serviceResult() and 'low' is the result
 * of a INTERNAL_LOW_RESULT channel operation.
 *
 * @param value the long value to return to the VM
 */
INLINE void returnLongResult(jlong value) {
    internalLowResult = (int)value;
    com_sun_squawk_ServiceOperation_result = (int)(value >> 32);
}

/**
 * Sets up the global used to pass an address result from an INTERNAL IO operation back to the VM.
 * The value is accessed in the VM by calling VM.addressResult().
 *
 * @param value the address value to return to the VM
 */
INLINE void returnAddressResult(Address value) {
    com_sun_squawk_ServiceOperation_addressResult = value;
}

//#if PLATFORM_TYPE_BARE_METAL
//#include "metal_sleep.h"
//#include "io_metal.h"
//#else
#ifdef IODOTC
#include IODOTC
#endif
//#endif

#if PLATFORM_TYPE_BARE_METAL
/* Different bare metal platforms handle this differently - eg set IODOTC to a specific file.
 * Also include io_metal.c*/
//#include "io_metal.c"
//#include "io_simple.c.inc"
#endif

#ifdef OLD_IIC_MESSAGES
/*
 * Include the message processing code.
 */
#include "msg.c"
#endif



#if HAS_STREAM_IO

void cioPrintAddress(Address o) {
    FILE   *vmOut   = streams[currentStream];
	fprintf(vmOut, "%p", o);
 	fflush(vmOut);
}

void cioPrintCString(char* s) {
    FILE   *vmOut   = streams[currentStream];
	fprintf(vmOut, "%s", s);
 	fflush(vmOut);
}

void cioPrintChar(int ch) {
    FILE   *vmOut   = streams[currentStream];
	fprintf(vmOut, "%c", ch);
 	fflush(vmOut);
}

void cioPrintWord(int x) {
    FILE   *vmOut   = streams[currentStream];
	fprintf(vmOut, "%d", x);
	fflush(vmOut);
}

void cioPrintUWord(int val) {
    FILE   *vmOut   = streams[currentStream];
	fprintf(vmOut, "0x%x", (UWord)val);
	fflush(vmOut);
}

void cioPrintDouble(double d) {
    FILE   *vmOut   = streams[currentStream];
	fprintf(vmOut, "%f", d);
	fflush(vmOut);
}

void cioPrintFloat(float f) {
    FILE   *vmOut   = streams[currentStream];
	fprintf(vmOut, "%f", f);
	fflush(vmOut);
}

void cioPrintLong(jlong x) {
    FILE   *vmOut   = streams[currentStream];
	fprintf(vmOut, "0x%llx", x);
	fflush(vmOut);
}

void cioPrintBytes(char* bytes, int len) {
    FILE   *vmOut   = streams[currentStream];
	int i;
	for (i = 0; i < len; i++) {
        fprintf(vmOut, "%c", getByte(bytes, i));
    }
    fflush(vmOut);
}

#elif HAS_STDIO

void cioPrintCString(char* s) {
	 puts(s);
}

void cioPrintBytes(char* bytes, int len) {
	 write(1, bytes, len);
}

void cioPrintChar(int ch) {
	 putchar(ch);
}

void cioPrintLong(jlong x) {
     fprintf(stdout, "0x%llx", x);
}

void cioPrintWord(int x) {
         printf("%d", x);
}

#endif /* HAS_STREAM_IO */

void cioPrintJavaString(Address str) {
	if (str == null) {
        cioPrintCString("null");
    } else {
        int length = getArrayLength(str);
        unsigned char *chars = (unsigned char *)str;
        cioPrintBytes(chars, length);
    }
}

/**
 * Execute a channel operation.
 *
 *
 *
 */
void cioExecute(void) {
/*  int     context = com_sun_squawk_ServiceOperation_context; */
    int     op      = com_sun_squawk_ServiceOperation_op;
/*  int     channel = com_sun_squawk_ServiceOperation_channel; */
    int     i1      = com_sun_squawk_ServiceOperation_i1;
    int     i2      = com_sun_squawk_ServiceOperation_i2;
 /* int     i3      = com_sun_squawk_ServiceOperation_i3;
    int     i4      = com_sun_squawk_ServiceOperation_i4;
    int     i5      = com_sun_squawk_ServiceOperation_i5;
    int     i6      = com_sun_squawk_ServiceOperation_i6;
  */
    Address o1      = com_sun_squawk_ServiceOperation_o1;
/*  Address o2      = com_sun_squawk_ServiceOperation_o2; */

    switch (op) {

#ifdef ChannelConstants_INTERNAL_SETSTREAM
        case ChannelConstants_INTERNAL_SETSTREAM: {
            com_sun_squawk_ServiceOperation_result = setStream(i1);
            break;
        }
#endif

#ifdef ChannelConstants_INTERNAL_PRINTSTRING
        case ChannelConstants_INTERNAL_PRINTSTRING: {
            cioPrintJavaString(o1);
            break;
        }
#endif

#ifdef ChannelConstants_INTERNAL_PRINTCHAR
        case ChannelConstants_INTERNAL_PRINTCHAR: {
		    cioPrintChar(i1);
            break;
        }
#endif

#ifdef ChannelConstants_INTERNAL_PRINTINT
        case ChannelConstants_INTERNAL_PRINTINT: {
		    cioPrintWord(i1);
            break;
        }
#endif

/* use this as a proxy for /*if[FLOATS], which can't be used because this file isn't an .spp file */
#ifdef F_POS_INFINITY
#ifdef ChannelConstants_INTERNAL_PRINTDOUBLE
        case ChannelConstants_INTERNAL_PRINTDOUBLE: {
			cioPrintDouble(lb2d(makeLong(i1, i2)));
            break;
        }
#endif
#ifdef ChannelConstants_INTERNAL_PRINTFLOAT
        case ChannelConstants_INTERNAL_PRINTFLOAT: {
			cioPrintFloat(ib2f(i1));
            break;
        }
#endif
#endif

#ifdef ChannelConstants_INTERNAL_PRINTUWORD
        case ChannelConstants_INTERNAL_PRINTUWORD: {
		    cioPrintUWord(i1);
            break;
        }
#endif

#ifdef ChannelConstants_INTERNAL_PRINTOFFSET
        case ChannelConstants_INTERNAL_PRINTOFFSET: {
		    cioPrintWord(i1);
            break;
        }
#endif

#ifdef ChannelConstants_INTERNAL_PRINTLONG
        case ChannelConstants_INTERNAL_PRINTLONG: {
            jlong val = makeLong(i1, i2);
			cioPrintLong(val);
            break;
        }
#endif

#ifdef ChannelConstants_INTERNAL_PRINTADDRESS
        case ChannelConstants_INTERNAL_PRINTADDRESS: {
            Address val = o1;
			cioPrintWord((int)val);
            break;
        }
#endif

#ifdef ChannelConstants_INTERNAL_PRINTBYTES
        case ChannelConstants_INTERNAL_PRINTBYTES: {
            Address bytes = o1;
            int offset = i1;
            int len = i2;
            int blen = (int)getArrayLength(bytes);
            int i;
            /*fprintf(stderr, "IPB: %d:%d\n", offset, len);*/
            assumeAlways(offset >= 0 && len >= 0);
            assumeAlways(offset + len <= blen);
			cioPrintBytes((char*)bytes + offset, len);
            break;
        }
#endif

//        case ChannelConstants_INTERNAL_PRINTCONFIGURATION: {
//            fprintf(stderr, "native VM build flags: %s\n", BUILD_FLAGS);
//            break;
//        }

#if com_sun_squawk_Klass_DEBUG_CODE_ENABLED
        case ChannelConstants_INTERNAL_PRINTGLOBALS: {
		    FILE   *vmOut   = streams[currentStream];
            printGlobals();
            break;
        }

        case ChannelConstants_INTERNAL_PRINTGLOBALOOPNAME: {
#if TRACE
	        cioPrintCString(getGlobalOopName(i1));
#else
			cioPrintCString("Global oop:");
			cioPrintWord(i1);
#endif /* TRACE */
            break;
        }
#endif /* com_sun_squawk_Klass_DEBUG_CODE_ENABLED */

#ifdef ChannelConstants_INTERNAL_GETPATHSEPARATORCHAR
        case ChannelConstants_INTERNAL_GETPATHSEPARATORCHAR: {
            com_sun_squawk_ServiceOperation_result = pathSeparatorChar;
            break;
        }
#endif

#ifdef ChannelConstants_INTERNAL_GETFILESEPARATORCHAR
        case ChannelConstants_INTERNAL_GETFILESEPARATORCHAR:  {
            com_sun_squawk_ServiceOperation_result = fileSeparatorChar;
            break;
        }
#endif

#ifdef ChannelConstants_INTERNAL_GETTIMEMICROS_HIGH
        case ChannelConstants_INTERNAL_GETTIMEMICROS_HIGH: {
            returnLongResult(sysTimeMicros());
            break;
        }
#endif

#ifdef ChannelConstants_INTERNAL_GETTIMEMILLIS_HIGH
        case ChannelConstants_INTERNAL_GETTIMEMILLIS_HIGH: {
            returnLongResult(sysTimeMillis());
            break;
        }
#endif

#ifdef ChannelConstants_INTERNAL_LOW_RESULT
        case ChannelConstants_INTERNAL_LOW_RESULT: {
            com_sun_squawk_ServiceOperation_result = internalLowResult;
            break;
        }
#endif

#ifdef ChannelConstants_INTERNAL_STOPVM
        case ChannelConstants_INTERNAL_STOPVM: {
            stopVM(i1);
            break;
        }
#endif

#ifdef OLD_IIC_MESSAGES
        case ChannelConstants_INTERNAL_ALLOCATE_MESSAGE_BUFFER: {
            deferInterruptsAndDo(
                allocateMessageBuffer();
            );
            break;
        }

        case ChannelConstants_INTERNAL_FREE_MESSAGE_BUFFER: {
            deferInterruptsAndDo(
                freeMessageBuffer(o2);
            );
            break;
        }

        case ChannelConstants_INTERNAL_SEND_MESSAGE_TO_SERVER: {
            sendMessage(o1, o2, i1, &toServerMessages, &toServerWaiters);
#if KERNEL_SQUAWK
            /*
             * We could use a special _TO_KERNEL type for messages
             * to the driver's request url but this is simpler.
             */
            if (kernelMode && !inKernelMode()) {
                void Squawk_enterKernel();
                Squawk_enterKernel();
            }
#endif /* KERNEL_SQUAWK */
            break;
        }

        case ChannelConstants_INTERNAL_RECEIVE_MESSAGE_FROM_CLIENT: {
            receiveMessage(o1, &toServerMessages, &toServerWaiters);
            break;
        }

        case ChannelConstants_INTERNAL_SEND_MESSAGE_TO_CLIENT: {
            sendMessage(o1, o2, i1, &toClientMessages, &toClientWaiters);
            break;
        }

        case ChannelConstants_INTERNAL_RECEIVE_MESSAGE_FROM_SERVER: {
            receiveMessage(o1, &toClientMessages, &toClientWaiters);
            break;
        }

        case ChannelConstants_INTERNAL_SEARCH_SERVER_HANDLERS: {
            searchServerHandlers(o2);
            break;
        }

#ifdef ChannelConstants_GLOBAL_WAITFOREVENT
        case ChannelConstants_GLOBAL_WAITFOREVENT: {
            if (!checkForMessageEvent()) {
                ioExecute();
            }
            break;
        }
#endif
#ifdef ChannelConstants_GLOBAL_GETEVENT
        case ChannelConstants_GLOBAL_GETEVENT: {
            if (!getMessageEvent()) {
                ioExecute();
            }
            break;
        }
#endif		
#endif /* OLD_IIC_MESSAGES */

        /* WARNING! NOT 64-bit safe! */
#ifdef ChannelConstants_INTERNAL_NATIVE_PLATFORM_NAME		
        case ChannelConstants_INTERNAL_NATIVE_PLATFORM_NAME: {
            com_sun_squawk_ServiceOperation_result = (int)sysPlatformName();
            break;
        }
#endif

#if NATIVE_VERIFICATION
        case ChannelConstants_INTERNAL_COMPUTE_SHA1_FOR_MEMORY_REGION:{
            int address=i1;
            int numberOfBytes=i2;
            unsigned char* buffer_to_write_sha_hash_into = o1;
            sha_for_memory_region(buffer_to_write_sha_hash_into,address,numberOfBytes);
            break;
        }
#endif
        default: {
            ioExecute();
        }
    }
}

/**
 * Post an event to the channelIO subsystem to wake up any waiters.
 */
static void cioPostEvent(void) {
#if KERNEL_SQUAWK
    ioPostEvent();
#endif
}
