BUILD_TOP ?= /tmp
TARGET ?= linux-i386
SQUAWK_TOP = ../../..

COLLECTOR = $(shell java -cp $(SQUAWK_TOP)/build.jar com.sun.squawk.builder.util.PropertyUtil $(SQUAWK_TOP)/build.properties | egrep '^GC=' | awk -F '=' '{print $$2}')
ifneq ($(filter com.sun.squawk.Lisp2%,$(COLLECTOR)),)
GC_CFLAG = "BUILD_FLAGS += -DLISP2_BITMAP"
GC_DCA_CONF = lisp2.config
endif
ifneq ($(filter com.sun.squawk.Cheney%,$(COLLECTOR)),)
GC_DCA_CONF = cheney.config
endif

STATIC_MAIN_CLASS = $(shell java -cp $(SQUAWK_TOP)/build.jar com.sun.squawk.builder.util.PropertyUtil $(SQUAWK_TOP)/build.properties | egrep '^STATIC_MAIN_CLASS=' | awk -F '=' '{print $$2}')

PROJECT ?= helloworld
PROJECT_DIR = $(BUILD_TOP)/$(TARGET)_$(PROJECT)

AT = @

default: all

FILELIST = files.txt

makefile:
	$(AT)echo GC_DCA_CONF = $(GC_DCA_CONF) > $(PROJECT_DIR)/Makefile
	$(AT)echo $(GC_CFLAG) >> $(PROJECT_DIR)/Makefile
	$(AT)echo STATIC_MAIN_CLASS = $(STATIC_MAIN_CLASS) >> $(PROJECT_DIR)/Makefile
	$(AT)cat makefile.tmpl >> $(PROJECT_DIR)/Makefile

all: copy_squawk makefile copy_app

copy_squawk:
	$(AT)mkdir -p $(PROJECT_DIR)/squawk
	$(AT)(files="$(shell cat $(FILELIST))"; cd $(SQUAWK_TOP); tar cf - $$files) | (cd $(PROJECT_DIR)/squawk; tar xf -)

copy_app:
	$(AT)(cd ../../projects/$(PROJECT); tar cf - .) | (cd $(PROJECT_DIR); tar xf -)
