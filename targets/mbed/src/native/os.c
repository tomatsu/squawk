/*
 * Copyright 2004-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */

#include <stdlib.h>
#include <stdio.h>

#define jlong  int64_t

/* The package that conmtains the native code to use for a "NATIVE" platform type*/

#include "rtc_api.h"
#include "gpio_api.h"
#include "ticker_api.h"
#include "us_ticker_api.h"
//#include "sleep_api.h"

#if defined(STM32L0) || defined(STM32L1) || defined(STM32L4) || defined(STM32F0) || defined(STM32F1) || defined(STM32F3) || defined(STM32F4) || defined(STM32F7)
//#define USE_SUBSECOND_RTC
#define USE_TICK
#elif defined(NRF51) || defined(TARGET_LPC176X)
#define USE_TICK
#else
#define USE_RTC
#endif

#ifdef USE_TICK
static void initTickCount();
#endif

void sysInitialize() {
#if defined(USE_SUBSECOND_RTC) || defined(USE_RTC)
		rtc_init();
#elif defined(USE_TICK)
		initTickCount();
#endif
		squawk_init_event();
}

jlong sysTimeMicros() {
    extern jlong sysTimeMillis(void);
    return sysTimeMillis() * 1000;
}

#ifdef USE_SUBSECOND_RTC
static void rtc_read_frac_s(time_t * t, int * millis)
{
    RTC_HandleTypeDef RtcHandle;
    RTC_DateTypeDef dateStruct;
    RTC_TimeTypeDef timeStruct;
    struct tm timeinfo;
 
    RtcHandle.Instance = RTC;
 
    HAL_RTC_GetTime(&RtcHandle, &timeStruct, FORMAT_BIN);
    HAL_RTC_GetDate(&RtcHandle, &dateStruct, FORMAT_BIN);
 
    // Setup a tm structure based on the RTC
    timeinfo.tm_wday = dateStruct.WeekDay;
    timeinfo.tm_mon  = dateStruct.Month - 1;
    timeinfo.tm_mday = dateStruct.Date;
    timeinfo.tm_year = dateStruct.Year + 100;
    timeinfo.tm_hour = timeStruct.Hours;
    timeinfo.tm_min  = timeStruct.Minutes;
    timeinfo.tm_sec  = timeStruct.Seconds;

    // Convert to timestamp
    *t = mktime(&timeinfo);
    *millis = (timeStruct.SecondFraction-timeStruct.SubSeconds*1000) / (timeStruct.SecondFraction+1);
    return;
}
#endif /* USE_SUBSECOND_RTC */

#ifdef USE_TICK

static ticker_event_t _event;
static timestamp_t _delay;
static jlong _counter;
static const ticker_data_t* _ticker_data;

static void insert(timestamp_t t) {
    ticker_insert_event(_ticker_data, &_event, t, 0);
}

static void handler(uint32_t id) {
    if (id == 0) {
      insert(_event.timestamp + _delay);
      _counter++;
	} else {
	  uint32_t* ptr = (uint32_t*)id;
	  ((void (*)(uint32_t))ptr[0])((uint32_t)&ptr[0]);
	}
}

static void initTickCount() {
    _ticker_data = get_us_ticker_data();
    ticker_set_handler(_ticker_data, &handler);
    _delay = 1000;
    _counter = 0;
    insert(_delay + ticker_read(_ticker_data));
}

jlong sysTickCount(){
    return _counter;
}
#endif /* USE_TICK */

jlong sysTimeMillis(void) {
#ifdef USE_SUBSECOND_RTC
    time_t t;
    int millis;
    rtc_read_frac_s(&t, &millis);
    return (jlong)t * 1000 + millis;
#elif defined(USE_RTC)
    return (jlong)rtc_read() * 1000;
#elif defined(USE_TICK)
	return sysTickCount();
#else
#error
#endif
}

/**
 * Sleep Squawk for specified milliseconds
 */
extern jlong sysTickCount();
 
void osMilliSleep(long long millis) {
    long long target = sysTickCount() + millis;
    long long maxValue = 0x7FFFFFFFFFFFFFFFLL;
    if (target <= 0) target = maxValue; // overflow detected
    while (1) {
        if (checkForEvents()) break;
        if (sysTickCount() > target) break;
		__WFE();
	}
}

/* entry point */

extern int Squawk_main_wrapper(int argc, char *argv[]);

int main(int argc, char *argv[]) {
    return Squawk_main_wrapper(argc, argv);
}
