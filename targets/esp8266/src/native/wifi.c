#include <stdint.h>
#include <user_interface.h>
#include "classes.h"

typedef int jboolean;
typedef int8_t jbyte;

//#define TRUE 1
//#define FALSE 0 

jbyte Java_esp8266_Wifi_get_opmode() {
	return (int8_t)wifi_get_opmode();
}

jbyte Java_esp8266_Wifi_get_opmode_default() {
	return (int8_t)wifi_get_opmode_default();
}

jboolean Java_esp8266_Wifi_set_opmode(jbyte mode) {
	return wifi_set_opmode(mode);
}

jboolean Java_esp8266_Wifi_set_opmode_current(jbyte mode) {
	return wifi_set_opmode_current(mode);
}

static jboolean get_station_config(void* arg, int is_default) {
	struct station_config config;
	int r = is_default ? wifi_station_get_config_default(&config) : wifi_station_get_config(&config);
	if (r == FALSE) {
		return r;
	}
	jbyte* ssid = esp8266_Wifi_StationConfig_ssid(arg);
	int ssid_len = getArrayLength(ssid);
	if (ssid_len < sizeof(config.ssid)) {
		return FALSE;
	}
	memcpy(ssid, &config.ssid, sizeof(config.ssid));
	
	int8_t* password = esp8266_Wifi_StationConfig_password(arg);
	int password_len = getArrayLength(password);
	if (password_len < sizeof(config.password)) {
		return FALSE;
	}
	memcpy(password, &config.password, sizeof(config.password));
	
	set_esp8266_Wifi_StationConfig_bssid_set(arg, config.bssid_set);
	
	int8_t* bssid = esp8266_Wifi_StationConfig_bassid(arg);
	int bssid_len = getArrayLength(bssid);
	if (bssid_len < sizeof(config.bssid)) {
		return FALSE;
	}
	memcpy(bssid, &config.bssid, sizeof(config.bssid));
	return TRUE;
}

jboolean Java_esp8266_Wifi_station_get_config(void* arg) {
	return get_station_config(arg, FALSE);
}

jboolean Java_esp8266_Wifi_station_get_config_default(void* arg) {
	return get_station_config(arg, TRUE);
}

static jboolean set_station_config(void* arg, int is_current) {
	struct station_config c;

	int8_t* ssid = esp8266_Wifi_StationConfig_ssid(arg);
	int8_t* password = esp8266_Wifi_StationConfig_password(arg);
	int8_t* bssid = esp8266_Wifi_StationConfig_bssid(arg);
	int8_t bssid_set = esp8266_Wifi_StationConfig_bssid_set(arg);
	
	memcpy(&c.ssid, ssid, sizeof(c.ssid));
	memcpy(&c.password, password, sizeof(c.password));
	memcpy(&c.bssid, bssid, sizeof(c.bssid));
	c.bssid_set = bssid_set;

	if (is_current) {
		return wifi_station_set_config_current(&c);
	} else {
		return wifi_station_set_config(&c);
	}
}
	
jboolean Java_esp8266_Wifi_station_set_config(void* config) {
	return set_station_config(config, FALSE);
}

jboolean Java_esp8266_Wifi_station_set_config_current(void* config) {
	return set_station_config(config, TRUE);
}

int Java_esp8266_Wifi_station_set_cert_key(int8_t* current_cert, int client_cert_len,
										   int8_t* private_key, int private_key_len,
										   char* private_key_passwd) {
	return wifi_station_set_cert_key(current_cert, client_cert_len,
									 private_key, private_key_len,
									 private_key_passwd, strlen(private_key_passwd));
}

void Java_esp8266_Wifi_station_clear_cert_key() {
	wifi_station_clear_cert_key();
}

int Java_esp8266_Wifi_station_set_username(char* name) {
	return wifi_station_set_username(name, strlen(name));
}

void Java_esp8266_Wifi_station_clear_username() {
	wifi_station_clear_username();
}

