#include <lwip/udp.h>
#include <stdint.h>
#include <mem.h>
#include "udp.h"
#include "events.h"

#define malloc os_malloc
#define CALLER (squawk_threadID() + 1)

static void recv_cb(void *arg, struct udp_pcb *upcb, struct pbuf *pb, ip_addr_t *addr, u16_t port) {
	udp_context_t* udp = (udp_context_t*)arg;
	if (udp->_rx_buf) {
		pbuf_cat(udp->_rx_buf, pb);
	} else {
		udp->_rx_buf = pb;
		udp->_rx_buf_offset = 0;
	}
	if (udp->_blocker) {
		squawk_post_event(udp->_blocker, READ_READY_EVENT, 0);
		udp->_blocker = 0;
	}
}

udp_context_t* squawk_udp_create(int port) {
	udp_context_t* udp = (udp_context_t*)malloc(sizeof(udp_context_t));
	udp->_rx_buf = 0;
	udp->_rx_buf_offset = 0;
	udp->_pcb = udp_new();
	ip_addr_t addr = {0};
	udp_bind(udp->_pcb, &addr, port);
	udp_recv(udp->_pcb, &recv_cb, udp);
	return udp;
}


bool squawk_udp_connect(udp_context_t* udp, ip_addr_t addr, uint16_t port) {
	ip_addr_copy(udp->_pcb->remote_ip, addr);
	udp->_pcb->remote_port = port;
	return true;
}

void squawk_udp_disconnect(udp_context_t* udp) {
	udp_disconnect(udp->_pcb);
}

bool squawk_udp_bind(udp_context_t* udp, ip_addr_t addr, uint16_t port) {
	err_t err = udp_bind(udp->_pcb, &addr, port);
	return err == ERR_OK;
}

void squawk_udb_localaddr(udp_context_t* udp, ip_addr_t* addr) {
	*addr = udp->_pcb->local_ip;
}

size_t squawk_udp_read(udp_context_t* udp, char* dst, size_t size) {
	if (!udp->_rx_buf) {
		udp->_blocker = CALLER;
		return 0;
	}

	size_t max_size = udp->_rx_buf->len - udp->_rx_buf_offset;
	if (max_size == 0) {
		udp->_blocker = CALLER;
		return 0;
	}
	size = (size < max_size) ? size : max_size;

	memcpy(dst, udp->_rx_buf->payload + udp->_rx_buf_offset, size);
	udp->_rx_buf_offset += size;

	return size;
}

bool squawk_udp_send(udp_context_t* udp, ip_addr_t* addr, uint16_t port, char* src, size_t size) {
	struct pbuf* tx = pbuf_alloc(PBUF_TRANSPORT, size, PBUF_RAM);
	if (!tx) {
		return false;
	}
	uint8_t* dst = tx->payload;
	memcpy(dst, src, size);
	
	if (!addr) {
		addr = &udp->_pcb->remote_ip;
		port = udp->_pcb->remote_port;
	}
	err_t err = udp_sendto(udp->_pcb, tx, addr, port);
	pbuf_free(tx);
	return err == ERR_OK;
}
