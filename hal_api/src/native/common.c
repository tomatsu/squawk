#include <stdint.h>
#include "bitset.h"

#ifndef MAX_DESC
#define MAX_DESC 32
#endif

static struct {
    int nwords;
    uint64_t words[bitset_datasize(MAX_DESC)];
} _bs = {bitset_datasize(MAX_DESC)};

static int unused_idx;
static bitset* bs = (bitset*)&_bs;
static void* desc[MAX_DESC];

int allocate_desc(void *ptr) {
    int idx = unused_idx;
    if (idx < 0) {
		return -1;
    } else {
		desc[idx] = ptr;
		unused_idx = bitset_next_clear_bit(bs, idx + 1);
	}
	return idx;
}

void* get_object_from_desc(int idx) {
	return desc[idx];
}	

int deallocate_desc(int idx) {
    if (bitset_get(bs, idx)) {
		bitset_clear(bs, idx);
		desc[idx] = 0;
		if (unused_idx < 0 || idx < unused_idx) {
			unused_idx = idx;
		}
	}
}
