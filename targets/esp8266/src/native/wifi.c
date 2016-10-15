#include <stdbool.h>
#include <stdint.h>
#include <string.h>
#include <user_interface.h>
#include <mem.h>
//#include <uart.h>
#include <osapi.h>
#include "classes.h"
#include "unused.h"
#define ICACHE_RAM_ATTR __attribute__((section(".iram0.text")))

//#define printf os_printf_plus
#define printf ets_uart_printf 
#define malloc os_malloc

#define DEFINE(type, name, args, body) Mask_esp8266_Wifi_ ## name ( type Java_esp8266_Wifi_ ## name args { body } )

typedef bool jboolean;
typedef int8_t jbyte;

jbyte Java_esp8266_Wifi_get_1opmode() {
	return (int8_t)wifi_get_opmode();
}

jbyte Java_esp8266_Wifi_get_1opmode_1default() {
	return (int8_t)wifi_get_opmode_default();
}

jboolean Java_esp8266_Wifi_set_1opmode(jbyte mode) {
	return wifi_set_opmode(mode);
}

jboolean Java_esp8266_Wifi_set_1opmode_1current(jbyte mode) {
	return wifi_set_opmode_current(mode);
}

DEFINE(jboolean, station_1get_1config, (void* arg), \
	struct station_config config; \
	int r = wifi_station_get_config(&config); \
	if (r == false) { \
		return r; \
	} \
	jbyte* ssid = esp8266_Wifi_StationConfig_ssid(arg); \
	int ssid_len = getArrayLength(ssid); \
	if (ssid_len < sizeof(config.ssid)) { \
		return false; \
	} \
	memcpy(ssid, &config.ssid, sizeof(config.ssid)); \
	\
	int8_t* password = esp8266_Wifi_StationConfig_password(arg); \
	int password_len = getArrayLength(password); \
	if (password_len < sizeof(config.password)) {\
		return false; \
	} \
	memcpy(password, &config.password, sizeof(config.password)); \
	\
	set_esp8266_Wifi_StationConfig_bssid_set(arg, config.bssid_set); \
	\
	int8_t* bssid = esp8266_Wifi_StationConfig_bssid(arg); \
	int bssid_len = getArrayLength(bssid); \
	if (bssid_len < sizeof(config.bssid)) { \
		return false; \
	} \
	memcpy(bssid, &config.bssid, sizeof(config.bssid)); \
	return true; \
)

DEFINE(jboolean, station_1get_1config_1default, (void* arg), \
	struct station_config config; \
	int r = wifi_station_get_config_default(&config); \
	if (r == false) { \
		return r; \
	} \
	jbyte* ssid = esp8266_Wifi_StationConfig_ssid(arg); \
	int ssid_len = getArrayLength(ssid); \
	if (ssid_len < sizeof(config.ssid)) { \
		return false; \
	} \
	memcpy(ssid, &config.ssid, sizeof(config.ssid)); \
	\
	int8_t* password = esp8266_Wifi_StationConfig_password(arg); \
	int password_len = getArrayLength(password); \
	if (password_len < sizeof(config.password)) {\
		return false; \
	} \
	memcpy(password, &config.password, sizeof(config.password)); \
	\
	set_esp8266_Wifi_StationConfig_bssid_set(arg, config.bssid_set); \
	\
	int8_t* bssid = esp8266_Wifi_StationConfig_bssid(arg); \
	int bssid_len = getArrayLength(bssid); \
	if (bssid_len < sizeof(config.bssid)) { \
		return false; \
	} \
	memcpy(bssid, &config.bssid, sizeof(config.bssid)); \
	return true; \
)

DEFINE(jboolean,  station_1set_1config, (void* config), \
	struct station_config c; \
	int8_t* ssid = esp8266_Wifi_StationConfig_ssid(arg);\
	int8_t* password = esp8266_Wifi_StationConfig_password(arg); \
	int8_t* bssid = esp8266_Wifi_StationConfig_bssid(arg); \
	int8_t bssid_set = esp8266_Wifi_StationConfig_bssid_set(arg); \
	\
	memcpy(&c.ssid, ssid, sizeof(c.ssid)); \
	memcpy(&c.password, password, sizeof(c.password)); \
	memcpy(&c.bssid, bssid, sizeof(c.bssid)); \
	c.bssid_set = bssid_set; \
	return wifi_station_set_config(&c); \
)

