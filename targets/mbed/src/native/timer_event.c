#include <stdint.h>

static uint32_t irq_status;

int get_timer_event(int clear, int *evt) {
	int ret = irq_status;
	if (clear) {
		irq_status = 0;
	}
	if (ret) {
		*evt = ret;
	}
	return ret;
}

void set_timer_event(int event) {
	irq_status |= event;
}
