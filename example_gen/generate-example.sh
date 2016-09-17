BUILD_TOP=${BUILD_TOP:-/tmp}
TARGET=${TARGET:-linux}

if [ "x${TARGET}" = "xlinux" ]; then
	MAIN_CLASS_NAME=${MAIN_CLASS_NAME:-Hello}
	PROJECT=${PROJECT:-helloworld}
	PROJECT_DIR=${BUILD_TOP}/$(echo $TARGET | tr '[:upper:]' '[:lower:]')_${PROJECT}

	# copy squawk
    mkdir -p ${PROJECT_DIR}/squawk
    (files="$(cat files_linux.txt)"; cd ..; tar cf - $files) | (cd ${PROJECT_DIR}/squawk; tar xf -)
    cp ../build-mbed.properties ${PROJECT_DIR}/build.properties
    cp ../squawk.exclude ${PROJECT_DIR}
    cp ../squawk.library.properties ${PROJECT_DIR}
	
	# copy application
    (cd ${PROJECT}; tar cf - .) | (cd ${PROJECT_DIR}; tar xf -)
	
	# create Makefile
	echo "MAIN_CLASS_NAME = ${MAIN_CLASS_NAME}" > ${PROJECT_DIR}/Makefile
	cat makefile_linux-i386.tmpl >> ${PROJECT_DIR}/Makefile
	exit 0
fi

MBED=mbed/.build/mbed
MAIN_CLASS_NAME=${MAIN_CLASS_NAME:-Blinky}
PROJECT=${PROJECT:-blinky}
PROJECT_DIR=${BUILD_TOP}/$(echo $TARGET | tr '[:upper:]' '[:lower:]')_${PROJECT}

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
cp ${MBED}/../../hal/targets.json ${PROJECT_DIR}

# copy hal_api
(cd ..; tar cf - hal_api/src) | (cd ${PROJECT_DIR}; tar xf -)

# copy squawk
mkdir -p ${PROJECT_DIR}/squawk
(files="$(cat files_mbed.txt)"; cd ..; tar cf - $files) | (cd ${PROJECT_DIR}/squawk; tar xf -)
cp ../build-mbed.properties ${PROJECT_DIR}/build.properties
cp ../squawk.exclude ${PROJECT_DIR}
cp ../squawk.library.properties ${PROJECT_DIR}

# create Makefile
echo "MBED = mbed" > ${PROJECT_DIR}/Makefile
echo "-include target.mk" >> ${PROJECT_DIR}/Makefile
echo "MAIN_CLASS_NAME ?= ${MAIN_CLASS_NAME}" >> ${PROJECT_DIR}/Makefile
python devices.py ${TARGET} >> ${PROJECT_DIR}/Makefile
cat makefile_mbed.tmpl >> ${PROJECT_DIR}/Makefile
(cd targets/${TARGET}/; tar cf - .) | (cd ${PROJECT_DIR}/; tar xf -)

# copy application
(cd ${PROJECT}; tar cf - .) | (cd ${PROJECT_DIR}; tar xf -)
