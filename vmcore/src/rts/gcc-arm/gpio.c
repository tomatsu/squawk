#include "gpio_api.h"
#include "common.h"
#include <stdint.h>
#include <stdlib.h>

int open_gpio(int pin, int is_output) {
	gpio_t* gpio = (gpio_t*)malloc(sizeof(gpio_t));
	if (!gpio) {
		return -1;
	}
	gpio_init(gpio, pin);
	if (is_output) {
		gpio_dir(gpio, PIN_OUTPUT);
		gpio_mode(gpio, PullNone);
	} else {
		gpio_dir(gpio, PIN_INPUT);
		gpio_mode(gpio, PullDefault);
	}
	int d = allocate_desc(gpio);
	if (d == -1) {
		free(gpio);
		return -1;
	}
	return d;
}

int read_gpio(int desc) {
	gpio_t* gpio = get_object_from_desc(desc);
	if (gpio) {
		return gpio_read(gpio);
    } else {
		return -1;
    }
}

int write_gpio(int desc, int value) {
	gpio_t* gpio = get_object_from_desc(desc);
	if (gpio) {
		gpio_write(gpio, value);
		return 0;
    } else {
		return -1;
	}
}

void close_gpio(int idx) {
	gpio_t* gpio = get_object_from_desc(idx);
	if (gpio) {
		free(gpio);
	}
    deallocate_desc(idx);
}

void set_mode_gpio(int desc, int mode) {
	gpio_t* gpio = get_object_from_desc(desc);
	if (gpio) {
		gpio_mode(gpio, mode);
	}
}

int is_connected_gpio(int idx) {
	gpio_t* gpio = get_object_from_desc(idx);
	if (gpio) {
		return gpio_is_connected(gpio);
	}
	return 0;
}