jboolean Java_esp8266_Wifi_station_connect() {
	return wifi_station_connect();
}

jboolean Java_esp8266_Wifi_station_disconnect() {
	return wifi_station_disconnect();
}

jbyte Java_esp8266_Wifi_station_get_connect_status() {
	return wifi_station_get_connect_status();
}

static void scan_cb(void* arg, STATUS status) {
	struct bss_info* info = (struct bss_info*)arg;
	switch (status) {
	case OK:
		// TODO
		break;
	case FAIL:
		// TODO
		break;
	} 
}

void Java_esp8266_Wifi_station_scan(void* config) {
	struct scan_config c;
	
	jbyte* _ssid = esp8266_Wifi_ScanConfig_ssid(config);
	jbyte* _bssid = esp8266_Wifi_ScanConfig_bssid(config);
	int len;

	memset(&c, 0, sizeof(struct scan_config));

	c.channel = esp8266_Wifi_ScanConfig_channel(config);
	c.show_hidden = esp8266_Wifi_ScanConfig_show_hidden(config);

	if (_ssid != 0) {
		len = getArrayLength(_ssid);
		c.ssid = (jbyte*)malloc(len);
		memcpy(c.ssid, _ssid, len);
	}
	if (_bssid != 0) {
		len = getArrayLength(_bssid);
		c.bssid = (jbyte*)malloc(len);
		memcpy(c.bssid, _bssid, len);
	}
	
	wifi_station_scan(&c, &scan_cb);
	
	if (c.ssid) free(c.ssid);
	if (c.bssid) free(c.bssid);
}

jboolean Java_esp8266_Wifi_station_ap_number_set(int8_t ap_number) {
	return wifi_station_ap_number_set(ap_number);
}

jbyte Java_esp8266_Wifi_station_get_ap_info(void* arg) {
	struct station_config config[5];
	int r = wifi_station_get_ap_info(config);

	if (r < 1) {
		return r;
	}
	
	int len = getArrayLength(arg);
	int i;
	if (len < 5) {
		return -1;
	}
	if (r > 5) r = 5;
	int* elem = (int*)arg;
	for (i = 0; i < r; i++) {
		jbyte* ssid = esp826_Wifi_StationConfig_ssid(elem);
		jbyte* password = esp826_Wifi_StationConfig_password(elem);
		jbyte* bssid = esp826_Wifi_StationConfig_bssid(elem);
		memcpy(ssid, config[i].ssid, 32);
		memcpy(password, config[i].password, 64);
		memcpy(bssid, config[i].bssid, 6);
		set_esp826_Wifi_StationConfig_bssid_set(elem, config[i].bssid_set);
		elem++;
	}
	return r;
}

jbyte Java_esp8266_Wifi_station_ap_change(int8_t ap_id) {
	return wifi_station_ap_change(ap_id);
}

jbyte Java_esp8266_Wifi_station_get_current_ap_id() {
	return wifi_station_get_current_ap_id();
}

jbyte Java_esp8266_Wifi_station_get_auto_connect() {
	return wifi_station_get_auto_connect();
}

jboolean Java_esp8266_Wifi_station_set_auto_connect(int8_t b) {
	return wifi_station_set_auto_connect(b);
}

jboolean Java_esp8266_Wifi_station_dhcpc_start() {
	return wifi_station_dhcpc_start();
}

jboolean Java_esp8266_Wifi_station_dhcpc_stop() {
	return wifi_station_dhcpc_stop();
}

int Java_esp8266_Wifi_station_dhcpc_status() {
	return (int)wifi_station_dhcpc_status();
}

jboolean Java_esp8266_Wifi_station_dhcpc_set_maxtry(int max) {
	return wifi_station_dhcpc_set_maxtry(max);
}

jboolean Java_esp8266_Wifi_station_set_reconnect_policy(int b) {
	return wifi_station_set_reconnect_policy(b);	
}

