# Spectator Plus

Système spectateur avancé pour serveurs Minecraft **1.8 → 26.x** (Spigot / Paper).

## Compilation (IntelliJ IDEA)

Projet **Gradle** multi-modules (wrapper inclus, Gradle 9.8) :

| Module | Contenu | Commande | Jar produit |
|---|---|---|---|
| `core` | logique commune, sans Bukkit ni loader (Java 8) | — (embarqué dans les autres) | — |
| `bukkit` | plugin Spigot / Paper / hybrides, 1.8 → 26.x | `gradlew :bukkit:build` | `bukkit/build/libs/SpectatorPlus-1.0.0.jar` |
| `fabric` | mod server-side, une version par Minecraft (Stonecutter) | `gradlew :fabric:26.3:build` | `fabric/versions/<mc>/build/libs/SpectatorPlus-Fabric-1.0.0+<mc>.jar` |

1. `File > Open…` → sélectionner le dossier du projet (IntelliJ détecte `settings.gradle.kts`).
2. `gradlew build` compile tout (plugin + toutes les versions Fabric).

Versions Fabric : 1.16.5, 1.18.2, 1.19.2, 1.20.1, 1.20.4, 1.21.1, 1.21.11, 26.3 (fichier
`fabric/stonecutter.properties.toml`). Le code Fabric est unique : les différences entre versions
s'écrivent avec les commentaires Stonecutter (`//? if >=1.18 {` … `//?}`), en mappings Mojang partout.

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

La détection se fait au démarrage (`compat/Platform`) et apparaît dans la console.

## Structure

| Package | Rôle |
|---|---|
| `api` | API publique (interfaces, évènements Bukkit, builders) |
| `compat` | Couche de compatibilité 1.8 → 26.x (réflexion, matériaux, sons, action bar…) |
| `spectator` | Entrée/sortie, protections, follow, POV, barre d'inventaire |
| `event` | Registre des évènements, diffusion, statistiques, combats, zones |
| `event.detector` | Détection des ~150 évènements natifs |
| `filter` | Filtres, presets, préférences joueurs |
| `gui` | Framework de menus + 15 menus |
| `storage` | Préférences en YAML / SQLite / MySQL |

## Fichiers de configuration

- `config.yml` : langue, mode (MANUAL / SEMI_AUTO / AUTO), comportement, suivi, passe-muraille, barre d'inventaire, zones, presets, filtres admin, stockage.
- `events.yml` : activation, importance et paramètres de chaque évènement.
- `lang/<code>.yml` : tous les textes (chat, menus, HUD, barre d'inventaire, presets, noms et messages des évènements).

## Langues

Fournies : **fr**, **en**, **es**, **de**, **pt**. Langue d'un joueur : son choix (`/spec lang` ou menu Paramètres)
→ langue de son client Minecraft (`language.per-player`) → `language.default`.
Une clé absente d'un fichier retombe sur la langue par défaut puis sur l'anglais.
Ajouter une langue : copier `lang/en.yml` en `lang/xx.yml` et traduire, puis `/spec reload`.

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
