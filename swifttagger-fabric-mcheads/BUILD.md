# Patch 0.2.9 — profils / classement / points / commandes

Correctifs appliqués :
- `/tier <joueur>` et `/tiers <joueur>` sont maintenant de vrais alias client, en plus de `/swift tier <joueur>` et `/swift tiers <joueur>`.
- Le cache des profils est désormais insensible à la casse du pseudo.
- Le client API accepte davantage de formats de réponse (`data`, `player`, `profile`, `stats`, `tiers`, `currentTiers`, `peaks`, etc.), ce qui évite des profils affichés sans tiers/points/classement lorsque le serveur utilise une structure différente.
- Le classement essaie plusieurs routes compatibles et retombe proprement sur le rang fourni directement par le profil.
- La carte tente de récupérer le skin via le fournisseur vanilla même si le joueur n'est plus connecté ou n'est plus dans le TAB, avec Steve seulement en dernier recours.

Note : le ZIP source fourni ne contient pas `gradle/wrapper/gradle-wrapper.jar`, donc le build n'a pas pu être exécuté ici avec `./gradlew build`.

---

# SwiftTagger 0.1.4 — build 1.21.11

## Correctifs 0.1.4 (icônes de mode qui s'affichaient en carré vide)

Symptôme signalé : dans l'écran `/tiers <joueur>` (et sur la touche
"joueur le plus proche"), chaque ligne de tier affichait bien le texte
(`Axe : ...`, `Crystal : ...`, etc.) mais l'icône du mode devant restait un
petit carré vide au lieu de l'émoji attendu (🪓 🔨 ☠️ ⚗️ ⛨ 🗡️ ❤️ ✦).

**Cause** : ces émojis sont de vrais caractères Unicode, mais la police par
défaut du client Minecraft (unifont) ne couvre pas tous ces points de code.
Le texte envoyé était donc correct, seul le rendu graphique du client
échouait — d'où le carré vide ("tofu"), qui dépend en plus de la police du
joueur qui regarde l'écran (peut marcher pour l'un, pas pour l'autre).

**Correctif** : le mod embarque désormais sa propre petite police bitmap
(8 sprites 16×16 dessinés pour chaque mode, voir
`assets/swifttagger/font/icons.json` +
`assets/swifttagger/textures/font/icon_*.png`), garantie présente dans le
jar du mod et donc toujours affichée correctement, quel que soit le système
du joueur. Les 8 icônes par défaut (`kit/KitIcons.java`) pointent maintenant
vers ce sprite sheet au lieu des émojis Unicode bruts :

| Mode           | Icône      |
|----------------|------------|
| Axe            | hache      |
| Mace           | marteau    |
| Netherite Pot  | crâne      |
| Diamond Pot    | fiole      |
| SMP            | bouclier   |
| Sword          | dague      |
| UHC            | cœur       |
| Vanilla        | étoile     |

Un serveur/joueur qui personnalise une icône dans `config/swifttagger.json`
avec son propre texte/émoji garde ce comportement : dans ce cas précis le
mod n'impose plus sa police custom (voir `KitIcons#iconText`), donc si
l'émoji choisi ne s'affiche pas chez un client donné, c'est de nouveau une
limite de la police de CE client, réglable en changeant la valeur dans le
fichier de config, sans recompiler.

L'écran `/tiers` (`PlayerSearchScreen`) a été réécrit pour composer chaque
ligne comme un vrai `Text` structuré (icône stylée + libellé coloré) au lieu
d'une simple chaîne concaténée, ce qui est ce qui permet à l'icône de porter
sa propre police indépendamment de la couleur du tier affiché à côté.

## Correctif 0.1.5 (erreur de compilation `incompatible types: Identifier cannot be converted`)

Minecraft **1.21.11** a changé la signature de `Style#withFont` : elle
attend désormais un `StyleSpriteSource` (nouveau système de sprites dans le
texte, avec les variantes `StyleSpriteSource.Font` / `.Player` / `.Sprite`)
et non plus un simple `Identifier` comme dans les versions précédentes.
`kit/KitIcons.java` appelait encore `.withFont(ICON_FONT)` avec un
`Identifier` brut, ce qui ne compile plus sur 1.21.11.

Corrigé en enveloppant une seule fois la police du mod dans
`new StyleSpriteSource.Font(ICON_FONT)` (voir `KitIcons#ICON_FONT_SOURCE`) et
en passant cet objet à `withFont(...)` à la place de l'`Identifier` brut. Le
comportement (icône bitmap embarquée, teinte blanche neutre) est identique à
avant, seul le type attendu par l'API Minecraft a changé.


La commande `/tiers <joueur>` (autocomplétion Tab sur tous les joueurs
connus de la liste TAB) et le message d'action bar vert/rouge
`Gamemode : ...` sur la touche de détection manuelle étaient déjà en place
dans le zip fourni et n'ont pas été modifiés — seul le rendu des icônes était
en cause.


_(mod anciennement nommé "OurTiers" — voir la section "Renommage" tout en bas)_

