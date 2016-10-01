#include <uart.h>
#include <osapi.h>

typedef void* Address;
typedef long long jlong;

void cioPrintCString(char* s) {
	uart0_tx_buffer(s, (uint16)strlen(s));
}

void cioPrintChar(int ch) {
    uart_tx_one_char(UART0, ch);	
}

void cioPrintWord(int x) {
	char buf[16];
	os_sprintf(buf, "%d", x);
	cioPrintCString(buf);
}

void cioPrintUWord(int val) {
	char buf[16];
	os_sprintf(buf, "%u", val);
	cioPrintCString(buf);
}

void cioPrintDouble(double d) {
	// TODO
}

void cioPrintFloat(float f) {
	// TODO
}

void cioPrintLong(jlong x) {
	char buf[32];
	os_sprintf(buf, "%lld", x);
	cioPrintCString(buf);
}

void cioPrintBytes(char* bytes, int len) {
	uart0_tx_buffer(bytes, len);
}
