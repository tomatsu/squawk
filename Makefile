COLLECTOR_CLASS = $(shell java -cp build.jar com.sun.squawk.builder.util.PropertyUtil build.properties | egrep '^GC=' | awk -F = '{print $$2}')

all: builder setup runvm2c preprocess
	@echo "Ready to generate project. Please read project_gen/README.md."

builder:
	cd builder; sh bld.sh

setup:
	java -jar build.jar setup

runvm2c:
	java -jar build.jar runvm2c	-o:vmcore/src/vm/vm2c.c.inc.spp \
		 -cp: -sp:.:cldc/preprocessed.target -root:com.sun.squawk.VM -root:com.sun.squawk.MethodHeader -root:$(COLLECTOR_CLASS) \
		 `find cldc/preprocessed.target -name '*\.java' -print`

preprocess:
	java -jar build.jar spp $(shell find vmcore/src -name '*.spp')

clean:
	java -jar build.jar clean

.PHONY: all builder setup runvm2c preprocess
