@echo off
set FDIR=files
set ABSDIR=external-libs\ActionBarSherlock\library

if not exist %ABSDIR% (
    git submodule update --init --recursive || pause & exit 1
) else (
    echo Project already initialized
)

copy local.properties %ABSDIR%\
copy %FDIR%/abs-build.xml %ABSDIR%\build.xml
copy %FDIR%/abs-dot-project %ABSDIR%\.project

pause