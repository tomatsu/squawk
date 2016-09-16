import sys
import json

f = open(sys.argv[1], 'r')
jsonData = json.load(f)
#print json.dumps(jsonData, sort_keys = True, indent = 4)
for i in jsonData[sys.argv[2]]['device_has']:
    print i
