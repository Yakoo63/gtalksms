# Introduction #

## Coding style ##
Coding style for new code (old code will be transformed step-by-step): http://source.android.com/source/code-style.html

## Revision-Control Browser ##
Fisheye of GTalkSMS: https://fisheye2.atlassian.com/changelog/gtalksms

Thanks to [Atlassian](http://www.atlassian.com/) for the free hosting. :-)

## Writing new commands ##

All classes for new commands are put into the "com.googlecode.gtalksms.cmd" package. This package also includes an [template](http://code.google.com/p/gtalksms/source/browse/src/com/googlecode/gtalksms/cmd/CommandTemplate.java), which gives a good starting point for writing new commands.

For an easy Command.class example, look at the [minmalistic bluetooth command](http://code.google.com/p/gtalksms/source/browse/src/com/googlecode/gtalksms/cmd/BluetoothCmd.java?spec=svndace095106d0eeffdaf4c337c57630d77c6b22f6&r=dace095106d0eeffdaf4c337c57630d77c6b22f6)

## Providing patches ##
Please provide commits and patches either as
  * Commit on your google code clone of GTalkSMS
  * hg bundle

## aSMACK development ##
  * The [fork of smack](https://github.com/Flowdalic/smack) can be found on github
  * The [aSMACK build environment](https://github.com/Flowdalic/asmack) is also there