DEFINE(jboolean,  station_1set_1config_1current, (void* config),  \
	struct station_config c; \
	int8_t* ssid = esp8266_Wifi_StationConfig_ssid(arg);\
	int8_t* password = esp8266_Wifi_StationConfig_password(arg); \
	int8_t* bssid = esp8266_Wifi_StationConfig_bssid(arg); \
	int8_t bssid_set = esp8266_Wifi_StationConfig_bssid_set(arg); \
	\
	memcpy(&c.ssid, ssid, sizeof(c.ssid)); \
	memcpy(&c.password, password, sizeof(c.password)); \
	memcpy(&c.bssid, bssid, sizeof(c.bssid)); \
	c.bssid_set = bssid_set; \
	return wifi_station_set_config_current(&c); \
)

int Java_esp8266_Wifi_station_1set_1cert_1key(int8_t* current_cert, int client_cert_len,
											  int8_t* private_key, int private_key_len,
											  char* private_key_passwd) {
	return wifi_station_set_cert_key(current_cert, client_cert_len,
									 private_key, private_key_len,
									 private_key_passwd, strlen(private_key_passwd));
}

void Java_esp8266_Wifi_station_1clear_1cert_1key() {
	wifi_station_clear_cert_key();
}

int Java_esp8266_Wifi_station_1set_1username(char* name) {
	return wifi_station_set_username(name, strlen(name));
}

void Java_esp8266_Wifi_station_1clear_1username() {
	wifi_station_clear_username();
}

jboolean Java_esp8266_Wifi_station_1connect() {
	return wifi_station_connect();
}

jboolean Java_esp8266_Wifi_station_1disconnect() {
	return wifi_station_disconnect();
}

jbyte Java_esp8266_Wifi_station_1get_1connect_1status() {
	return wifi_station_get_connect_status();
}

typedef struct {
    uint32 event;
	uint32 n;
	struct bss_info bss_info[0];
} scan_done_event_t;

typedef union {
    uint32 event;
	System_Event_t system_event;
	scan_done_event_t scan_done_event;
} wifi_event_t;

extern void set_wifi_event(void*);

#define SCAN_DONE_EVENT 0xff
extern void cioPrintCString(char* s);


#if 1
static void ICACHE_FLASH_ATTR
_scan_done(void *arg, STATUS status)
{
	uint8 ssid[33];
	char temp[128];

	if (status == OK)
	{
		struct bss_info *bss_link = (struct bss_info *)arg;
		bss_link = bss_link->next.stqe_next;//ignore the first one , it's invalid.
 
		while (bss_link != NULL)
		{
			os_memset(ssid, 0, 33);
			if (os_strlen(bss_link->ssid) <= 32)
			{
				os_memcpy(ssid, bss_link->ssid, os_strlen(bss_link->ssid));
			}
			else
			{
				os_memcpy(ssid, bss_link->ssid, 32);
			}
			os_printf("(%d,\"%s\",%d,\""MACSTR"\",%d)\r\n",
					  bss_link->authmode, ssid, bss_link->rssi,
					  MAC2STR(bss_link->bssid),bss_link->channel);
			bss_link = bss_link->next.stqe_next;
		}
	}
	else
	{
		os_printf("scan fail !!!\r\n");
	}
}
#endif

static void ICACHE_FLASH_ATTR
scan_done(void *arg, STATUS status)
{
//	_scan_done(arg, status);
	if (status == OK) {
		printf("sizeof(struct bss_info)=%d\n", sizeof(struct bss_info));
		struct bss_info *bss_link = (struct bss_info *)arg;
		printf("%d %d %d %d %d %d %d %d %d %d\n",
			   ((uintptr_t)bss_link->bssid - (uintptr_t)bss_link),
			   ((uintptr_t)bss_link->ssid - (uintptr_t)bss_link),
			   ((uintptr_t)&bss_link->ssid_len - (uintptr_t)bss_link),
			   ((uintptr_t)&bss_link->channel - (uintptr_t)bss_link),
			   ((uintptr_t)&bss_link->rssi - (uintptr_t)bss_link),
			   ((uintptr_t)&bss_link->authmode - (uintptr_t)bss_link),
			   ((uintptr_t)&bss_link->is_hidden - (uintptr_t)bss_link),
			   ((uintptr_t)&bss_link->freq_offset - (uintptr_t)bss_link),
			   ((uintptr_t)&bss_link->freqcal_val - (uintptr_t)bss_link),
			   ((uintptr_t)&bss_link->esp_mesh_ie - (uintptr_t)bss_link));
			   
		bss_link = bss_link->next.stqe_next;//ignore the first one , it's invalid.
		int count = 0;
		while (bss_link != NULL) {
			count++;
			bss_link = bss_link->next.stqe_next;
		}
		wifi_event_t* event = (wifi_event_t*)malloc(sizeof(wifi_event_t) + count * sizeof(struct bss_info));
		if (event == 0) {
			os_printf("out of memory\r\n");
			return;
		}
		event->event = SCAN_DONE_EVENT;
		event->scan_done_event.n = count;
		bss_link = (struct bss_info *)arg;
		bss_link = bss_link->next.stqe_next;//ignore the first one , it's invalid.
		int i = 0;
		struct bss_info* info = event->scan_done_event.bss_info;
		while (bss_link != NULL && i < count) {
			os_memcpy(&info[i++], bss_link, sizeof(struct bss_info));
			bss_link = bss_link->next.stqe_next;
		}
		set_wifi_event(event);
	} else {
		os_printf("scan fail !!!\r\n");
	}
}

