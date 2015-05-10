# Setup #

## Method 1 : For the lazy, using your existing gmail address ##

  * **Address to notify**: enter you gmail address
  * **Use a different account**: leave unchecked
  * **Password**: enter your gmail password

GTalkSMS will connect to Jabber using your gmail account and it
will appear as if you're receiving messages **from yourself**. This
method has a drawback: since gtalk doesn't let you have conversations
with yourself, you will have to **do it from a chat client** like
[Pidgin](http://pidgin.im/) (just add yourself to your contacts).

**If you have some issues with this method, use second method.**

## Method 2 : Using another gmail account specially created for your phone ##

  * **Address to notify**: enter you gmail address
  * **Use a different account**: check the option
  * **Login** enter the other gmail account login
  * **Password**: enter your other GMail password (Note that you never enter your master GMail account password)

Using a chat client, set the second gmail account to be friend with your gtalk account and check that the accounts can talk to each other **in a two-way conversation** (tip #1: There are [web-based clients](http://jwchat.org/), use them at your own risk - tip #2: with Pidgin, you can modify the jabber nickname so that it will appear in a nice way to your gmail account).

## Method 3 : Using a jabber account specially created for your phone ##

  * Go to https://register.jabber.org/ and create an account.
  * Using a chat client, set the jabber account to be friend with your gtalk account and check that the accounts can talk to each other **in a two-way conversation** (tip #1: There are [web-based clients](http://jwchat.org/), use them at your own risk - tip #2: with Pidgin, you can modify the jabber nickname so that it will appear in a nice way to your GMail account).
  * Then, you can configure the phone:
    * **Address to notify** enter you gmail address
    * **Use a different account** check the option
    * **Login** enter the jabber login
    * **Password** enter the jabber password
    * In **Additional settings**:
      * **Server host**: jabber.org
      * **Server port**: 5222
      * **Service name**: jabber.org

GTalkSMS will connect to Jabber using its own account and you will
be able to reach it from the web-based gtalk like a regular contact.