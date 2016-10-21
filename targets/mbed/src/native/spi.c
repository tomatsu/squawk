#include <stdlib.h>
#include "spi_api.h"
#include "common.h"

#if DEVICE_SPI

int Java_com_sun_squawk_hal_AbstractSPI_init(int mosi, int miso, int sclk, int ssel) {
	spi_t* spi = (spi_t*)malloc(sizeof(spi_t));
	if (!spi) {
		return -1;
	}
	spi_init(spi, mosi, miso, sclk, ssel);
    int idx = allocate_desc(spi);
    if (idx < 0) {
		free(spi);
		return -1;
    }
	return idx;
}

int Java_com_sun_squawk_hal_AbstractSPI_format(int desc, int bits, int mode, int slave) {
	spi_t* spi = get_object_from_desc(desc);
	if (spi) {
		spi_format(spi, bits, mode, slave);
		return 0;
	}
	return -1;
}

int Java_com_sun_squawk_hal_AbstractSPI_freq(int desc, int hz) {
	spi_t* spi = get_object_from_desc(desc);
	if (spi) {
		spi_frequency(spi, hz);
		return 0;
	}
	return -1;
}

int Java_com_sun_squawk_hal_AbstractSPI_write(int desc, int value, int slave) {
	spi_t* spi = get_object_from_desc(desc);
	if (spi) {
		if (slave) {
			spi_slave_write(spi, value);
		} else {
			spi_master_write(spi, value);
		}
		return 0;
	}
	return -1;
}

int Java_com_sun_squawk_hal_SPISlave_read0(int desc) {
	spi_t* spi = get_object_from_desc(desc);
	if (spi) {
		return spi_slave_read(spi);
	}
	return -1;
}

int Java_com_sun_squawk_hal_SPISlave_receive0(int desc) {
	spi_t* spi = get_object_from_desc(desc);
	if (spi) {
		return spi_slave_receive(spi);
	}
	return -1;
}
#endif
