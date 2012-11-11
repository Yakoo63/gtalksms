#!/bin/bash

FDIR='files'
ABSDIR='external-libs/ActionBarSherlock/library'

if [ -f local.properties ] ; then
    cp local.properties $ABSDIR/
else
    echo Warning, please create your local properties with the sdk location
    echo See local.properties.example
    exit 1
fi

cp $FDIR/abs-build.xml $ABSDIR/build.xml
cp $FDIR/abs-AndroidManifest.xml $ABSDIR/AndroidManifest.xml
cp $FDIR/abs-project.properties $ABSDIR/project.properties
cp $FDIR/abs-dot-project $ABSDIR/.project
