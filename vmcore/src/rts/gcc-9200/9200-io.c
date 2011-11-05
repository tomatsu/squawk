/*
 * Copyright 2004-2010 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright 2011 Oracle. All Rights Reserved.
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

#include "spi.h"
#include "i2c.h"
#include "flash.h"
#include "avr.h"
#include "system.h"
#include "9200-io.h"

// Forward declarations
int getEvent(int, int);
static int check_irq(int irq_mask, int clear_flag);
INLINE boolean checkForMessageEvent();

// General helper method
long long rebuildLongParam(int i1, int i2) {
	return ((long long)i1 << 32) | ((long long)i2 & 0xFFFFFFFF);
}

extern int dma_buffer_size;
extern char* dma_buffer_address;
volatile long long last_device_interrupt_time;
extern volatile long long clock_counter;

/**************************************************************************
 * Sleep support
 **************************************************************************/
int deepSleepEnabled = 0; // indicates whether the feature is currently enabled (=1)
int sleepManagerRunning = 1;	   // assume that sleepManager is running until it calls WAIT_FOR_DEEP_SLEEP
int outstandingDeepSleepEvent = 0; // whether the sleep manager thread should be unblocked at the next reschedule
long long storedDeepSleepWakeupTarget; // The millis that the next deep sleep should end at
long long minimumDeepSleepMillis = 0x7FFFFFFFFFFFFFFFLL;
 		// minimum time we're prepared to deep sleep for: avoid deep sleeping initially.
long long totalShallowSleepTime; // total time the SPOT has been shallow sleeping

#define SHALLOW_SLEEP_CLOCK_SWITCH_THRESHOLD 20
static const int peripheral_bus_speed[] = {PERIPHERAL_BUS_SPEEDS};
int shallow_sleep_clock_mode = SHALLOW_SLEEP_CLOCK_MODE_NORMAL;


/*
 * Enter deep sleep
 */
static void doDeepSleep(long long targetMillis, int remain_powered) {
	long long millisecondsToWait = targetMillis - getMilliseconds();
	if (remain_powered) {
		avrSetAlarmAndWait(millisecondsToWait);
		synchroniseWithAVRClock();
	} else {
    	unsigned int statusReturnedFromDeepSleep = deepSleep(millisecondsToWait);
	    lowLevelSetup(); //need to repeat low-level setup after a restart
    	avrSetOutstandingEvents(statusReturnedFromDeepSleep);
	}
}

/*
 * Enter shallow sleep
 */
static void doShallowSleep(long long targetMillis) {
	long long start_time;
	long long last_time;
	int cpsr;
	int main_clock_sleep = FALSE;
	start_time = getMilliseconds();
	last_time = start_time;
	if ((shallow_sleep_clock_mode != SHALLOW_SLEEP_CLOCK_MODE_NORMAL) && (targetMillis - start_time > SHALLOW_SLEEP_CLOCK_SWITCH_THRESHOLD)) {
		main_clock_sleep = TRUE;
		setupClocks(peripheral_bus_speed[shallow_sleep_clock_mode]);
		cpsr = disableARMInterrupts();
		switch (shallow_sleep_clock_mode) {
			case SHALLOW_SLEEP_CLOCK_MODE_45_MHZ:
				select_45_clock();
				break;
			case SHALLOW_SLEEP_CLOCK_MODE_18_MHZ:
				select_18_clock();
				break;
			case SHALLOW_SLEEP_CLOCK_MODE_9_MHZ:
				select_9_clock();
				break;
			default:
				error("Ignoring invalid clock mode", shallow_sleep_clock_mode);
				break;
		}
		setARMInterruptBits(cpsr);
	}
	while (1) {
		if (checkForEvents()) break;
#ifdef OLD_IIC_MESSAGES
		if (checkForMessageEvent()) break;
#endif
		last_time = getMilliseconds();
		if (last_time > targetMillis) break;
		stopProcessor();
	}
	if (main_clock_sleep) {
		cpsr = disableARMInterrupts();
		switch (shallow_sleep_clock_mode) {
			case SHALLOW_SLEEP_CLOCK_MODE_45_MHZ:
				select_normal_clock_from_plla();
				break;
			case SHALLOW_SLEEP_CLOCK_MODE_18_MHZ:
			case SHALLOW_SLEEP_CLOCK_MODE_9_MHZ:
				select_normal_clock_from_main();
				break;
		}
		setARMInterruptBits(cpsr);
		setupClocks(MASTER_CLOCK_FREQ);
	}
	totalShallowSleepTime += (last_time - start_time);
}

