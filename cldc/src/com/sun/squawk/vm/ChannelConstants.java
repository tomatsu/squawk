/*
 * Copyright 2004-2009 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */

package com.sun.squawk.vm;

public final class ChannelConstants {
    
    /**
     * Purely static class should not be instantiated.
     */
    private ChannelConstants() {}

    /**
     * The channel identifier for the generic IO channel.
     */
    public final static int CHANNEL_GENERIC     = 1;

/*if[!ENABLE_CHANNEL_GUI]*/
    /**
     * The last fixed channel number.
     */
    public final static int CHANNEL_LAST_FIXED  = CHANNEL_GENERIC;
/*else[ENABLE_CHANNEL_GUI]*/
//    /**
//     * The channel identifier for the GUI input channel.
//     */
//    public final static int CHANNEL_GUIIN       = 2;
//
//    /**
//     * The channel identifier for the GUI output channel.
//     */
//    public final static int CHANNEL_GUIOUT      = 3;
//    /**
//     * The last fixed channel number.
//     */
//    public final static int CHANNEL_LAST_FIXED  = CHANNEL_GUIOUT;
/*end[ENABLE_CHANNEL_GUI]*/

/*if[!OLD_IIC_MESSAGES]*/
/*else[OLD_IIC_MESSAGES]*/
//    /**
//     * The channel for message I/O.
//     */
//    public final static int CHANNEL_MESSAGEIO   = 4;
//
//    /**
//     * The last fixed channel number.
//     */
//    public final static int CHANNEL_LAST_FIXED  = CHANNEL_MESSAGEIO;
/*end[OLD_IIC_MESSAGES]*/

/*if[!ENABLE_CHANNEL_GUI]*/
/*else[ENABLE_CHANNEL_GUI]*/
//    /**
//     * The GUI input repaint message.
//     */
//    public final static int GUIIN_REPAINT       = 0;
//
//    /**
//     * The GUI key input message.
//     */
//    public final static int GUIIN_KEY           = 1;
//
//    /**
//     * The GUI mouse message.
//     */
//    public final static int GUIIN_MOUSE         = 2;
//
//    /**
//     * The GUI exit message.
//     */
//    public final static int GUIIN_EXIT          = 3;
//
//    /**
//     * The GUI input repaint message.
//     */
//    public final static int GUIIN_HIBERNATE     = 4;
/*end[ENABLE_CHANNEL_GUI]*/


    public final static int

        /* Channel I/O result codes */

        RESULT_OK                               = 0,
        RESULT_BADCONTEXT                       = -1,
        RESULT_EXCEPTION                        = -2,
        RESULT_BADPARAMETER                     = -3,
        RESULT_MALLOCFAILURE                    = -4,

        /* I/O channel opcodes */

        GLOBAL_CREATECONTEXT                    = 1,
        GLOBAL_GETEVENT                         = 2,
        GLOBAL_POSTEVENT                        = 3,
        GLOBAL_WAITFOREVENT                     = 4,

