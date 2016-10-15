#include <stdint.h>
#include <sys/types.h>

/*
 * memcpy and memmove implementation that can read irom0.text section, along with -mforce-l32 option.
 */
void* memcpy(void *dest, void *src, size_t n)
{
	uintptr_t dp = (uintptr_t)dest;
	uintptr_t sp = (uintptr_t)src;
	if ((dp & 3) != (sp & 3)) {
		while (n--) {
			*(uint8_t*)dest++ = *(uint8_t*)src++;
		}
		return dest;
	}
	uint8_t* d = (uint8_t*)dest;
	uint8_t* s = (int8_t*)src;
	if (dp & 3) {
		for (int i = 0; i < 4 - (dp & 3); i++) {
			*d++ = *s++;
		}
		n -= (4 - (dp & 3));
	}
	int m = (n + 3) / 4;
	uint32_t* dw = (uint32_t*)d;
	uint32_t* sw = (uint32_t*)s;
	while (m--) {
		*dw++ = *sw++;
	}
	d = dw;
	s = sw;
	int l = (n % 4);
	while (l--) {
		*d++ = *s++;
	}
	return dest;
}

void* memmove(void *dest, void *src, size_t n)
{
	uintptr_t dp = (uintptr_t)dest;
	uintptr_t sp = (uintptr_t)src;
	if ((dp & 3) != (sp & 3)) {
		if (dest > src) {
			uint8_t* d = (uint8_t*)(dp + n - 1);
			uint8_t* s = (uint8_t*)(sp + n - 1);
			while (n--) {
				*d-- = *s--;
			}
		} else {
			while (n--) {
				*(uint8_t*)dest++ = *(uint8_t*)src++;
			}
		}
		return dest;
	}
		
	if (dest > src) {
		uint8_t* d;
		uint8_t* s;
		
		if ((dp + n) & 3) {
			d = dest + n - 1;
			s = src + n - 1;
			for (int i = 0; i < ((dp + n) & 3); i++) {
				*d-- = *s--;
			}
			n -= ((dp + n) & 3);
			d -= 3;
			s -= 3;
		} else {
			d = dest + n - 4;
			s = src + n - 4;
		}
		int m = n / 4;
		uint32_t* dw = (uint32_t*)d;
		uint32_t* sw = (uint32_t*)s;

		while (m-- > 0) {
			*dw-- = *sw--;
		}
		d = dw;
		s = sw;
		int l = (n % 4);
		while (l-- > 0) {
			*d-- = *s--;
		}
		return dest;
	} else {
		uint8_t* d = (uint8_t*)dest;
		uint8_t* s = (uint8_t*)src;

		if (dp & 3) {
			for (int i = 0; i < 4 - (dp & 3); i++) {
				*d++ = *s++;
			}
			n -= (4 - (dp & 3));
		}
		int m = n / 4;
		uint32_t* dw = (uint32_t*)d;
		uint32_t* sw = (uint32_t*)s;
		while (m--) {
			*dw++ = *sw++;
		}
		d = dw;
		s = sw;
		int l = (n % 4);
		while (l--) {
			*d++ = *s++;
		}
		return dest;
	}
}
