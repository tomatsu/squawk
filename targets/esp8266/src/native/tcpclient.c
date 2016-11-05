#include <stdbool.h>
#include "tcp.h"
#include "udp.h"
#include "unused.h"
#include "classes.h"

/* mbedtls */
#include "mbedtls/net_sockets.h"
#include "mbedtls/ssl.h"
#include "mbedtls/entropy.h"
#include "mbedtls/ctr_drbg.h"
#include "mbedtls/certs.h"
#include "mbedtls/x509.h"
#include "mbedtls/error.h"
#include "mbedtls/debug.h"
#include "mbedtls/timing.h"


extern int os_printf_plus(const char *format, ...)  __attribute__ ((format (printf, 1, 2)));
#define printf os_printf_plus
#define malloc os_malloc
#define free os_free

#define DEFINE(type, name, args, body) Mask_ ## name ( type Java_ ## name args { body } )

/*
 * NetUtil
 */
DEFINE(int, com_sun_squawk_io_NetUtil_resolve, (char* host), \
    int len = getArrayLength(host); \
  	char buf[32]; \
	char* p; \
	if (len < sizeof(buf)) { \
	  p = buf; \
	} else { \
	  p = malloc(len + 1); \
	  if (!p) { \
		 return 0; \
	  } \
	} \
	memcpy(p, host, len); \
	p[len] = 0; \
	ip_addr_t addr; \
	int n = squawk_resolve(buf, &addr); \
	int result = (n == 1) ? addr.addr : 0; \
	if (p != buf) { \
	    free(p); \
	} \
	return result; \
)

/*
 * Socket
 */
DEFINE(int, com_sun_squawk_io_Socket_getlocaladdr, (int handle),  \
	return squawk_tcp_getlocaladdr((tcp_connection_t*)handle); \
)

DEFINE(int, com_sun_squawk_io_Socket_getlocalport, (int handle), \
	return squawk_tcp_getlocalport((tcp_connection_t*)handle); \
)

DEFINE(int, com_sun_squawk_io_Socket_connect0, (int addr, int port), \
	ip_addr_t ipaddr; \
	ipaddr.addr = addr; \
	return (int)squawk_tcp_connect(&ipaddr, port);\
)

DEFINE(int, com_sun_squawk_io_Socket_read0, (int handle, void* buf, int off, int len), \
	return squawk_tcp_read((tcp_connection_t*)handle, buf + off, len); \
)

DEFINE(int, com_sun_squawk_io_Socket_read1, (int handle), \
	return squawk_tcp_read1((tcp_connection_t*)handle); \
)

DEFINE(int, com_sun_squawk_io_Socket_write0, (int handle, void* buf, int off, int len), \
	return squawk_tcp_write((tcp_connection_t*)handle, buf + off, len); \
)

DEFINE(int, com_sun_squawk_io_Socket_write1, (int handle, uint8_t value), \
	return squawk_tcp_write((tcp_connection_t*)handle, &value, 1); \
)

DEFINE(int, com_sun_squawk_io_Socket_available0, (int handle), \
	return squawk_tcp_available((tcp_connection_t*)handle); \
)

DEFINE(int, com_sun_squawk_io_Socket_close0, (int handle), \
	squawk_tcp_close((tcp_connection_t*)handle); \
)

/*
 * ServerSocket
 */
DEFINE(int, com_sun_squawk_io_ServerSocket_create, (int port), \
	return (int)squawk_tcp_create_server(0, port); \
)

DEFINE(int, com_sun_squawk_io_ServerSocket_accept0, (int handle), \
	struct tcp_pcb* pcb = (struct tcp_pcb*)handle; \
	return squawk_tcp_accept(pcb); \
)

/*
 * DatagramSocket
 */
DEFINE(int, com_sun_squawk_io_DatagramSocket_create0, (int port), \
	return (int)squawk_udp_create(port); \
)

DEFINE(int, com_sun_squawk_io_DatagramSocket_connect0, (int handle, int addr, int port), \
)

DEFINE(int, com_sun_squawk_io_DatagramSocket_close0, (int handle), \
)

DEFINE(int, com_sun_squawk_io_DatagramSocket_send0, (int handle, uint8_t* buf, int off, int len), \
	udp_context_t* udp = (udp_context_t*)handle; \
	return squawk_udp_send(udp, &udp->_pcb->remote_ip, udp->_pcb->remote_port, buf + off, len); \
)

