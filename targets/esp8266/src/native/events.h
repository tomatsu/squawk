#define WIFI_STAMODE_CONNECTED_EVENT 0
#define WIFI_STAMODE_DISCONNECTED_EVENT 1
#define WIFI_STAMODE_AUTHMODE_CHANGE_EVENT 2
#define WIFI_STAMODE_GOT_IP_EVENT 3
#define WIFI_STAMODE_DHCP_TIMEOUT_EVENT 4
#define WIFI_SCAN_DONE_EVENT 8
#define RESOLVED_EVENT 9
#define CONNECTED_EVENT 10
#define READ_READY_EVENT 11
#define WRITE_READY_EVENT 12
#define ACCEPTED_EVENT 13
#define CONNECT_FAILED_EVENT 14

#define MAX_EVENT_TYPE 15

extern int squawk_get_event(int type, bool clear);
extern void squawk_post_event(int type, uint32_t value);
