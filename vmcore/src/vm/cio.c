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

//#define UNICODE true

/**
 * Converts two 32 bit ints into a Java long.
 *
 * @param high the high word
 * @param low the loe word
 * @return the resulting Java long
 */
INLINE jlong makeLong(int high, int low) {
    return (((jlong)high) << 32) | (((jlong)low) & 0x00000000FFFFFFFFL);
}

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

#include IODOTC

#ifdef OLD_IIC_MESSAGES
/*
 * Include the message processing code.
 */
#include "msg.c"
#endif

/**
 * Execute a channel operation.
 */
void cioExecute(void) {
    int     context = com_sun_squawk_ServiceOperation_context;
    int     op      = com_sun_squawk_ServiceOperation_op;
    int     channel = com_sun_squawk_ServiceOperation_channel;
    int     i1      = com_sun_squawk_ServiceOperation_i1;
    int     i2      = com_sun_squawk_ServiceOperation_i2;
    int     i3      = com_sun_squawk_ServiceOperation_i3;
    int     i4      = com_sun_squawk_ServiceOperation_i4;
    int     i5      = com_sun_squawk_ServiceOperation_i5;
    int     i6      = com_sun_squawk_ServiceOperation_i6;
    Address o1      = com_sun_squawk_ServiceOperation_o1;
    Address o2      = com_sun_squawk_ServiceOperation_o2;
    FILE   *vmOut   = streams[currentStream];

    switch (op) {

        case ChannelConstants_INTERNAL_SETSTREAM: {
            com_sun_squawk_ServiceOperation_result = setStream(i1);
            assume(streams[currentStream] != null);
            break;
        }

        case ChannelConstants_INTERNAL_PRINTSTRING: {
            int i;
            printJavaString(o1, vmOut, null, 0);
            break;
        }

        case ChannelConstants_INTERNAL_PRINTCHAR: {
            fprintf(vmOut, "%c", i1);
            fflush(vmOut);
            break;
        }

        case ChannelConstants_INTERNAL_PRINTINT: {
            fprintf(vmOut, "%d", i1);
            fflush(vmOut);
            break;
        }

/*if[FLOATS]*/
        case ChannelConstants_INTERNAL_PRINTDOUBLE: {
            fprintf(vmOut, format("%D"), makeLong(i1, i2));
            fflush(vmOut);
            break;
        }

        case ChannelConstants_INTERNAL_PRINTFLOAT: {
            fprintf(vmOut, "%f", i1);
            fflush(vmOut);
            break;
        }
/*else[FLOATS]*/
//      case ChannelConstants_INTERNAL_PRINTDOUBLE:
//      case ChannelConstants_INTERNAL_PRINTFLOAT: {
//          fatalVMError("floats not supported");
//          break;
//      }
/*if[FLOATS]*/

        case ChannelConstants_INTERNAL_PRINTUWORD: {
            jlong val = makeLong(i1, i2);
            fprintf(vmOut, format("%A"), (UWord)val);
            fflush(vmOut);
            break;
        }

        case ChannelConstants_INTERNAL_PRINTOFFSET: {
            jlong val = makeLong(i1, i2);
            fprintf(vmOut, format("%O"), val);
            fflush(vmOut);
            break;
        }

        case ChannelConstants_INTERNAL_PRINTLONG: {
            jlong val = makeLong(i1, i2);
            fprintf(vmOut, format("%L"), val);
            fflush(vmOut);
            break;
        }

        case ChannelConstants_INTERNAL_PRINTADDRESS: {
            Address val = o1;
            fprintf(vmOut, format("%A"), val);
            if (hieq(val, com_sun_squawk_VM_romStart) && lo(val, com_sun_squawk_VM_romEnd)) {
                fprintf(vmOut, format(" (image @ %O)"), Address_sub(val, com_sun_squawk_VM_romStart));
            }
            fflush(vmOut);
            break;
        }

        case ChannelConstants_INTERNAL_PRINTCONFIGURATION: {
            fprintf(stderr, "native VM build flags: %s\n", BUILD_FLAGS);
            break;
        }

        case ChannelConstants_INTERNAL_PRINTGLOBALS: {
            printGlobals();
            fflush(vmOut);
            break;
        }

        case ChannelConstants_INTERNAL_PRINTGLOBALOOPNAME: {
#if TRACE
            fprintf(vmOut, "%s", getGlobalOopName(i1));
#else
            fprintf(vmOut, "Global oop:%d", i1);
#endif
            fflush(vmOut);
            break;
        }

        case ChannelConstants_INTERNAL_GETPATHSEPARATORCHAR: {
            com_sun_squawk_ServiceOperation_result = pathSeparatorChar;
            break;
        }

        case ChannelConstants_INTERNAL_GETFILESEPARATORCHAR:  {
            com_sun_squawk_ServiceOperation_result = fileSeparatorChar;
            break;
        }

/*      Moved to native method:
        case ChannelConstants_INTERNAL_COPYBYTES:  {
            copyBytes(o1, i2, o2, i3, i1, i4 != 0);
            break;
        }
*/

        case ChannelConstants_INTERNAL_GETTIMEMICROS_HIGH: {
            returnLongResult(sysTimeMicros());
            break;
        }

        case ChannelConstants_INTERNAL_GETTIMEMILLIS_HIGH: {
            returnLongResult(sysTimeMillis());
            break;
        }

        case ChannelConstants_INTERNAL_LOW_RESULT: {
            com_sun_squawk_ServiceOperation_result = internalLowResult;
            break;
        }

        case ChannelConstants_INTERNAL_STOPVM: {
            stopVM(i1);
            break;
        }

        case ChannelConstants_INTERNAL_MATH: {
            fatalVMError("Unimplemented internal channel I/O operation");
        }

#ifdef OLD_IIC_MESSAGES
        case ChannelConstants_INTERNAL_ALLOCATE_MESSAGE_BUFFER: {
            deferInterruptsAndDo(
                allocateMessageBuffer();
            );
//printf("ALLOCATE_MESSAGE_BUFFER result = %d\n", com_sun_squawk_ServiceOperation_addressResult);
            break;
        }

        case ChannelConstants_INTERNAL_FREE_MESSAGE_BUFFER: {
            deferInterruptsAndDo(
                freeMessageBuffer(o2);
            );
//printf("FREE_MESSAGE_BUFFER %d\n", o2);
            break;
        }

        case ChannelConstants_INTERNAL_SEND_MESSAGE_TO_SERVER: {
            sendMessage(o1, o2, i1, &toServerMessages, &toServerWaiters);
//printf("SEND_MESSAGE_TO_SERVER key = %d addr = %d result = %d\n", o1, o2, com_sun_squawk_ServiceOperation_addressResult);
//dumpOutMessageQueues();
#if KERNEL_SQUAWK
            /*
             * We could use a special _TO_KERNEL type for messages
             * to the driver's request url but this is simpler.
             */
            if (kernelMode && !inKernelMode()) {
                void Squawk_enterKernel();
                Squawk_enterKernel();
            }
#endif
            break;
        }

        case ChannelConstants_INTERNAL_RECEIVE_MESSAGE_FROM_CLIENT: {
            receiveMessage(o1, &toServerMessages, &toServerWaiters);
//printf("RECEIVE_MESSAGE_FROM_CLIENT result = %d\n", com_sun_squawk_ServiceOperation_addressResult);
//dumpOutMessageQueues();
            break;
        }

        case ChannelConstants_INTERNAL_SEND_MESSAGE_TO_CLIENT: {
            sendMessage(o1, o2, i1, &toClientMessages, &toClientWaiters);
//printf("SEND_MESSAGE_TO_CLIENT key = %d addr = %d result = %d\n", o1, o2, com_sun_squawk_ServiceOperation_addressResult);
//dumpOutMessageQueues();
            break;
        }

        case ChannelConstants_INTERNAL_RECEIVE_MESSAGE_FROM_SERVER: {
            receiveMessage(o1, &toClientMessages, &toClientWaiters);
//printf("RECEIVE_MESSAGE_FROM_SERVER result = %d\n", com_sun_squawk_ServiceOperation_addressResult);
//dumpOutMessageQueues();
            break;
        }

        case ChannelConstants_INTERNAL_SEARCH_SERVER_HANDLERS: {
            searchServerHandlers(o2);
            break;
        }

        case ChannelConstants_GLOBAL_WAITFOREVENT: {
            if (!checkForMessageEvent()) {
                ioExecute();
            }
            break;
        }

        case ChannelConstants_GLOBAL_GETEVENT: {
            if (!getMessageEvent()) {
                ioExecute();
            }
            break;
        }

#else

	 /* case ChannelConstants_GLOBAL_WAITFOREVENT:
        case ChannelConstants_GLOBAL_GETEVENT:
		 ... handled by default case below
	 */

#endif /* OLD_IIC_MESSAGES */


#if NATIVE_VERIFICATION
        case ChannelConstants_COMPUTE_SHA1_FOR_MEMORY_REGION:{
			int address=i1;
			int numberOfBytes=i2;
			unsigned char* buffer_to_write_sha_hash_into = o1;
			//printf("In cio.c.ioexecute\r\n");
			//printf("ChannelConstants_COMPUTE_SHA_FOR_MEMORY_REGION:\n address: %x\nbuffer_to-write_sha_hash_into %x\nnumberofbyte: %d\r\n",address,buffer_to_write_sha_hash_into,numberOfBytes);
			sha_for_memory_region(buffer_to_write_sha_hash_into,address,numberOfBytes);
		}
			break;
#else
    case ChannelConstants_COMPUTE_SHA1_FOR_MEMORY_REGION:{
			fprintf(vmOut,"Internal Error: vmcore/cio.c called with op COMPUTE_SHA1_FOR_MEMORY_REGION, but was compiled with NATIVE_VERIFICATION=false.");
            fflush(vmOut);
		}
			break;
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