## Ce qui a été corrigé dans cette passe

Le zip précédent (0.1.2) ne compilait même pas : le système de détection de
kit livré séparément (`kit/`, `text/TierTextRenderer`, `config/TierResolver`,
`config/TierFormat`, `config/TierColors`) référençait une classe
`KitDefinition` qui n'existait dans AUCUN fichier fourni, et rien de tout ça
n'était branché au reste du mod (les mixins nametag/tab utilisaient encore un
préfixe `[tier] ` codé en dur, sans passer par le détecteur de kit).

Corrections apportées :

1. **`kit/KitDefinition.java` créée** (la classe manquante qui empêchait toute
   compilation).
2. **Détection de kit branchée de bout en bout** : `KitDetectionService` est
   maintenant instancié dans `OurTiersClient`, alimenté par une détection
   automatique en tâche de fond (toutes les ~10 ticks, avec cache interne
   donc quasi gratuite) ET par la touche dédiée (qui affichait juste un TODO
   avant).
3. **`TierResolver` + `TierTextRenderer` utilisés dans les mixins** (nametag,
   TAB) et dans le chat, au lieu du texte doré codé en dur. Le kit détecté
   choisit désormais réellement quel tier afficher en mode `ADAPTIVE_HIGHEST`.
4. **`ModConfig` complété** : il manquait la moitié des champs utilisés par
   les classes fournies (`tierFormat`, `useTierColor`, `tierColors`,
   `nicknameColorMode`, `customNicknameColorRgb`, `enabled`,
   `autoDetectEnabled`, seuils de détection, priorité des kits, URL de
   l'API...). `ConfigManager` réécrit désormais systématiquement le fichier
   au chargement pour combler les champs manquants d'un ancien
   `ourtiers.json`.
5. **Mod Menu corrigé** : l'écran renvoyé était un placeholder vide avec
   "(à faire)" dans le titre. `OurTiersConfigScreen` est un vrai écran avec
   les interrupteurs des sections 9/10/11 (activé, détection auto, chat/TAB/
   nametag, mode d'affichage, couleur du tier/pseudo, format `{tier}
   {player} {kit}`). Les couleurs par tier, la priorité des kits en cas
   d'égalité et l'URL de l'API restent éditables dans `config/ourtiers.json`
   (pas de picker en jeu dans cette passe).
6. **Touches (keybinds) réellement fonctionnelles** : la touche de recherche
   ouvre l'écran du joueur le plus proche (et prévient si personne n'est à
   proximité au lieu de ne rien faire), la touche de détection lance
   vraiment `KitDetectionService#detectLocal` et affiche le kit + le
   pourcentage de correspondance dans le chat local.
7. **Affichage dans le chat implémenté** (section 10, désactivé par défaut,
   `showOnChat` dans la config) via `ChatDisplay` + les évènements Fabric
   `ClientReceiveMessageEvents.ALLOW_CHAT`, plutôt qu'un mixin fragile sur les
   méthodes internes de `ChatHud` (dont la signature change souvent d'une
   version de Minecraft à l'autre).
8. **URL de l'API rendue configurable** (`ModConfig#apiBaseUrl`) au lieu
   d'être codée en dur dans `TierApiClient`, dans le même esprit que les
   couleurs de tier (section 6 : "valeurs configurables, jamais hardcodées
   de manière impossible à modifier").
9. **Robustesse** : `ConfigManager`, `TierApiClient` et les mixins gèrent
   maintenant explicitement les cas de config/API absente ou malformée sans
   planter (section 16).

## Important

Le JAR `ourtiers-0.1.0-dev.jar` (ou tout jar `*-dev.jar`) ne doit pas être
installé dans `mods` : c'est le JAR de développement avec les noms Yarn.
Le fichier à installer est celui produit par la tâche `remapJar`/`build`
dans `build/libs/`, sans le suffixe `-dev`.

## Environnement cible

- Minecraft 1.21.11
- Java 21
- Yarn 1.21.11+build.3
- Fabric Loader 0.19.3
- Fabric API 0.141.6+1.21.11
- Fabric Loom Remap 1.14.5
- Mod Menu 17.0.0 (optionnel, seulement pour l'écran de configuration)

## Compilation

Depuis ce dossier :

```text
./gradlew build
```

Sur Windows :

```text
gradlew.bat build
```

Le fichier à installer sera dans `build/libs/` et **ne doit pas avoir le
suffixe `-dev`**.

## À savoir / limites connues

- La détection de kit sur un **autre** joueur ne peut se baser que sur
  l'armure + les deux mains (limite du protocole Minecraft, voir le
  Javadoc de `InventoryScanner`) : elle est donc "meilleur effort" et
  peut renvoyer `Unknown` si trop peu d'items sont visibles. Sur le
  joueur local, la détection scanne le sac complet (36 slots) et est fiable.
- Pas encore de sélecteur de couleur en jeu pour `tierColors` : à éditer
  dans `config/ourtiers.json` (voir `TierColors#defaults()` pour la liste
  des clés de tier attendues).
- `ChatDisplay` reformate le message en texte simple (perd le style
  éventuel du message d'origine) pour pouvoir y insérer le tag de tier ;
  c'est un compromis assumé pour rester sur l'API stable de Fabric plutôt
  qu'un mixin fragile sur `ChatHud`.

## Renommage OurTiers -> SwiftTagger + nouvelles fonctionnalités

- **Nom / description / icône** : `fabric.mod.json` déclare maintenant
  `"name": "SwiftTagger"`, une description en français ("TierTagger de
  SwiftPvP...") et une icône (`assets/swifttagger/icon.png`, l'image fournie).
  Le paquet Java est passé de `com.example.ourtiers` à
  `com.example.swifttagger`, la classe d'entrée client s'appelle
  `SwiftTaggerClient` (`OurTiersClient` avant), l'écran de config
  `SwiftTaggerConfigScreen`.
- **Fichier de config renommé** : `config/ourtiers.json` devient
  `config/swifttagger.json`. `ConfigManager` migre automatiquement un ancien
  `ourtiers.json` s'il en trouve un et qu'aucun `swifttagger.json` n'existe
  encore (une seule fois, au premier lancement après mise à jour). Le fichier
  `config-example/swifttagger.json` de ce dépôt montre les valeurs par défaut
  telles qu'elles seront écrites au premier lancement (calées sur le
  `ourtiers.json` de référence fourni : `showOnChat: true`,
  `displayedTierMode: ABSOLUTE_HIGHEST`, `tierFormat: "{player} {tier}"`,
  `nicknameColorMode: GRAY`).
- **Vraies émojis par mode** (`ModConfig#kitIcons`, éditable en JSON) :
  AXE 🪓, MACE 🔨, Netherite Pot ☠️, Diamond Pot ⚗️, SMP ⛨, Sword 🗡️, UHC ❤️,
  Vanilla ✦. Attention : ce sont de vrais émojis Unicode, pas les symboles
  "safe font" utilisés avant — si l'un d'eux s'affiche en carré vide chez un
  joueur (police client qui ne le couvre pas), il suffit de le remplacer par
  un autre caractère directement dans `config/swifttagger.json`, sans
  recompiler.
- **Icône à gauche ou à droite du tier** : nouveau champ
  `ModConfig#iconPosition` (`"LEFT"` ou `"RIGHT"`), réglable aussi depuis
  l'écran Mod Menu. S'applique à l'écran /tiers (section suivante).
- **`/tiers <joueur>` et la touche de recherche du joueur le plus proche**
  ouvrent désormais un écran "carte joueur" façon capture d'écran de
  référence : `PLAYER: <pseudo>`, puis `TOP <rang> (<points> points)` si
  l'API les fournit (nouveaux champs optionnels `rank`/`points` dans la
  réponse JSON — absents, la ligne est simplement masquée), puis deux
  sections `TIERS:` (tiers actuels) et `RETIRED TIERS:` (peaks), une ligne
  par mode avec son émoji et son tier coloré.
- **Autocomplétion de pseudo sur `/tiers`** : appuyer sur Tab après avoir
  commencé à taper un nom propose tous les joueurs actuellement connus du
  client (liste TAB du serveur), pas seulement ceux à portée de vue.
- **Détection manuelle silencieuse côté chat** : la touche de détection
  (Y par défaut) n'écrit plus rien dans le tchat. Le résultat est affiché
  uniquement dans l'action bar : `Gamemode : <Mode>` en vert si un kit est
  détecté, `Gamemode : Unknown` en rouge sinon.

## Messages centralisés (0.2.14, revu en 0.2.24)
Tous les textes du mod sont regroupés dans `com.example.swifttagger.text.Messages`, en dur dans le code (constante `VALUES`, remplie une fois au chargement de la classe). Depuis 0.2.24, il n'y a plus de fichier `config/swifttagger/messages.json` lu/écrit sur le disque : seul un rebuild du mod permet de changer un texte, aucun joueur ne peut les modifier après coup.

## Écran de configuration basé sur Cloth Config (0.2.24)
`SwiftTaggerConfigScreen` ne construit plus son propre `Screen` à la main : il assemble un écran via l'API Cloth Config (`me.shedaniel.clothconfig2`), la même bibliothèque que beaucoup d'autres mods (onglets avec flèches quand ça déborde, lignes compactes sans fond, survol qui surligne en gris clair et affiche l'info-bulle, petite flèche de reset par ligne). Cloth Config est désormais une dépendance **requise** (voir `depends` dans `fabric.mod.json` et `modImplementation` dans `build.gradle`), contrairement à Mod Menu qui reste optionnel.

## Patch 0.2.15 — raccourcissement des raccourcis clavier

- La catégorie des touches est maintenant correctement localisée avec la clé
  `key.category.swifttagger.general` et s'affiche simplement comme **SwiftTagger**.
- `Chercher le tier du joueur le plus proche` devient **Chercher un tier**.
- `Forcer la détection de kit` devient **Détecter le kit**.
