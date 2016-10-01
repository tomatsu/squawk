#include <stdint.h>
#include <user_interface.h>
#include "classes.h"

int8_t Java_esp8266_Wifi_get_opmode() {
	return (int8_t)wifi_get_opmode();
}

int8_t Java_esp8266_Wifi_get_opmode_default() {
	return (int8_t)wifi_get_opmode_default();
}

int Java_esp8266_Wifi_set_opmode(int8_t mode) {
	return wifi_set_opmode(mode);
}

int Java_esp8266_Wifi_set_opmode_current(int8_t mode) {
	return wifi_set_opmode_current(mode);
}

static int get_config(void* arg, int is_default) {
	struct station_config config;
	int r = is_default ? wifi_station_get_config_default(&config) : wifi_station_get_config(&config);
	if (r) {
		int8_t* ssid = esp8266_Wifi_StationConfig_ssid(arg);
		int ssid_len = getArrayLength(ssid);
		memcpy(ssid, &config.ssid, sizeof(config.ssid));

		int8_t* password = esp8266_Wifi_StationConfig_password(arg);
		int password_len = getArrayLength(password);
		memcpy(password, &config.password, sizeof(config.password));
			
		set_esp8266_Wifi_StationConfig_bssid_set(arg, config.bssid_set);
			
		int8_t* bssid = esp8266_Wifi_StationConfig_bassid(arg);
		int bssid_len = getArrayLength(bssid);
		memcpy(bssid, &config.bssid, sizeof(config.bssid));

		return 1;
	}
	return 0;
}

int Java_esp8266_Wifi_station_get_config(void* arg) {
	return get_config(arg, 0);
}

int Java_esp8266_Wifi_station_get_config_default(void* arg) {
	return get_config(arg, 1);
}
