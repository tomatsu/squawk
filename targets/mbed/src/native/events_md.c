#include <stdint.h>
#include <cmsis.h>
#include "events.h"

typedef int (*event_check_routine_t)(int, int*);

extern int get_gpio_event(int);
extern int get_timer_event(int);

/*
 * Functions to retrieve events defined in com.sun.squawk.hal.Events.
 */
static event_check_routine_t event_accessor[] = {
	&get_gpio_event,
	&get_timer_event
};

/*
 * Return UNBOUND_EVENT or BOUND_EVENT
 */
int squawk_event_type(int type)
{
	if (type >= sizeof(event_accessor) / sizeof(event_check_routine_t)) {
		return BOUND_EVENT;
	} else {
		return UNBOUND_EVENT;
	}
}

/*
 * Poll unbound event.  If clear_flag is true, consume the event.
 */
int squawk_check_unbound_event(int type, int clear_flag, int *event) {
	int result;
	__disable_irq();
	result = (event_accessor[type])(clear_flag, event);
	__enable_irq();
	return result;
}

void squawk_update_event() {}
