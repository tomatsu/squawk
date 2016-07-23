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

#define TRUE 1
#define FALSE 0

#include <stdlib.h>
#include <sys/time.h>
//#include <dlfcn.h>
#include <jni.h>

#define jlong  int64_t

/* The package that conmtains the native code to use for a "NATIVE" platform type*/
#if defined(sun)
#define sysPlatformName() "solaris"
#else
#define sysPlatformName() "linux"
#endif

#if !PLATFORM_TYPE_BARE_METAL
#include "os_posix.c"
#else
#ifdef __arm__
#include "rtc_api.h"
#include "gpio_api.h"
#include "ticker_api.h"
#include "us_ticker_api.h"
#endif
#define sysGetPageSize() 128
#define sysToggleMemoryProtection(x,y,z)
//#define ioExecute() 
#define sysValloc(s) memalign(sysGetPageSize(),s)
#define sysVallocFree(p) free(p)

#if !PLATFORM_TYPE_BARE_METAL
jlong sysTimeMicros() {
    struct timeval tv;
//    printf("######## sysTimeMicros()\n");
    gettimeofday(&tv, NULL);
    /* We adjust to 1000 ticks per second */
    return (jlong)tv.tv_sec * 1000000 + tv.tv_usec;
}
#else
jlong sysTimeMicros() {
    extern jlong sysTimeMillis(void);
    return sysTimeMillis() * 1000;
}
#endif

#if defined(STM32L0) || defined(STM32L1) || defined(STM32L4) || defined(STM32F0) || defined(STM32F1) || defined(STM32F3) || defined(STM32F4) || defined(STM32F7)
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
#endif


jlong sysTimeMillis(void) {
#ifdef __arm__
#if defined(STM32L4) || defined(STM32F4)
    time_t t;
    int millis;
    rtc_read_frac_s(&t, &millis);
    return (jlong)t * 1000 + millis;
#else    
    static int initialized = 0;
    if (!initialized) {
	rtc_init();
	initialized = 1;
    }
    return (jlong)rtc_read() * 1000;
#endif
#else
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return (jlong)tv.tv_sec * 1000;
#endif
}

static ticker_event_t _event;
static timestamp_t _delay;
static jlong _counter;
static const ticker_data_t* _ticker_data;

static void insert(timestamp_t t) {
    ticker_insert_event(_ticker_data, &_event, t, 0);
}

static void handler(uint32_t id) {
    insert(_event.timestamp + _delay);
    _counter++;
}
    
void initTickCount() {
    _ticker_data = get_us_ticker_data();
    ticker_set_handler(_ticker_data, &handler);
    _delay = 1000;
    _counter = 0;
    insert(_delay + ticker_read(_ticker_data));
}

jlong sysTickCount(){
    return _counter;
}

#endif

/** 
 * Return another path to find the bootstrap suite with the given name.
 * On some platforms the suite might be stored in an odd location
 * 
 * @param bootstrapSuiteName the name of the boostrap suite
 * @return full or partial path to alternate location, or null
 */
INLINE char* sysGetAlternateBootstrapSuiteLocation(char* bootstrapSuiteName) { return NULL; }

#if PLATFORM_TYPE_DELEGATING
jint createJVM(JavaVM **jvm, void **env, void *args) {
    jint (JNICALL *CreateJavaVM)(JavaVM **jvm, void **env, void *args) = 0;
    const char* name = "libjvm.so";
    void* libVM = dlopen(name, RTLD_LAZY);
    if (libVM == 0) {
        fprintf(stderr, "Cannot load %s\n", name);
        fprintf(stderr, "Please add the directories containing libjvm.so and libverify.so\n");
        fprintf(stderr, "to the LD_LIBRARY_PATH environment variable.\n");
        return false;
    }

    CreateJavaVM = (jint (JNICALL *)(JavaVM **,void **, void *)) dlsym(libVM, "JNI_CreateJavaVM");

    if (CreateJavaVM == 0) {
        fprintf(stderr,"Cannot resolve JNI_CreateJavaVM in %s\n", name);
        return false;
    }

    return CreateJavaVM(jvm, env, args) == 0;
}
#endif

#define osloop()        /**/
#define osbackbranch()  /**/
#define osfinish()      /**/
