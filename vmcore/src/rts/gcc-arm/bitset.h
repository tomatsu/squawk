#ifndef _BITSET_H_
#define _BITSET_H_

#include <stdint.h>

typedef struct _bitset {
    int nwords;
    uint64_t words[0];
} bitset;

#define ADDRESS_BITS_PER_WORD 6
#define BITS_PER_WORD  (1U << ADDRESS_BITS_PER_WORD)
#define BIT_INDEX_MASK (BITS_PER_WORD - 1)
#define WORD_MASK 0xffffffffffffffffLL

#define word_index(idx) ((idx)>>ADDRESS_BITS_PER_WORD)
#define bitset_datasize(nbits) (word_index((nbits)-1)+1)

extern void bitset_set(bitset* bs, unsigned int idx);
extern void bitset_clear(bitset* bs, unsigned int idx);
extern int bitset_get(bitset* bs, unsigned int idx);
extern int bitset_next_clear_bit(bitset* bs, unsigned int fromIdx);

#endif
