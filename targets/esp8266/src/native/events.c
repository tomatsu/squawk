#include <stdint.h>
#include <stdlib.h>
#include <mem.h>
#include "events.h"
#include "classes.h"
#include "ets_alt_task.h"

extern int os_printf_plus(const char *format, ...)  __attribute__ ((format (printf, 1, 2)));
#define printf os_printf_plus
#define malloc os_malloc
#define free os_free
#define EVENT_NUMBER (squawk_cioRequestor()+1)

/*
 * event delivery mechanism
 */
struct irqRequest {
	int eventNumber;
	int type;
	bool signaled;
	int event;
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
        newRequest->signaled = false;
        newRequest->event = 0;

        if (irqRequests == NULL) {
        	irqRequests = newRequest;
        	newRequest->eventNumber = EVENT_NUMBER;
        } else {
        	IrqRequest* current = irqRequests;
        	while (current->next != NULL) {
        		current = current->next;
        	}
        	current->next = newRequest;
        	newRequest->eventNumber = EVENT_NUMBER;
        }
        return newRequest->eventNumber;
}

int getEventPrim(int);
static int check_event(IrqRequest* requests, int clear_flag, int* result);

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
	ets_loop_iter();
	
        int res = 0;
        if (irqRequests == NULL) {
        	return 0;
        }
        IrqRequest* current = irqRequests;
        IrqRequest* previous = NULL;
        while (current != NULL) {
			int evt;
        	if (check_event(current, removeEventFlag, &evt)) {
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
static int check_event(IrqRequest* current, int clear_flag, int *evt) {
	if (current->signaled) {
		*evt = current->event;
		if (clear_flag) {
			current->signaled = false;
		}
		return 1;
	} else {
		return 0;
	}
}

void squawk_post_event(int threadNumber, int type, uint32_t value) {
	if (irqRequests == NULL) {
		return;
	}
	IrqRequest* current = irqRequests;
	while (current != NULL) {
		int evt;
		if ((threadNumber == 0 || current->eventNumber == threadNumber) && current->type == type) {
			current->event = value;
			current->signaled = true;
			break;
		}
		current = current->next;
	}
}
