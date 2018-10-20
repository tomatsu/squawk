BUILD_TOP ?= /tmp
TARGET ?= LPC1768
SQUAWK_TOP = ../../..
include $(SQUAWK_TOP)/build.log

property = $(shell java -cp $(SQUAWK_TOP)/build.jar com.sun.squawk.builder.util.PropertyUtil $(PROPERTIES) | egrep '^$(1)=' | awk -F '=' '{print $$2}')
COLLECTOR = $(call property,GC)
ifneq ($(filter com.sun.squawk.Lisp2%,$(COLLECTOR)),)
  GC_CFLAG = "BUILD_FLAGS += -DLISP2_BITMAP"
  ifneq ($(filter com.sun.squawk.Lisp2Generational%,$(COLLECTOR)),)
    GC_JAVAFLAG = "-DGC_com.sun.squawk.Lisp2GenerationalCollector=true"
  else
    GC_JAVAFLAG = "-DGC_com.sun.squawk.Lisp2Collector=true"
  endif
  GC_DCA_CONF = lisp2.config
endif
ifneq ($(filter com.sun.squawk.Cheney%,$(COLLECTOR)),)
  GC_DCA_CONF = cheney.config
  GC_JAVAFLAG = "-DGC_com.sun.squawk.CheneyCollector=true"
endif

STATIC_MAIN_CLASS = $(call property,STATIC_MAIN_CLASS)
ENABLE_MULTI_ISOLATE = $(call property,ENABLE_MULTI_ISOLATE)
ENABLE_SDA_DEBUGGER = $(call property,ENABLE_SDA_DEBUGGER)

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
	$(AT) mbed new --mbedlib -v mbed
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
	$(AT)echo STATIC_MAIN_CLASS = $(STATIC_MAIN_CLASS) >> $(PROJECT_DIR)/Makefile
	$(AT)echo ENABLE_MULTI_ISOLATE = $(ENABLE_MULTI_ISOLATE) >> $(PROJECT_DIR)/Makefile
	$(AT)echo ENABLE_SDA_DEBUGGER = $(ENABLE_SDA_DEBUGGER) >> $(PROJECT_DIR)/Makefile
	$(AT)cat $(TEMPLATE) >> $(PROJECT_DIR)/Makefile
	$(AT)(cd $(TARGET); tar cf - .) | (cd $(PROJECT_DIR)/; tar xf -)

all: mbed mbed_hdr copy_squawk makefile copy_app

copy_squawk:
	$(AT)mkdir -p $(PROJECT_DIR)/squawk
	$(AT)(files="$(shell cat $(FILELIST))"; cd $(SQUAWK_TOP); tar cf - $$files) | (cd $(PROJECT_DIR)/squawk; tar xf -)
ifeq ($(ENABLE_SDA_DEBUGGER),true)
	$(AT)mkdir -p $(PROJECT_DIR)/squawk/debugger $(PROJECT_DIR)/squawk/debugger-proxy
	$(AT)cd $(SQUAWK_TOP)/debugger/build/classes; jar cfM $(PROJECT_DIR)/squawk/debugger/debugger.jar com
	$(AT)cp $(SQUAWK_TOP)/debugger-proxy/dist/Squawk-debugger-proxy.jar $(PROJECT_DIR)/squawk/debugger-proxy/debugger-proxy.jar
endif

copy_app:
	$(AT)(cd ../../projects/$(PROJECT); tar cf - .) | (cd $(PROJECT_DIR); tar xf -)

.PHONY: all mbed_hdr makefile copy_squawk copy_app