static struct scan_config _scan_config = {0, 0, 0, 0};

DEFINE(void, station_1scan, (void* config), \
	   printf("station_1scan\n"); \
	wifi_station_scan(0, scan_done); \
)
//DEFINE(void, station_1scan, (void* config), \
//	   return wifi_station_scan(&_scan_config, scan_cb); \
//)

jboolean Java_esp8266_Wifi_station_ap_number_set(int8_t ap_number) {
	return wifi_station_ap_number_set(ap_number);
}

DEFINE(jbyte, station_1get_1ap_1info, (void* arg), \
	struct station_config config[5]; \
	int r = wifi_station_get_ap_info(config); \
\
	if (r < 1) { \
		return r; \
	} \
	\
	int len = getArrayLength(arg); \
	int i; \
	if (len < 5) { \
		return -1; \
	} \
	if (r > 5) r = 5; \
	int* elem = (int*)arg; \
	for (i = 0; i < r; i++) { \
		jbyte* ssid = esp826_Wifi_StationConfig_ssid(elem); \
		jbyte* password = esp826_Wifi_StationConfig_password(elem); \
		jbyte* bssid = esp826_Wifi_StationConfig_bssid(elem); \
		memcpy(ssid, config[i].ssid, 32); \
		memcpy(password, config[i].password, 64); \
		memcpy(bssid, config[i].bssid, 6); \
		set_esp826_Wifi_StationConfig_bssid_set(elem, config[i].bssid_set); \
		elem++; \
	} \
	return r; \
)

jbyte Java_esp8266_Wifi_station_1ap_1change(int8_t ap_id) {
	return wifi_station_ap_change(ap_id);
}

jbyte Java_esp8266_Wifi_station_1get_1current_1ap_1id() {
	return wifi_station_get_current_ap_id();
}

jbyte Java_esp8266_Wifi_station_1get_1auto_1connect() {
	return wifi_station_get_auto_connect();
}

jboolean Java_esp8266_Wifi_station_1set_1auto_1connect(int8_t b) {
	return wifi_station_set_auto_connect(b);
}

jboolean Java_esp8266_Wifi_station_1dhcpc_1start() {
	return wifi_station_dhcpc_start();
}

jboolean Java_esp8266_Wifi_station_1dhcpc_1stop() {
	return wifi_station_dhcpc_stop();
}

int Java_esp8266_Wifi_station_1dhcpc_1status() {
	return (int)wifi_station_dhcpc_status();
}

jboolean Java_esp8266_Wifi_station_1dhcpc_1set_1maxtry(int max) {
	return wifi_station_dhcpc_set_maxtry(max);
}

jboolean Java_esp8266_Wifi_station_1set_1reconnect_1policy(int b) {
	return wifi_station_set_reconnect_policy(b);	
}

int Java_esp8266_Wifi_station_1get_1rssi() {
	return wifi_station_get_rssi();
}

jboolean Java_esp8266_Wifi_station_1set_1hostname(char* name) {
	return wifi_station_set_hostname(name);
}

int Java_esp8266_Wifi_station_1get_1hostname(void* name) {
	char* hostname = wifi_station_get_hostname();
	int len = getArrayLength(name);
	int hostname_len = strlen(hostname);
	if (hostname_len < len) {
		len = hostname_len;
	}
	memcpy(name, hostname, len);
	return len;
}

