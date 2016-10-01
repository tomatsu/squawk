#include <mem.h>
#include <osapi.h>
#include <uart.h>
#include <stdint.h>
#include <stdlib.h>
#include <user_interface.h>

//#define IODOTC
extern int os_printf_plus(const char *format, ...)  __attribute__ ((format (printf, 1, 2)));
#define printf os_printf_plus

#define CIO_STDIO_H cio_esp.c.inc

#define jlong  int64_t

static jlong _counter;

static void cb(void) {
	_counter++;
}

static void initTickCount() {
	hw_timer_init(0, 1);
	hw_timer_set_func(cb);
	hw_timer_arm(1000);
}

static void initSerial() {
	uart_init(BIT_RATE_115200, BIT_RATE_115200);
}

void sysInitialize() {
    initTickCount();
	initSerial();
}

jlong sysTickCount(){
    return _counter;
}

jlong sysTimeMicros() {
    extern jlong sysTimeMillis(void);
    return sysTimeMillis() * 1000;
}

jlong sysTimeMillis(void) {
	return sysTickCount();
}

void *sysValloc(int size) {
	return (void*)os_malloc(size);
}

void sysVallocFree(void* p){
	os_free(p);
}

int getEvent() {
	system_soft_wdt_feed();
	return 0;
}

void osMilliSleep(long long millis) {
    long long target = sysTickCount() + millis;
    long long maxValue = 0x7FFFFFFFFFFFFFFFLL;
    if (target <= 0) target = maxValue; // overflow detected
    while (1) {
//        if (checkForEvents()) break;
        if (sysTickCount() > target) break;
		os_delay_us(1000);
		system_soft_wdt_feed();
	}
}
