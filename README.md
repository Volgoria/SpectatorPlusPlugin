# Spectator Plus

Système spectateur avancé pour serveurs Minecraft : plugin **1.8 → 26.x** (Spigot / Paper) et mod server-side
**Fabric**, **Forge** et **NeoForge** (clients vanilla).

## Compilation (IntelliJ IDEA)

Projet **Gradle** multi-modules (wrapper inclus, Gradle 9.8) :

| Module | Contenu | Commande | Jar produit |
|---|---|---|---|
| `core` | logique commune, sans Bukkit ni loader (Java 8) | — (embarqué dans les autres) | — |
| `bukkit` | plugin Spigot / Paper / hybrides, 1.8 → 26.x | `gradlew :bukkit:build` | `bukkit/build/libs/SpectatorPlus-1.0.0.jar` |
| `mod` | mod server-side, un nœud par version et loader (Stonecutter) | `gradlew :mod:26.3-fabric:build` (ou `-forge`, `-neoforge`) | `mod/versions/<mc>-<loader>/build/libs/SpectatorPlus-<Loader>-1.0.0+<mc>.jar` |

1. `File > Open…` → sélectionner le dossier du projet (IntelliJ détecte `settings.gradle.kts`).
2. `gradlew build` compile tout (plugin + toutes les versions des mods). Le premier build télécharge et
   décompile Minecraft pour chaque version et loader : compter une bonne heure.

### Versions des mods

| Loader | Versions | Outil de build (fichier) |
|---|---|---|
| Fabric | 1.16.5, 1.18.2, 1.19.2, 1.20.1, 1.20.4, 1.21.1, 1.21.11, 26.3 | Fabric Loom (`build.fabric.gradle.kts`) |
| Forge | 1.18.2, 1.19.2, 1.20.1 | ModDevGradle legacyforge, refmap SRG (`build.forge.gradle.kts`) |
| Forge | 1.20.4 | ForgeGradle 7 + renamer, SnakeYAML relocalisé (`build.forge7srg.gradle.kts`) |
| Forge | 1.21.1, 1.21.11, 26.3 | ForgeGradle 7, jar-in-jar (`build.forge7.gradle.kts`) |
| NeoForge | 1.21.1, 1.21.11, 26.3 | ModDevGradle (`build.neoforge.gradle.kts`) |

Non couverts : Forge 1.16.5 (ses mappings « officiels » gardent les classes MCP, incompatibles avec le code
commun), Forge 1.12.2 / 1.8.9 (code du jeu entièrement différent), NeoForge 1.20.4 (noms SRG à l'exécution,
non gérés par ModDevGradle). NeoForge 1.20.1 accepte en principe le jar Forge 1.20.1.

Pour Forge, seul le jar de `build/libs` est à distribuer (les jars intermédiaires sont dans `build/devlibs`).

Les versions et dépendances sont dans `mod/stonecutter.properties.toml`. Le code du mod est unique : les
différences entre versions et loaders s'écrivent avec les commentaires Stonecutter (`//? if >=1.18 {` …
`//?}`, `//? if forge {`), en mappings Mojang partout ; elles sont regroupées dans `mod/.../Mc.java`,
`ModItems.java` et `Keys.java`. Les points d'entrée des loaders sont dans `mod.fabric`, `mod.forge` et
`mod.neoforge` ; les mixins (package `mod.mixin`) sont communs aux trois. **Le code source est écrit pour
26.3-fabric** (version active) : ne pas changer de version active dans IntelliJ sans revenir sur 26.3-fabric avant de commit.

Le plugin Bukkit compile contre l'API **Spigot 1.8.8** en bytecode **Java 8** : le même jar tourne sur un
serveur 1.8 (Java 8) comme sur un serveur 26.x (Java 25). Tout ce qui n'existe pas en 1.8 est
appelé par réflexion (`fr.spectatorplus.compat`). **Ne jamais référencer directement une constante
`Material`, `Sound`, `Biome`, `Enchantment` ou `PotionEffectType` renommée après 1.8** : passer par
`Mat`, `Compat` ou `Sounds`.

## Plateformes

| Serveur | Support |
|---|---|
| Spigot / CraftBukkit | 1.8 → 26.x (base) |
| Paper, Purpur, forks | 1.8 → 26.x, API Paper utilisée quand elle existe (`compat/PaperHooks`) |
| Hybrides Forge / NeoForge (Mohist, Youer, Arclight, Magma, Ketting, CatServer) | oui, joueurs fictifs des mods ignorés, inventaires de mods bloqués |
| Hybrides Fabric (Banner, Cardboard, Arclight Fabric) | oui, idem |
| Folia | non (planificateur Bukkit indisponible) |
| Fabric, Forge, NeoForge (mod natif) | voir « Versions des mods », server-side (clients vanilla) |

