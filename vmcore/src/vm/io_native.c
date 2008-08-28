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


/*************** NOTE: this file is included when PLATFORM_TYPE_NATIVE=true **************************/

#include <netdb.h>
#include <dlfcn.h>

/****** HARD CODED FOR MAC FOR NOW:  *************/
#define MAX_MICRO_SLEEP 999999

void osMilliSleep(long long millis) {
    if (millis <= 0) {
        return;
    }
    long long elapsed = sysTimeMillis();
    long long seconds = millis / 1000;
    if (seconds > 0) {
        // too long for usleep, so get close
        sleep(seconds);
    } 
    elapsed = sysTimeMillis() - elapsed;
    if (elapsed < millis) {
        millis = millis - elapsed;
        long long micro = millis * 1000;
        if (micro > MAX_MICRO_SLEEP) {
            micro = MAX_MICRO_SLEEP;
        }
        usleep(micro);
    }
}


int sysFD_SET(int i1, fd_set* set) {
    FD_SET(i1, set);
}

int sysFD_CLR(int i1, fd_set* set) {
    FD_CLR(i1, set);
}

int sysFD_ISSET(int i1, fd_set* set) {
    return FD_ISSET(i1, set);
}

int sysFD_SIZE; __attribute__((used))
int sysSIZEOFSTAT;  __attribute__((used))

/*---------------------------- Event Queue ----------------------------*/

/*
 * @TODO: This is a polling mechanism. GetEvent has to search all event requests looking for events that occurred.
 *        May want to queue up events that have occurred. In any case, pay attention to linear searches, especially at 
 *        Event signalling time.
 */
struct eventRequest {
        int eventNumber;
        //int eventData;
        int eventStatus; // true if event occurred
        struct eventRequest *next;
};
typedef struct eventRequest EventRequest;

EventRequest *eventRequests;

int nextEventNumber = 1;

/*
 * Java has requested wait for an event. Store the request,
 * and each time Java asks for events, signal the event if it has happened
 *
 * @return the event number
 */
int storeEventRequest (int eventData) {
    EventRequest* newRequest = (EventRequest*)malloc(sizeof(EventRequest));
    if (newRequest == NULL) {
        //TODO set up error message for GET_ERROR and handle
        //one per channel and clean on new requests.
        return ChannelConstants_RESULT_EXCEPTION;
    }

    newRequest->eventNumber = nextEventNumber++;
    //newRequest->eventData = eventData;
    newRequest->eventStatus = false;
    newRequest->next = NULL;

    if (newRequest->eventNumber <= 0) {
        // @TODO  Statically assign event number to thread. Thread can only wait for one event at a time.
        //        Could lazily incr counter at first IO operation by thread, or search for unused ID (keeping IDs compact).
        fatalVMError("Reached event number limit");
    }

    if (eventRequests == NULL) {
        eventRequests = newRequest;
    } else {
        EventRequest* current = eventRequests;
        while (current->next != NULL) {
            current = current->next;
        }
        current->next = newRequest;
    }
    return newRequest->eventNumber;
}

/*
 * If there are outstanding events then return its eventNumber. Otherwise return 0
 */
int checkForEvents() {
    return getEvent(false);
}

static void printOutstandingEvents() {
    EventRequest* current = eventRequests;
    while (current != NULL) {
    	diagnosticWithValue("event request id", current->eventNumber);
    	current = current->next;
    }
}

/*
 * If there are outstanding event then return its eventNumber. If removeEventFlag is true, then
 * also remove the event from the queue. If no requests match the interrupt status
 * return 0.
 *
 * Does linear search from older requests to newer. 
 */
int getEvent(int removeEventFlag) {
    int res = 0;
    
    if (eventRequests != NULL) {
    	EventRequest* current = eventRequests;
        EventRequest* previous = NULL;
        while (current != NULL) {
            if (current->eventStatus) {
                res = current->eventNumber;
                //unchain
                if (removeEventFlag) {
                    if (previous == NULL) {
                        eventRequests = current->next;
                    } else {
                        previous->next = current->next;
                    }
                    free(current);
                }
                break;
            } else {
                previous = current;
                current = current->next;
            }
        }
    }

    return res;
}

/*---------------------------- IO Impl ----------------------------*/

/**
 * Initializes the IO subsystem.
 *
 * @param  classPath   the class path with which to start the embedded JVM
 * @param  args        extra arguments to pass to the embedded JVM
 * @param  argc        the number of extra arguments in 'args'
 */
void CIO_initialize(char *classPath, char** args, int argc) {
    sysFD_SIZE = sizeof(fd_set);
    sysSIZEOFSTAT = sizeof(struct stat);
}

/******* per-context data ************/

/* From ChannelIO.java:
    private int context;
    private boolean rundown;
    private SerializableIntHashtable channels = new SerializableIntHashtable();
    private int nextAvailableChannelID = ChannelConstants.CHANNEL_LAST_FIXED + 1;
    private String exceptionClassName;
    private long theResult;
*/

char* exceptionClassName;

long long retValue = 0;  // holds the value to be returned on the next "get result" call

/**
 * Registers an exception that occurred on a non-channel specific call to this IO system.
 *
 * @param exceptionClassName   the name of the class of the exception that was raised
 * @return the negative value returned to the Squawk code indicating both that an error occurred
 *         as well as the length of the exception class name
 */
int raiseException(char* theExceptionClassName) {
    if (*theExceptionClassName == 0) {
        theExceptionClassName = "?raiseException?";
    }
    exceptionClassName = theExceptionClassName;
    return ChannelConstants_RESULT_EXCEPTION;
}

