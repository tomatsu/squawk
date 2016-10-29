#include <lwip/tcp.h>
#include <lwip/opt.h>
#include <lwip/err.h>
#include <lwip/ip_addr.h>
#include <lwip/dns.h>
#include <lwip/netif.h>
#include <stdint.h>
#include <osapi.h>
#include <mem.h>
#include "tcp.h"
#include "events.h"

extern int os_printf_plus(const char *format, ...)  __attribute__ ((format (printf, 1, 2)));
#define printf os_printf_plus
#define malloc os_malloc

static uint16_t _localPort = 1;

#define CALLER (squawk_threadID() + 1)

static err_t recv_cb(void *arg, struct tcp_pcb *tpcb, struct pbuf *p, err_t err) {
	tcp_connection_t* conn = (tcp_connection_t*)arg;
	int state = tpcb->state;
	
	if (state == CLOSE_WAIT) {
		tcp_recv(tpcb, NULL);
		tcp_sent(tpcb, NULL);
		tcp_err(tpcb, NULL);
		if (conn->_blocker) {
			squawk_post_event(conn->_blocker, READ_READY_EVENT, -1);
		}
		return ERR_OK;
	}
	
    if (p == 0) { // connection closed
        if (conn->_pcb) {
            tcp_arg(conn->_pcb, NULL);
            tcp_sent(conn->_pcb, NULL);
            tcp_recv(conn->_pcb, NULL);
            tcp_err(conn->_pcb, NULL);
            tcp_poll(conn->_pcb, NULL, 0);
            tcp_abort(conn->_pcb);
            conn->_pcb = 0;
        }
        return ERR_ABRT;
    }

    if (conn->_rx_buf) {
        pbuf_cat(conn->_rx_buf, p);
    } else {
        conn->_rx_buf = p;
        conn->_rx_buf_offset = 0;
    }
	if (conn->_blocker) {
		squawk_post_event(conn->_blocker, READ_READY_EVENT, 0);
	}
    return ERR_OK;
}

static err_t sent_cb(void *arg, struct tcp_pcb *tpcb, uint16_t len) {
	tcp_connection_t* conn = (tcp_connection_t*)arg;
	if (conn->_blocker) {
		size_t can_send = tcp_sndbuf(conn->_pcb);
		if (can_send > 0) {
			squawk_post_event(conn->_blocker, WRITE_READY_EVENT, 0);
		}
	}
    return ERR_OK;
}

//static err_t poll_cb(void *arg, struct tcp_pcb *tpcb) {
//     return ERR_OK;
//}

static err_t connected_cb(void *arg, struct tcp_pcb *pcb, err_t err) {
	tcp_connection_t* conn = (tcp_connection_t*)arg;	
	conn->_pcb = (struct tcp_pcb*)pcb;
    tcp_setprio(pcb, TCP_PRIO_MIN);
    tcp_recv(pcb, recv_cb);
    tcp_sent(pcb, sent_cb);
//    tcp_poll(pcb, poll_cb, 1);

	squawk_post_event(conn->_blocker, CONNECTED_EVENT, (pcb && err == ERR_OK) ? 0 : -1);
}

static void err_cb(void *arg, err_t err) {
	tcp_connection_t* conn = (tcp_connection_t*)arg;
	if (conn->_blocker) {
		squawk_post_event(conn->_blocker, conn->_operation, -1);
	}
}

tcp_connection_t* squawk_tcp_connect(ip_addr_t* addr, int port) {
	struct netif* interface = ip_route(addr);
    if (!interface) {
        printf("no route to host\r\n");
        return NULL;
    }

    struct tcp_pcb* pcb = tcp_new();
	if (pcb == NULL) {
		return NULL;
	}
    pcb->local_port = _localPort++;
	tcp_connection_t* conn = (tcp_connection_t*)malloc(sizeof(tcp_connection_t));
	if (conn == NULL) {
		return NULL;
	}
	conn->_rx_buf = 0;
	conn->_rx_buf_offset = 0;
	conn->_operation = CONNECTED_EVENT;
	conn->_blocker = CALLER;
	
    tcp_arg(pcb, conn);
    tcp_err(pcb, err_cb);
    int err = tcp_connect(pcb, addr, (uint16_t)port, connected_cb);
	if (err != ERR_OK) {
		printf("connect error %d\n", err);
	}
	return conn;
}


