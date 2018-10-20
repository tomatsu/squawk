PROPERTIES ?= build.properties

PROPERTIES_ABS = $(filter /%,$(PROPERTIES))
ifeq ($(PROPERTIES_ABS),)
  PROPERTIES_ABS = $(shell pwd)/$(PROPERTIES)
endif

property = $(shell test -f build.jar && java -cp build.jar com.sun.squawk.builder.util.PropertyUtil $(PROPERTIES) | egrep '^$(1)=' | awk -F '=' '{print $$2}')
COLLECTOR_CLASS = $(call property,GC)
ENABLE_SDA_DEBUGGER = $(call property,ENABLE_SDA_DEBUGGER)

AT ?= @

all: builder
	$(MAKE) PROPERTIES=$(PROPERTIES) setup runvm2c preprocess debugger-proxy
	@echo PROPERTIES=$(PROPERTIES_ABS) > build.log
	@echo "Ready to generate project. Please read project_gen/README.md."

builder:
	$(AT)cd builder; sh bld.sh

setup:
	$(AT)java -jar build.jar -override:$(PROPERTIES) setup

runvm2c:
	$(AT)java -jar build.jar -override:$(PROPERTIES) runvm2c	-o:vmcore/src/vm/vm2c.c.inc.spp \
		 -cp: -sp:.:cldc/preprocessed.target -root:com.sun.squawk.VM -root:com.sun.squawk.MethodHeader -root:$(COLLECTOR_CLASS) \
		 `find cldc/preprocessed.target -name '*\.java' -print`

preprocess:
	$(AT)java -jar build.jar -override:$(PROPERTIES) spp $(shell find vmcore/src -name '*.spp')

ifeq ($(ENABLE_SDA_DEBUGGER),true)
debugger:
	cd debugger; ant -Djavac.classpath=../cldc/classes.jar jar

debugger-proxy: debugger
	cd debugger-proxy; ant -Djavac.classpath=../debugger/dist/Squawk-debugger.jar:../cldc/classes.jar:../translator/classes.jar jar
else
debugger-proxy:
endif

clean:
	$(AT)if [ -f build.jar ]; then java -jar build.jar clean; fi
	$(AT)rm -f build.jar build-commands.jar squawk.jar build.log
	$(AT)cd debugger; ant clean
	$(AT)cd debugger-proxy; ant clean

.PHONY: all builder setup runvm2c preprocess debugger debugger-proxy
