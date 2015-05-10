# Known Hardware/Software Issues #

## GoSMS and GTalkSMS: No SMS notifications (Loged as [Issue 203](http://code.google.com/p/gtalksms/issues/detail?id=203)) ##
This is caused by GoSMS: Disable "duplicate message blocking" as [described by craiglennox](http://code.google.com/p/gtalksms/issues/detail?id=203#c53)

## Messages get relayed back to gtalk on the phone ##
This should be resolved with GTalkSMS 3.4.

## Pidgin does not show xhtml formatting ##
This is a "feature" of pidgin. When the message has more than 100 XHTML (format) tags, pidgin simply won't render it. See this [Pidgin Bug Report](http://developer.pidgin.im/ticket/1021).
We have added a suggestion to let the user decide if he want's this feature or how big the limit of xhtml tags should be.

We can only appeal to the pidgin people under the GTalkSMS users: **You want this too**, please [vote](http://developer.pidgin.im/vote/up/ticket/1021) for the request. Don't add comments like "me too" or "+1" to the bug, simply vote for it.
See also this [list](http://developer.pidgin.im/report/12), where the issue stands

## Pidgin throws an XMPP delivery error upon trying to issue a command ##
Seems only to occur when using a **one gtalk account** setup. When you hit this please report at the [Issue](http://code.google.com/p/gtalksms/issues/detail?id=4)

## No calling notification until call gets answered or muted ##
It looks like only a few users are affected by this one. If you are one of the users who experience this report your GTalkSMS version, android version and smartphone model in [Issue 59](http://code.google.com/p/gtalksms/issues/detail?id=59)

## Smiley instead of a bold result ##
This depends on how your client handles the 'poor man formating' with `*` and `_`.
See [Issue 50](http://code.google.com/p/gtalksms/issues/detail?id=50)

## GTalkSMS gets a new resource String every time it connects ##
GtalkSMS itself has the resource String hard-wired. It depends on how your XMPP server handles resource conflicts, some of them assign an random string to the resource. More info: [Issue 82](http://code.google.com/p/gtalksms/issues/detail?id=82)

## XMPP/Gtalk fails to create a room (Groupchat/Multi User Conference) ##
This is mostly a issue on the XMPP server. Some servers don't allow us to set the room owners, in this case we try to set a room password. Sometimes the whole room creation fails.

## My problem isn't listed here ##
Please report a bug then. But make sure you have read [FAQ#How\_to\_report\_a\_bug?](FAQ#How_to_report_a_bug?.md)