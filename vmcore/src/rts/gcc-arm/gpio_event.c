#include <stdint.h>
#include "cmsis.h"

#define GPIO_BUFFER_LIMIT 4

#define MASK(pin, fall) ((fall ? 1LL : 2LL) << (pin * 2))
#define EVENT(pin, fall) (((uint64_t)pin << 2) | (fall ? 1 : 2))
#define EVENT_PIN(event) (event >> 2)
#define EVENT_FALL(event) ((event & 1) != 0)

typedef int gpio_event_t;

typedef struct {
    gpio_event_t events[GPIO_BUFFER_LIMIT];
	int start;
	int end;
	int size;
} ringbuf_t;

static ringbuf_t buf;
static uint64_t event_mask;

static void put(ringbuf_t* buf, int event) {
	if (buf->size < GPIO_BUFFER_LIMIT) {
		gpio_event_t *e = &buf->events[buf->start];
		*e = event;
		buf->start = (buf->start + 1) % GPIO_BUFFER_LIMIT;
		buf->size++;
	}
}

static int get(ringbuf_t* buf, int clear, int* event) {
	if (buf->size > 0) {
	    gpio_event_t* e = &buf->events[buf->end];
		*event = *e;
		if (clear) {
			buf->end = (buf->end + 1) % GPIO_BUFFER_LIMIT;
			buf->size--;
		}
		return 0;
	}
	return -1;
}


void del_gpio_event_mask(int pin, int fall) {
	event_mask &= ~MASK(pin, fall);
}

void add_gpio_event_mask(int pin, int fall) {
	event_mask |= MASK(pin, fall);
}

int get_gpio_event(int clear) {
	int r, result;
	int event;
	
	__disable_irq();
	r = get(&buf, clear, &event);
	__enable_irq();
	if (r < 0) {
		result = 0;
	} else {
		result = event;
	}
	return result;
}

void post_gpio_event(int pin, int type) {
	if (MASK(pin, type) & event_mask) {
		__disable_irq();
		put(&buf, EVENT(pin, type));
		__enable_irq();
	}

}
