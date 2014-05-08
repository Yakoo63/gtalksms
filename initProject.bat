@echo off
set ABSDIR=external-libs\ActionBarSherlock
set MTMDIR=external-libs\MemorizingTrustManager

mkdir .git\modules\external-libs
mkdir external-libs

if not exist %ABSDIR% (
	echo Initializing Module ActionBarSherlock
	git submodule add -f git://github.com/JakeWharton/ActionBarSherlock.git/actionbarsherlock external-libs\ActionBarSherlock
	echo Copy Android Studio configuration
	cp external-libs\ActionBarSherlock.iml %ABSDIR%
) else (
    echo Project ActionBarSherlock already initialized
)

if not exist %MTMDIR% (
	echo Initializing Module MemorizingTrustManager
	git submodule add -f https://github.com/ge0rg/MemorizingTrustManager.git external-libs\MemorizingTrustManager
	echo Copy Android Studio configuration
	cp external-libs\MemorizingTrustManager.iml %MTMDIR%
) else (
    echo Project MemorizingTrustManager already initialized
)

pause

