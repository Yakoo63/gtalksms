# Présentation #
Pour vous notifier des évènements se produisant sur votre téléphone, GTalkSMS va se connecter à jabber et discute avec votre compte **gtalk** (ou un tout compte XMPP).
## Comment GTalkSMS se connecte ##
Pour se connecter à jabber GTalkSMS a besoin des identifiants utilisateur et mot de passe, cela peut être un compte jabber ou gtalk crée spécifiquement pour GTalkSMS, cela peut être aussi votre compte Gtalk ou jabber déjà existant.
## Paramétrer les comptes et connexions ##

Allez dans Préférences, puis Connexion
### Compte (adresse) de notification ###

C'est ce compte que vous allez utiliser pour envoyer les commandes et recevoir les réponses de GTalkSMS, en règle générale c'est votre compte Gtalk (mais cela peut être n'importe que compte Jabber que vous utilisez).

### Utilisation d'un compte différent pour le téléphone ###

En activant le paramètre **Utiliser un compte différent** vous allez pouvoir indiquer dans le champ **Nom d'utilisateur** le compte (autre que votre compte Gtalk) qui sera utilisé par le téléphone pour se connecter à Jabber.

### Mot de passe ###
Si vous avez coché la case **Utiliser un compte différent** entrez le mot de passe du compte qui sera utilisé par le téléphone pour se connecter à Jabber.

Autrement entrez le mot de passe du **Compte (adresse) de notification**.

## Les trois possibilités ##
Il y a 3 possibilités pour configurer les comptes (adresse) de connexion Jabber pour GTalkSMS

  * **1 : Pour aller vite, utilisez uniquement votre adresse gmail**

  * **2 : Utilisez votre adresse gmail pour les notifications et une seconde adresse gmail spécialement crée pour votre téléphone**

  * **3 : Utilisez votre adresse gmail pour les notifications et un compte Jabber pour votre téléphone**
## Utiliser votre adresse gmail pour les notifications et le téléphone ##

Si vous choisissez cette solution vous allez envoyer les commandes et recevoir les réponses du téléphone comme si vous les aviez envoyées vous même.

Cela peut donc prêter à confusion. Cette solution sera à privilégier si vous ne souhaitez pas créer de second compte gmail ou jabber.

Il y a une limitation avec cette méthode, gmail ne pouvant pas ouvrir de discussion avec vous même il faudra d'installer et utiliser un logiciel jabber complémentaire. Pidgin est celui que nous recommandons. Il vous suffit de vous ajouter vous même dans la liste de contact et de discuter avec vous même.
## Utilisez votre adresse gmail pour les notifications et une seconde pour votre téléphone ##
La second possibilité est d'utiliser un second compte gmail qui sera associé au téléphone.
Voici les paramètres pour se connecter au serveur Gtalk

  * server name: talk.google.com

  * server port: 5222

  * service name: gmail.com
## Utilisez votre adresse gmail pour les notifications et un compte Jabber pour votre téléphone ##
Si vous ne souhaitez pas créer un second compte gmail, vous pouvez créer un compte sur le serveur jabber.org

Choisissez un nom d'utilisateur et un mot de passe et vous pourrez utiliser le compte directement avec GTalkSMS.

Voici les paramètres pour se connecter au serveur jabber.org

  * server name: jabber.org

  * server port: 5222

  * service name: jabber.org
## Erreurs courantes et recommandations ##

  * Attention de bien verifier l'extension "@domain.tld", cela peut être **@gmail.com** ou **@jabber.org**.

  * Si vous avez modifié des options stoppez et lancez l'application pour que les modifications prennent effet.