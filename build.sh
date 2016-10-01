#!/bin/sh

case "$1" in
"clean" )
	java -jar build.jar clean || exit 1
	exit ;;
esac

(cd builder; sh bld.sh) || exit 1
java -jar build.jar setup || exit 1
java -jar build.jar runvm2c	-o:vmcore/src/vm/vm2c.c.inc.spp \
		 -cp: -sp:.:cldc/preprocessed.target -root:com.sun.squawk.VM -root:com.sun.squawk.MethodHeader -root:com.sun.squawk.CheneyCollector \
		 $(find cldc/preprocessed.target -name '*\.java' -print) || exit 1
java -jar build.jar spp $(find vmcore/src -name '*.spp')

echo 
echo "Ready to generate project. Please read project_gen/README.md."
echo

