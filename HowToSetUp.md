# How does this work? #

To notify you for events happening on your phone, GTalkSMS **connects** to
jabber and talks to your **gtalk user** (or any other xmpp jid).

## Connecting GTalkSMS ##

To connect to jabber, GTalkSMS needs **identifiers**. That means, a login and a password. This can be the identifiers for any jabber account, including your gmail account. You can either connect from your gmail account, or from any jabber compatible address.

## Notified account (aka "Notification Address") ##

Once GTalkSMS is connected to jabber, it has to know which jabber user
it should send notifications to and receive commands from. Most people
will want to notify their gmail account, but you can actually choose to
notify any jabber account.

# The possibilities #

So, GTalkSMS **connects** from an account and **notifies** another account.
These accounts can be the same, which leads to 2 typical ways to configure
the application.

## Using your gmail address for controls and notifications (NOT RECOMMENDED) ##

If you choose this method, the phone will connect to jabber using your
gmail address. Thus, when you will receive a message from the phone, it
will appear as a message sent by... you. While this can be confusing, this
has been chosen as the default method since most users don't like having
to create a different jabber address for there device. To control the
phone, you'll just have to respond to the notifications sent by
GTalkSMS in the same chat window.

There is an issue coming from this method: gmail does not allow opening a
conversation with yourself, so in the end, you'll be more or less obliged
to use a chat program. Pidgin is the recommanded one. Just add yourself
in your own contacts and talk to yourself to command the phone.

## Using an account for connecting and another one for receiving notifications ##

The other way to go is to use a different address (XMPP JID) for the phone. We highly recommend this option, since it provides usually the best experience. A list of free and public XMPP provider can be found in the wiki page [PublicXMPPServers](PublicXMPPServers.md)

## Common mistakes, settings advices ##

  * Verify the extension, that is, double check the "@domain.tld". It is **@gmail.com** or **@jabber.org**. It is very common to make a mistake on this. Verify the domain **and** the extension: Don't put **.org** instead of **.com**.
  * Login/Logout gtalk won't help. The only thing that can help is start/stop the application when you have modified the options.

## The Methods ##
This leaves us with 3 options on how one can configure GTalkSMS:
  * **Method 1 : For the lazy, using your existing gmail address (NOT RECOMMENDED)**
  * **Method 2 : Using another gmail account specially created for your phone**
  * **Method 3 : Using a jabber account specially created for your phone (HIGHLY RECOMMENDED)**