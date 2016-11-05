BUILD_TOP ?= /tmp
TARGET ?= LPC1768
SQUAWK_TOP = ../../..

PROJECT ?= helloworld
PROJECT_DIR = $(BUILD_TOP)/$(shell echo $(TARGET) | tr '[:upper:]' '[:lower:]')_$(PROJECT)

AT = @

default: all

FILELIST = files.txt
MBED = mbed/.build/mbed
TOOL ?= GCC_ARM
SDK_URL ?= https://github.com/mbedmicro/mbed.git

ifeq ($(TOOL), GCC_ARM)
TEMPLATE = makefile_mbed_gcc.tmpl
endif
ifeq ($(TOOL), ARM)
TEMPLATE = makefile_mbed_arm.tmpl
endif
ifeq ($(TEMPLATE),)
$(error unknown tool)
endif

mbed_sdk :
	$(AT)if [ ! -d mbed ]; then	git clone $(SDK_URL); fi
	$(AT)if [ ! -d $(MBED)/TARGET_$(TARGET) ]; then	(cd mbed; python tools/build.py -m $(TARGET) -t $(TOOL)); fi

mbed_hdr :
	$(AT)mkdir -p $(PROJECT_DIR)/mbed
	$(AT)tar cf - -C $(MBED) hal platform TARGET_$(TARGET) | (cd $(PROJECT_DIR)/mbed; tar xf -)

makefile:
	$(AT)echo "MBED = mbed" > $(PROJECT_DIR)/Makefile
	$(AT)echo "-include target.mk" >> $(PROJECT_DIR)/Makefile
	$(AT)python mbed_devices.py ${TARGET} >> $(PROJECT_DIR)/Makefile
	$(AT)cat $(TEMPLATE) >> $(PROJECT_DIR)/Makefile
	$(AT)(cd $(TARGET); tar cf - .) | (cd $(PROJECT_DIR)/; tar xf -)

all: mbed_sdk mbed_hdr copy_squawk makefile copy_app

copy_squawk:
	$(AT)mkdir -p $(PROJECT_DIR)/squawk
	$(AT)(files="$(shell cat $(FILELIST))"; cd $(SQUAWK_TOP); tar cf - $$files) | (cd $(PROJECT_DIR)/squawk; tar xf -)

copy_app:
	$(AT)(cd ../../projects/$(PROJECT); tar cf - .) | (cd $(PROJECT_DIR); tar xf -)