DEFINE(jboolean, softap_1get_1config, (void* config),  \
	struct softap_config c; \
	\
	bool r = wifi_softap_get_config(&c); \
	if (r == false) { \
		return r; \
	} \
	jbyte* ssid = esp8266_Wifi_SoftApConfig_ssid(config); \
	jbyte* password = esp8266_Wifi_SoftApConfig_password(config); \
	memcpy(ssid, c.ssid_len, sizeof(c.ssid_len)); \
	memcpy(ssid, c.password, sizeof(c.password)); \
	set_esp8266_Wifi_SoftApConfig_ssid_len(config, c.ssid_len); \
	set_esp8266_Wifi_SoftApConfig_channel(config, c.channel); \
	set_esp8266_Wifi_SoftApConfig_authmode(config, c.authmode); \
	set_esp8266_Wifi_SoftApConfig_ssid_hidden(config, c.ssid_hidden); \
	set_esp8266_Wifi_SoftApConfig_max_connection(config, c.max_connection); \
	set_esp8266_Wifi_SoftApConfig_beacon_interval(config, c.beacon_interval); \
	return r; \
)

DEFINE(jboolean, softap_1get_1config_1default, (void* config), \
	struct softap_config c; \
	\
	bool r = wifi_softap_get_config_default(&c); \
	if (r == false) { \
		return r; \
	} \
	jbyte* ssid = esp8266_Wifi_SoftApConfig_ssid(config); \
	jbyte* password = esp8266_Wifi_SoftApConfig_password(config); \
	memcpy(ssid, c.ssid_len, sizeof(c.ssid_len)); \
	memcpy(ssid, c.password, sizeof(c.password)); \
	set_esp8266_Wifi_SoftApConfig_ssid_len(config, c.ssid_len); \
	set_esp8266_Wifi_SoftApConfig_channel(config, c.channel); \
	set_esp8266_Wifi_SoftApConfig_authmode(config, c.authmode); \
	set_esp8266_Wifi_SoftApConfig_ssid_hidden(config, c.ssid_hidden); \
	set_esp8266_Wifi_SoftApConfig_max_connection(config, c.max_connection); \
	set_esp8266_Wifi_SoftApConfig_beacon_interval(config, c.beacon_interval); \
	return r; \
)

DEFINE(jboolean, softap_1set_1config, (void* config), \
	struct softap_config c; \
\
	jbyte* ssid = esp8266_Wifi_SoftApConfig_ssid(config); \
	jbyte* password = esp8266_Wifi_SoftApConfig_password(config);\
	memcpy(c.ssid, ssid, sizeof(c.ssid)); \
	memcpy(c.password, password, sizeof(c.password)); \
	c.ssid_len = esp8266_Wifi_SoftApConfig_ssid_len(config); \
	c.channel = esp8266_Wifi_SoftApConfig_channel(config); \
	c.authmode = esp8266_Wifi_SoftApConfig_authmode(config); \
	c.ssid_hidden = esp8266_Wifi_SoftApConfig_ssid_hidden(config); \
	c.max_connection = esp8266_Wifi_SoftApConfig_max_connection(config);  \
	c.beacon_interval = esp8266_Wifi_SoftApConfig_beacon_interval(config); \
	return wifi_softap_set_config(&c); \
)

DEFINE(jboolean, softap_1set_1config_1current, (void* config), \
	struct softap_config c; \
\
	jbyte* ssid = esp8266_Wifi_SoftApConfig_ssid(config); \
	jbyte* password = esp8266_Wifi_SoftApConfig_password(config);\
	memcpy(c.ssid, ssid, sizeof(c.ssid)); \
	memcpy(c.password, password, sizeof(c.password)); \
	c.ssid_len = esp8266_Wifi_SoftApConfig_ssid_len(config); \
	c.channel = esp8266_Wifi_SoftApConfig_channel(config); \
	c.authmode = esp8266_Wifi_SoftApConfig_authmode(config); \
	c.ssid_hidden = esp8266_Wifi_SoftApConfig_ssid_hidden(config); \
	c.max_connection = esp8266_Wifi_SoftApConfig_max_connection(config);  \
	c.beacon_interval = esp8266_Wifi_SoftApConfig_beacon_interval(config); \
	return wifi_softap_set_config_current(&c); \
)

jbyte Java_esp8266_Wifi_softap_get_station_num() {
	return wifi_softap_get_station_num();
}

DEFINE(void, softap_1get_1station_1info, (void* info), \
	struct station_info *si; \
	si = wifi_softap_get_station_info(); \
\
	if (si) { \
		jbyte* bssid = esp8266_Wifi_StationInfo_bssid(info); \
		memcpy(bssid, si->bssid, sizeof(si->bssid)); \
		set_esp8266_Wifi_StationInfo_ip(info, si->ip.addr); \
	} \
)