static void setDeepSleepEventOutstanding(long long target) {
	storedDeepSleepWakeupTarget = target;
	outstandingDeepSleepEvent = 1;
}

/**
 * Sleep Squawk for specified milliseconds
 */
void osMilliSleep(long long millisecondsToWait) {
    long long target = ((long long) getMilliseconds()) + millisecondsToWait;
    if (target <= 0) {
        target = 0x7FFFFFFFFFFFFFFFLL; // overflow detected
    }
//  diagnosticWithValue("GLOBAL_WAITFOREVENT - deepSleepEnabled", deepSleepEnabled);
//	diagnosticWithValue("GLOBAL_WAITFOREVENT - sleepManagerRunning", sleepManagerRunning);
//	diagnosticWithValue("GLOBAL_WAITFOREVENT - minimumDeepSleepMillis", minimumDeepSleepMillis);
    if (deepSleepEnabled && !sleepManagerRunning && (millisecondsToWait >= minimumDeepSleepMillis)) {
//	    diagnosticWithValue("GLOBAL_WAITFOREVENT - deep sleeping for", (int)millisecondsToWait);
        setDeepSleepEventOutstanding(target);
    } else {
//	    diagnosticWithValue("GLOBAL_WAITFOREVENT - shallow sleeping for", (int)millisecondsToWait);
        doShallowSleep(target);
    }
}

/******************************************************************
 * Serial port support
 ******************************************************************/
#define WAIT_FOR_DEEP_SLEEP_EVENT_NUMBER (DEVICE_LAST+1)
#define FIRST_IRQ_EVENT_NUMBER (WAIT_FOR_DEEP_SLEEP_EVENT_NUMBER+1)
int serialPortInUse[] = {0,0,0,0,0,0};

/* Java has requested serial chars */
int getSerialPortEvent(int device_type) {
	serialPortInUse[device_type-DEVICE_FIRST] = 1;
	return device_type;
}

int isSerialPortInUse(int device_type) {
	return serialPortInUse[device_type-DEVICE_FIRST];
}

void freeSerialPort(int device_type) {
	serialPortInUse[device_type-DEVICE_FIRST] = 0;
}

/*
 * ****************************************************************
 * Interrupt Handling Support
 *
 * See comment in AT91_AIC.java for details
 * ****************************************************************
 */

unsigned int java_irq_status = 0; // bit set = that irq has outstanding interrupt request

void usb_state_change()	{
	int cpsr = disableARMInterrupts();
#if AT91SAM9G20
	java_irq_status |= (1<<10); // USB Device ID
#else
	java_irq_status |= (1<<11); // USB Device ID
#endif
    setARMInterruptBits(cpsr);
}

IrqRequest *irqRequests;

extern void java_irq_hndl();

void setup_java_interrupts() {
	// This routine is called from os.c
	// NB interrupt handler coded in java-irq-hndl.s
	unsigned int id;
#if AT91SAM9G20
    diagnosticWithValue("initial val of irqRequests", (int)irqRequests);
#endif
	for (id = 0; id <= 31; id++) {
		if (!((1 << id) & RESERVED_PERIPHERALS)) {
			at91_irq_setup (id, &java_irq_hndl);
		}
	}
}

/*
 * Java has requested wait for an interrupt. Store the request,
 * and each time Java asks for events, signal the event if the interrupt has happened
 *
 * @return the event number
 */
int storeIrqRequest (int irq_mask) {
        IrqRequest* newRequest = (IrqRequest*)malloc(sizeof(IrqRequest));
        if (newRequest == NULL) {
        	//TODO set up error message for GET_ERROR and handle
        	//one per channel and clean on new requests.
        	return ChannelConstants_RESULT_EXCEPTION;
        }

        newRequest->next = NULL;
        newRequest->irq_mask = irq_mask;
#if AT91SAM9G20
    diagnosticWithValue("storeIrqRequest  - irqRequests", (int)irqRequests);
    diagnosticWithValue("storeIrqRequest  - newRequest", (int)newRequest);
#endif
        if (irqRequests == NULL) {
        	irqRequests = newRequest;
        	newRequest->eventNumber = FIRST_IRQ_EVENT_NUMBER;
        } else {
        	IrqRequest* current = irqRequests;
        	while (current->next != NULL) {
        		current = current->next;
        	}
        	current->next = newRequest;
        	newRequest->eventNumber = current->eventNumber + 1;
        }
        return newRequest->eventNumber;
}

