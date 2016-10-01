package com.sun.squawk.hal;

/*
 * NRF51_MICROBIT
 */
public interface PinNames {
	int LED1 = 3;
	int LED2 = 2;
	int LED3 = 1;
	int LED4 = 16;
	int USBTX = 24;
	int TX_PIN_NUMBER = 25;
	int USBRX = 25;
	int RX_PIN_NUMBER = 25;
	int BUTTON_A = 17;
	int TGT_RESET = 19;
	int BUTTON_B = 26;
	int I2C_SCL0 = 0;
	int I2C_SDA0 = 30;
	int MOSI = 21;
	int MISO = 22;
	int SCK = 23;
	int ACCEL_INT2 = 27;
	int ACCEL_INT1 = 28;
	int MAG_INT1 = 29;

    int PAD3 = 1;
    int PAD2 = 2;
	int PAD1 = 3;
    
    //LED MATRIX COLS
    int COL1 = 4;
    int COL2 = 5;
    int COL3 = 6;
    int COL4 = 7;
    int COL5 = 8;
    int COL6 = 9;
    int COL7 = 10;
    int COL8 = 11;
	int COL9 = 12;

    //LED MATRIX ROWS
    int ROW1 = 13;
    int ROW2 = 14;
    int ROW3 = 15;

	// Pin Mode
	int PullDown = 1;
	int PullUp = 3;
}
