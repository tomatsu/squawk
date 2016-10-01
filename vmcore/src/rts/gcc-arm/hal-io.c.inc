
long long rebuildLongParam(int i1, int i2) {
    return ((long long)i1 << 32) | ((long long)i2 & 0xFFFFFFFF);
}

/**
 * Executes an operation on a given channel for an isolate.
 *
 * @param  context the I/O context
 * @param  op      the operation to perform
 * @param  channel the identifier of the channel to execute the operation on
 * @param  i1
 * @param  i2
 * @param  i3
 * @param  i4
 * @param  i5
 * @param  i6
 * @param  send
 * @param  receive
 * @return the operation result
 */
static void ioExecute(void) {
//  int     context = com_sun_squawk_ServiceOperation_context;
    int     op      = com_sun_squawk_ServiceOperation_op;
//  int     channel = com_sun_squawk_ServiceOperation_channel;
    int     i1      = com_sun_squawk_ServiceOperation_i1;
    int     i2      = com_sun_squawk_ServiceOperation_i2;
    int     i3      = com_sun_squawk_ServiceOperation_i3;
//  int     i4      = com_sun_squawk_ServiceOperation_i4;
//  int     i5      = com_sun_squawk_ServiceOperation_i5;
//  int     i6      = com_sun_squawk_ServiceOperation_i6;
    Address send    = com_sun_squawk_ServiceOperation_o1;
    Address receive = com_sun_squawk_ServiceOperation_o2;

    int res = ChannelConstants_RESULT_OK;

    switch (op) {
	case ChannelConstants_GLOBAL_GETEVENT: {
		res = getEvent();
		break;
	}

	case ChannelConstants_GLOBAL_WAITFOREVENT: {
		long long millisecondsToWait = rebuildLongParam(i1, i2);
		osMilliSleep(millisecondsToWait);
		break;
	}

	case ChannelConstants_IRQ_WAIT: {
		int addr = i1;
		int s = storeIrqRequest(addr);
		if (s == -1) {
		   res = ChannelConstants_RESULT_EXCEPTION;
		} else {
		   res = s;
		}
	}
		break;
	default: {
//            ioExecuteSys(); // do platform-specific
//            res = com_sun_squawk_ServiceOperation_result; // result set by ioExecuteSys.
	    printf("###### skip op: %d\n", op);
		break;
	}
    }
    com_sun_squawk_ServiceOperation_result = res;
}


void set_event(int evt) {
	 com_sun_squawk_ServiceOperation_event = evt;
}