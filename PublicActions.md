**Note: This page discusses features introduced in GTalkSMS version 1.6 - it will not work in earlier versions.**

# GTalkSMS public actions #

There are 4 Android 'actions' which GTalkSMS handles.  Any application on the system can send these actions and have GTalkSMS perform a corresponding command.  For example, the 'Tasker' application can be used to send these actions in response to many different things happening on the phone.
**Note:** When GTalkSMS stays disconnected, the service will also stop, freeing memory and CPU. But GTalkSMS can be restarted anytime with an CONNECT/TOGGLE Action, which will (re-)start the service.

## Public Actions ##

|**Action**|**Version**|**Description**|
|:---------|:----------|:--------------|
|com.googlecode.gtalksms.action.CONNECT|1.6|Connects or disconnects the xmpp connection.  By default, the request is considered a connection request, but an extra called 'disconnect' set to 'true' will cause it to be considered a disconnection request.  If xmpp is already in the requested state, no action is taken.|
|com.googlecode.gtalksms.action.DISCONNECT|4.0|Disconnects the xmpp connection.  If xmpp is already in the requested state, no action is taken.|
|com.googlecode.gtalksms.action.TOGGLE|1.6|Toggles the current connection state (ie, disconnects if currently connected, and connects if disconnected.)  If GTalkSMS is in a 'waiting' state (ie, with a yellow icon), it will become disconnected.|
|com.googlecode.gtalksms.action.SEND|1.6|Sends a message over xmpp - the message itself must be in an String extra called 'message'.  No action is taken if xmpp is disconnected - if you want to force connection first, just send a _com.googlecode.gtalksms.action.CONNECT_ action before the send action.|
|com.googlecode.gtalksms.action.COMMAND|1.6|Issues an command to GTalkSMS. The base command must be in an string extra called "cmd", all further arguments in an extra called "args".|
# Working with Tasker #

Tasker is a commercial application which performs Tasks or Actions based on a large range of criteria.  While GTalkSMS does not endorse the Tasker application, it is popular enough that we will explain how to integrate it with GTalkSMS.

You send commands to GTalkSMS by using a Tasker _Action Intent_ - when Tasker presents its list of Actions, select _Misc Action_, then _Action Intent_.  Note the following:

  * When tasker prompts for the 'Action', enter one of the strings above - eg, _com.googlecode.gtalksms.action.SEND_.

  * The Category should remain at its default of _None_, and the optional Data field should be left blank.

  * One of the _Extra_ fields should be used if the action requires an 'extra'.  You enter a string in the format _name:value_ - eg, _message:hello_ could be used for a _com.googlecode.gtalksms.action.SEND_ action.  The second Extra field can be left blank as no GTalkSMS action require multiple extras.

  * The **Target field** must be changed to _BroadcastReceiver_ .

  * Note that you should never configure tasker to exit or terminate the gtalksms application - sending it a request to disconnect will cause gtalksms to automatically terminate after disconnection.