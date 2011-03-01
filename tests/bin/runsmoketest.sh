#!/bin/sh
#
 
if [ $1 = SOCKET ]; then
    echo "Can't run smake test on SOCKET platform"
    exit 0
fi
 
if [ $1 = BARE_METAL ]; then
    echo "Can't run smoke test on BARE_METAL platform"
    exit 0
fi
 
./squawk com.sun.squawk.Test
STAT=$?
# status got trunctated from 12345 to 57
if [ $STAT -eq 57 ]; then
	echo good  $STAT
	exit 0
else
	echo bad $STAT
	exit $STAT
fi
