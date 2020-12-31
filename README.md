![alt tag](https://github.com/Xav21/Eyes-Have-Ears/blob/master/logo%20(Small).png)

# Eyes-Have-Ears
Système de guidage de pilotes de parapente/delta malentendant produit par la Fédération Française de Vol Libre.

Fonctionnement de la solution
=============================
  - ![Eyes Have Ears - Objectifs & Principe de fonctionnement .pdf](https://github.com/Xav21/Eyes-Have-Ears/blob/master/Eyes%20Have%20Ears%20-%20Objectifs%20%26%20Principe%20de%20fonctionnement%20.pdf)
  - ![Eyes have ears - Manuel utilisateur v01.02.pdf](https://github.com/Xav21/Eyes-Have-Ears/blob/master/Eyes%20have%20ears%20-%20Manuel%20utilisateur%20v01.02.pdf)
  
Points de vigilence
===================
  - Ce système est en cours de test et de validation en conditions réelles d'utilisation. 
  - Il ne peut être utilisé que par des moniteurs de parapente ou delta qualifiés.

Prérequis
=========
  - Mode LORA
    - Hardware
      - 3 boitiers d'interface LORA/Bluetooth Eyes Have Ears.
      - Une paire de lunette de vison tête haute Altitude EyeWear AR1, Optivent ORA-2.
    - Sotfware
      - Téléphone Android 5.0 ou supérieur.
      - Application EyesHaveEars-x.y-release.apk installée sur les téléphones des moniteurs.
      
  - Mode connecté réseau 3/4/5G
    - Hardware
      - Une paire de lunette de vison tête haute Altitude EyeWear AR1. (Les lunettes ORA62 ne sont pas supportées)
    - Sotfware
      - Téléphone Android 5.0 ou supérieur.
      - Application EyesHaveEars-x.y-release.apk installée sur les téléphones des moniteurs et élèves.

Futur 
=====
  - La prise en charge d'autre type de lunettes à vision tête haute poura être envisagée.

Versions
========
  - V01.04.04
      - Ajout Notification Android pour un usage sur montre conectée. Cas typique de l'utilisation du vibreur. Ne fonctionne pas en mode LORA
  - V01.04.03
     - Correction anomalie SSL sur ORA2.
  - V01.04.02
     - Implementation de MQTT/SSL.
  - V01.04.01
     - Ajout de la configuration l'orientation de l'écran smartphone/lunette. Par défaut, paysage.
  - V01.04.00 
     - Ajout du support de la lunette Optivent/ORA-2 en mode réseau.
     - Un téléphone Android 5.0 ou supérieur peut être utilisé pour l'affichage "elève/pilote guidé" si une paire de lunette n'est pas disponible.
     - Intégration de l'application Optivent/Ora-2 dans EyesHaveEars. Selection via la configuration de l'application. 
     - Selection de la couleur d'affichage des consignes de la lunette ORA-2
     - Refonte de la gestion de la configuration (préférences).
     - L'application supporte Android V4.4 pour l'Optivent en mode réseau ou lora.
     - Prise en charge des oreillettes 'usb'
  - V01.03.00 
     - Amélioration de l'interface utilisateur.
     - Application renommée en Eyes Have Ears.
  - V01.02.00 
    - Ajout du support "Mode Réseau" qui utilise une connexion 3/4/5G au lieu des boitiers LORA. L'élève est equipé de l'application sur sont téléphone Android. 
    - Attention, les lunettes ORA-2 ne sont pas supportées dans le mode réseau.
  - V01.01.00 
    - Ajout support lunettes Altitude EyesWear AR1.
  - V01.00.00 
    - Version initiale pour lunettes Optivent ORA-2. Utilise LORA comme communicateur.
  
