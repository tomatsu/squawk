#include "gpio_api.h"
#include "bitset.h"
#include <stdint.h>
#include <stdio.h>

#ifndef MAX_GPIO_DESC
#define MAX_GPIO_DESC 32
#endif

static struct {
    int nwords;
    uint64_t words[bitset_datasize(MAX_GPIO_DESC)];
} _bs = {bitset_datasize(MAX_GPIO_DESC)};

static int unused_idx;
static bitset* bs = (bitset*)&_bs;
static gpio_t gpio_desc[MAX_GPIO_DESC];

int open_gpio(int pin, int is_output) {
    int idx = unused_idx;
    if (idx < 0) {
		return -1;
    } else {
		gpio_t* gpio = &gpio_desc[idx];
		bitset_set(bs, idx);
		gpio_init(gpio, pin);
		if (is_output) {
			gpio_dir(gpio, PIN_OUTPUT);
			gpio_mode(gpio, PullNone);
		} else {
			gpio_dir(gpio, PIN_INPUT);
			gpio_mode(gpio, PullDefault);
		}
		unused_idx = bitset_next_clear_bit(bs, idx + 1);
		return idx;
    }
}

int read_gpio(int desc) {
    if (bitset_get(bs, desc)) {
		return gpio_read(&gpio_desc[desc]);
    } else {
		return -1;
    }
}

void write_gpio(int desc, int value) {
    if (bitset_get(bs, desc)) {
		gpio_write(&gpio_desc[desc], value);
    }
}

void close_gpio(int desc) {
    bitset_clear(bs, desc);
    if (unused_idx < 0 || desc < unused_idx) {
		unused_idx = desc;
    }
}

void set_mode_gpio(int desc, int mode) {
    if (bitset_get(bs, desc)) {
		gpio_mode(&gpio_desc[desc], mode);
	}
}

int is_connected_gpio(int desc) {
    if (bitset_get(bs, desc)) {
		return gpio_is_connected(&gpio_desc[desc]);
	}
	return 0;
}