int Java_esp8266_Wifi_station_get_rssi() {
	return wifi_station_get_rssi();
}

jboolean Java_esp8266_Wifi_station_set_hostname(char* name) {
	return wifi_station_set_hostname(name);
}

int Java_esp8266_Wifi_station_get_hostname(void* name) {
	char* hostname = wifi_station_get_hostname();
	int len = getArrayLength(name);
	int hostname_len = strlen(hostname);
	if (hostname_len < len) {
		len = hostname_len;
	}
	memcpy(name, hostname, len);
	return len;
}

static jboolean softap_get_config(void* config, int is_default) {
	struct softap_config c;
	
	bool r;
	if (is_default) {
		r = wifi_softap_get_config_default(&c);
	} else {
		r = wifi_softap_get_config(&c);
	}
	if (r == FALSE) {
		return r;
	}
	jbyte* ssid = esp8266_Wifi_SoftApConfig_ssid(config);
	jbyte* password = esp8266_Wifi_SoftApConfig_password(config);
	memcpy(ssid, c.ssid_len, sizeof(c.ssid_len));
	memcpy(ssid, c.password, sizeof(c.password));
	set_esp8266_Wifi_SoftApConfig_ssid_len(config, c.ssid_len);
	set_esp8266_Wifi_SoftApConfig_channel(config, c.channel);
	set_esp8266_Wifi_SoftApConfig_authmode(config, c.authmode);
	set_esp8266_Wifi_SoftApConfig_ssid_hidden(config, c.ssid_hidden);
	set_esp8266_Wifi_SoftApConfig_max_connection(config, c.max_connection);
	set_esp8266_Wifi_SoftApConfig_beacon_interval(config, c.beacon_interval);
	return r;
}

jboolean Java_esp8266_Wifi_softap_get_config(void* config) {
	return softap_get_config(config, FALSE);
}

jboolean Java_esp8266_Wifi_softap_get_config_default(void* config) {
	return softap_get_config(config, TRUE);
}

static jboolean softap_set_config(void* config, int is_current) {
	struct softap_config c;
	
	jbyte* ssid = esp8266_Wifi_SoftApConfig_ssid(config);
	jbyte* password = esp8266_Wifi_SoftApConfig_password(config);
	memcpy(c.ssid, ssid, sizeof(c.ssid));
	memcpy(c.password, password, sizeof(c.password));
	c.ssid_len = esp8266_Wifi_SoftApConfig_ssid_len(config);
	c.channel = esp8266_Wifi_SoftApConfig_channel(config);
	c.authmode = esp8266_Wifi_SoftApConfig_authmode(config);
	c.ssid_hidden = esp8266_Wifi_SoftApConfig_ssid_hidden(config);
	c.max_connection = esp8266_Wifi_SoftApConfig_max_connection(config);
	c.beacon_interval = esp8266_Wifi_SoftApConfig_beacon_interval(config);
	
	if (is_current) {
		return wifi_softap_set_config_current(&c);
	} else {
		return wifi_softap_set_config(&c);
	}
}
	
jboolean Java_esp8266_Wifi_softap_set_config(void* config) {
	return softap_set_config(config, FALSE);
}

jboolean Java_esp8266_Wifi_softap_set_config_current(void* config) {
	return softap_set_config(config, TRUE);
}

jbyte Java_esp8266_Wifi_softap_get_station_num() {
	return wifi_softap_get_station_num();
}

void Java_esp8266_Wifi_softap_get_station_info(void* info) {
	struct station_info *si;
	si = wifi_softap_get_station_info();

	if (si) {
		jbyte* bssid = esp8266_Wifi_StationInfo_bssid(info);
		memcpy(bssid, si->bssid, sizeof(si->bssid));
		set_esp8266_Wifi_StationInfo_ip(info, si->ip.addr);
	}
}

jboolean Java_esp8266_Wifi_softap_dhcps_start() {
	return wifi_softap_dhcps_start();
}

