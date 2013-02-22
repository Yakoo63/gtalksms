.PHONY: all clean

all: debug

debug:
	ant debug

install: debug
	adb install -r bin/GTalkSMS-debug.apk
	adb shell am start -a android.intent.action.MAIN -n com.googlecode.gtalksms/com.googlecode.gtalksms.panels.MainActivity

cinstall: clean install

clean:
	ant clean
