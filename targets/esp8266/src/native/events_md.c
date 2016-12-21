#include "events.h"

extern int os_printf_plus(const char *format, ...)  __attribute__ ((format (printf, 1, 2)));
#define printf os_printf_plus

int squawk_event_type(int type) {
	// TODO
	return BOUND_EVENT;
}

int squawk_check_unbound_event(int type, int clear_flag, int *evt) {
	// do nothing for now
}

void squawk_update_event() {
	ets_loop_iter();
}