jboolean Java_esp8266_Wifi_softap_dhcps_stop() {
	return wifi_softap_dhcps_stop();
}

jboolean Java_esp8266_Wifi_softap_set_dhcps_lease(void* lease) {
	struct dhcps_lease p;
	jboolean enable = esp8266_Wifi_DHCPLease_enable(lease);
	int start_ip = esp8266_Wifi_DHCPLease_start_ip(lease);
	int end_ip = esp8266_Wifi_DHCPLease_end_ip(lease);
	p.enable = enable;
	p.start_ip.addr = start_ip;
	p.end_ip.addr = end_ip;
	return wifi_softap_set_dhcps_lease(&p);
}

jboolean Java_esp8266_Wifi_softap_get_dhcps_lease(void* lease) {
	struct dhcps_lease p;
	int r = wifi_softap_get_dhcps_lease(&p);
	if (r == FALSE) {
		return r;
	}
	set_esp8266_Wifi_DHCPLease_enable(lease, p.enable);	
	set_esp8266_Wifi_DHCPLease_start_ip(lease, p.start_ip.addr);	
	set_esp8266_Wifi_DHCPLease_end_ip(lease, p.end_ip.addr);
	return r;
}

jboolean Java_esp8266_Wifi_softap_set_dhcps_lease_time(int minute) {
	return wifi_softap_set_dhcps_lease_time(minute);
}

int Java_esp8266_Wifi_softap_get_dhcps_lease_time() {
	return wifi_softap_get_dhcps_lease_time();
}

int Java_esp8266_Wifi_softap_dhcps_status() {
	return (int)wifi_softap_dhcps_status();
}

jboolean Java_esp8266_Wifi_softap_set_dhcps_offer_option(jbyte level, int optarg) {
	jbyte arg = optarg;
	return wifi_softap_set_dhcps_offer_option(level, &arg);
}

jboolean Java_esp8266_Wifi_set_phy_mode(int mode) {
	return wifi_set_phy_mode(mode);
}

int Java_esp8266_Wifi_get_phy_mode() {
	return (int)wifi_get_phy_mode();
}

jboolean Java_esp8266_Wifi_get_ip_info(int index, void* info) {
	jboolean r;
	struct ip_info ip;
	
	r =  wifi_get_ip_info(index, &ip);
	if (r == FALSE) {
		return r;
	}
	set_esp8266_Wifi_IPInfo_ip(info, ip.ip.addr);
	set_esp8266_Wifi_IPInfo_netmask(info, ip.netmask.addr);
	set_esp8266_Wifi_IPInfo_gw(info, ip.gw.addr);
	return r;
}

jboolean Java_esp8266_Wifi_set_ip_info(int index, void* info) {
	struct ip_info ip;
	ip.ip.addr = esp8266_Wifi_IPInfo_ip(info);	
	ip.netmask.addr = esp8266_Wifi_IPInfo_netmask(info);	
	ip.gw.addr = esp8266_Wifi_IPInfo_gw(info);
	return wifi_set_ip_info(index, &ip);
}

jboolean Java_esp8266_Wifi_set_macaddr(int if_index, void* macaddr) {
	return wifi_set_macaddr(if_index, macaddr);
}

jboolean Java_esp8266_Wifi_get_macaddr(int if_index, void* macaddr) {
	return wifi_get_macaddr(if_index, macaddr);
}

jboolean Java_esp8266_Wifi_set_sleep_type(int type) {
	return wifi_set_sleep_type(type);	
}

int Java_esp8266_Wifi_get_sleep_type() {
	return (int)wifi_get_sleep_type();
}

void Java_esp8266_Wifi_status_led_install(int gpio_id, int gpio_name, int gpio_func) {
	wifi_status_led_install(gpio_id, gpio_name, gpio_func);	
}

void Java_esp8266_Wifi_status_led_uninstall() {
	wifi_status_led_uninstall();	
}