err_t squawk_tcp_close(tcp_connection_t* conn)
{
    err_t err = ERR_OK;
    if (conn->_pcb) {
        tcp_arg(conn->_pcb, NULL);
        tcp_sent(conn->_pcb, NULL);
        tcp_recv(conn->_pcb, NULL);
        tcp_err(conn->_pcb, NULL);
        tcp_poll(conn->_pcb, NULL, 0);
        err = tcp_close(conn->_pcb);
        if (err != ERR_OK) {
            tcp_abort(conn->_pcb);
            err = ERR_ABRT;
        }
        conn->_pcb = 0;
    }
    return err;
}

int squawk_tcp_available(tcp_connection_t* conn) {
    if (!conn->_rx_buf) {
        return 0;
    }
    return conn->_rx_buf->tot_len - conn->_rx_buf_offset;
}

static void consume(tcp_connection_t* conn, size_t size) {
    ptrdiff_t left = conn->_rx_buf->len - conn->_rx_buf_offset - size;
    if (left > 0) {
        conn->_rx_buf_offset += size;
    } else if (!conn->_rx_buf->next) {
        if (conn->_pcb) {
			tcp_recved(conn->_pcb, conn->_rx_buf->len);
        }
        pbuf_free(conn->_rx_buf);
        conn->_rx_buf = 0;
        conn->_rx_buf_offset = 0;
    } else {
        struct pbuf* head = conn->_rx_buf;
        conn->_rx_buf = conn->_rx_buf->next;
        conn->_rx_buf_offset = 0;
        pbuf_ref(conn->_rx_buf);
        if (conn->_pcb) {
            tcp_recved(conn->_pcb, head->len);
        }
        pbuf_free(head);
    }
}

int squawk_tcp_read1(tcp_connection_t* conn) {
    if (!conn->_pcb) {
        return -1; // EOF
    }
    if (!conn->_rx_buf) {
		conn->_operation = READ_READY_EVENT;
		conn->_blocker = CALLER;
        return -2; // blocking
    }
    size_t max_size = conn->_rx_buf->tot_len - conn->_rx_buf_offset;
	if (max_size == 0) {
		conn->_operation = READ_READY_EVENT;
		conn->_blocker = CALLER;
		return -2; // blocking
	}
    uint8_t c = ((uint8_t*)conn->_rx_buf->payload)[conn->_rx_buf_offset];
    consume(conn, 1);
    return c;
}

int squawk_tcp_read(tcp_connection_t* conn, char* dst, size_t size) {
    if (!conn->_pcb) {
        return -1;
    }
    if (!conn->_rx_buf) {
		conn->_operation = READ_READY_EVENT;
		conn->_blocker = CALLER;
        return 0;
    }

    size_t max_size = conn->_rx_buf->tot_len - conn->_rx_buf_offset;
	if (max_size == 0) {
		conn->_operation = READ_READY_EVENT;
		conn->_blocker = CALLER;
		return 0;
	}
    size = (size < max_size) ? size : max_size;

    size_t size_read = 0;
    while (size > 0) {
        size_t buf_size = conn->_rx_buf->len - conn->_rx_buf_offset;
        size_t copy_size = (size < buf_size) ? size : buf_size;
        os_memcpy(dst, conn->_rx_buf->payload + conn->_rx_buf_offset, copy_size);
        dst += copy_size;
        consume(conn, copy_size);
        size -= copy_size;
        size_read += copy_size;
    }
    return size_read;
}

