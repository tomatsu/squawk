//#define USE_US_TIMER
#include <mem.h>
#include <osapi.h>
#include <uart.h>
#include <stdint.h>
#include <stdlib.h>
#include <ets_sys.h>
#include <user_interface.h>

//#define IODOTC
extern int os_printf_plus(const char *format, ...)  __attribute__ ((format (printf, 1, 2)));
#define printf os_printf_plus
#define malloc os_malloc
#define free os_free

#define CIO_STDIO_H cio_esp.c.inc

#define jlong  int64_t
static jlong _counter;

static os_timer_t timer;

static void initTickCount() {
}

static void initSerial() {
	uart_init(BIT_RATE_115200, BIT_RATE_115200);
}

void sysInitialize() {
	squawk_init_event();
}

jlong sysTickCount(){
	return (jlong)(system_get_time() / 1000);
}

jlong sysTimeMicros() {
	return (jlong)system_get_time();
}

jlong sysTimeMillis(void) {
	return sysTickCount();
}

void *sysValloc(int size) {
	return (void*)malloc(size);
}

void sysVallocFree(void* p){
	free(p);
}


void osMilliSleep(long long millis) {
//	printf("osMilliSleep\r\n");
	int64_t target = sysTickCount() + millis;
    if (target <= 0) target = 0x7FFFFFFFFFFFFFFFLL; // overflow detected

    for (;;) {
        if (checkForEvents()) break;
        if (sysTickCount() > target) break;
		os_delay_us(1000);
	}
}


/* entry point */
extern int Squawk_main_wrapper(int argc, char *argv[]);

static void init_done(void) {
	Squawk_main_wrapper(0, 0);
}

uint32 ICACHE_FLASH_ATTR
user_rf_cal_sector_set(void)
{
    enum flash_size_map size_map = system_get_flash_size_map();
    uint32 rf_cal_sec = 0;

    switch (size_map) {
        case FLASH_SIZE_4M_MAP_256_256:
            rf_cal_sec = 128 - 5;
            break;

        case FLASH_SIZE_8M_MAP_512_512:
            rf_cal_sec = 256 - 5;
            break;

        case FLASH_SIZE_16M_MAP_512_512:
        case FLASH_SIZE_16M_MAP_1024_1024:
            rf_cal_sec = 512 - 5;
            break;

        case FLASH_SIZE_32M_MAP_512_512:
        case FLASH_SIZE_32M_MAP_1024_1024:
            rf_cal_sec = 1024 - 5;
            break;

        default:
            rf_cal_sec = 0;
            break;
    }

    return rf_cal_sec;
}

void user_init(void) {
    initTickCount();
	initSerial();
	initWifi();
    system_init_done_cb(init_done);
}
