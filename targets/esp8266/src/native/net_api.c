#include <stdbool.h>
#include <stdint.h>
#include "tcp.h"
#include "udp.h"
#include "unused.h"
#include "classes.h"

extern int os_printf_plus(const char *format, ...)  __attribute__ ((format (printf, 1, 2)));
#define printf os_printf_plus
#define malloc os_malloc
#define free os_free

/*
 * NetUtil
 */
int Java_com_sun_squawk_io_NetUtil_resolve(char* host) {
    int len = getArrayLength(host);
  	char buf[32];
	char* p;
	if (len < sizeof(buf)) {
		p = buf;
	} else {
		p = malloc(len + 1);
		if (!p) {
			return 0;
		}
	}
	memcpy(p, host, len);
	p[len] = 0;
	ip_addr_t addr;
	int n = squawk_resolve(buf, &addr);
	int result = (n == 1) ? addr.addr : 0;
	if (p != buf) {
	    free(p);
	}
	return result;
}

/*
 * Socket
 */
int Java_com_sun_squawk_io_Socket_getlocaladdr(int handle) {
	return squawk_tcp_getlocaladdr((tcp_connection_t*)handle);
}

int Java_com_sun_squawk_io_Socket_getlocalport(int handle) {
	return squawk_tcp_getlocalport((tcp_connection_t*)handle);
}

int Java_com_sun_squawk_io_Socket_connect0(int addr, int port) {
	ip_addr_t ipaddr;
	ipaddr.addr = addr;
	return (int)squawk_tcp_connect(&ipaddr, port);
}

int Java_com_sun_squawk_io_Socket_read0(int handle, void* buf, int off, int len) {
	return squawk_tcp_read((tcp_connection_t*)handle, buf + off, len);
}

int Java_com_sun_squawk_io_Socket_read1(int handle) {
	return squawk_tcp_read1((tcp_connection_t*)handle);
}

int Java_com_sun_squawk_io_Socket_write0(int handle, void* buf, int off, int len) {
	return squawk_tcp_write((tcp_connection_t*)handle, buf + off, len);
}

int Java_com_sun_squawk_io_Socket_write1(int handle, uint8_t value) {
	return squawk_tcp_write((tcp_connection_t*)handle, &value, 1);
}

int Java_com_sun_squawk_io_Socket_available0(int handle) {
	return squawk_tcp_available((tcp_connection_t*)handle);
}

int Java_com_sun_squawk_io_Socket_close0(int handle) {
	squawk_tcp_close((tcp_connection_t*)handle);
}

/*
 * ServerSocket
 */
int Java_com_sun_squawk_io_ServerSocket_create(int port) {
	return (int)squawk_tcp_create_server(0, port);
}

int Java_com_sun_squawk_io_ServerSocket_accept0(int handle) {
	struct tcp_pcb* pcb = (struct tcp_pcb*)handle;
	return squawk_tcp_accept(pcb);
}

/*
 * DatagramSocket
 */
int Java_com_sun_squawk_io_DatagramSocket_create0(int port) {
	return (int)squawk_udp_create(port);
}

int Java_com_sun_squawk_io_DatagramSocket_connect0(int handle, int addr, int port) {
	ip_addr_t ipaddr;
	ipaddr.addr = addr;
	return (int)squawk_udp_connect((udp_context_t*)handle, &ipaddr, port);
}

int Java_com_sun_squawk_io_DatagramSocket_close0(int handle) {
	free((udp_context_t*)handle);
}

int Java_com_sun_squawk_io_DatagramSocket_send0(int handle, uint8_t* buf, int off, int len) {
	udp_context_t* udp = (udp_context_t*)handle;
	return squawk_udp_send(udp, &udp->_pcb->remote_ip, udp->_pcb->remote_port, buf + off, len);
}

int Java_com_sun_squawk_io_DatagramSocket_send1(int handle, int addr, int port, uint8_t* buf, int off, int len) {
	udp_context_t* udp = (udp_context_t*)handle;
	ip_addr_t a;
    a.addr = addr;
	return squawk_udp_send(udp, &a, port, buf + off, len);
}

int Java_com_sun_squawk_io_DatagramSocket_receive0(int handle, uint8_t* buf, int off, int len) {
	return squawk_udp_read((udp_context_t*)handle, buf + off, len);
}
