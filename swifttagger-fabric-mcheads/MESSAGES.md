# SwiftTagger - textes du mod

Depuis la version 0.2.24, tous les textes affichés par le mod sont centralisés
dans `com.example.swifttagger.text.Messages`, **en dur dans le code source**.

Il n'y a plus de fichier `config/swifttagger/messages.json` : rien n'est lu ni
écrit sur le disque pour les textes. Un joueur (ou un serveur) ne peut donc
plus les modifier — seul le développeur peut changer un texte, en éditant
`Messages.java` puis en recompilant (`./gradlew build`).

- `action.no_nearby` : message affiché dans l'action bar lorsqu'aucun joueur
  n'est trouvé dans un rayon de 25 blocs.
- Les messages de commandes, GUI, boutons, chat et action bar utilisent tous
  cette même classe (`Messages.get("clé")`).
