static void init_done(void) {
    Squawk_main_wrapper(0, 0);
}

void user_init(void) {
	uart_div_modify(0, 80*1000000 / 115200);
    system_init_done_cb(init_done);
}
