import sys
sys.path.append('mbed')
from tools.targets import Target

print "DEVICE_OPTIONS += ",
for i in Target.get_target(sys.argv[1]).device_has:
    print "-DDEVICE_" + i + "=1",
