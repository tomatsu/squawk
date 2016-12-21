#pragma once

#include <stdint.h>

#define BOOTSTRAP_SUITE_ADDR_SYM _bootstrap_suite

typedef struct {
  uint32_t off;
  uint32_t hash;
  uint32_t size;
  uint32_t memory[];
} romized_suite_t;

extern const romized_suite_t BOOTSTRAP_SUITE_ADDR_SYM;
