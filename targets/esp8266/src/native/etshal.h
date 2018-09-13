#ifndef _INCLUDED_ETSHAL_H_
#define _INCLUDED_ETSHAL_H_

#include <os_type.h>

void ets_intr_lock(void);
void ets_intr_unlock(void);
void ets_isr_attach(int irq_no, void (*handler)(void *), void *arg);
void ets_set_idle_cb(void (*handler)(void *), void *arg);

#endif // _INCLUDED_ETSHAL_H_