int squawk_tcp_write(tcp_connection_t* conn, uint8_t* data, size_t size) {
    if (!conn->_pcb) {
        return -1;
    }

    size_t can_send = tcp_sndbuf(conn->_pcb);
    if (conn->_pcb->snd_queuelen >= TCP_SND_QUEUELEN) {
        can_send = 0;
    }
    size_t will_send = (can_send < size) ? can_send : size;
    if (will_send) {
        err_t err = tcp_write(conn->_pcb, data, will_send, TCP_WRITE_FLAG_COPY);
        if (err == ERR_OK) {
            tcp_output(conn->_pcb);
        }
    } else {
		conn->_operation = WRITE_READY_EVENT;
		conn->_blocker = CALLER;
	}
    return will_send;
}

static void dns_found_cb(const char *name, ip_addr_t *ipaddr, void *arg) {
	uint32_t caller = (uint32_t)arg;
	squawk_post_event(caller, RESOLVED_EVENT, ipaddr->addr);
}

int squawk_resolve(char* hostname, ip_addr_t* ipaddr) {
	ip_addr_t addr;
	uint32_t arg;

	if (ipaddr_aton(hostname, &addr)) {
		ipaddr->addr = addr.addr;
		return 1;
	}

	arg = CALLER;
    err_t err = dns_gethostbyname(hostname, &addr, dns_found_cb, arg);
    if (err == ERR_OK) {
		ipaddr->addr = addr.addr;
		return 1;
	}
	if (err == ERR_INPROGRESS) {
		return 0;
	}
	return -1;
}

uint32_t squawk_tcp_getlocaladdr(tcp_connection_t* conn) {
	if (!conn->_pcb) {
		return 0;
	}
	return conn->_pcb->local_ip.addr;
}


uint16_t squawk_tcp_getlocalport(tcp_connection_t* conn) {
	if (!conn->_pcb) {
		return 0;
	}
	return conn->_pcb->local_port;
}


/*
 * server
 */

struct tcp_pcb* squawk_tcp_create_server(int _addr, int _port) {
    struct tcp_pcb* pcb = tcp_new();	
    if (!pcb) {
        return NULL;
	}
    ip_addr_t local_addr;
    local_addr.addr = (uint32_t) _addr;
    pcb->so_options |= SOF_REUSEADDR;
    err_t err = tcp_bind(pcb, &local_addr, _port);
    if (err != ERR_OK) {
        tcp_close(pcb);
        return NULL;
    }
	return pcb;
}

static int8_t accept_cb(void *arg, struct tcp_pcb* newpcb, int8_t err) {
	tcp_connection_t* conn = (tcp_connection_t*)arg;
	conn->_pcb = newpcb;
    tcp_accepted(conn->_pcb);
    tcp_setprio(conn->_pcb, TCP_PRIO_MIN);
    tcp_recv(conn->_pcb, recv_cb);
    tcp_sent(conn->_pcb, sent_cb);
    tcp_err(conn->_pcb, err_cb);
	squawk_post_event(conn->_blocker, ACCEPTED_EVENT, 0);
    return ERR_OK;
}

tcp_connection_t* squawk_tcp_accept(struct tcp_pcb* pcb) {
    struct tcp_pcb* listen_pcb = tcp_listen(pcb);
    if (!listen_pcb) {
        tcp_close(pcb);
        return NULL;
    }
	tcp_connection_t* conn = (tcp_connection_t*)malloc(sizeof(tcp_connection_t));
	if (!conn) {
		return NULL;
	}
	conn->_rx_buf = 0;
	conn->_rx_buf_offset = 0;
	conn->_operation = ACCEPTED_EVENT;
	conn->_blocker = CALLER;
	
    tcp_accept(listen_pcb, accept_cb);
    tcp_arg(listen_pcb, conn);
    tcp_err(pcb, err_cb);
	return conn;
}

