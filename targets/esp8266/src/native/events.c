#include <stdint.h>
#include <stdbool.h>
#include <stdlib.h>
#include <mem.h>
#include "events.h"
#include "ets_alt_task.h"

#define malloc os_malloc
#define free os_free

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

static int irqRequestCount[MAX_EVENT_TYPE] = {0};
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
		irqRequestCount[type]++;
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
//	printf("ets_loop_iter\n");
	ets_loop_iter();
	
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

/**
 * Check if an irq bit is set in the status, return 1 if yes
 * Also, clear bit if it is set and clear_flag = 1
 */
static int check_event(int type, int clear_flag, int *evt) {
        int result;
		int r = squawk_get_event(type, clear_flag);
		if (r) {
		    *evt = r;
            result = 1;
        } else {
        	result = 0;
        }
        return result;
}

static uint32_t event_status = 0;
static uint32_t wifi_event;
#define IS_WIFI_EVENT(type) (type >= WIFI_STAMODE_CONNECTED_EVENT && type <= WIFI_SCAN_DONE_EVENT)

int squawk_get_event(int type, bool clear) {
	int bit = (1 << type);
	int e = (event_status & bit) != 0;
	if (clear && e) {
		if (--irqRequestCount[type] == 0) {
			event_status &= ~bit;
		}
	}
	if (IS_WIFI_EVENT(type)) {
		return e ? wifi_event : 0;
	}
	return e;
}

void squawk_post_event(int type, uint32_t value) {
	event_status |= (1 << type);
	if (IS_WIFI_EVENT(type)) {
		wifi_event = value;
	}
}
