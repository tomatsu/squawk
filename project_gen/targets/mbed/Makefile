BUILD_TOP ?= /tmp
TARGET ?= LPC1768
SQUAWK_TOP = ../../..

COLLECTOR = $(shell java -cp $(SQUAWK_TOP)/build.jar com.sun.squawk.builder.util.PropertyUtil $(SQUAWK_TOP)/build.properties | egrep '^GC=' | awk -F '=' '{print $$2}')
ifneq ($(filter com.sun.squawk.Lisp2%,$(COLLECTOR)),)
GC_CFLAG = "BUILD_FLAGS += -DLISP2_BITMAP"
GC_DCA_CONF = lisp2.config
endif
ifneq ($(filter com.sun.squawk.Cheney%,$(COLLECTOR)),)
GC_DCA_CONF = cheney.config
endif

PROJECT ?= helloworld
PROJECT_DIR = $(BUILD_TOP)/$(shell echo $(TARGET) | tr '[:upper:]' '[:lower:]')_$(PROJECT)

AT = @

default: all

FILELIST = files.txt
MBED = mbed/BUILD/mbed
TOOL ?= GCC_ARM
#SDK_URL ?= https://github.com/mbedmicro/mbed.git

TEMPLATE = makefile_mbed_cli.tmpl

mbed :
	$(AT) mbed new --mbedlib mbed
	(cd mbed; mbed deploy --depth 1 -v)

mbed_hdr :
	$(AT)mkdir -p $(PROJECT_DIR)
	$(AT)rev=$$(cd mbed/mbed; echo *); tar cf - -C mbed \
		.mbed \
		.temp \
		mbed.bld \
		mbed_settings.py \
		-C mbed/$${rev} \
		TARGET_$(TARGET) \
		hal \
		drivers \
		targets.json \
		platform \
		mbed.h | (cd $(PROJECT_DIR)/; tar xf -)
	$(AT)cp $(TARGET)/small.json $(PROJECT_DIR)

makefile:
	$(AT)echo "MBED = mbed" > $(PROJECT_DIR)/Makefile
	$(AT)echo "-include target.mk" >> $(PROJECT_DIR)/Makefile
	$(AT)echo GC_DCA_CONF = $(GC_DCA_CONF) >> $(PROJECT_DIR)/Makefile
	$(AT)echo $(GC_CFLAG) >> $(PROJECT_DIR)/Makefile
	$(AT)cat $(TEMPLATE) >> $(PROJECT_DIR)/Makefile
	$(AT)(cd $(TARGET); tar cf - .) | (cd $(PROJECT_DIR)/; tar xf -)

all: mbed mbed_hdr copy_squawk makefile copy_app

copy_squawk:
	$(AT)mkdir -p $(PROJECT_DIR)/squawk
	$(AT)(files="$(shell cat $(FILELIST))"; cd $(SQUAWK_TOP); tar cf - $$files) | (cd $(PROJECT_DIR)/squawk; tar xf -)

copy_app:
	$(AT)(cd ../../projects/$(PROJECT); tar cf - .) | (cd $(PROJECT_DIR); tar xf -)
