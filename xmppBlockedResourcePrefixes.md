# Description #

This page explains how to fill the setting "xmppBlockedResourcePrefixes" in the connection preferences.

During the connection Google applications set a specific token named resources to identify the application and the support (Phone, Gmail...).

This feature allow to disable redundant notifications by adding the prefix of the resource in this setting.

Use the new line as a separator.

Default value is:
| android    |
|:-----------|
| MessagingA |

# List of known resources #

| **Resource**           | **Description** |
|:-----------------------|:----------------|
| android              | GTalk on Android |
| gmail                | GTalk on GMail |
| MessagingA           | Hangout on Android |
| MessagingB           | Hangout on IPhone |
| messaging-smgmail    | Hangout on GMail |
| messaging-TalkGadget | Google+ chat |