#include <stdint.h>
#include <stdbool.h>
#include <stdlib.h>
#include "events.h"

#define IS_BOUND_EVENT(type) (squawk_event_type(type) == BOUND_EVENT)
#define EVENT_NUMBER (squawk_cioRequestor()+1)

struct irqRequest {
	int eventNumber;
	int type;
	bool signaled;
	int event;
	struct irqRequest *next;
};
typedef struct irqRequest IrqRequest;

static IrqRequest *irqRequests;

#ifndef IRQREQ_POOL_SIZE
#define IRQREQ_POOL_SIZE 4
#endif
static IrqRequest pool[IRQREQ_POOL_SIZE];
static IrqRequest* freeList = &pool[0];

/*
 * Initialize memory pool
 */
void squawk_init_event() {
	freeList = (IrqRequest*)&pool[0];
	IrqRequest* prev = freeList;
	for (int i = 1; i < sizeof(pool) / sizeof(IrqRequest); i++) {
		IrqRequest* r = &pool[i];
		prev->next = r;
		prev = r;
	}
	prev->next = NULL;
}

/*
 * Fire a bound event
 */
void squawk_post_event(int threadNumber, int type, int value) {
	if (irqRequests == NULL) {
		return;
	}
	IrqRequest* current = irqRequests;
	while (current != NULL) {
		int evt;
		if (current->type == type) {
				if (current->eventNumber == threadNumber) {
						current->event = value;
						current->signaled = true;
						break;
				} else if (threadNumber == 0) {
						current->event = value;
						current->signaled = true;
				}
		}
		current = current->next;
	}
}

static IrqRequest* allocateIrqRequest() {
	if (freeList) {
		IrqRequest* r = freeList;
		freeList = freeList->next;
		return r;
	}
	return NULL;
}

static void recycleIrqRequest(IrqRequest* r) {
	r->next = freeList;
	freeList = r;
}

/*
 * Java has requested wait for an interrupt. Store the request,
 * and each time Java asks for events, signal the event if the interrupt has happened
 *
 * @return the event number, -1 on error
 */
int storeIrqRequest (int type) {
	IrqRequest* newRequest = allocateIrqRequest();
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

/*
 * Check bound event in a single irq request.  If an event has been signaled, the value will be stored in the 3rd argument.
 */
static int check_bound_event(IrqRequest* request, int clear_flag, int* event) {
	if (!IS_BOUND_EVENT(request->type)) {
		return 0;
	}
	if (request->signaled) {
		*event = request->event;
		if (clear_flag) {
			request->signaled = false;
		}
		return 1;
	} else {
		return 0;
	}
}

/**
 * Check if an irq bit is set in the status, return 1 if yes
 * Also, clear bit if it is set and clear_flag = 1
 */
static int check_event(IrqRequest* request, int clear_flag, int *evt) {
	if (IS_BOUND_EVENT(request->type)) {
		return check_bound_event(request, clear_flag, evt);
	} else {
		return squawk_check_unbound_event(request->type, clear_flag, evt);
	}
}

/*
 * If there are outstanding irqRequests and one of them is for an interrupt that has
 * occurred return its eventNumber. If removeEventFlag is true, then
 * also remove the event from the queue. If no requests match the interrupt status
 * return 0.
 */
static int getEventPrim(int removeEventFlag) {
	squawk_update_event();
	
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
				recycleIrqRequest(current);				
			}
			break;
		} else {
			previous = current;
			current = current->next;
		}
	}
	return res;
}

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

