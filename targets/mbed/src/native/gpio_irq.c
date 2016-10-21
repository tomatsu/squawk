#include <stdlib.h>
#include "gpio_irq_api.h"
#include "common.h"

#if DEVICE_INTERRUPTIN

extern void post_gpio_event(int pin, int type);

static void _irq_handler(uint32_t id, gpio_irq_event event) {
	post_gpio_event(id, event);
}

int Java_com_sun_squawk_hal_GpioIRQ_init(int pin, int fall) {
	int ret = 0;
	int desc;
	gpio_t gpio;
	gpio_irq_t* irq = (gpio_irq_t*)malloc(sizeof(gpio_irq_t));
	if (!irq) {
		return -1;
	}
	gpio_irq_init(irq, pin, &_irq_handler, pin);
	gpio_init_in(&gpio, pin);
	gpio_irq_set(irq, fall ? IRQ_FALL : IRQ_RISE, 1);
	
	desc = allocate_desc(irq);
	if (desc == -1) {
		free(irq);
		return -1;
	}
	add_gpio_event_mask(pin, fall);
	
	return desc;
}
#endif