        CONTEXT_DELETE                          = 5,
        CONTEXT_HIBERNATE                       = 6,
        CONTEXT_GETHIBERNATIONDATA              = 7,
        CONTEXT_GETCHANNEL                      = 8,
        CONTEXT_FREECHANNEL                     = 9,
        CONTEXT_GETRESULT                       = 10,
        CONTEXT_GETRESULT_2                     = 11,
        CONTEXT_GETERROR                        = 12,
        OPENCONNECTION                          = 13,       /* Opcodes for Generic connections */
        CLOSECONNECTION                         = 14,
        ACCEPTCONNECTION                        = 15,
        OPENINPUT                               = 16,
        CLOSEINPUT                              = 17,
        WRITEREAD                               = 18,
        READBYTE                                = 19,
        READSHORT                               = 20,
        READINT                                 = 21,
        READLONG                                = 22,
        READBUF                                 = 23,
        SKIP                                    = 24,
        AVAILABLE                               = 25,
        MARK                                    = 26,
        RESET                                   = 27,
        MARKSUPPORTED                           = 28,
        OPENOUTPUT                              = 29,
        FLUSH                                   = 30,
        CLOSEOUTPUT                             = 31,
        WRITEBYTE                               = 32,
        WRITESHORT                              = 33,
        WRITEINT                                = 34,
        WRITELONG                               = 35,
        WRITEBUF                                = 36,

/*if[!ENABLE_CHANNEL_GUI]*/
/*else[ENABLE_CHANNEL_GUI]*/
//        SETWINDOWNAME                           = 37,       /* Opcodes for KAWT graphics API */
//        SCREENWIDTH                             = 38,
//        SCREENHEIGHT                            = 39,
//        BEEP                                    = 40,
//        SETOFFSCREENMODE                        = 41,
//        FLUSHSCREEN                             = 42,
//        CREATEIMAGE                             = 43,
//        CREATEMEMORYIMAGE                       = 44,
//        GETIMAGE                                = 45,
//        IMAGEWIDTH                              = 46,
//        IMAGEHEIGHT                             = 47,
//        DRAWIMAGE                               = 48,
//        FLUSHIMAGE                              = 49,
//        CREATEFONTMETRICS                       = 50,
//        FONTSTRINGWIDTH                         = 51,
//        FONTGETHEIGHT                           = 52,
//        FONTGETASCENT                           = 53,
//        FONTGETDESCENT                          = 54,
//        SETFONT                                 = 55,
//        SETCOLOR                                = 56,
//        SETCLIP                                 = 57,
//        DRAWSTRING                              = 58,
//        DRAWLINE                                = 59,
//        DRAWOVAL                                = 60,
//        DRAWRECT                                = 61,
//        FILLRECT                                = 62,
//        DRAWROUNDRECT                           = 63,
//        FILLROUNDRECT                           = 64,
//        FILLARC                                 = 65,
//        FILLPOLYGON                             = 66,
//        REPAINT                                 = 67,
/*end[ENABLE_CHANNEL_GUI]*/

        /*
         * Internal codes used to execute C code on the service stack.
         */

        INTERNAL_SETSTREAM                      = 1000,
        INTERNAL_OPENSTREAM                     = 1001,
        INTERNAL_PRINTCHAR                      = 1002,
        INTERNAL_PRINTSTRING                    = 1003,
        INTERNAL_PRINTINT                       = 1004,
        INTERNAL_PRINTFLOAT                     = 1005,
        INTERNAL_PRINTDOUBLE                    = 1006,
        INTERNAL_PRINTUWORD                     = 1007,
        INTERNAL_PRINTOFFSET                    = 1008,
        INTERNAL_PRINTLONG                      = 1009,
        INTERNAL_PRINTADDRESS                   = 1010,
        INTERNAL_LOW_RESULT                     = 1011,
        INTERNAL_GETTIMEMILLIS_HIGH             = 1012,
        INTERNAL_GETTIMEMICROS_HIGH             = 1013,
        INTERNAL_STOPVM                         = 1014,
        /*INTERNAL_COPYBYTES                      = 1015,*/
        INTERNAL_PRINTCONFIGURATION             = 1016,
        INTERNAL_PRINTGLOBALOOPNAME             = 1017,
        INTERNAL_PRINTGLOBALS                   = 1018,
        /*INTERNAL_MATH                           = 1019,*/
        INTERNAL_GETPATHSEPARATORCHAR           = 1020,
        INTERNAL_GETFILESEPARATORCHAR           = 1021,

/*if[OLD_IIC_MESSAGES]*/
        /* Message I/O Operations */

        INTERNAL_ALLOCATE_MESSAGE_BUFFER        = 1022,
        INTERNAL_FREE_MESSAGE_BUFFER            = 1023,
        INTERNAL_SEND_MESSAGE_TO_SERVER         = 1024,
        INTERNAL_SEND_MESSAGE_TO_CLIENT         = 1025,
        INTERNAL_RECEIVE_MESSAGE_FROM_SERVER    = 1026,
        INTERNAL_RECEIVE_MESSAGE_FROM_CLIENT    = 1027,
        INTERNAL_SEARCH_SERVER_HANDLERS         = 1028,
/*end[OLD_IIC_MESSAGES]*/

