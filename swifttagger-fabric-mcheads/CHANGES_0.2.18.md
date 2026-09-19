# SwiftTagger 0.2.18

- `/classement`, `/swift classement`, `/tierstagger classement` ouvrent le classement global.
- Le classement utilise `/api/players`, affiche rang, tête, pseudo et points et ouvre le profil au clic.
- Autocomplétion des joueurs depuis tous les profils ayant des tiers, pas seulement le TAB/serveur actuel.
- Couleurs classement : GOLD #1, SILVER #2, BRONZE #3, VIOLET #4-#99, gris #100+.
- Le pseudo canonique renvoyé par l'API/database est conservé.
- Recherche de skin par UUID API et comparaison insensible à la casse dans le TAB.
- Styles d'icônes renommés : PVPHQ, VANILLA, MCTIERS, PVPTIERS, avec interrupteurs de visibilité préparés dans la config.
