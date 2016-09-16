#include <stdlib.h>
#include "i2c_api.h"
#include "common.h"

#if DEVICE_I2C

int Java_com_sun_squawk_hal_AbstractI2C_init(int sda, int scl) {
	i2c_t* i2c = (i2c_t*)malloc(sizeof(i2c_t));
	if (!i2c) {
		return -1;
	}
	i2c_init(i2c, sda, scl);
	int idx = allocate_desc(i2c);
	if (idx == -1) {
		free(i2c);
		return -1;
	}
	return idx;
}

int Java_com_sun_squawk_hal_AbstractI2C_freq(int desc, int hz) {
	i2c_t* i2c = get_object_from_desc(desc);
	if (i2c) {
		i2c_frequency(i2c, hz);
		return 0;
	} else {
		return -1;
	}
}

int Java_com_sun_squawk_hal_AbstractI2C_read(int desc, int address, uint8_t* data, int offset, int len, int repeat) {
	i2c_t* i2c = get_object_from_desc(desc);
	if (i2c) {
		int stop = (repeat) ? 0 : 1;
		int read = i2c_read(i2c, address, data, len, stop);
		return len != read;
	} else {
		return -1; // TODO
	}
}

int Java_com_sun_squawk_hal_I2C_read(int desc, int ack) {
	i2c_t* i2c = get_object_from_desc(desc);
	if (i2c) {
		if (ack) {
			return i2c_byte_read(i2c, 0);
		} else {
			return i2c_byte_read(i2c, 1);
		}
	} else {
		return -1;		// TODO
	}
}

int Java_com_sun_squawk_hal_AbstractI2C_write0(int desc, int address, uint8_t* data, int offset, int len, int repeat) {
	i2c_t* i2c = get_object_from_desc(desc);
	if (i2c) {
		int stop = (repeat) ? 0 : 1;
		int written = i2c_write(i2c, address, data, len, stop);
		return len != written;
	} else {
		return -1;  // TODO
	}
}

int Java_com_sun_squawk_hal_AbstractI2C_write1(int desc, int data) {
	i2c_t* i2c = get_object_from_desc(desc);
	if (i2c) {
		return i2c_byte_write(i2c, data);
	} else {
		return -1; // TODO
	}
}

void Java_com_sun_squawk_hal_I2C_start(int desc) {
	i2c_t* i2c = get_object_from_desc(desc);
	if (i2c) {
		i2c_start(i2c);
	} else {
		// TODO
	}
}

int Java_com_sun_squawk_hal_AbstractI2C_stop(int desc) {
	i2c_t* i2c = get_object_from_desc(desc);
	if (i2c) {
		i2c_stop(i2c);
		return 0;
	} else {
		return -1; // TODO
	}
}

void Java_com_sun_squawk_hal_I2CSlave_init(int desc) {
	i2c_t* i2c = get_object_from_desc(desc);
	if (i2c) {
		i2c_slave_mode(i2c, 1);
	} else {
		// TODO
	}
}

void Java_com_sun_squawk_hal_I2CSlave_address(int desc, int address) {
	i2c_t* i2c = get_object_from_desc(desc);
	if (i2c) {
		i2c_slave_address(i2c, 0, address, 0);
	} else {
		// TODO
	}
}

int Java_com_sun_squawk_hal_I2CSlave_receive0(int desc) {
	i2c_t* i2c = get_object_from_desc(desc);
	if (i2c) {
		return i2c_slave_receive(i2c);
	} else {
		return -1; // TODO
	}
}
#endif