/* ioPostEvent is a no-op for us */
static void ioPostEvent(void) { }

/*
 * If there are outstanding irqRequests and one of them is for an interrupt that has
 * occurred return its eventNumber. Otherwise return 0
 */
int checkForEvents() {
        return getEvent(0, false);
}

static void printOutstandingEvents() {
	IrqRequest* current = irqRequests;
	while (current != NULL) {
    	diagnosticWithValue("event request", current->irq_mask);
    	current = current->next;
    }
}

/*
 * If there are outstanding irqRequests and one of them is for an interrupt that has
 * occurred return its eventNumber. If removeEventFlag is true, then
 * also remove the event from the queue. If no requests match the interrupt status
 * return 0.
 */
int getEvent(int removeEventFlag, int fiqOnly) {
    int res = 0;
    int device_type;
    
    if (irqRequests != NULL) {
    	IrqRequest* current = irqRequests;
        IrqRequest* previous = NULL;
        while (current != NULL) {
        	if ((!fiqOnly || (current->irq_mask & (1 << AT91C_ID_FIQ)) != 0) && check_irq(current->irq_mask, removeEventFlag)) {
        		res = current->eventNumber;
        		//unchain
        		if (removeEventFlag) {
        			if (previous == NULL) {
        				irqRequests = current->next;
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
    if (res == 0 && !fiqOnly) {
    	// check for serial chars available
    	for (device_type = DEVICE_FIRST; device_type<=DEVICE_LAST; device_type++) {
	    	if (isSerialPortInUse(device_type) && sysAvailable(device_type)) {
	    		res = device_type;
	    		if (removeEventFlag) {
	    			freeSerialPort(device_type);
	    		}
		    	break;
	    	}
    	}
    }

   	if (res == 0) {
    	if (outstandingDeepSleepEvent) {
    		sleepManagerRunning = 1;
    		res = WAIT_FOR_DEEP_SLEEP_EVENT_NUMBER;
    	}
   	}
	if (removeEventFlag) {
		// always clear the deep sleep event, as we will want to reconsider
		// whether deep sleep is appropriate after any event.
		outstandingDeepSleepEvent = 0;
	}
    return res;
}

/**
 * Check if an irq bit is set in the status, return 1 if yes
 * Also, clear bit if it is set and clear_flag = 1
 */
static int check_irq(int irq_mask, int clear_flag) {
        int result;
        disableARMInterrupts();
        if ((java_irq_status & irq_mask) != 0) {
        	if (clear_flag) {
            	java_irq_status = java_irq_status & ~irq_mask;
        	}
            result = 1;
        } else {
        	result = 0;
        }
        enableARMInterrupts();
        return result;
}




int retValue = 0;  // holds the value to be returned on the next "get result" call
int avr_low_result = 0;

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
    int     i6      = com_sun_squawk_ServiceOperation_i6;
    Address send    = com_sun_squawk_ServiceOperation_o1;
    Address receive = com_sun_squawk_ServiceOperation_o2;

    int res = ChannelConstants_RESULT_OK;

    switch (op) {
    	case ChannelConstants_GLOBAL_CREATECONTEXT:
            res = 1; //let all Isolates share a context for now
            break;
    	case ChannelConstants_CONTEXT_GETCHANNEL: {
                int channelType = i1;
                if (channelType == ChannelConstants_CHANNEL_IRQ) {
                    res = 1;
                } else if (channelType == ChannelConstants_CHANNEL_SPI) {
                    res = 2;
                } else {
                    res = ChannelConstants_RESULT_BADPARAMETER;
                }
            }
            break;
    	case ChannelConstants_IRQ_WAIT: {
                int irq_no = i1;
                if (check_irq(irq_no, 1)) {
                    res = 0;
                } else {
                    res = storeIrqRequest(irq_no);
                }
            }
            break;
    		
    	case ChannelConstants_AVAILABLE_SERIAL_CHARS: {
                int deviceType = i1;
                res = sysAvailable(deviceType);
            }
    	    break;

        case ChannelConstants_GET_SERIAL_CHARS: {
                int deviceType = i3;
                if (sysAvailable(deviceType)) {
                    // Return 0 if there are chars available (which we will return in the receive param)
                    res = 0;
                    int offset = i1;
                    int len = i2;
                    int* countBuf = send;
                    char* buf = receive;
                    *countBuf = sysReadSeveral(buf + offset, len, deviceType);
                    freeSerialPort(deviceType); // free serial port for future use
                } else {
                    // Otherwise return event number to say there might be later
                    res = getSerialPortEvent(deviceType);
                }
            }
            break;

        case ChannelConstants_WRITE_SERIAL_CHARS: {
                int offset = i1;
                int len = i2;
                int deviceType = i3;
                char* buf = send;
                sysWriteSeveral(buf + offset, len, deviceType);
                res = 0;
            }
            break;

        case ChannelConstants_SPI_SEND_RECEIVE_8:
            // CE pin in i1
            // SPI config in i2
            // data in i3
            res = spi_sendReceive8(i1, i2, i3);
            break;
        case ChannelConstants_SPI_SEND_RECEIVE_8_PLUS_SEND_16:
            // CE pin in i1
            // SPI config in i2
            // data in i3
            // 16 bits in i4
            res = spi_sendReceive8PlusSend16(i1, i2, i3, i4);
            break;
        case ChannelConstants_SPI_SEND_RECEIVE_8_PLUS_SEND_N:
            // CE pin in i1
            // SPI config in i2
            // data in i3
            // size in i4
            res = spi_sendReceive8PlusSendN(i1, i2, i3, i4, send);
            break;
        case ChannelConstants_SPI_SEND_RECEIVE_8_PLUS_RECEIVE_16:
            // CE pin in i1
            // SPI config in i2
            // data in i3
            // 16 bits encoded in result
            res = spi_sendReceive8PlusReceive16(i1, i2, i3);
            break;
        case ChannelConstants_SPI_SEND_RECEIVE_8_PLUS_VARIABLE_RECEIVE_N:
            // CE pin in i1
            // SPI config in i2
            // data in i3
            // fifo_pin in i4
            // fifo pio in i5
            // data in receive
            res = spi_sendReceive8PlusVariableReceiveN(i1, i2, i3, receive, i4, i5);
            break;
        case ChannelConstants_SPI_SEND_AND_RECEIVE:
            // CE pin in i1
            // SPI config in i2
            // tx size in i4
            // rx size in i5
            // rx offset in i6
            // tx data in send
            // rx data in receive
            spi_sendAndReceive(i1, i2, i4, i5, i6, send, receive);
            break;
        case ChannelConstants_SPI_SEND_AND_RECEIVE_WITH_DEVICE_SELECT:
            // CE pin in i1
            // SPI config in i2
            // device address in i3
            // tx size in i4
            // rx size in i5
            // rx offset in i6
            // tx data in send
            // rx data in receive
            spi_sendAndReceiveWithDeviceSelect(i1, i2, i3, i4, i5, i6, send, receive);
            break;
        case ChannelConstants_SPI_PULSE_WITH_DEVICE_SELECT:
            // CE pin in i1
            // device address in i2
            // pulse duration in i3
            spi_pulseWithDeviceSelect(i1, i2, i3);
            break;
        case ChannelConstants_SPI_GET_MAX_TRANSFER_SIZE:
            res = SPI_DMA_BUFFER_SIZE;
            break;

        case ChannelConstants_I2C_OPEN:
            i2c_open();
            break;
        case ChannelConstants_I2C_CLOSE:
            i2c_close();
            break;
        case ChannelConstants_I2C_SET_CLOCK_SPEED:
            // clock speed in i1
            i2c_setClockSpeed(i1);
            break;
        case ChannelConstants_I2C_READ:
            // slave address in i1
            // internal address in i2
            // internal address size in i3
            // rx offset in i4
            // rx size in i5
            // rx data in receive
            res = i2c_read(i1, i2, i3, i4, i5, receive);
            break;
        case ChannelConstants_I2C_WRITE:
            // slave address in i1
            // internal address in i2
            // internal address size in i3
            // tx offset in i4
            // tx size in i5
            // tx data in send
            res = i2c_write(i1, i2, i3, i4, i5, send);
            break;
        case ChannelConstants_I2C_BUSY:
            res = i2c_busy();
            break;
        case ChannelConstants_I2C_PROBE:
            // slave address in i1
            // probe data in i2
            res = i2c_probe(i1, i2);
            break;

        case ChannelConstants_GET_HARDWARE_REVISION:
        	res = get_hardware_revision();
    		break;

        case ChannelConstants_FLASH_ERASE:
            data_cache_disable();
            res = flash_erase_sector((Flash_ptr)i2);
            data_cache_enable();
            // the 9200 seems to lose time during flash operations, so need to reset the clock
            synchroniseWithAVRClock();
            break;
    	case ChannelConstants_FLASH_WRITE: {
                int i, d, address = i1, size = i2, offset = i3;
                char *buffer = (char*) send;
                data_cache_disable();
                res = flash_write_words((Flash_ptr) address, (unsigned char*) (((int) send) + offset), size);
                data_cache_enable();
                // the 9200 seems to lose time during flash operations, so need to reset the clock
                synchroniseWithAVRClock();
            }
            break;
    	case ChannelConstants_USB_GET_STATE:
            res = usb_get_state();
            break;

    	case ChannelConstants_CONTEXT_GETERROR:
            res = *((char*) retValue);
            if (res == 0)
                retValue = 0;
            else
                retValue++;
            break;
    		
    	case ChannelConstants_CONTEXT_GETRESULT:
    	case ChannelConstants_CONTEXT_GETRESULT_2:
            res = retValue;
            retValue = 0;
            break;
    	case ChannelConstants_GLOBAL_GETEVENT:
            // since this function gets called frequently it's a good place to put
            // the call that periodically resyncs our clock with the power controller
            maybeSynchroniseWithAVRClock();
            // don't return any events other than FIQ while sleep manager is running because
            // the sleep manager might call SHALLOW_SLEEP and any threads
            // unblocked before that point will end up waiting for sleep to
            // finish even though they are runnable
            res = getEvent(true, sleepManagerRunning);
            // improve fairness of thread scheduling - see bugzilla #568
            bc = -TIMEQUANTA;
            break;
    	case ChannelConstants_GLOBAL_WAITFOREVENT: {
                long long millisecondsToWait = rebuildLongParam(i1, i2);
                osMilliSleep(millisecondsToWait);
                res = 0;
            }
            break;
    	case ChannelConstants_CONTEXT_DELETE:
    		// TODO delete all the outstanding events on the context
    		// But will have to wait until we have separate contexts for each isolate
    		res=0;
    		break;
        case ChannelConstants_CONTEXT_HIBERNATE:
            // TODO this is faked, we have no implementation currently.
            res = ChannelConstants_RESULT_OK;
            break;
        case ChannelConstants_CONTEXT_GETHIBERNATIONDATA:
            // TODO this is faked, we have no implementation currently.
            res = ChannelConstants_RESULT_OK;
            break;
        case ChannelConstants_DEEP_SLEEP: {
        	doDeepSleep(rebuildLongParam(i1, i2), i3);
    		res = 0;
	        } 
            break;
        case ChannelConstants_SHALLOW_SLEEP: {
    		long long target = rebuildLongParam(i1, i2);
    		if (target <= 0) target = 0x7FFFFFFFFFFFFFFFLL; // overflow detected
    		doShallowSleep(target);
    		res = 0;
	        } 
            break;
        case ChannelConstants_WAIT_FOR_DEEP_SLEEP:
    		minimumDeepSleepMillis = rebuildLongParam(i1, i2);
    		sleepManagerRunning = 0;
    		res = WAIT_FOR_DEEP_SLEEP_EVENT_NUMBER;
        	break;

        case ChannelConstants_DEEP_SLEEP_TIME_MILLIS_HIGH:
        	res = (int) (storedDeepSleepWakeupTarget >> 32);
        	break;

        case ChannelConstants_DEEP_SLEEP_TIME_MILLIS_LOW:
        	res = (int) (storedDeepSleepWakeupTarget & 0xFFFFFFFF);
        	break;
        	
        case ChannelConstants_TOTAL_SHALLOW_SLEEP_TIME_MILLIS_HIGH:
        	res = (int) (totalShallowSleepTime >> 32);
        	break;

        case ChannelConstants_TOTAL_SHALLOW_SLEEP_TIME_MILLIS_LOW:
        	res = (int) (totalShallowSleepTime & 0xFFFFFFFF);
        	break;
        	
        case ChannelConstants_SET_MINIMUM_DEEP_SLEEP_TIME:
    		minimumDeepSleepMillis = rebuildLongParam(i1, i2);
    		res = 0;
        	break;
        	
        case ChannelConstants_SET_SHALLOW_SLEEP_CLOCK_MODE:
        	shallow_sleep_clock_mode = i1;
    		res = 0;
        	break;
        	
        case ChannelConstants_GET_LAST_DEVICE_INTERRUPT_TIME_ADDR:
        	res = (int) &last_device_interrupt_time;
        	break;

        case ChannelConstants_GET_CURRENT_TIME_ADDR:
        	res = (int) &clock_counter;
        	break;

        case ChannelConstants_AVR_GET_TIME_HIGH: {
        	jlong avr_time = avrGetTime();
        	avr_low_result = (int) avr_time;
        	res = (int)(avr_time >> 32);
        	}
        	break;

        case ChannelConstants_AVR_GET_TIME_LOW:
        	res = avr_low_result;
        	break;
        	
        case ChannelConstants_AVR_GET_STATUS:
        	res = avrGetOutstandingEvents();
        	break;
        	
        case ChannelConstants_SET_DEEP_SLEEP_ENABLED:
        	deepSleepEnabled = i1;
        	res = 0;
        	break;
        	
        case ChannelConstants_WRITE_SECURED_SILICON_AREA:
        	data_cache_disable();
        	write_secured_silicon_area((Flash_ptr)i1, (short)i2);
        	data_cache_enable();
        	break;
        	
        case ChannelConstants_READ_SECURED_SILICON_AREA:
        	data_cache_disable();
        	read_secured_silicon_area((unsigned char*)send);
        	data_cache_enable();
        	break;
        	
        case ChannelConstants_SET_SYSTEM_TIME:
    		setMilliseconds(rebuildLongParam(i1, i2));
    		res = 0;
        	break;
        	
        case ChannelConstants_ENABLE_AVR_CLOCK_SYNCHRONISATION:
    		enableAVRClockSynchronisation(i1);
    		res = 0;
        	break;
        	
		case ChannelConstants_OPENCONNECTION:
			res = ChannelConstants_RESULT_EXCEPTION;
			retValue = (int)"javax.microedition.io.ConnectionNotFoundException";
			break;
			
		case ChannelConstants_GET_PUBLIC_KEY: {
		    	int maximum_length = i1;
		    	char* buffer_to_write_public_key_into = send;		    	
		    	res = retrieve_public_key(buffer_to_write_public_key_into, maximum_length);
    		}
    	    break;
    	case ChannelConstants_COMPUTE_CRC16_FOR_MEMORY_REGION:{
			int address=i1;
			int numberOfBytes=i2;
			res = crc(address, numberOfBytes);
			}
			break;
    	    
    	case ChannelConstants_REPROGRAM_MMU: {
    		reprogram_mmu(FALSE);
    		}
    		break;
        case ChannelConstants_GET_ALLOCATED_FILE_SIZE: {
        	res = get_allocated_file_size(i1);
    		}
    		break;
        case ChannelConstants_GET_FILE_VIRTUAL_ADDRESS: {
        	res = get_file_virtual_address(i1, send);
    		}
    		break;
    	case ChannelConstants_GET_DMA_BUFFER_SIZE:
    		res = dma_buffer_size;
    		break;
    	case ChannelConstants_GET_DMA_BUFFER_ADDRESS:
    		res = (int)dma_buffer_address;
    		break;
        case ChannelConstants_GET_RECORDED_OUTPUT: {
            int len = i1;
            int just_last = i2;
            char* buf = send;
            res = read_recorded_output(buf, len, just_last);
            }
            break;
        default:
    		res = ChannelConstants_RESULT_BADPARAMETER;
    }
    com_sun_squawk_ServiceOperation_result = res;
}

