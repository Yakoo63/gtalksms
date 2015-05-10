# Notifications #

This application opens a XMPP/gtalk conversation with you and:

  * forwards you the incoming text messages (SMS)
  * notifies you about incoming calls
  * notifies you about battery state

# Commands #

## Core Commands ##
With simple commands, you get the ability to

  * reply to the incoming sms (using `"reply:<message>"`)
  * send sms (using `"sms:<contact>:<message>"` - contact can be a name or a phone number)
  * read last 5 sms from a contact (using `"sms:<contact>"` with no argument)

## Further Commands ##

  * send and receive files to/from the phone: `"send"`, `"ls"`
  * enable disable bluetooth: `"bluetooth"`
  * get and set the current ringmode (normal/silent/...): `"ringmode:<mode>"`
  * take a photo and send it via XMPP: `"photo"`
  * make you phone ring in case you loose it: using `"ring"` or `"ring:X"` where X is the ringing volume level from 0-vibrate to 100%
  * geolocalize your phone - it will send you google maps links: `"where"`
  * copy text to the keyboard: `"copy:<text>"`
  * retrieve and set the clipboard content
  * get information about a contact (using `"contact:<contact>"`)
  * open any url: just send the plain url
  * dial a contact: `"dial"`
  * view the dial log
  * initiate navigation
  * get help, using `"help:all"`

## Root only commands ##
Also if you have root on your phone, you can:

  * open a new chat and issue console commands and retrieve the output via chat
  * make an screenshot, store and set the picture via XMPP

## Beta commands ##
See [beta commands](BetaFeaturesOfGtalksms.md) (not official released yet)