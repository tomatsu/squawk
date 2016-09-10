struct irqRequest {
        int eventNumber;
        int type;
        struct irqRequest *next;
};
typedef struct irqRequest IrqRequest;
typedef int (*event_check_routine_t)(int);

IrqRequest *irqRequests;

/*
 * Java has requested wait for an interrupt. Store the request,
 * and each time Java asks for events, signal the event if the interrupt has happened
 *
 * @return the event number
 */
int storeIrqRequest (int type) {
        IrqRequest* newRequest = (IrqRequest*)malloc(sizeof(IrqRequest));
        if (newRequest == NULL) {
        	//TODO set up error message for GET_ERROR and handle
        	//one per channel and clean on new requests.
        	return ChannelConstants_RESULT_EXCEPTION;
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
			    com_sun_squawk_ServiceOperation_event = evt;
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

extern int get_gpio_event(int);
extern int get_timer_event(int);

static event_check_routine_t get_event_check_routine(int type) {
	   switch (type) {
	   case 0:
	   	   return &get_gpio_event;
	   case 1:
	   	   return &get_timer_event;
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
		
		__disable_irq();
		int r = (*checker)(clear_flag);
		if (r) {
		    *evt = r;
            result = 1;
        } else {
        	result = 0;
        }
		__enable_irq();
        return result;
}


/**
 * Sleep Squawk for specified milliseconds
 */
extern jlong sysTickCount();
 
void osMilliSleep(long long millis) {
    long long target = sysTickCount() + millis;
    long long maxValue = 0x7FFFFFFFFFFFFFFFLL;
    if (target <= 0) target = maxValue; // overflow detected
    while (1) {
        if (checkForEvents()) break;
        if (sysTickCount() > target) break;
		__WFE();
	}
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
		res = getEvent();
		break;
	}

	case ChannelConstants_GLOBAL_WAITFOREVENT: {
		long long millisecondsToWait = rebuildLongParam(i1, i2);
		osMilliSleep(millisecondsToWait);
		break;
	}

	case ChannelConstants_IRQ_WAIT: {
		int addr = i1;
		res = storeIrqRequest(addr);
	}
		break;
	default: {
//            ioExecuteSys(); // do platform-specific
//            res = com_sun_squawk_ServiceOperation_result; // result set by ioExecuteSys.
	    printf("###### skip op: %d\n", op);
		break;
	}
    }
    com_sun_squawk_ServiceOperation_result = res;
}

