.PHONY: all clean

all: debug

debug:
	ant debug

install: debug
	adb install -r bin/GTalkSMS-debug.apk

cinstall: clean install
	
clean:
	ant clean
