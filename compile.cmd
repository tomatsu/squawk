copy c:\WindRiver\workspace\WPILib\PPC603gnu_DEBUG\WPILib.a vmcore\src\rts\vxworks\WPILib
call d
call d user-clean ../rollingthunder/NewWPILibJ
call d user-compile ../rollingthunder/NewWPILibJ
call d -verbose -comp:vxworks -prod -mac -o2 rom -strip:d -lnt -endian:big -metadata cldc imp debugger