# SwiftTagger 0.2.24

- Le classement a maintenant un timeout explicite : plus de chargement infini.
- `/api/players` est la route utilisée pour la liste complète.
- Les erreurs de chargement affichent un message au lieu de rester bloquées.
- Ajout du nombre total de joueurs classés en bas du classement.
- Le skin hors ligne utilise l'UUID et le pseudo canonique fournis par l'API, sans UUID OfflinePlayer.
- Les échecs temporaires du téléchargement de skin n'empêchent plus les nouvelles tentatives.
