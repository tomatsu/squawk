BUILD_TOP ?= /tmp
TARGET ?= linux

MAIN_CLASS_NAME ?= Hello
PROJECT ?= helloworld
PROJECT_DIR = $(BUILD_TOP)/$(shell echo $(TARGET) | tr '[:upper:]' '[:lower:]')_$(PROJECT)

AT = @

default: all

ifeq ($(TARGET),linux)

FILELIST = files_linux.txt

makefile:
	$(AT)echo "MAIN_CLASS_NAME = $(MAIN_CLASS_NAME)" > $(PROJECT_DIR)/Makefile
	$(AT)cat makefile_linux-i386.tmpl >> $(PROJECT_DIR)/Makefile

all: copy_squawk makefile copy_app

else

ifeq ($(TARGET),ESP8266)

FILELIST = files_esp8266.txt

makefile:
	$(AT)echo "-include target.mk" > $(PROJECT_DIR)/Makefile
	$(AT)echo "MAIN_CLASS_NAME = $(MAIN_CLASS_NAME)" >> $(PROJECT_DIR)/Makefile
	$(AT)cat makefile_esp.tmpl >> $(PROJECT_DIR)/Makefile
	$(AT)(cd targets/$(TARGET)/; tar cf - .) | (cd $(PROJECT_DIR)/; tar xf -)

all: copy_squawk makefile copy_app

else

# assume MBED
FILELIST = files_mbed.txt
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
	$(AT)echo "MAIN_CLASS_NAME ?= $(MAIN_CLASS_NAME)" >> $(PROJECT_DIR)/Makefile
	$(AT)python mbed_devices.py ${TARGET} >> $(PROJECT_DIR)/Makefile
	$(AT)cat $(TEMPLATE) >> $(PROJECT_DIR)/Makefile
	$(AT)(cd targets/$(TARGET)/; tar cf - .) | (cd $(PROJECT_DIR)/; tar xf -)

all: mbed_sdk mbed_hdr copy_squawk makefile copy_app

endif
endif

#
# common 
#

copy_squawk:
	$(AT)mkdir -p $(PROJECT_DIR)/squawk
	$(AT)(files="$(shell cat $(FILELIST))"; cd ..; tar cf - $$files) | (cd $(PROJECT_DIR)/squawk; tar xf -)

copy_app:
	$(AT)(cd projects/$(PROJECT); tar cf - .) | (cd $(PROJECT_DIR); tar xf -)