DEFINE(jboolean, softap_1dhcps_1start, (), \
	return wifi_softap_dhcps_start(); \
)

jboolean Java_esp8266_Wifi_softap_1dhcps_1stop() {
	return wifi_softap_dhcps_stop();
}

DEFINE(jboolean, softap_1set_1dhcps_1lease, (void* lease), \
	struct dhcps_lease p;\
	jboolean enable = esp8266_Wifi_DHCPLease_enable(lease);\
	int start_ip = esp8266_Wifi_DHCPLease_start_ip(lease);\
	int end_ip = esp8266_Wifi_DHCPLease_end_ip(lease);\
	p.enable = enable;\
	p.start_ip.addr = start_ip;\
	p.end_ip.addr = end_ip;\
	return wifi_softap_set_dhcps_lease(&p);\
	)

DEFINE(jboolean, softap_1get_1dhcps_1lease, (void* lease),  \
	struct dhcps_lease p; \
	int r = wifi_softap_get_dhcps_lease(&p); \
	if (r == false) { \
		return r; \
	} \
	set_esp8266_Wifi_DHCPLease_enable(lease, p.enable);	 \
	set_esp8266_Wifi_DHCPLease_start_ip(lease, p.start_ip.addr);	 \
	set_esp8266_Wifi_DHCPLease_end_ip(lease, p.end_ip.addr); \
	return r; \
)

jboolean Java_esp8266_Wifi_softap_1set_1dhcps_1lease_1time(int minute) {
	return wifi_softap_set_dhcps_lease_time(minute);
}

int Java_esp8266_Wifi_softap_1get_1dhcps_1lease_1time() {
	return wifi_softap_get_dhcps_lease_time();
}

int Java_esp8266_Wifi_softap_1dhcps_1status() {
	return (int)wifi_softap_dhcps_status();
}

jboolean Java_esp8266_Wifi_softap_1set_1dhcps_1offer_1option(jbyte level, int optarg) {
	jbyte arg = optarg;
	return wifi_softap_set_dhcps_offer_option(level, &arg);
}

jboolean Java_esp8266_Wifi_set_1phy_1mode(int mode) {
	return wifi_set_phy_mode(mode);
}

int Java_esp8266_Wifi_get_1phy_1mode() {
	return (int)wifi_get_phy_mode();
}

DEFINE(jboolean, get_1ip_1info, (int index, void* info), \
	jboolean r; \
	struct ip_info ip; \
	\
	r =  wifi_get_ip_info(index, &ip); \
	if (r == false) { \
		return r; \
	} \
	set_esp8266_Wifi_IPInfo_ip(info, ip.ip.addr); \
	set_esp8266_Wifi_IPInfo_netmask(info, ip.netmask.addr); \
	set_esp8266_Wifi_IPInfo_gw(info, ip.gw.addr); \
	return r; \
	)

DEFINE(jboolean, set_1ip_1info, (int index, void* info),  \
	struct ip_info ip; \
	ip.ip.addr = esp8266_Wifi_IPInfo_ip(info);	\
	ip.netmask.addr = esp8266_Wifi_IPInfo_netmask(info);	\
	ip.gw.addr = esp8266_Wifi_IPInfo_gw(info); \
	return wifi_set_ip_info(index, &ip); \
)

jboolean Java_esp8266_Wifi_set_1macaddr(int if_index, void* macaddr) {
	return wifi_set_macaddr(if_index, macaddr);
}

jboolean Java_esp8266_Wifi_get_1macaddr(int if_index, void* macaddr) {
	return wifi_get_macaddr(if_index, macaddr);
}

jboolean Java_esp8266_Wifi_set_1sleep_1type(int type) {
	return wifi_set_sleep_type(type);	
}

int Java_esp8266_Wifi_get_1sleep_1type() {
	return (int)wifi_get_sleep_type();
}

void Java_esp8266_Wifi_status_1led_1install(int gpio_id, int gpio_name, int gpio_func) {
	wifi_status_led_install(gpio_id, gpio_name, gpio_func);	
}

void Java_esp8266_Wifi_status_1led_1uninstall() {
	wifi_status_led_uninstall();	
}

jboolean Java_esp8266_Wifi_set_1broadcast_1if(int intf) {
	return wifi_set_broadcast_if(intf);
}

