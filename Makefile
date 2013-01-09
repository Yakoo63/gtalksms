all: debug

debug:
	ant debug

install: debug
	adb install -r bin/gtalksmsdonate-debug.apk