/**
 * Get the next character of the error.
 *
 * @return the next character or 0 if none remain
 */
int getError() {
    if (exceptionClassName != NULL) {
        int ch = *exceptionClassName;
        if (ch != '\0') {
            exceptionClassName++;
        } else {
            exceptionClassName = null;
        }
        return ch;
    }
    return 0;
}


/* INTERNAL DYNAI SYMBOL SUPPORT */
typedef struct dlentryStruct {
    char* name;
    void* entry;
} dlentry;

#define DL_TABLE_SIZE 5

static dlentry dltable[DL_TABLE_SIZE] = {
    {"sysFD_SIZE",      &sysFD_SIZE},
    {"sysSIZEOFSTAT",   &sysSIZEOFSTAT},
    {"sysFD_CLR",       &sysFD_CLR},
    {"sysFD_SET",       &sysFD_SET},
    {"sysFD_ISSET",     &sysFD_ISSET}
};
    

void* sysdlsym(void* handle, char* name) {
    int i;
    for (i = 0; i < DL_TABLE_SIZE; i++) {
        if (strcmp(name, dltable[i].name) == 0) {
            return dltable[i].entry;
        }
    }
    
    return dlsym(handle, name);
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
    int     context = com_sun_squawk_ServiceOperation_context;
    int     op      = com_sun_squawk_ServiceOperation_op;
    int     channel = com_sun_squawk_ServiceOperation_channel;
    int     i1      = com_sun_squawk_ServiceOperation_i1;
    int     i2      = com_sun_squawk_ServiceOperation_i2;
    int     i3      = com_sun_squawk_ServiceOperation_i3;
    int     i4      = com_sun_squawk_ServiceOperation_i4;
   int     i5      = com_sun_squawk_ServiceOperation_i5;
/*     int     i6      = com_sun_squawk_ServiceOperation_i6;
    Address send    = com_sun_squawk_ServiceOperation_o1;
    Address receive = com_sun_squawk_ServiceOperation_o2;
*/

    int res = ChannelConstants_RESULT_OK;

    switch (op) {

        /*--------------------------- GLOABL OPS ---------------------------*/
    	case ChannelConstants_GLOBAL_CREATECONTEXT:  {
            res = 1; //let all Isolates share a context for now
            break;
        }

    	case ChannelConstants_GLOBAL_GETEVENT: {
            // since this function gets called frequently it's a good place to put
            // the call that periodically resyncs our clock with the power controller
            res = getEvent(true);
            // improve fairness of thread scheduling - see bugzilla #568
// @TODO: Check that bare-matel version is OK: It unconditionally resets the bc.
//        This can give current thread more time, if there was no event.
//        better idea is to give new thread new quanta in threadswitch?
            if (res) {
                bc = -TIMEQUANTA;
            }
            break;
        }

    	case ChannelConstants_GLOBAL_WAITFOREVENT: {
            long long millisecondsToWait = makeLong(i1, i2);
            /*long long target = ((long long)sysTimeMillis()) + millisecondsToWait;
              if (target <= 0) target = 0x7FFFFFFFFFFFFFFFLL; // overflow detected*/
            osMilliSleep(millisecondsToWait);
            res = 0;
            break;
    	}

    	// case ChannelConstants_GLOBAL_POSTEVENT: // handled by external process, not Squawk

        /*--------------------------- CONTEXT OPS ---------------------------*/
    	case ChannelConstants_CONTEXT_DELETE: {
            // TODO delete all the outstanding events on the context
            // But will have to wait until we have separate contexts for each isolate
            res=0;
            break;
        }

    	case ChannelConstants_CONTEXT_GETERROR: {
            res = getError();
            break;
        }
    		
        case ChannelConstants_CONTEXT_HIBERNATE: {
            // TODO this is faked, we have no implementation currently.
            res = ChannelConstants_RESULT_OK;
            break;
        }

        case ChannelConstants_CONTEXT_GETHIBERNATIONDATA: {
            // TODO this is faked, we have no implementation currently.
            res = ChannelConstants_RESULT_OK;
            break;
        }

        case ChannelConstants_CONTEXT_GETCHANNEL: {
            res = ChannelConstants_RESULT_BADPARAMETER;
            break;
        }

        case ChannelConstants_CONTEXT_FREECHANNEL: {
            res = ChannelConstants_RESULT_BADPARAMETER;
            break;
        }

    	case ChannelConstants_CONTEXT_GETRESULT: {
            res = (int)retValue;
            break;
        }

    	case ChannelConstants_CONTEXT_GETRESULT_2: {
            res = (int)(retValue >> 32);
            retValue = 0;
            break;
        }

        /*--------------------------- POSIX NATIVE OPS ---------------------------*/

/* WARNING! NONE OF THIS IS 64-bit safe! */

        case ChannelConstants_DLSYM: {
            void* handle = RTLD_DEFAULT;
            if (i1 != 0) {
                handle = (void*) i1;
            }
            res = (int)sysdlsym(handle, (char*)i2);
            break;
        }


        /*--------------------------- CHANNEL OPS ---------------------------*/




        default: {
printf("Channel IO operand %d not yet implemented\n", op);
            res = ChannelConstants_RESULT_BADPARAMETER;
        }
    }
    com_sun_squawk_ServiceOperation_result = res;
}

#if KERNEL_SQUAWK
/**
 * Posts an event via ChannelIO to wake up any waiters.
 */
static void ioPostEvent(void) {
    /*
     * Check if there is no embedded JVM.
     */
    void os_postEvent(boolean notify);
    os_postEvent(true);
}
#endif