DEFINE(int, com_sun_squawk_io_DatagramSocket_send1, (int handle, int addr, int port, uint8_t* buf, int off, int len), \
	udp_context_t* udp = (udp_context_t*)handle; \
	ip_addr_t a; \
    a.addr = addr; \
	return squawk_udp_send(udp, &a, port, buf + off, len); \
)

DEFINE(int, com_sun_squawk_io_DatagramSocket_receive0, (int handle, uint8_t* buf, int off, int len), \
	return squawk_udp_read((udp_context_t*)handle, buf + off, len); \
)


/*
 * SSLSocket
 */
static mbedtls_ssl_config ssl_conf;
static mbedtls_ctr_drbg_context ctr_drbg;
static mbedtls_entropy_context entropy;
static mbedtls_x509_crt cacert;

static bool ssl_init() {
	int ret;
	
	mbedtls_ssl_config_init(&ssl_conf);
    if ((ret = mbedtls_ssl_config_defaults(&ssl_conf,
                    MBEDTLS_SSL_IS_CLIENT,
                    MBEDTLS_SSL_TRANSPORT_STREAM,
                    MBEDTLS_SSL_PRESET_DEFAULT)) != 0) {
        printf("failed\n  ! mbedtls_ssl_config_defaults returned -0x%x\n\n", -ret);
		return false;
    }
	mbedtls_ssl_conf_authmode(&ssl_conf, MBEDTLS_SSL_VERIFY_REQUIRED);

    mbedtls_entropy_init(&entropy);
    mbedtls_ctr_drbg_init(&ctr_drbg);
    if ((ret = mbedtls_ctr_drbg_seed(&ctr_drbg, mbedtls_entropy_func, &entropy, NULL, 0)) != 0) {
        printf("failed\n  ! mbedtls_ctr_drbg_seed returned %d\n", ret);
		return false;
    }
    mbedtls_ssl_conf_rng(&ssl_conf, mbedtls_ctr_drbg_random, &ctr_drbg);
//    mbedtls_ssl_conf_dbg(&ssl_conf, my_debug, stdout);
//    mbedtls_ssl_conf_read_timeout(&ssl_conf, 0);

    mbedtls_x509_crt_init(&cacert);

// TODO
//	ret = mbedtls_x509_crt_parse_path(&cacert, "/cacerts");
//	mbedtls_ssl_conf_ca_chain(&ssl_conf, &cacert, NULL);
	
	return true;
}

DEFINE(int, com_sun_squawk_io_SSLSocket_init, (),	\
	   return ssl_init();
)

DEFINE(int, com_sun_squawk_io_SSLSocket_createSSLContext, (int handle), \
	   mbedtls_ssl_context* context = (mbedtls_ssl_context*)malloc(sizeof(mbedtls_ssl_context));
	   if (!context) {
		   return 0;
	   }
	   mbedtls_ssl_init(context);
	   if ((ret = mbedtls_ssl_setup(context, &ssl_conf)) != 0) {
		   free(context);
		   return 0;
	   }
	   mbedtls_ssl_set_bio(context, (void*)handle, squawk_tcp_write, squawk_tcp_read, NULL);

	   int ret;
	   while ((ret = mbedtls_ssl_handshake(context)) != 0) {
		   if (ret != MBEDTLS_ERR_SSL_WANT_READ && ret != MBEDTLS_ERR_SSL_WANT_WRITE) {
			   printf("failed\n  ! mbedtls_ssl_handshake returned -0x%x\n", -ret);
			   if (ret == MBEDTLS_ERR_X509_CERT_VERIFY_FAILED) {
				   printf("Unable to verify the server's certificate.\n");
			   }
			   free(context);
			   return 0;
		   }
	   }
	   return context;
)

DEFINE(int, com_sun_squawk_io_SSLSocket_read0, (int context, uint8_t* buf, int off, int len), \
	   return mbedtls_ssl_read((mbedtls_ssl_context*)context, buf + off, len);	\
)

DEFINE(int, com_sun_squawk_io_SSLSocket_write0, (int context, uint8_t* buf, int off, int len), \
	   return mbedtls_ssl_write((mbedtls_ssl_context*)context, buf + off, len); \
)

DEFINE(int, com_sun_squawk_io_SSLSocket_close0, (int context), \
)