        INTERNAL_NATIVE_PLATFORM_NAME           = 1029,
        
        /* Native Platform Support */
        DLOPEN                                  = 1996,
        DLCLOSE                                 = 1997,
        DLERROR                                 = 1998,
        DLSYM                                   = 1999,
        
        /* Book keeping */
        LAST_OPCODE                             = 1999;

            
/*if[DEBUG_CODE_ENABLED]*/
    private static final String[] Mnemonics = {
        "[invalid opcode]",
        "GLOBAL_CREATECONTEXT",     // 1
        "GLOBAL_GETEVENT",          // 2
        "GLOBAL_POSTEVENT",         // 3
        "GLOBAL_WAITFOREVENT",      // 4
        "CONTEXT_DELETE",           // 5
        "CONTEXT_HIBERNATE",        // 6
        "CONTEXT_GETHIBERNATIONDATA", // 7
        "CONTEXT_GETCHANNEL",       // 8
        "CONTEXT_FREECHANNEL",      // 9
        "CONTEXT_GETRESULT",        // 10
        "CONTEXT_GETRESULT_2",      // 11
        "CONTEXT_GETERROR",         // 12
        "OPENCONNECTION ",          // 13
        "CLOSECONNECTION ",         // 14
        "ACCEPTCONNECTION ",        // 15
        "OPENINPUT",                // 16
        "CLOSEINPUT",               // 17
        "WRITEREAD",                // 18
        "READBYTE",                 // 19
        "READSHORT",                // 20
        "READINT",                  // 21
        "READLONG",                 // 22
        "READBUF",                  // 23
        "SKIP",                     // 24
        "AVAILABLE",                // 25
        "MARK",                     // 26
        "RESET",                    // 27
        "MARKSUPPORTED",            // 28
        "OPENOUTPUT",               // 29
        "FLUSH",                    // 30
        "CLOSEOUTPUT",              // 31
        "WRITEBYTE",                // 32
        "WRITESHORT",               // 33
        "WRITEINT",                 // 34
        "WRITELONG",                // 35
        "WRITEBUF"                  // 36
/*if[!ENABLE_CHANNEL_GUI]*/
/*else[ENABLE_CHANNEL_GUI]*/
//        ,
//        "SETWINDOWNAME",            // 37
//        "SCREENWIDTH",              // 38
//        "SCREENHEIGHT",             // 39
//        "BEEP",                     // 40
//        "SETOFFSCREENMODE",         // 41
//        "FLUSHSCREEN",              // 42
//        "CREATEIMAGE",              // 43
//        "CREATEMEMORYIMAGE",        // 44
//        "GETIMAGE",                 // 45
//        "IMAGEWIDTH",               // 46
//        "IMAGEHEIGHT",              // 47
//        "DRAWIMAGE",                // 48
//        "FLUSHIMAGE",               // 49
//        "CREATEFONTMETRICS",        // 50
//        "FONTSTRINGWIDTH",          // 51
//        "FONTGETHEIGHT",            // 52
//        "FONTGETASCENT",            // 53
//        "FONTGETDESCENT",           // 54
//        "SETFONT",                  // 55
//        "SETCOLOR",                 // 56
//        "SETCLIP",                  // 57
//        "DRAWSTRING",               // 58
//        "DRAWLINE",                 // 59
//        "DRAWOVAL",                 // 60
//        "DRAWRECT",                 // 61
//        "FILLRECT",                 // 62
//        "DRAWROUNDRECT",            // 63
//        "FILLROUNDRECT",            // 64
//        "FILLARC",                  // 65
//        "FILLPOLYGON",              // 66
//        "REPAINT"                   // 67
/*end[ENABLE_CHANNEL_GUI]*/
    };

