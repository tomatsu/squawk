#include <stdbool.h>
#include <stdint.h>
#include <spiffs.h>
#include <spiffs_nucleus.h>
#include <c_types.h>
#include <spi_flash.h>
#include <mem.h>
#include <osapi.h>
#include "unused.h"
#include "classes.h"

#define malloc os_malloc
#define free os_free
#define min(a,b) ((a) > (b) ? (b) : (a))

#define FLASH_INT_MASK 0x23a

#define CSTR(str, n)							\
	int str ## _len = getArrayLength(str);		\
	if (str ## _len > n) str ## _len = n;		\
	uint8_t c_ ## str [str ## _len + 1];		\
	memcpy(c_ ## str, str, str ## _len);		\
	c_ ## str[str ## _len] = 0;						   

static void check_cb_f(spiffs_check_type type, spiffs_check_report report, uint32_t a1, uint32_t a2) {
	/* skip */
}

#ifndef SPIFFS_MAX_OPEN_FILES
#define SPIFFS_MAX_OPEN_FILES 4
#endif

static spiffs squawk_spiffs_fs = {0};
static uint8_t* squawk_spiffs_work;
static uint8_t* squawk_spiffs_fd_space;
static uint8_t* squawk_spiffs_cache;
static size_t squawk_cache_size;

static bool initialize_cache() {
	squawk_cache_size = SPIFFS_buffer_bytes_for_cache(&squawk_spiffs_fs, SPIFFS_MAX_OPEN_FILES);
	squawk_spiffs_cache = (uint8_t*)malloc(squawk_cache_size);
	return squawk_spiffs_cache != 0;
}

static int32_t spiffs_hal_read(uint32_t addr, uint32_t size, uint8_t *dst) {
	if (addr < SPIFFS_CFG_PHYS_ADDR(&squawk_spiffs_fs)) {
		printf("FATAL write addr too low %08x < %08x\n", addr, SPIFFS_CFG_PHYS_ADDR(&squawk_spiffs_fs));
		return -1;
	}
	if (addr + size > SPIFFS_CFG_PHYS_ADDR(&squawk_spiffs_fs) + SPIFFS_CFG_PHYS_SZ(&squawk_spiffs_fs)) {
		printf("FATAL write addr too high %08x + %08x > %08x\n", addr, size, SPIFFS_CFG_PHYS_ADDR(&squawk_spiffs_fs) + SPIFFS_CFG_PHYS_SZ(&squawk_spiffs_fs));
		return -1;
	}
	uint8_t* dst0 = dst;
	if (addr & 3) {
		uint32_t v;
		spi_flash_read(addr & ~3, &v, 4);
		uint32_t n = 4 - (addr & 3);
		uint8_t* p = ((uint8_t*)&v) + n;
		for (int i = 0; i < n; i++) {
			*dst++ = *p++;
		}
		size -= n;
		addr += n;
	}
	uint32_t n = (size & ~3);
	if (n > 0) {
		spi_flash_read(addr, dst, n);
		size -= n;
		addr += n;
		dst += n;
	}
	if (size > 0) {
		uint32_t v;
		uint8_t* p = &v;
		spi_flash_read(addr, &v, 4);
		for (int i = 0; i < size; i++) {
			*dst++ = *p++;
		}
	}
}

static int32_t spiffs_hal_write(uint32_t addr, uint32_t size, uint8_t *src) {
	uint8_t* _src = src;
	uint32_t _addr = addr;
	uint32_t _size = size;
	
	if (addr < SPIFFS_CFG_PHYS_ADDR(&squawk_spiffs_fs)) {
		printf("FATAL write addr too low %08x < %08x\n", addr, SPIFFS_CFG_PHYS_ADDR(&squawk_spiffs_fs));
		return -1;
	}
	if (addr + size > SPIFFS_CFG_PHYS_ADDR(&squawk_spiffs_fs) + SPIFFS_CFG_PHYS_SZ(&squawk_spiffs_fs)) {
		printf("FATAL write addr too high %08x + %08x > %08x\n", addr, size, SPIFFS_CFG_PHYS_ADDR(&squawk_spiffs_fs) + SPIFFS_CFG_PHYS_SZ(&squawk_spiffs_fs));
		return -1;
	}
	int m = addr & 3;
	if (m) {
		uint32_t v = ~0;
		uint8_t* p = (uint8_t*)&v;
		for (int i = 0; i < 4 - m; i++) {
			if (size == 0) {
				break;
			}
			p[m + i] = src[i];
			size--;
		}
		int rc = spi_flash_write(addr & ~3, p, 4);
		if (rc != SPI_FLASH_RESULT_OK) {
			printf("spi_flash_write failed %d\n", rc);
			return -1;
		}
		src += (4 - m);
		addr += (4 - m);
	}

	if (size > 0) {
		if ((uintptr_t)src & 3) {
			uint32_t buf[64];
			while (size > 3) {
				int len = min(sizeof(buf), size);
				memcpy(buf, src, len);
				int rc = spi_flash_write(addr, buf, len);
				if (rc != SPI_FLASH_RESULT_OK) {
					printf("spi_flash_write failed %d\n", rc);
					return -1;
				}
				src += len;
				addr += len;
				size -= len;
			}

			if (size > 0) {
				uint32_t v = ~0;
				uint8_t* p = (uint8_t*)&v;
				for (int i = 0; i < size; i++) {
					*p++ = *src++;
				}
				int rc = spi_flash_write(addr, &v, 4);
				if (rc != SPI_FLASH_RESULT_OK) {
					printf("spi_flash_write failed %d\n", rc);
					return -1;
				}
			}
			
		} else {
			if (size & ~3) {
				int rc = spi_flash_write(addr, src, size & ~3);
				if (rc != SPI_FLASH_RESULT_OK) {
					printf("spi_flash_write failed %d\n", rc);
					return -1;
				}
			}
			addr += (size & ~3);
			src += (size & ~3);
			
			int k = size & 3;
			if (k) {
				uint32_t v = ~0;
				uint8_t* p = (uint8_t*)&v;
				for (int i = 0; i < k; i++) {
					p[i] = src[i];
				}
				int rc = spi_flash_write(addr, p, 4);
				if (rc != SPI_FLASH_RESULT_OK) {
					printf("spi_flash_write failed %d\n", rc);
					return -1;
				}
			}
		}
	}

#ifdef DEBUG
	uint8_t tmp[_size];
	spi_flash_read(_addr, tmp, _size);
	for (int i = 0; i < _size; i++) {
		if (_src[i] != tmp[i]) {
			printf("spiffs_hal_write failed to write data.\n");
			return -1;
		}
	}
#endif
	return SPIFFS_OK;
}

static int32_t spiffs_hal_erase(uint32_t addr, uint32_t size) {
	if (addr & (SPIFFS_CFG_PHYS_ERASE_SZ(&squawk_spiffs_fs) - 1)) {
		printf("trying to erase at addr %08x, out of boundary\n", addr);
		return -1;
	}
	if (size & (SPIFFS_CFG_PHYS_ERASE_SZ(&squawk_spiffs_fs) - 1)) {
		printf("trying to erase at with size %08x, out of boundary\n", size);
		return -1;
	}
    uint32_t sector = addr / SPIFFS_CFG_PHYS_ERASE_SZ(&squawk_spiffs_fs);
    uint32_t sectorCount = size / SPIFFS_CFG_PHYS_ERASE_SZ(&squawk_spiffs_fs);
    for (int i = 0; i < sectorCount; ++i) {
		int rc = spi_flash_erase_sector(sector + i);
		if (rc != SPI_FLASH_RESULT_OK) {
			return -1;
		}
	}
	return SPIFFS_OK;
}
	
bool Java_spiffs_FileSystem_mount0() {
	if (!SPIFFS_mounted(&squawk_spiffs_fs)) {
		spiffs_config config = {0};
		config.hal_read_f = &spiffs_hal_read;
		config.hal_write_f = &spiffs_hal_write;
		config.hal_erase_f = &spiffs_hal_erase;
		squawk_spiffs_work = (uint8_t*)malloc(512);
		if (!squawk_spiffs_work) {
			return false;
		}
		squawk_spiffs_fd_space = (uint8_t*)malloc(SPIFFS_MAX_OPEN_FILES * sizeof(spiffs_fd));
		if (!squawk_spiffs_fd_space) {
			free(squawk_spiffs_work);
			return false;
		}
		if (!initialize_cache()) {
			free(squawk_spiffs_work);
			free(squawk_spiffs_fd_space);
			return false;
		}
		int32_t res = SPIFFS_mount(&squawk_spiffs_fs,
								   &config,
								   squawk_spiffs_work,
								   squawk_spiffs_fd_space,
								   SPIFFS_MAX_OPEN_FILES * sizeof(spiffs_fd),
								   squawk_spiffs_cache,
								   squawk_cache_size, check_cb_f);
		if (res != SPIFFS_OK) {
			printf("SPIFFS_mount failed, errono=%d\n", SPIFFS_errno(&squawk_spiffs_fs));
			return false;
		}
		return SPIFFS_mounted(&squawk_spiffs_fs);
	}
	return true;
}

bool Java_spiffs_FileSystem_unmount0() {
	if (SPIFFS_mounted(&squawk_spiffs_fs)) {
		SPIFFS_unmount(&squawk_spiffs_fs);
		free(squawk_spiffs_work);
		free(squawk_spiffs_fd_space);
	}
}	   

bool Java_spiffs_FileSystem_format0() {
	int32_t res = SPIFFS_format(&squawk_spiffs_fs);
	return (res == SPIFFS_OK);
}	   

bool Java_spiffs_FileSystem_rename0(char* from,  char* to) {
	CSTR(from, 31);
	CSTR(to, 31);
	int32_t res = SPIFFS_rename(&squawk_spiffs_fs, c_from, c_to);
	return (res == SPIFFS_OK);
}	   

bool Jav_spiffs_FileSystem_delete0(char* path) {
	CSTR(path, 31);
	int res = SPIFFS_remove(&squawk_spiffs_fs, c_path);
	return (res == SPIFFS_OK);
}	   

bool Java_spiffs_FileSystem_exists0(char* path) {
	CSTR(path, 31);
	spiffs_stat s;
	int rc = SPIFFS_stat(&squawk_spiffs_fs, c_path, &s);
	return rc == SPIFFS_OK;
}	   

int Java_spiffs_FileSystem_opendir(char* path) {
	spiffs_DIR* dir = (spiffs_DIR*)malloc(sizeof(spiffs_DIR));
	if (!dir) {
		printf("out of memory\n");
		return 0;
	}
	CSTR(path, 31);
	int32_t res = SPIFFS_opendir(&squawk_spiffs_fs, c_path, dir);
	if (res == 0) {
		printf("SPIFFS_opendir failed, errono=%d\n", SPIFFS_errno(&squawk_spiffs_fs));
		free(dir);
		return 0;
	}
	return dir;
}	   

int Java_spiffs_FileSystem_readdir(int handle, char* name) {
	struct spiffs_dirent e;
	int32_t res = SPIFFS_readdir((spiffs_DIR*)handle, &e);
	if (res == 0) {
		return 0;
	}
	int len = strlen(e.name);
	if (len > 31) len = 31;
	memcpy(name, e.name, len);
	return len;
}	   

int Java_spiffs_FileSystem_closedir(int handle) {
	return SPIFFS_closedir((spiffs_DIR*)handle);
}	   

int Java_spiffs_FileSystem_getLastError0() {
	return SPIFFS_errno(&squawk_spiffs_fs);
}

int Java_spiffs_FileInputStream_open(char* path) {
	CSTR(path, 31);
	return SPIFFS_open(&squawk_spiffs_fs, c_path, SPIFFS_RDONLY, 0);
}

int Java_spiffs_FileInputStream_read1(int handle) {
	uint8_t buf[1];
	if (SPIFFS_read(&squawk_spiffs_fs, handle, buf, 1) < 0) {
		if (SPIFFS_eof(&squawk_spiffs_fs, handle)) {
			return -1;
		}
		return -2;
	}
	return (int)buf[0];
}

int Java_spiffs_FileInputStream_read0(int handle, uint8_t* buf, int offset, int size) {
	int n = SPIFFS_read(&squawk_spiffs_fs, handle, buf + offset, size);
	if (n < 0) {
		if (SPIFFS_eof(&squawk_spiffs_fs, handle)) {
			return -1;
		}
		return -2;
	}
	return n;
}

int Java_spiffs_FileInputStream_skip0(int handle, int n) {
	SPIFFS_lseek(&squawk_spiffs_fs, (spiffs_file)handle, n, SPIFFS_SEEK_CUR);
}

int Java_spiffs_FileInputStream_available0(int handle) {
	int total = SPIFFS_tell(&squawk_spiffs_fs, (spiffs_file)handle);
	int cur = SPIFFS_lseek(&squawk_spiffs_fs, (spiffs_file)handle, 0, SPIFFS_SEEK_CUR);
	if (total > cur) {
		return total - cur;
	} else {
		return 0;
	}
}

int Java_spiffs_FileInputStream_close0(int handle) {
	SPIFFS_close(&squawk_spiffs_fs, (spiffs_file)handle);
}

int Java_spiffs_FileOutputStream_open(char* path) {
	CSTR(path, 31);
	return SPIFFS_open(&squawk_spiffs_fs, c_path, SPIFFS_CREAT|SPIFFS_WRONLY|SPIFFS_TRUNC, 0);
}

int Java_spiffs_FileOutputStream_write1(int handle, int ch) {
	uint8_t u8 = (uint8_t)ch;
	return SPIFFS_write(&squawk_spiffs_fs, (spiffs_file)handle, &u8, 1);
}

int Java_spiffs_FileOutputStream_write0(int handle, uint8_t* buf, int offset, int size) {
	int ret = SPIFFS_write(&squawk_spiffs_fs, (spiffs_file)handle, buf + offset, size);
	return ret;
}

int Java_spiffs_FileOutputStream_flush0(int handle) {
	SPIFFS_fflush(&squawk_spiffs_fs, (spiffs_file)handle);
	return 0;
}

int Java_spiffs_FileOutputStream_close0(int handle) {
	SPIFFS_close(&squawk_spiffs_fs, (spiffs_file)handle);
	return 0;
}
