# Wie funktioniert GTalkSMS? #

GTalkSMS informiert dich bei bestimmten Ereignissen (neue SMS, Anruf, usw.) auf deinem Android Smartphone uber GTalk oder XMPP (Jabber).

## GTalkSMS Verbinden ##

GTalkSMS verbindet sich mit einem XMPP Account. Mit deinem Google Account hast du, neben Google Mail, auch einen Google Talk Account. Da Google Talk auf XMPP basiert, kann sich GTalkSMS darüber verbinden.
Alternativ kann jeder andere XMPP / Jabber Account verwendet werden (z.B. von jabber.org, jabber.ccc.de, ...)

## Benachrichtigungs- / Steueradresse ##

Sobald GTalkSMS verbunden ist, muss es wissen, an welchen Account es die Benachrichtigungen schicken soll und wer berechtigt ist, Befehle zu senden. Für die meisten wird auch hier wieder der Google Mail Account in Frage kommen, aber es kann auch jeder andere Jabber Account verwendet werden.

# Verschiedene Möglichkeiten #

Einfach ausgedrückt verbindet sich GTalkSMS mit einen Account und benachrichtigt einen anderen. Diese beiden Accounts müssen nicht unterschiedlich sein. Das ermöglicht zwei grundlegende Wege GTalkSMS zu konfigurieren.

## Verschiedene Accounts ##

Alternativ kann für GTalkSMS ein zweiter Account erstellen werden (z.B. bei jabber.org, jabber.ccc.de, ...). Die Registrierung ist im Normalfall einfach und schnell gemacht.

Nun muss die Option "Benutze anderen Account" aktiviert werden und der XMPP Server konfiguriert werden.
Hier ein paar Beispiele:

### jabber.org: ###
  * server name: jabber.org
  * server port: 5222
  * service name: jabber.org

### Google Talk ###
  * server name: talk.google.com
  * server port: 5222
  * service name: gmail.com

## Einen Account (z.B. Google Mail) für GTalkSMS ( NICHT EMPFOHLEN ) ##

Bei dieser Methode verwendet GTalkSMS den **selben**, typischerweise Google-, Account um sich zu verbinden und um dahin Benachrichtigungen zu schicken.

Einen Nachteil hat das ganze allerdings: Google Talk erlaubt keine chats zwischen den selben Acccounts. Dazu muss ein XMPP Client wie z.B. Pidgin verwendet werden. Es reicht sich selbst zu den Kontakten hinzuzufügen und dann eine Unterhaltung mit diesen Kontakt zu beginnen.

## Häufige Fehler, Konfigurationsempfehlungen ##

  * Achte auf den genauen Login Namen bzw. JID: Oft verwechselt man die TLD z.B. jabber.com statt jabber.org
  * Login/Logout gtalk won't help. The only thing that can help is start/stop the application when you have modified the options.
  * Damit manche Änderungen an der Konfiguration wirksam werden, muss GTalkSMS neu gestartet werden (Start/Stop Button).

## Die Methoden ##
  * **Methode 1: Den eigenen GTalk Account für GTalkSMS verwenden ( NICHT EMPFOHLEN )**
  * **Methode 2: Einen anderen GTalk Account speziell für GTalkSMS verwenden**
  * **Methode 3: Einen Jabber Account speziell für GTalkSMs verwenden**