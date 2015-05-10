# Introduction #

In order to help debug or diagnose GTalkSMS issues, the developers may ask you to generate
and submit an application log from your device.  This page outlines that process.

**WARNING** - the application log may include sensitive information, such as your username (but _not_ your password), the content of SMS messages seen by the application, names and numbers of your contacts, etc.  Further, the instructions below ask you to send the log to a public forum (the GTalkSMS issues database) - so **you should assume everyone in the world will see this information**.  Thus, **you should carefully examine the log before sending it and remove any sensitive information.**

# Generating the log #

  * **Start with enabling debug log messages.** Go to Preferences -> Application settings -> Advanced settings and enable "Debug Log"

  * Reproduce your problem.  This is important as only the last few hours of logs are kept by the system, so we want to ensure your problem is recorded in the logs.

  * Go to the main GTalkSMS application screen.

  * Press and hold the "back" button for a couple of seconds.

  * Release the button - you will be presented with confirmation that a device log will be generated.  Press OK.

  * You will then be presented with a list of applications on your device which can send a generic "message".  Choose your email client.

  * Your email client will start with the log in the body.

  * **Review the log for sensitive information, remove them if desired**

  * If you know the Issue # in the [GTalkSMS issue database](https://code.google.com/p/gtalksms/issues/list), specify this number in the subject, by only changing the number, this will help us to keep all logs for a issue within a thread. Otherwise **set the subject to something meaningful**.

  * Send the message to gtalksms-logs@googlegroups.com. Optionally: Add your e-mail address, if you want the log too.

# Viewing the GTalkSMS Logs #

  * **Start with enabling debug log messages.** Go to Preferences -> Application settings -> Advanced settings and enable "Debug Log"

  * Reproduce your problem.  This is important as only the last few hours of logs are kept by the system, so we want to ensure your problem is recorded in the logs.

  * Install [algocat](https://market.android.com/details?id=org.jtb.alogcat) and filter for "gtalksms"