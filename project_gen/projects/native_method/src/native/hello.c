#include <stdio.h>

#include "classes.h"

void Java_Hello_sayHello() {
	printf("Hello from native method.\n");
}

void Java_Hello_passString(void* arg) {
	char* s = (char*)arg;
	int i;
	for (i = 0; i < 8; i++) {
		printf("%02x ", s[i]);
	}
	printf("\n");
}

void Java_Hello_receive(void* arg) {
	printf("integer = %d\n", Hello_Struct_integer(arg));
	printf("singleByte = %d\n", Hello_Struct_singleByte(arg));
	set_Hello_Struct_integer(arg, 4321);
	set_Hello_Struct_singleByte(arg, 0x0f);
	char* bytearray = (char*)Hello_Struct_bytearray(arg);
	int len = getArrayLength(bytearray);
	int i;
	for (i = 0; i < len; i++) {
		printf("%02d ", bytearray[i]);
	}
	printf("\n");
}