La détection se fait au démarrage (`compat/ServerType`) et apparaît dans la console.

## Mods (Fabric, Forge, NeoForge)

- Configuration : `config/spectatorplus/` (mêmes fichiers que le plugin). Les mondes s'appellent par leur
  identifiant de dimension (`minecraft:overworld`, `minecraft:the_nether`…) dans `zones` et `events.disabled-worlds`.
- Permissions : valeurs par défaut du `plugin.yml` (true / op / false). Sur Fabric, avec un gestionnaire compatible
  fabric-permissions-api (LuckPerms…), les nœuds `spectatorplus.*` sont utilisés tels quels ; sur Forge / NeoForge,
  uniquement les valeurs par défaut pour l'instant.
- Commande `/spectatorplus` (alias `/spec`, `/sp`, `/spectate`), menus en coffres, barre d'inventaire, suivi, POV,
  passe-muraille, joueurs cachés, protections (clics, objets, collisions, ciblage des monstres, dégâts).
- Stockage : YAML (SQLite / MySQL demandent un driver JDBC installé sur le serveur, sinon retour au YAML).
- Évènements natifs : même logique que le plugin (`core` › `event.detect`), branchée par mixins sur le jeu
  (dégâts réels, morts, déclencheurs de progrès vanilla, minage, crafts, enclume, ramassage...).
  Différences : le passage d'une passerelle de l'End n'est pas détecté ; « portail » est déduit du changement de dimension.
- Pas encore portés : séparation du chat, blocage de commandes, portails bloqués pour les spectateurs, sommeil.
- Langue du client détectée à partir de 1.18 (en 1.16.5 : `/spec lang` ou langue par défaut).

## Structure

Toute la logique est dans `core`, écrite contre une petite API de plateforme (`core.platform` : joueur,
monde, serveur, icônes de menu). Chaque plateforme implémente cette API et transmet les évènements du jeu.

| Module / package | Rôle |
|---|---|
| `core` › `SpectatorCore` | Racine commune : crée et relie tous les services |
| `core` › `core.platform` | API de plateforme : `Platform`, `PlatformPlayer`, `PlatformWorld`, `Icon`, `ApiBridge`… |
| `core` › `core.config` | YAML indépendant de Bukkit (`ConfigSection`, même comportement que Bukkit, testé) |
| `core` › `spectator` | Entrée/sortie, visibilité, follow, POV, passe-muraille, HUD, barre d'inventaire |
| `core` › `event` | Registre des évènements, diffusion, statistiques, combats, zones |
| `core` › `filter` | Filtres, presets, préférences joueurs |
| `core` › `gui` | Framework de menus + 15 menus (décrits en `Icon`, affichés par la plateforme) |
| `core` › `command`, `game`, `placeholder`, `storage`, `config` | Commande `/spec`, partie, placeholders, stockage, langues |
| `bukkit` › `bukkit` | Implémentation Bukkit de la plateforme + implémentation de l'API publique |
| `bukkit` › `api` | API publique Bukkit (interfaces, évènements Bukkit, builders) — inchangée |
| `bukkit` › `compat` | Couche de compatibilité 1.8 → 26.x (réflexion, matériaux, action bar…) |
| `bukkit` › `spectator` | Protections du spectateur (listeners Bukkit) |
| `core` › `event.detect` | Logique des ~150 évènements natifs (« signaux » envoyés par les plateformes) |
| `bukkit` › `event.detector` | Traduction des évènements Bukkit en signaux |
| `mod` | Mod Fabric (puis NeoForge / Forge), un code source compilé pour chaque version |

## Fichiers de configuration

