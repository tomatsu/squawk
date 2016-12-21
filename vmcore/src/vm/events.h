#pragma once

/*
 * Unbound event can be delivered to any thread that is listening to the event.
 */
#define UNBOUND_EVENT 0

/*
 * Bound event is delivered to a certain thread and it is not queued.  For example, in network I/O operation, a bound event is delivered when data to be read is available.
 * @See also squawk_post_event
 */
#define BOUND_EVENT 1

/*
 * Initialize event interface
 */
extern void squawk_init_event();

/*
 * Return UNBOUND_EVENT or BOUND_EVENT
 */
extern int squawk_event_type(int eventType);

/*
 * Poll unbound event.  If clear_flag is true, consume the event.
 */
extern int squawk_check_unbound_event(int eventType, int clear_flag, int *event);

/*
 * Fire a bound event.   The event must be a bound event.
 */
extern void squawk_post_event(int threadNumber, int eventType, int value);

/*
 * Listen to the specified event.
 */
extern int storeIrqRequest (int eventType);

/*
 * If there are outstanding irqRequests and one of them is for an interrupt that has
 * occurred return its eventNumber. Otherwise return 0
 */
extern int checkForEvents();

/*
 * If there are outstanding irqRequests and one of them is for an irq that has
 * occurred remove it and return its eventNumber. Otherwise return 0
 */
extern int getEvent();

extern void squawk_update_event();