    public static String getMnemonic(int op) {
        try {
            return Mnemonics[op];
        } catch (ArrayIndexOutOfBoundsException ex) {
            return "invalid opcode:" + op;
        }
    }
/*end[DEBUG_CODE_ENABLED]*/

    /**
     * The channel identifier for the irq channel.
     */
    public static final int CHANNEL_IRQ = 101;

    /**
     * The irq wait message.
     */
    public static final int IRQ_WAIT = 201;

    public static final int CHANNEL_SPI = 102;
    public static final int SPI_SEND_RECEIVE_8 = 203;
    public static final int SPI_SEND_RECEIVE_8_PLUS_SEND_16 = 204;
    public static final int SPI_SEND_RECEIVE_8_PLUS_SEND_N = 205;
    public static final int SPI_SEND_RECEIVE_8_PLUS_RECEIVE_16 = 206;
    public static final int SPI_SEND_RECEIVE_8_PLUS_VARIABLE_RECEIVE_N = 207;
    public static final int SPI_SEND_AND_RECEIVE_WITH_DEVICE_SELECT = 208;
    public static final int SPI_SEND_AND_RECEIVE = 209;
    public static final int SPI_GET_MAX_TRANSFER_SIZE = 210;
    public static final int SPI_PULSE_WITH_DEVICE_SELECT = 211;

    public static final int GET_HARDWARE_REVISION = 290;
    
    public static final int I2C_OPEN            = 250;
    public static final int I2C_CLOSE           = 251;
    public static final int I2C_SET_CLOCK_SPEED = 252;
    public static final int I2C_READ            = 253;
    public static final int I2C_WRITE           = 254;
    public static final int I2C_BUSY            = 255;
    public static final int I2C_PROBE           = 256;

    public static final int GET_LAST_DEVICE_INTERRUPT_TIME_ADDR = 302;
    public static final int GET_CURRENT_TIME_ADDR = 303;

    public static final int FLASH_ERASE = 310;
    public static final int FLASH_WRITE = 311;

    public static final int USB_GET_STATE = 320;

    public static final int DEEP_SLEEP = 330;
    public static final int SHALLOW_SLEEP = 331;
    public static final int WAIT_FOR_DEEP_SLEEP = 332;
    public static final int DEEP_SLEEP_TIME_MILLIS_HIGH = 333;
    public static final int DEEP_SLEEP_TIME_MILLIS_LOW = 334;
    public static final int SET_DEEP_SLEEP_ENABLED = 335;
    public static final int SET_MINIMUM_DEEP_SLEEP_TIME = 336;
    public static final int TOTAL_SHALLOW_SLEEP_TIME_MILLIS_HIGH = 337;
    public static final int TOTAL_SHALLOW_SLEEP_TIME_MILLIS_LOW = 338;
    public static final int SET_SHALLOW_SLEEP_CLOCK_MODE = 339;

    public static final int GET_SERIAL_CHARS = 341;
    public static final int WRITE_SERIAL_CHARS = 342;
    public static final int AVAILABLE_SERIAL_CHARS = 343;
    
    public static final int AVR_GET_TIME_HIGH = 360;
    public static final int AVR_GET_TIME_LOW = 361;
    public static final int AVR_GET_STATUS = 362;
    
    public static final int WRITE_SECURED_SILICON_AREA = 370;
    public static final int READ_SECURED_SILICON_AREA = 371;
    
    public static final int SET_SYSTEM_TIME = 380;
    public static final int ENABLE_AVR_CLOCK_SYNCHRONISATION = 390;

	public static final int GET_PUBLIC_KEY = 400;
	
	public static final int COMPUTE_SHA1_FOR_MEMORY_REGION=410;
	public static final int COMPUTE_CRC16_FOR_MEMORY_REGION=420;
	
	public static final int REPROGRAM_MMU=430;
	public static final int GET_ALLOCATED_FILE_SIZE = 431;
	public static final int GET_FILE_VIRTUAL_ADDRESS = 432;
	
	public static final int GET_DMA_BUFFER_SIZE = 450;
	public static final int GET_DMA_BUFFER_ADDRESS = 451;
}
