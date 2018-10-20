#define __GNU_SOURCE

#include "classes.h"
#include "events.h"
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <netdb.h>
#include <errno.h>
#include <fcntl.h>
#include <signal.h>
#include <poll.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#define READ_READY_EVENT 11
#define WRITE_READY_EVENT 12

void sigio_handler(int sig, siginfo_t* info, void *ptr) {
    struct pollfd pfd;

    if (sig == SIGIO) {
	pfd.fd = info->si_fd;
	pfd.events = POLLIN | POLLOUT;
	if (poll(&pfd, 1, 0) > 0) {
	    if (pfd.revents & POLLIN) {
		squawk_post_event(0, READ_READY_EVENT, 1);
	    }
	    if (pfd.revents & POLLOUT) {
		squawk_post_event(0, WRITE_READY_EVENT, 1);
	    }
	}
    }
}

void install_signal_handler() {
    struct sigaction sa;

    sigemptyset(&sa.sa_mask);
    sa.sa_flags = SA_RESTART | SA_SIGINFO;
    sa.sa_sigaction = sigio_handler;    
    if (sigaction(SIGIO, &sa, NULL) == -1){
	fprintf(stderr, "error: sigaction");
	exit(-1);
    }	
}

static int set_nonblocking(int s) {
    if (fcntl(s, F_SETOWN, getpid()) == -1){
	fprintf(stderr, "fcntl(F_SETOWN) failed %d\n", errno);
	return 0;
    }
    
    int flags = fcntl(0, F_GETFL);
    if (fcntl(s, F_SETFL, flags | O_ASYNC | O_NONBLOCK) == -1) {
	fprintf(stderr, "fcntl(F_SETFL) failed %d\n", errno);
	return 0;
    }
    if (fcntl(s, F_SETSIG, SIGIO) == -1) {
	fprintf(stderr, "fcntl(F_SETSIG) failed %d\n", errno);
	return 0;
    }
    return 1;
}

/*
 * NetUtil
 */
int Java_com_sun_squawk_io_NetUtil_resolve(char* hostname) {
    int len = getArrayLength(hostname);
    struct hostent *host;
    int32_t result;
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
    memcpy(p, hostname, len);
    p[len] = 0;
    host = gethostbyname(p);
    result = 0;
    if (host != NULL) {
	if (host->h_addrtype == AF_INET) {
	    for (int i = 0; host->h_addr_list[i] != NULL; i++) {
		struct in_addr* addr = (struct in_addr*)host->h_addr_list[i];
		result = addr->s_addr;
		break;
	    }
	}
    }

    if (p != buf) {
	free(p);
    }
    return result;
}


/*
 * Socket
 */
int Java_com_sun_squawk_io_Socket_getlocaladdr(int handle) {
    struct sockaddr_in addr;
    socklen_t addrlen;
    
    if (getsockname(handle, (struct sockaddr *)&addr, &addrlen)) {
        return -1;
    }
    return addr.sin_addr.s_addr;
}

int Java_com_sun_squawk_io_Socket_getlocalport(int handle) {
    struct sockaddr_in addr;
    socklen_t addrlen;
    
    if (getsockname(handle, (struct sockaddr *)&addr, &addrlen)) {
        return -1;
    }
    return (int)addr.sin_port;
}

