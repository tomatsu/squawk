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

#include "system.h"
#include "AT91RM9200.h"
#include "systemtimer.h"
#include <syscalls-9200-io.h>
#include "syscalls-impl.h"
#include "spi.h"
#include "avr.h"

// Define the maximum number of user-supplied command line args we can accept
#define SQUAWK_STARTUP_ARGS_MAX 20

// Override setting from platform.h
#undef PLATFORM_UNALIGNED_LOADS
#define PLATFORM_UNALIGNED_LOADS false

#define SERVICE_CHUNK_SIZE (8*1024)
#define IODOTC "9200-io.c"

#include <stdlib.h>
#include <sys/time.h>
#include "jni.h"

#define printf iprintf
#define fprintf fiprintf
#define sprintf siprintf

int main(int argc, char *argv[]);
extern int disableARMInterrupts();
extern void enableARMInterrupts();
void initTrapHandlers();
void stopVM(int);

int dma_buffer_size;
char* dma_buffer_address;

volatile jlong sysTimeMillis(void) {
    jlong res = getMilliseconds();
    return res;
}

jlong sysTimeMicros() {
    return sysTimeMillis() * 1000;
}

void startTicker(int interval) {
    fprintf(stderr, "Profiling not implemented");
    exit(0);
}

extern void setup_java_interrupts();
extern void usb_state_change();

/* low-level setup task required after cold and warm restarts */
void lowLevelSetup() {
//	diagnostic("in low level setup");
	mmu_enable();
	data_cache_enable();
	spi_init();
	register_usb_state_callback(usb_state_change);
	setup_java_interrupts();
//	diagnostic("java interrupts setup");
    synchroniseWithAVRClock();
    init_system_timer();
    diagnosticWithValue("Current time is ", getMilliseconds());
//	diagnostic("system timer inited");
}

/**
 * Program entrypoint (cold start).
 */
void arm_main(int cmdLineParamsAddr, unsigned int outstandingAvrStatus) {
	int i;

	diagnostic("in vm");

	page_table_init();
	if (!reprogram_mmu(TRUE)) {
		error("VM not launching because the FAT appears to be invalid", 0);
		asm(SWI_ATTENTION_CALL);
	}

	lowLevelSetup();
//	diagnostic("low level setup complete");
    
    avrSetOutstandingStatus(outstandingAvrStatus);

    iprintf("\n");
    iprintf("Squawk VM Starting (");
	iprintf(BUILD_DATE);
	iprintf(")...\n");
	
	char* startupArgs = (char*)cmdLineParamsAddr;
	char *fakeArgv[SQUAWK_STARTUP_ARGS_MAX + 1];
	fakeArgv[0] = "dummy"; // fake out the executable name

	int fakeArgc = 1;
	int index = 0;
	/* The startupArgs structure comprises a sequence of null-terminated string
	 * with another null to indicate the end of the structure
	 */
	while (startupArgs[index] != 0) {
		if (startsWith(&startupArgs[index], "-dma:")) {
			dma_buffer_size = parseQuantity(&startupArgs[index+5], "-dma:");
			dma_buffer_address = malloc(dma_buffer_size);
			if (dma_buffer_address == NULL) {
				fprintf(stderr, "Unable to allocate %i bytes for DMA\n", dma_buffer_size);
            	stopVM(-1);
			}
			dma_buffer_address = (char*)(((int)dma_buffer_address & 0x000FFFFF) + UNCACHED_RAM_START_ADDDRESS);
//			iprintf("Allocated %i bytes for DMA buffers at address 0x%x\n", dma_buffer_size, (int)dma_buffer_address);
		} else {
			fakeArgv[fakeArgc] = &startupArgs[index];
			// iprintf("Parsed arg: %s\n", fakeArgv[fakeArgc]);
			fakeArgc++;
			if (fakeArgc > SQUAWK_STARTUP_ARGS_MAX + 1) {
				iprintf("Number of startup args exceeds maximum permitted\n");
				exit(-1);
			}
		}
		while (startupArgs[index] != 0) {
			index++;
		}
		// skip over the terminating null
		index++;
	}
	
    main(fakeArgc, fakeArgv);
    error("main function returned, restarting", 0);
    disableARMInterrupts();
    hardwareReset();
}

/**
 * Support for util.h
 */

long sysconf(int code) {
    if (code == _SC_PAGESIZE) {
        return 4;
    } else {
        return -1; // failure
    }
}

INLINE void osloop() {
	//no-op on spot platform
}

INLINE void osbackbranch() {
}

void osfinish() {
	disable_system_timer();
	diagnostic("OSFINISH");
    asm(SWI_ATTENTION_CALL);
}

