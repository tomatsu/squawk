#include "tcp.h"

int Java_esp8266_Resolver_resolve(char* host) {
	ip_addr_t addr;
	int n = squawk_resolve(host, &addr);
	if (n == ERR_OK) {
		return addr.addr;
	}
	return 0;
}

int Java_esp8266_TCPClient_getlocaladdr(int handle) {
	return squawk_tcp_getlocaladdr((tcp_connection_t*)handle);
}

int Java_esp8266_TCPClient_getlocalport(int handle) {
	return squawk_tcp_getlocalport((tcp_connection_t*)handle);
}

int Java_esp8266_TCPClient_connect0(int addr, int port) {
	ip_addr_t ipaddr;
	ipaddr.addr = addr;
	return (int)squawk_tcp_connect(&ipaddr, port);
}

int Java_esp8266_TCPClient_read0(int handle, void* buf, int off, int len) {
	return squawk_tcp_read((tcp_connection_t*)handle, buf + off, len);
}

int Java_esp8266_TCPClient_write0(int handle, void* buf, int off, int len) {
	return squawk_tcp_write((tcp_connection_t*)handle, buf + off, len);
}

int Java_esp8266_TCPClient_available0(int handle) {
	return squawk_tcp_available((tcp_connection_t*)handle);
}

int Java_esp8266_TCPClient_close0(int handle) {
	squawk_tcp_close((tcp_connection_t*)handle);
}
