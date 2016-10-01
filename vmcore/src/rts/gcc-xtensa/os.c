#include <mem.h>
#include <osapi.h>
#include <uart.h>
#include <stdint.h>
#include <stdlib.h>
#include <user_interface.h>

//#define IODOTC
extern int os_printf_plus(const char *format, ...)  __attribute__ ((format (printf, 1, 2)));
#define printf os_printf_plus

#define CIO_STDIO_H cio_esp.c.inc

#define jlong  int64_t

static jlong _counter;

static void cb(void) {
	_counter++;
}

static void initTickCount() {
	hw_timer_init(0, 1);
	hw_timer_set_func(cb);
	hw_timer_arm(1000);
}

static void initSerial() {
	uart_init(BIT_RATE_115200, BIT_RATE_115200);
}

void sysInitialize() {
    initTickCount();
	initSerial();
}

jlong sysTickCount(){
    return _counter;
}

jlong sysTimeMicros() {
    extern jlong sysTimeMillis(void);
    return sysTimeMillis() * 1000;
}

jlong sysTimeMillis(void) {
	return sysTickCount();
}

void *sysValloc(int size) {
	return (void*)os_malloc(size);
}

void sysVallocFree(void* p){
	os_free(p);
}

int getEvent() {
	system_soft_wdt_feed();
	return 0;
}

void osMilliSleep(long long millis) {
    long long target = sysTickCount() + millis;
    long long maxValue = 0x7FFFFFFFFFFFFFFFLL;
    if (target <= 0) target = maxValue; // overflow detected
    while (1) {
//        if (checkForEvents()) break;
        if (sysTickCount() > target) break;
		os_delay_us(1000);
		system_soft_wdt_feed();
	}
}

/*
 * event delivery mechanism
 */
struct irqRequest {
        int eventNumber;
        int type;
        struct irqRequest *next;
};
typedef struct irqRequest IrqRequest;
typedef int (*event_check_routine_t)(int);

static IrqRequest *irqRequests;

/*
 * Java has requested wait for an interrupt. Store the request,
 * and each time Java asks for events, signal the event if the interrupt has happened
 *
 * @return the event number, -1 on error
 */
int storeIrqRequest (int type) {
        IrqRequest* newRequest = (IrqRequest*)malloc(sizeof(IrqRequest));
        if (newRequest == NULL) {
        	//TODO set up error message for GET_ERROR and handle
        	//one per channel and clean on new requests.
				return -1;
        }

        newRequest->next = NULL;
        newRequest->type = type;

        if (irqRequests == NULL) {
        	irqRequests = newRequest;
        	newRequest->eventNumber = 1;
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

int getEventPrim(int);
static int check_event(int type, int clear_flag, int* result);

/*
 * If there are outstanding irqRequests and one of them is for an irq that has
 * occurred remove it and return its eventNumber. Otherwise return 0
 */
int getEvent() {
        return getEventPrim(1);
}

/*
 * If there are outstanding irqRequests and one of them is for an interrupt that has
 * occurred return its eventNumber. Otherwise return 0
 */
int checkForEvents() {
        return getEventPrim(0);
}

/*
 * If there are outstanding irqRequests and one of them is for an interrupt that has
 * occurred return its eventNumber. If removeEventFlag is true, then
 * also remove the event from the queue. If no requests match the interrupt status
 * return 0.
 */
int getEventPrim(int removeEventFlag) {
        int res = 0;
        if (irqRequests == NULL) {
        	return 0;
        }
        IrqRequest* current = irqRequests;
        IrqRequest* previous = NULL;
        while (current != NULL) {
			int evt;
        	if (check_event(current->type, removeEventFlag, &evt)) {
			    set_event(evt);
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
        return res;
}

extern int get_wifi_event(int);

static event_check_routine_t get_event_check_routine(int type) {
	   switch (type) {
	   case 1:
	   	   return &get_wifi_event;
	   default:
		   printf("unknown event type %d\n", type);
	   }
}

/**
 * Check if an irq bit is set in the status, return 1 if yes
 * Also, clear bit if it is set and clear_flag = 1
 */
static int check_event(int type, int clear_flag, int *evt) {
        int result;
		event_check_routine_t checker = get_event_check_routine(type);
		
		int r = (*checker)(clear_flag);
		if (r) {
		    *evt = r;
            result = 1;
        } else {
        	result = 0;
        }
        return result;
}


/*
 * Wifi Event
 */
static int wifi_event;

int get_wifi_event(int clear) {
	int ret = wifi_event;
	if (clear) {
		wifi_event = 0;
	}
	return ret;
}

void set_wifi_event(int event) {
	wifi_event = event;
}
