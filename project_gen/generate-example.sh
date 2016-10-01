BUILD_TOP=${BUILD_TOP:-/tmp}
TARGET=${TARGET:-linux}

MAIN_CLASS_NAME=${MAIN_CLASS_NAME:-Hello}
PROJECT=${PROJECT:-helloworld}
PROJECT_DIR=${BUILD_TOP}/$(echo $TARGET | tr '[:upper:]' '[:lower:]')_${PROJECT}

if [ "x${TARGET}" = "xlinux" ]; then
	# copy squawk
    mkdir -p ${PROJECT_DIR}/squawk
    (files="$(cat files_linux.txt)"; cd ..; tar cf - $files) | (cd ${PROJECT_DIR}/squawk; tar xf -)
	
	# copy application
    (cd projects/${PROJECT}; tar cf - .) | (cd ${PROJECT_DIR}; tar xf -)
	
	# create Makefile
	echo "MAIN_CLASS_NAME = ${MAIN_CLASS_NAME}" > ${PROJECT_DIR}/Makefile
	cat makefile_linux-i386.tmpl >> ${PROJECT_DIR}/Makefile
	exit 0
fi

if [ "x${TARGET}" = "xESP8266" ]; then
	# copy squawk
	mkdir -p ${PROJECT_DIR}/squawk
	(files="$(cat files_esp8266.txt)"; cd ..; tar cf - $files) | (cd ${PROJECT_DIR}/squawk; tar xf -)
	
	# create Makefile	
	echo "-include target.mk" >> ${PROJECT_DIR}/Makefile
	echo "MAIN_CLASS_NAME ?= ${MAIN_CLASS_NAME}" >> ${PROJECT_DIR}/Makefile
	cat makefile_esp.tmpl >> ${PROJECT_DIR}/Makefile
	(cd targets/${TARGET}/; tar cf - .) | (cd ${PROJECT_DIR}/; tar xf -)

	# copy application
	(cd projects/${PROJECT}; tar cf - .) | (cd ${PROJECT_DIR}; tar xf -)
	
	exit 0
fi

# assume MBED

MBED=mbed/.build/mbed

# clone mbed
if [ ! -d mbed ]; then
	git clone https://github.com/mbedmicro/mbed.git
fi

if [ ! -d ${MBED}/TARGET_${TARGET} ]; then
	(cd mbed; python tools/build.py -m ${TARGET} -t GCC_ARM)
fi

# copy mbed
mkdir -p ${PROJECT_DIR}/mbed
cp $MBED/*h ${PROJECT_DIR}/mbed
tar cf - -C $MBED TARGET_${TARGET} | (cd ${PROJECT_DIR}/mbed; tar xf -)

# copy hal_api
(cd ..; tar cf - hal_api/src) | (cd ${PROJECT_DIR}; tar xf -)

# copy squawk
mkdir -p ${PROJECT_DIR}/squawk
(files="$(cat files_mbed.txt)"; cd ..; tar cf - $files) | (cd ${PROJECT_DIR}/squawk; tar xf -)

# create Makefile
echo "MBED = mbed" > ${PROJECT_DIR}/Makefile
echo "-include target.mk" >> ${PROJECT_DIR}/Makefile
echo "MAIN_CLASS_NAME ?= ${MAIN_CLASS_NAME}" >> ${PROJECT_DIR}/Makefile
python mbed_devices.py ${TARGET} >> ${PROJECT_DIR}/Makefile
cat makefile_mbed_gcc.tmpl >> ${PROJECT_DIR}/Makefile
(cd targets/${TARGET}/; tar cf - .) | (cd ${PROJECT_DIR}/; tar xf -)

# copy application
(cd projects/${PROJECT}; tar cf - .) | (cd ${PROJECT_DIR}; tar xf -)
