#include <lwip/tcp.h>
#include <lwip/err.h>
#include <lwip/ip_addr.h>
#include <sys/types.h>
#include <stdbool.h>

typedef struct {
	struct tcp_pcb* _pcb;
	struct pbuf* _rx_buf;
	size_t _rx_buf_offset;
	int _operation;
	int _blocker;
} tcp_connection_t;

extern tcp_connection_t* squawk_tcp_connect(ip_addr_t* addr, int port);
extern err_t squawk_tcp_close(tcp_connection_t* conn);
extern int squawk_tcp_available(tcp_connection_t* conn);
extern int squawk_tcp_read1(tcp_connection_t* conn);
extern int squawk_tcp_read(tcp_connection_t* conn, char* dst, size_t size);
extern int squawk_tcp_write(tcp_connection_t* conn, uint8_t* data, size_t size);
extern int squawk_resolve(char* hostname, ip_addr_t* ipaddr);
extern uint32_t squawk_tcp_getlocaladdr(tcp_connection_t* conn);
extern uint16_t squawk_tcp_getlocalport(tcp_connection_t* conn);
extern struct tcp_pcb* squawk_tcp_create_server(int _addr, int _port);
extern tcp_connection_t* squawk_tcp_accept(struct tcp_pcb* pcb);