jboolean Java_esp8266_Wifi_set_broadcast_if(int intf) {
	return wifi_set_broadcast_if(intf);
}

int Java_esp8266_Wifi_get_broadcast_if() {
	return wifi_get_broadcast_if();
}

jboolean Java_esp8266_Wifi_wps_enable(int wps_type) {
	return wifi_wps_enable(wps_type);
}

jboolean Java_esp8266_Wifi_wps_disable() {
	return wifi_wps_disable();
}

jboolean Java_esp8266_Wifi_wps_start() {
	return wifi_wps_start();
}

int Java_esp8266_Wifi_send_pkt_freedom(void* buf, int len, int sys_seq) {
	return wifi_send_pkt_freedom(buf, len, sys_seq);	
}

int Java_esp8266_Wifi_rfid_locp_recv_open() {
	return wifi_rfid_locp_recv_open();
}

void Java_esp8266_Wifi_rfid_locp_recv_close() {
	wifi_rfid_locp_recv_close();	
}

void Java_esp8266_Wifi_enable_gpio_wakeup(int i, int intr_status) {
	wifi_enable_gpio_wakeup(i, intr_status);
}

void Java_esp8266_Wifi_disable_gpio_wakeup() {
	wifi_disable_gpio_wakeup();
}

extern void set_wifi_event(int);

static void event_handler_cb(System_Event_t *event) {
	// need to copy event?
	set_wifi_event((int)event);
}

void Java_esp8266_Wifi_init_event_handler() {
	wifi_set_event_handler_cb(&event_handler_cb);
}

int Java_esp8266_Wifi_wifi_get_event_id(void* evt) {
	System_Event_t* systemEvent = (System_Event_t*)evt;
	if (systemEvent) {
		int e = systemEvent->event;
		if (e >= 0 && e <= EVENT_MAX) {
			return e;
		}
	} 
	return -1;
}

void Java_esp8266_Wifi_receive_event(int addr, void* evt) {
	System_Event_t* systemEvent = (System_Event_t*)addr;
	switch (systemEvent->event) {
	case EVENT_STAMODE_CONNECTED: {
		Event_StaMode_Connected_t* event_info = (Event_StaMode_Connected_t*)&systemEvent->event_info.connected;
		jbyte* ssid = esp8266_Wifi_StaMode_Connected_Event_ssid(evt);
		jbyte* bssid = esp8266_Wifi_StaMode_Connected_Event_bssid(evt);
		memcpy(ssid, event_info->ssid, sizeof(event_info->ssid));
		memcpy(bssid, event_info->bssid, sizeof(event_info->bssid));
		set_esp8266_Wifi_StaMode_Connected_Event_ssid_len(evt, event_info->ssid_len);
		set_esp8266_Wifi_StaMode_Connected_Event_channel(evt, event_info->channel);
		break;
	}
	case EVENT_STAMODE_GOT_IP: {
		Event_StaMode_Got_IP_t* event_info = (Event_StaMode_Got_IP_t*)&systemEvent->event_info.got_ip;
		set_esp8266_Wifi_StaMode_Got_IP_Event_ip(evt, event_info->ip);
		set_esp8266_Wifi_StaMode_Got_IP_Event_mask(evt, event_info->mask);
		set_esp8266_Wifi_StaMode_Got_IP_Event_gw(evt, event_info->gw);
		break;
	}
	case EVENT_SOFTAPMODE_STACONNECTED: {
		Event_SoftAPMode_StaConnected_t* event_info = (Event_SoftAPMode_StaConnected_t*)&systemEvent->event_info.sta_connected;
		jbyte* mac = esp8266_Wifi_SoftAPMode_StaConnected_Event_mac(evt);
		memcpy(mac, event_info->mac, sizeof(event_info->mac));
		set_esp8266_Wifi_SoftAPMode_StaConnected_Event(evt, event_info->aid);
		break;
	}
	default:
		// TODO
		;
	}
}
