#include <stdlib.h>
#include <string.h>
#include "bitset.h"

#define NWORDS(bs) bs->nwords

void
bitset_set(bitset* bs, unsigned int idx)
{
    int wordIndex;

    wordIndex = word_index(idx);
    if (wordIndex < NWORDS(bs)){
	bs->words[wordIndex] |= (1LL << (idx % BITS_PER_WORD));
    }
}

void
bitset_clear(bitset* bs, unsigned int idx)
{
    int wordIndex;

    wordIndex = word_index(idx);
    if (wordIndex < NWORDS(bs)){
	bs->words[wordIndex] &= ~(1LL << (idx % BITS_PER_WORD));
    }
}

int
bitset_get(bitset* bs, unsigned int idx)
{
    int wordIndex;

    wordIndex = word_index(idx);
    if (wordIndex < NWORDS(bs)){
	return ((bs->words[wordIndex] & (1LL << (idx % BITS_PER_WORD))) != 0);
    } else {
	return 0;
    }
}

static int
number_of_trailing_zeros(unsigned long long i)
{
    unsigned int x;
    unsigned int y;
    unsigned int n;

    if (i == 0) return 64;
    n = 63;
    y = (unsigned int)i;
    if (y != 0) { n = n -32; x = y; } else { x = (unsigned int)(i >> 32); }
    y = x <<16; if (y != 0) { n = n -16; x = y; }
    y = x << 8; if (y != 0) { n = n - 8; x = y; }
    y = x << 4; if (y != 0) { n = n - 4; x = y; }
    y = x << 2; if (y != 0) { n = n - 2; x = y; }
    return n - ((x << 1) >> 31);
}

int
bitset_next_clear_bit(bitset* bs, unsigned int fromIdx)
{
    unsigned long long word;
    int u;

    u = word_index(fromIdx);
    if (u >= NWORDS(bs)){
	return -1;
    }
    word = ~bs->words[u] & (WORD_MASK << (fromIdx % 64));

    for (;;) {
	if (word != 0){
	    return (u * BITS_PER_WORD) + number_of_trailing_zeros(word);
	}
	if (++u == NWORDS(bs)){
	    return NWORDS(bs) * BITS_PER_WORD;
	}
	word = ~bs->words[u];
    }
}