int Java_com_sun_squawk_io_Socket_connect0(int addr, int port) {
    struct sockaddr_in saddr;
    int addrlen;
    int s, n;

    if ((s = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
	fprintf(stderr, "socket failed %d\n", errno);
	return 0;
    }
    saddr.sin_family = AF_INET;
    saddr.sin_addr.s_addr = addr;
    saddr.sin_port = htons((unsigned short)port);
    addrlen = sizeof(saddr);
    n = connect(s, &saddr, addrlen);
    if (n == -1) {
	fprintf(stderr, "connect failed %d\n", errno);	
    }
    if (!set_nonblocking(s)) {
	return 0;
    }
    return s;
}

static int socket_read(int s, char* buf, int len) {
    int n = read(s, buf, len);
    if (n != -1) {
	return n;
    }
    if (errno == EAGAIN || errno == EWOULDBLOCK) {
	return -2;
    } else {
	fprintf(stderr, "read failed %d\n", errno);
	return -1;
    }
}

static int socket_write(int s, char* buf, int len) {
    int n = write(s, buf, len);
    if (n != -1) {
	return n;
    }
    if (errno == EAGAIN || errno == EWOULDBLOCK) {
	return -2;
    } else {
	fprintf(stderr, "write failed %d\n", errno);
	return -1;
    }
}

int Java_com_sun_squawk_io_Socket_read0(int handle, void* buf, int off, int len) {
    return socket_read(handle, (char*)buf + off, len);
}

int Java_com_sun_squawk_io_Socket_read1(int handle) {
    uint8_t c;
    int n = socket_read(handle, &c, 1);
    if (n <= 0) {
	return n;
    } else {
	return (int)c;
    }
}

int Java_com_sun_squawk_io_Socket_write0(int handle, void* buf, int off, int len) {
    return socket_write(handle, (char*)buf + off, len);
}

int Java_com_sun_squawk_io_Socket_write1(int handle, int value) {
    int8_t c = (int8_t)value;
    return socket_write(handle, (char*)&c, 1);
}

int Java_com_sun_squawk_io_Socket_available0(int handle) {
    int n;

    if (handle < 0 || ioctl(handle, FIONREAD, &n) < 0) {
	n = 0;
    } 
    return n;    
}

int Java_com_sun_squawk_io_Socket_close0(int handle) {
    return close(handle);
}

/*
 * ServerSocket
 */
int Java_com_sun_squawk_io_ServerSocket_create(int port) {
    struct sockaddr_in addr;
    int addrlen;
    int s;
    
    if ((s = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
	fprintf(stderr, "socket failed %d\n", errno);
	return 0;
    }
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = INADDR_ANY;
    addr.sin_port = htons((unsigned short)port);

    if (bind(s, (struct sockaddr *)&addr, sizeof(addr)) < 0) {
	fprintf(stderr, "bind failed %d\n", errno);
	return 0;
    }
    
    if (listen(s, 5) < 0) {
	fprintf(stderr, "listen failed %d\n", errno);
	return 0;
    }
    if (!set_nonblocking(s)) {
	return 0;
    }
    return s;
}

int Java_com_sun_squawk_io_ServerSocket_accept0(int handle) {
    struct sockaddr_in addr;
    int addrlen = sizeof(addr);
    int cs = accept(handle, (struct sockaddr *)&addr, &addrlen);
    if (cs != -1) {
	if (!set_nonblocking(cs)) {
	    return -1;
	}
	return cs;
    }
    if (errno == EAGAIN || errno == EWOULDBLOCK) {
	return -2;
    } else {
	fprintf(stderr, "accept failed %d\n", errno);
	return -1;
    }
}

/*
 * DatagramSocket
 */

int Java_com_sun_squawk_io_DatagramSocket_create0(int port) {
    struct sockaddr_in saddr;
    int addrlen;
    int s;

    if ((s = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
	fprintf(stderr, "socket failed %d\n", errno);
	return 0;
    }
    saddr.sin_family = AF_INET;
    saddr.sin_addr.s_addr = INADDR_ANY;
    saddr.sin_port = htons((unsigned short)port);
    addrlen = sizeof(saddr);
    if (bind(s, (struct sockaddr *)&saddr, sizeof(saddr)) < 0) {
	fprintf(stderr, "bind failed %d\n", errno);
	return 0;
    }
    if (!set_nonblocking(s)) {
	return 0;
    }
    return s;
}

int Java_com_sun_squawk_io_DatagramSocket_connect0(int handle, int addr, int port) {
    struct sockaddr_in saddr;
    int addrlen;
    int n;
    
    saddr.sin_family = AF_INET;
    saddr.sin_addr.s_addr = addr;
    saddr.sin_port = htons((unsigned short)port);
    addrlen = sizeof(saddr);
    n = connect(handle, &saddr, addrlen);
    if (n == -1) {
	fprintf(stderr, "connect failed %d\n", errno);	
    }
    return n;
}

int Java_com_sun_squawk_io_DatagramSocket_close0(int handle) {
    return close(handle);
}

int Java_com_sun_squawk_io_DatagramSocket_send0(int handle, void *buf, int off, int len) {
    int n = send(handle, (char*)buf + off, len, 0);
    if (n != -1) {
	return n;
    }
    if (errno == EAGAIN || errno == EWOULDBLOCK) {
	return -2;
    } else {
	fprintf(stderr, "send failed %d\n", errno);
	return -1;
    }
}

int Java_com_sun_squawk_io_DatagramSocket_send1(int handle, int addr, int port, void *buf, int off, int len) {
    struct sockaddr_in saddr;
    
    saddr.sin_family = AF_INET;
    saddr.sin_addr.s_addr = addr;
    saddr.sin_port = htons((unsigned short)port);
    
    int n = sendto(handle, (char*)buf + off, len, 0, &saddr, sizeof(saddr));
    if (n != -1) {
	return n;
    }
    if (errno == EAGAIN || errno == EWOULDBLOCK) {
	return -2;
    } else {
	fprintf(stderr, "sendto failed %d\n", errno);
	return -1;
    }
}

int Java_com_sun_squawk_io_DatagramSocket_receive0(int handle, void *buf, int off, int len) {
    int n = recv(handle, (char*)buf + off, len, 0);
    if (n != -1) {
	return n;
    }
    if (errno == EAGAIN || errno == EWOULDBLOCK) {
	return -2;
    } else {
	fprintf(stderr, "recv failed %d\n", errno);
	return -1;
    }
}
