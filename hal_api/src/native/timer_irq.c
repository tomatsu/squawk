#include "bitset.h"
#include "ticker_api.h"
#include "us_ticker_api.h"
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>

typedef struct {
	void (* callback)(uint32_t arg);
	int delay;
	const ticker_data_t* ticker_data;
	ticker_event_t event;
	int cancelled;
	int repeat;
	int desc;
} timer_request_t;

#define DELAY(id) ((timer_request_t*)id)->delay
#define TICKER_DATA(id) ((timer_request_t*)id)->ticker_data
#define EVENT(id) (&((timer_request_t*)id)->event)
#define CANCELLED(id) ((timer_request_t*)id)->cancelled
#define REPEAT(id) ((timer_request_t*)id)->repeat

#define MAX_DESC 32

extern void set_timer_event(int);

static struct {
    int nwords;
    uint64_t words[bitset_datasize(MAX_DESC)];
} _bs = {bitset_datasize(MAX_DESC)};

static int unused_idx = 0;
static bitset* bs = (bitset*)&_bs;

static timer_request_t* requests[MAX_DESC];

static int allocate_slot(void *ptr) {
    int idx = unused_idx;
    if (idx < 0) {
		return -1;
    } else {
		requests[idx] = ptr;
		unused_idx = bitset_next_clear_bit(bs, idx + 1);
	}
	return idx;
}

int free_slot(int idx) {
    if (bitset_get(bs, idx)) {
		bitset_clear(bs, idx);
		requests[idx] = 0;
		if (unused_idx < 0 || idx < unused_idx) {
			unused_idx = idx;
		}
	}
}

static void handler(uint32_t arg) {
	timer_request_t* request = (timer_request_t*)arg;
	int desc = request->desc;
    if (!CANCELLED(request)) {
		if (REPEAT(request)) {
			ticker_insert_event(TICKER_DATA(request), EVENT(request), EVENT(request)->timestamp + DELAY(request), (int)request);
		}
		set_timer_event(1 << desc);
    } else {
	    free((timer_request_t*)request);
		free_slot(desc);
	}
}

int
schedule(int _delay, int _repeat) {
    const ticker_data_t* _ticker_data = get_us_ticker_data();
	
	timer_request_t* req = (timer_request_t*)malloc(sizeof(timer_request_t));
	if (!req) {
		printf("out of memory\n");
		return 0;
	}
	req->callback = &handler;
	req->delay = _delay * 1000;
	req->ticker_data = _ticker_data;
	req->cancelled = 0;
	req->repeat = _repeat;
	int desc = allocate_slot(req);
	if (desc < 0) {
		printf("can't allocate descriptor\n");
		free(req);
		return 0;
	}
	req->desc = desc;
	
    ticker_insert_event(_ticker_data,
						&req->event,
					   	req->delay + ticker_read(_ticker_data),
						(int)req);
	return desc;
}

void
Java_com_sun_squawk_hal_TimerIRQ_cancel(int desc) {
	timer_request_t* req = requests[desc];
	req->cancelled = 1;
	ticker_remove_event(req->ticker_data, &req->event);
	free_slot(desc);
}

int Java_com_sun_squawk_hal_TimerIRQ_init(int delay, int repeat) {
	return schedule(delay, repeat);
}