int Java_esp8266_Wifi_get_1broadcast_1if() {
	return wifi_get_broadcast_if();
}

jboolean Java_esp8266_Wifi_wps_1enable(int wps_type) {
	return wifi_wps_enable(wps_type);
}

jboolean Java_esp8266_Wifi_wps_1disable() {
	return wifi_wps_disable();
}

jboolean Java_esp8266_Wifi_wps_1start() {
	return wifi_wps_start();
}

int Java_esp8266_Wifi_send_1pkt_1freedom(void* buf, int len, int sys_seq) {
	return wifi_send_pkt_freedom(buf, len, sys_seq);	
}

int Java_esp8266_Wifi_rfid_1locp_1recv_1open() {
	return wifi_rfid_locp_recv_open();
}

void Java_esp8266_Wifi_rfid_1locp_1recv_1close() {
	wifi_rfid_locp_recv_close();	
}

void Java_esp8266_Wifi_enable_1gpio_1wakeup(int i, int intr_status) {
//	wifi_enable_gpio_wakeup(i, intr_status);
}

void Java_esp8266_Wifi_disable_1gpio_1wakeup() {
//	wifi_disable_gpio_wakeup();
}

static void event_handler_cb(System_Event_t *event) {
	// need to copy event?
	wifi_event_t *e = (wifi_event_t*)malloc(sizeof(wifi_event_t));
	if (e == 0) {
		printf("out of memory\n");
		return;
	}
	memcpy(e, event, sizeof(System_Event_t));
	
	set_wifi_event(e);
}

void Java_esp8266_Wifi_init_1event_1handler() {
	wifi_set_event_handler_cb(&event_handler_cb);
}

int Java_esp8266_Wifi_wifi_1get_1event_1id(void* evt) {
	wifi_event_t* wifi_event = (wifi_event_t*)evt;
	if (wifi_event) {
		return wifi_event->event;
	} 
	return -1;
}

DEFINE(void, receive_1event, (int addr, void* evt),	\
	wifi_event_t* wifiEvent = (wifi_event_t*)addr;\
	switch (wifiEvent->event) {\
	case SCAN_DONE_EVENT: {\
		int n = wifiEvent->scan_done_event.n; \
		struct bss_info* bss_info = wifiEvent->scan_done_event.bss_info;\
		set_esp8266_Wifi_BSS_Info_Event_n(evt, n); \
		set_esp8266_Wifi_BSS_Info_Event_rawData(evt, bss_info); \
		break;\
	}\
	case EVENT_STAMODE_CONNECTED: {\
		Event_StaMode_Connected_t* event_info = (Event_StaMode_Connected_t*)&wifiEvent->system_event.event_info.connected;\
		jbyte* ssid = esp8266_Wifi_StaMode_Connected_Event_ssid(evt);\
		jbyte* bssid = esp8266_Wifi_StaMode_Connected_Event_bssid(evt);\
		memcpy(ssid, event_info->ssid, sizeof(event_info->ssid));\
		memcpy(bssid, event_info->bssid, sizeof(event_info->bssid));\
		set_esp8266_Wifi_StaMode_Connected_Event_ssid_len(evt, event_info->ssid_len);\
		set_esp8266_Wifi_StaMode_Connected_Event_channel(evt, event_info->channel);\
		break;\
	}\
	case EVENT_STAMODE_GOT_IP: {\
		Event_StaMode_Got_IP_t* event_info = (Event_StaMode_Got_IP_t*)&wifiEvent->system_event.event_info.got_ip;\
		set_esp8266_Wifi_StaMode_Got_IP_Event_ip(evt, event_info->ip.addr);\
		set_esp8266_Wifi_StaMode_Got_IP_Event_mask(evt, event_info->mask.addr);\
		set_esp8266_Wifi_StaMode_Got_IP_Event_gw(evt, event_info->gw.addr);\
		break;\
	}\
	case EVENT_SOFTAPMODE_STACONNECTED: {\
		Event_SoftAPMode_StaConnected_t* event_info = (Event_SoftAPMode_StaConnected_t*)&wifiEvent->system_event.event_info.sta_connected;\
		jbyte* mac = esp8266_Wifi_SoftAPMode_StaConnected_Event_mac(evt);\
		memcpy(mac, event_info->mac, sizeof(event_info->mac));\
		set_esp8266_Wifi_SoftAPMode_StaConnected_Event_aid(evt, event_info->aid);\
		break;\
	}\
	default:\
		;\
	}\
)
