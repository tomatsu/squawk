#include <lwip/udp.h>
#include <stdint.h>
#include <sys/types.h>
#include <stdbool.h>

typedef struct {
	struct udp_pcb* _pcb;
	struct pbuf* _rx_buf;
	size_t _rx_buf_offset;
	uint32_t _blocker;
} udp_context_t;

extern udp_context_t* squawk_udp_create(int port);
extern bool squawk_udp_connect(udp_context_t* udp, ip_addr_t *addr, uint16_t port);
extern void squawk_udp_disconnect(udp_context_t* udp);
extern bool squawk_udp_bind(udp_context_t* udp, ip_addr_t *addr, uint16_t port);
extern size_t squawk_udp_read(udp_context_t* udp, char* dst, size_t size);
extern bool squawk_udp_send(udp_context_t* udp, ip_addr_t* addr, uint16_t port, char* src, size_t size);
