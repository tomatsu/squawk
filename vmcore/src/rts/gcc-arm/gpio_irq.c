#include "gpio_irq_api.h"
#include "bitset.h"

#ifndef MAX_GPIOIRQ_DESC
#define MAX_GPIOIRQ_DESC 4
#endif

static struct {
    int nwords;
    uint64_t words[bitset_datasize(MAX_GPIOIRQ_DESC)];
} _bs = {bitset_datasize(MAX_GPIOIRQ_DESC)};

static int unused_idx;
static bitset* bs = (bitset*)&_bs;
static gpio_irq_t gpio_irq_desc[MAX_GPIOIRQ_DESC];

extern void post_gpio_event(int pin, int type);

static void _irq_handler(uint32_t id, gpio_irq_event event) {
	post_gpio_event(id, event);
}

int GpioIRQ_init(int pin, int fall) {
	int ret = 0;
    int idx = unused_idx;
    if (idx < 0) {
		return 0;
    } else {
		gpio_irq_t* irq = &gpio_irq_desc[idx];
		gpio_t gpio;
		
		bitset_set(bs, idx);
		gpio_irq_init(irq, pin, &_irq_handler, pin);
		gpio_init_in(&gpio, pin);
		gpio_irq_set(irq, fall ? IRQ_FALL : IRQ_RISE, 1);

		add_gpio_event_mask(pin, fall);
		
		unused_idx = bitset_next_clear_bit(bs, idx + 1);
		return ret;
	}
}