- `config.yml` : langue, mode (MANUAL / SEMI_AUTO / AUTO), comportement, suivi, passe-muraille, barre d'inventaire, zones, presets, filtres admin, stockage.
- `events.yml` : activation, importance et paramètres de chaque évènement.
- `lang/<code>.yml` : tous les textes (chat, menus, HUD, barre d'inventaire, presets, noms et messages des évènements).

## Langues

Fournies : **fr**, **en**, **es**, **de**, **pt**. Langue d'un joueur : son choix (`/spec lang` ou menu Paramètres)
→ langue de son client Minecraft (`language.per-player`) → `language.default`.
Une clé absente d'un fichier retombe sur la langue par défaut puis sur l'anglais.
Ajouter une langue : copier `lang/en.yml` en `lang/xx.yml` et traduire, puis `/spec reload`.

## Fil d'évènements et vie des joueurs

- Seuls les **spectateurs** reçoivent les évènements dans le chat. Pour les casters / le staff non spectateurs :
  `events.staff-receive: true` + permission `spectatorplus.events.receive` (donnée à personne par défaut).
- Les points de vie des joueurs s'affichent sous leur pseudo **pour les spectateurs uniquement**
  (`spectator.health-below-name`, unité `spectator.health-title` dans les fichiers de langue).
  Bukkit : le spectateur reçoit une copie synchronisée du scoreboard principal (équipes, sidebar) avec l'objectif
  de vie en plus ; s'il a déjà un scoreboard personnel donné par un autre plugin, l'objectif y est ajouté.
  Mods : objectif envoyé par paquets aux seuls spectateurs, le scoreboard du serveur n'est pas modifié.

## Intégration UHCCore

Si UHCCore est installé (`hooks.uhccore: true`, par défaut), c'est lui qui décide qui est spectateur :

- un joueur que UHCCore passe spectateur (mort, élimination, connexion en cours de partie) entre dans Spectator Plus
  après sa réapparition ; un revive ou une arrivée tardive le rend à UHCCore sans toucher à son mode de jeu, sa
  position ni son kit (son inventaire d'avant n'est rendu que s'il n'a pas reçu autre chose entre-temps) ;
- les joueurs en vie, les équipes, le début de partie, le PvP et les gagnants viennent de l'API UHCCore : seuls les
  joueurs en vie sont proposés pour la téléportation, le suivi et le POV ;
- à la fin de la partie, les spectateurs sont rendus à UHCCore (spectateur vanilla puis retour au lobby) ;
- `mode`, `auto.*`, `spectator.keep-on-quit` et `spectator.persist-on-restart` sont ignorés ;
- avec le scénario SelfDiagnosis, la vie des joueurs est cachée aux spectateurs (sous les pseudos, HUD, menus,
  évènements de vie).

## Suivi et passe-muraille

- Le suivi n'utilise plus de téléportations en boucle : une vélocité est appliquée chaque tick vers la
  position idéale (anticipation du déplacement de la cible). La caméra reste libre. Styles : `SMOOTH`, `BEHIND`, `LEASH`.
- En mode Adventure, les collisions sont calculées par le client : impossible de traverser un bloc.
  Le passe-muraille bascule donc le spectateur en mode Spectator vanilla au contact d'un bloc,
  puis le remet en Adventure à l'air libre. Pendant la traversée, la barre d'inventaire n'est pas utilisable.

## API

```java
SpectatorPlusAPI api = SpectatorPlusProvider.get();

// Spectateurs
api.enterSpectator(player, EnterReason.ELIMINATION);
Spectator spec = api.getSpectator(player);
spec.follow(target);

// Progression de la partie
GameService game = api.getGameService();
game.startGame();
game.startPvp();
game.startEpisode(2);
game.setTeamProvider(p -> myTeams.get(p.getUniqueId()));

// Évènement personnalisé
EventService events = api.getEventService();
events.registerType(SpectatorEventType.builder("uhc.role.reveal")
        .category(EventCategory.CUSTOM)
        .displayName("Révélation d'un rôle")
        .importance(Importance.IMPORTANT)
        .message("&d{player} &7est &d{role}")
        .icon("NAME_TAG")
        .build());
events.fire(events.builder("uhc.role.reveal").player(player).data("role", "Loup-Garou").build());

// Placeholder externe, utilisable partout sous la forme {role}
api.getPlaceholderService().register("role", ctx -> roles.get(ctx.getSubjectId()));

// Filtres
api.getFilterService().registerCondition("same-team", (viewer, e) -> true);
api.getFilterService().lockFilter(player, "pvp");
```

Évènements Bukkit : `SpectatorEnterEvent` (annulable en SEMI_AUTO), `SpectatorLeaveEvent`,
`SpectatorTeleportEvent`, `SpectatorFollowStartEvent`, `SpectatorFollowStopEvent`,
`SpectatorFollowTargetChangeEvent`, `SpectatorInspectEvent`, `SpectatorInventoryInspectEvent`,
`SpectatorFreezeEvent`, `SpectatorUnfreezeEvent`, `SpectatorFilterChangeEvent`,
`SpectatorMenuOpenEvent`, `SpectatorMenuCloseEvent`, `SpectatorGameEventTriggerEvent`.

Dans le `plugin.yml` du plugin externe : `depend: [SpectatorPlus]` (ou `softdepend`), et ajouter le jar
de Spectator Plus en dépendance `provided`.
