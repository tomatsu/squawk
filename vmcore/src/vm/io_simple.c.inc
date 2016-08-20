/*
 * Copyright 2004-2010 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright 2011-2012 Oracle. All Rights Reserved.
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
 * Please contact Oracle, 16 Network Circle, Menlo Park, CA 94025 or
 * visit www.oracle.com if you need additional information or have
 * any questions.
 */

/*
 * If there are outstanding irqRequests and one of them is for an interrupt that has
 * occurred return its eventNumber. If removeEventFlag is true, then
 * also remove the event from the queue. If no requests match the interrupt status
 * return 0.
 */

#define MAX_MICRO_SLEEP 999999

/**
 * Sleep Squawk for specified milliseconds
 */
void osMilliSleep(long long millis) {
//    printf("#### osMilliSleep %lld\n", millis);
    if (millis <= 0) {
        return;
    }
    if (millis > 0x7fffffffL) {
	millis = 0x7fffffffL;
    }
//    wait_ms((int)millis);
}

long long rebuildLongParam(int i1, int i2) {
    return ((long long)i1 << 32) | ((long long)i2 & 0xFFFFFFFF);
}


/**
 * Executes an operation on a given channel for an isolate.
 *
 * @param  context the I/O context
 * @param  op      the operation to perform
 * @param  channel the identifier of the channel to execute the operation on
 * @param  i1
 * @param  i2
 * @param  i3
 * @param  i4
 * @param  i5
 * @param  i6
 * @param  send
 * @param  receive
 * @return the operation result
 */
static void ioExecute(void) {
//  int     context = com_sun_squawk_ServiceOperation_context;
    int     op      = com_sun_squawk_ServiceOperation_op;
//  int     channel = com_sun_squawk_ServiceOperation_channel;
    int     i1      = com_sun_squawk_ServiceOperation_i1;
    int     i2      = com_sun_squawk_ServiceOperation_i2;
    int     i3      = com_sun_squawk_ServiceOperation_i3;
//  int     i4      = com_sun_squawk_ServiceOperation_i4;
//  int     i5      = com_sun_squawk_ServiceOperation_i5;
//  int     i6      = com_sun_squawk_ServiceOperation_i6;
    Address send    = com_sun_squawk_ServiceOperation_o1;
    Address receive = com_sun_squawk_ServiceOperation_o2;

    int res = ChannelConstants_RESULT_OK;

    switch (op) {
        case ChannelConstants_GLOBAL_GETEVENT: {
            break;
        }

        case ChannelConstants_GLOBAL_WAITFOREVENT: {
            long long millisecondsToWait = rebuildLongParam(i1, i2);
            osMilliSleep(millisecondsToWait);
            break;
        }

        default: {
//            ioExecuteSys(); // do platform-specific
//            res = com_sun_squawk_ServiceOperation_result; // result set by ioExecuteSys.
//	    printf("###### skip op: %d\n", op);
            break;
        }
    }
    com_sun_squawk_ServiceOperation_result = res;
}

