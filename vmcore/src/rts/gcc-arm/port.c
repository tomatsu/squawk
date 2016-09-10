#include "port_api.h"
#include "common.h"

#if DEVICE_PORTIN || DEVICE_PORTOUT

int Port_init(int port_name, int mask, int dir) {
	port_t* port = (port_t*)malloc(sizeof(port_t));
	if (!port) {
		return -1;
	}
	port_init(port, (PortName)port_name, mask, (PinDirection)dir);
    int idx = allocate_desc(port);
    if (idx < 0) {
		free(port);
		return -1;
    }
	return idx;
}

int Port_mode(int desc, int mode) {
	port_t* port = get_object_from_desc(desc);
	if (port) {
		port_mode(port, (PinMode)mode);
		return 0;
	}
	return -1;
}

int Port_dir(int desc, int dir) {
	port_t* port = get_object_from_desc(desc);
	if (port) {
		port_dir(port, (PinDirection)dir);
		return 0;
	}
	return -1;
}

int Port_write(int desc, int value) {
	port_t* port = get_object_from_desc(desc);
	if (port) {
		port_write(port, value);
		return 0;
	}
	return -1;
}

int Port_read(int desc) {
	port_t* port = get_object_from_desc(desc);
	if (port) {
		return port_read(port);
	}
	return -1;
}
#endif
