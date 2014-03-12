@echo off
set ABSDIR=external-libs\ActionBarSherlock\actionbarsherlock

if not exist %ABSDIR% (
	mkdir .git\modules\external-libs
	mkdir external-libs
	
	echo Initializing Module
	git submodule add -f git://github.com/JakeWharton/ActionBarSherlock.git external-libs\ActionBarSherlock
	echo Copy Android Studio configuration
	cp actionbarsherlock.iml %ABSDIR%
) else (
    echo Project already initialized
)

pause