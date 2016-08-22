#ifndef __GPIO_H__
#define __GPIO_H__

extern int open_gpio(int pin, int is_output);
extern int read_gpio(int desc);
extern void write_gpio(int desc, int value);
extern void close_gpio(int desc);
extern void set_mode_gpio(int desc, int mode);
extern int is_connected_gpio(int desc);

#endif
