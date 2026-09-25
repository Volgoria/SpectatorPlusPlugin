package fr.spectatorplus.event;

import fr.spectatorplus.api.event.EventCategory;
import fr.spectatorplus.api.event.Importance;
import fr.spectatorplus.api.event.SpectatorEventType;
import org.bukkit.configuration.ConfigurationSection;

import static fr.spectatorplus.api.event.Importance.CRITICAL;
import static fr.spectatorplus.api.event.Importance.IMPORTANT;
import static fr.spectatorplus.api.event.Importance.LOW;
import static fr.spectatorplus.api.event.Importance.NORMAL;

/**
 * Déclaration de toutes les catégories et de tous les évènements natifs.
 * Les messages, paramètres et activations sont dans events.yml.
 */
final class NativeEvents {

    private final EventManager manager;
    private final ConfigurationSection names;

    private NativeEvents(EventManager manager, ConfigurationSection names) {
        this.manager = manager;
        this.names = names;
    }

    static void register(EventManager manager, ConfigurationSection categoryNames) {
        new NativeEvents(manager, categoryNames).registerAll();
    }

    private void category(String id, String defName, String icon) {
        String name = names == null ? defName : names.getString(id, defName);
        manager.registerCategory(new EventCategory(id, name, icon));
    }

    private void t(String id, Importance importance, String icon, String displayName) {
        String category = id.substring(0, id.indexOf('.'));
        manager.registerType(SpectatorEventType.builder(id)
                .category(category)
                .importance(importance)
                .icon(icon)
                .displayName(displayName)
                .message("&7" + displayName + " &8» &f{player}")
                .nativeType(true)
                .build());
    }

    private void registerAll() {
        category(EventCategory.WORLD, "&2Monde et dimensions", "GRASS_BLOCK|GRASS");
        category(EventCategory.MINING, "&7Minage et blocs", "IRON_PICKAXE");
        category(EventCategory.PLAYER, "&eJoueurs", "PLAYER_HEAD|SKULL_ITEM:3");
        category(EventCategory.PVP, "&cCombat PvP", "IRON_SWORD");
        category(EventCategory.DEATH, "&4Mort / élimination", "SKELETON_SKULL|SKULL_ITEM:0");
        category(EventCategory.CRAFT, "&6Crafts et objets", "CRAFTING_TABLE|WORKBENCH");
        category(EventCategory.ENCHANT, "&dEnchantements", "ENCHANTING_TABLE|ENCHANTMENT_TABLE");
        category(EventCategory.PVE, "&aPvE et créatures", "ZOMBIE_HEAD|SKULL_ITEM:2");
        category(EventCategory.POTION, "&5Potions et effets", "BREWING_STAND|BREWING_STAND_ITEM");
        category(EventCategory.GAME, "&bProgression de la partie", "CLOCK|WATCH");
        category(EventCategory.CUSTOM, "&fÉvènements personnalisés", "NAME_TAG");

        // ------------------------------------------------------------- Monde et dimension
        t("world.nether_enter", NORMAL, "NETHERRACK", "Entrée dans le Nether");
        t("world.nether_leave", LOW, "NETHERRACK", "Sortie du Nether");
        t("world.end_enter", IMPORTANT, "END_STONE|ENDER_STONE", "Entrée dans l'End");
        t("world.end_leave", NORMAL, "END_STONE|ENDER_STONE", "Sortie de l'End");
        t("world.change", LOW, "MAP|EMPTY_MAP", "Changement de monde");
        t("world.end_gateway", NORMAL, "ENDER_PEARL", "Utilisation d'une End Gateway");
        t("world.nether_portal", LOW, "OBSIDIAN", "Portail du Nether");
        t("world.end_portal", IMPORTANT, "END_PORTAL_FRAME|ENDER_PORTAL_FRAME", "Portail de l'End");
        t("world.zone_enter", NORMAL, "OAK_FENCE|FENCE", "Entrée dans une zone");
        t("world.zone_leave", LOW, "OAK_FENCE|FENCE", "Sortie d'une zone");
        t("world.coordinate", NORMAL, "COMPASS", "Coordonnée atteinte");
        t("world.structure", NORMAL, "MOSSY_COBBLESTONE", "Structure découverte");
        t("world.biome", LOW, "OAK_SAPLING|SAPLING", "Biome découvert");
        t("world.altitude", LOW, "FEATHER", "Altitude atteinte");
        t("world.depth", LOW, "BEDROCK", "Profondeur atteinte");
        t("world.spawn_away", LOW, "LEATHER_BOOTS", "Éloignement du spawn");
        t("world.spawn_approach", NORMAL, "RED_BED|BED", "Rapprochement du spawn");
        t("world.border_cross", NORMAL, "BARRIER", "Franchissement de la bordure");
        t("world.border_start", IMPORTANT, "BARRIER", "La bordure bouge");
        t("world.border_stop", NORMAL, "BARRIER", "La bordure s'arrête");
        t("world.border_size", IMPORTANT, "BARRIER", "Taille de bordure atteinte");

        // ------------------------------------------------------------- Minage
        t("mining.block_break", LOW, "IRON_PICKAXE", "Bloc miné");
        t("mining.block_place", LOW, "BRICKS|BRICK", "Bloc posé");
        t("mining.first_diamond", IMPORTANT, "DIAMOND_ORE", "Premier diamant");
        t("mining.first_gold", NORMAL, "GOLD_ORE", "Premier or");
        t("mining.first_debris", IMPORTANT, "ANCIENT_DEBRIS|OBSIDIAN", "Premier ancien débris");
        t("mining.block_count", LOW, "COBBLESTONE", "X blocs d'un même type");
        t("mining.diamond_count", IMPORTANT, "DIAMOND", "X diamants minés");
        t("mining.gold_count", NORMAL, "GOLD_INGOT", "X minerais d'or minés");
        t("mining.debris_count", IMPORTANT, "NETHERITE_SCRAP|OBSIDIAN", "X anciens débris minés");
        t("mining.spawner_break", NORMAL, "SPAWNER|MOB_SPAWNER", "Spawner cassé");
        t("mining.rare_place", NORMAL, "BEACON", "Bloc rare posé");
        t("mining.first_ore", NORMAL, "IRON_ORE", "Premier minerai");
        t("mining.silk_touch", LOW, "GLASS", "Minage Silk Touch");
        t("mining.fortune", LOW, "EMERALD", "Minage Fortune");

        // ------------------------------------------------------------- Joueurs
        t("player.damage", LOW, "REDSTONE", "Dégâts reçus");
        t("player.big_damage", NORMAL, "REDSTONE_BLOCK", "Gros dégâts");
        t("player.low_health", IMPORTANT, "FERMENTED_SPIDER_EYE", "Vie basse");
        t("player.health_recovered", LOW, "GLISTERING_MELON_SLICE|SPECKLED_MELON", "Vie remontée");
        t("player.totem", CRITICAL, "TOTEM_OF_UNDYING|TOTEM", "Totem d'immortalité");
        t("player.golden_apple", NORMAL, "GOLDEN_APPLE", "Pomme dorée");
        t("player.enchanted_golden_apple", IMPORTANT, "ENCHANTED_GOLDEN_APPLE|GOLDEN_APPLE:1", "Pomme de Notch");
        t("player.potion", LOW, "POTION", "Utilisation d'une potion");
        t("player.effect_gain", LOW, "GLASS_BOTTLE", "Effet reçu");
        t("player.effect_lose", LOW, "GLASS_BOTTLE", "Effet perdu");
        t("player.absorption_gain", LOW, "GOLD_NUGGET", "Absorption obtenue");
        t("player.absorption_lose", LOW, "GOLD_NUGGET", "Absorption perdue");
        t("player.xp_level", LOW, "EXPERIENCE_BOTTLE|EXP_BOTTLE", "Niveau d'XP atteint");
        t("player.ender_pearl", LOW, "ENDER_PEARL", "Ender Pearl");
        t("player.item_use", LOW, "FLINT_AND_STEEL", "Objet utilisé");
        t("player.armor_equip", LOW, "IRON_CHESTPLATE", "Armure équipée");
        t("player.armor_full", NORMAL, "DIAMOND_CHESTPLATE", "Armure complète");
        t("player.join", LOW, "OAK_DOOR|WOOD_DOOR", "Connexion");
        t("player.quit", NORMAL, "IRON_DOOR", "Déconnexion");
        t("player.reconnect", NORMAL, "OAK_DOOR|WOOD_DOOR", "Reconnexion");
        t("player.afk", LOW, "CLOCK|WATCH", "AFK");
        t("player.afk_back", LOW, "CLOCK|WATCH", "Retour d'AFK");

        // ------------------------------------------------------------- PvP
        t("pvp.attack", LOW, "WOODEN_SWORD|WOOD_SWORD", "Attaque");
        t("pvp.first_hit", NORMAL, "STONE_SWORD", "Premier coup");
        t("pvp.combat_start", NORMAL, "IRON_SWORD", "Début de combat");
        t("pvp.combat_end", LOW, "IRON_SWORD", "Fin de combat");
        t("pvp.damage_dealt", LOW, "DIAMOND_SWORD", "Dégâts infligés");
        t("pvp.damage_taken", LOW, "IRON_CHESTPLATE", "Dégâts reçus (PvP)");
        t("pvp.critical", LOW, "GOLDEN_SWORD|GOLD_SWORD", "Coup critique");
        t("pvp.bow_shot", NORMAL, "BOW", "Tir à longue distance");
        t("pvp.trident", NORMAL, "TRIDENT|ARROW", "Touché par un trident");
        t("pvp.shield_block", LOW, "SHIELD|IRON_DOOR", "Coup bloqué au bouclier");
        t("pvp.shield_break", NORMAL, "SHIELD|IRON_DOOR", "Bouclier désactivé");
        t("pvp.assist", NORMAL, "LEAD|LEASH", "Assistance");
        t("pvp.first_kill", CRITICAL, "NETHERITE_SWORD|DIAMOND_SWORD", "Premier kill");
        t("pvp.double_kill", IMPORTANT, "DIAMOND_SWORD", "Double kill");
        t("pvp.triple_kill", CRITICAL, "DIAMOND_SWORD", "Triple kill");
        t("pvp.kill_streak", IMPORTANT, "BLAZE_POWDER", "Série de kills");
        t("pvp.streak_end", IMPORTANT, "BONE", "Fin d'une série");
        t("pvp.repeat_kill", NORMAL, "SKELETON_SKULL|SKULL_ITEM:0", "Kills répétés");

        // ------------------------------------------------------------- Mort / élimination
        t("death.death", NORMAL, "BONE", "Mort");
        t("death.eliminated", IMPORTANT, "WITHER_SKELETON_SKULL|SKULL_ITEM:1", "Élimination");
        t("death.first_death", CRITICAL, "SKELETON_SKULL|SKULL_ITEM:0", "Première mort");
        t("death.killed_by_player", IMPORTANT, "IRON_SWORD", "Tué par un joueur");
        t("death.pve", NORMAL, "ROTTEN_FLESH", "Mort PvE");
        t("death.fall", NORMAL, "FEATHER", "Mort de chute");
        t("death.lava", NORMAL, "LAVA_BUCKET", "Mort dans la lave");
        t("death.fire", NORMAL, "BLAZE_POWDER", "Mort dans le feu");
        t("death.explosion", NORMAL, "TNT", "Mort d'une explosion");
        t("death.mob", NORMAL, "ZOMBIE_HEAD|SKULL_ITEM:2", "Tué par un mob");
        t("death.void", NORMAL, "ENDER_EYE|EYE_OF_ENDER", "Mort dans le vide");
        t("death.border", NORMAL, "BARRIER", "Mort par la bordure");
        t("death.after_totem", IMPORTANT, "TOTEM_OF_UNDYING|TOTEM", "Mort après un totem");
        t("death.after_combat", NORMAL, "IRON_SWORD", "Mort après un combat");
        t("death.respawn", LOW, "RED_BED|BED", "Réapparition");
        t("death.count", NORMAL, "BONE", "Nombre de morts");
        t("death.players_left", IMPORTANT, "PLAYER_HEAD|SKULL_ITEM:3", "Joueurs restants");

        // ------------------------------------------------------------- Crafts et objets
        t("craft.craft", LOW, "CRAFTING_TABLE|WORKBENCH", "Craft");
        t("craft.first_craft", NORMAL, "CRAFTING_TABLE|WORKBENCH", "Premier craft");
        t("craft.obtain", NORMAL, "CHEST", "Objet obtenu");
        t("craft.pickup", LOW, "HOPPER", "Objet ramassé");
        t("craft.drop", LOW, "DROPPER", "Objet jeté");
        t("craft.amount", NORMAL, "CHEST", "Quantité possédée");
        t("craft.item_use", LOW, "FLINT_AND_STEEL", "Objet utilisé");
        t("craft.item_break", LOW, "ANVIL", "Outil / armure cassé");

        // ------------------------------------------------------------- Enchantements
        t("enchant.enchant", LOW, "ENCHANTING_TABLE|ENCHANTMENT_TABLE", "Enchantement");
        t("enchant.specific", NORMAL, "ENCHANTED_BOOK", "Enchantement spécifique");
        t("enchant.level", NORMAL, "ENCHANTED_BOOK", "Enchantement de haut niveau");
        t("enchant.weapon", NORMAL, "DIAMOND_SWORD", "Arme enchantée");
        t("enchant.armor", NORMAL, "DIAMOND_CHESTPLATE", "Armure enchantée");
        t("enchant.anvil", LOW, "ANVIL", "Utilisation d'une enclume");
        t("enchant.combine", LOW, "ANVIL", "Combinaison d'objets");
        t("enchant.book", NORMAL, "ENCHANTED_BOOK", "Livre enchanté appliqué");

        // ------------------------------------------------------------- PvE
        t("pve.attack", LOW, "WOODEN_SWORD|WOOD_SWORD", "Attaque d'un mob");
        t("pve.kill", LOW, "IRON_SWORD", "Mob tué");
        t("pve.damage", LOW, "ROTTEN_FLESH", "Dégâts d'un mob");
        t("pve.first_kill", NORMAL, "STONE_SWORD", "Premier mob d'un type");
        t("pve.kill_count", NORMAL, "BONE", "X mobs tués");
        t("pve.encounter", LOW, "SPIDER_EYE", "Rencontre d'un mob");
        t("pve.boss_kill", CRITICAL, "NETHER_STAR", "Boss tué");
        t("pve.dragon_attack", IMPORTANT, "DRAGON_HEAD|SKULL_ITEM:5", "Attaque de l'Ender Dragon");
        t("pve.dragon_kill", CRITICAL, "DRAGON_EGG", "Ender Dragon tué");
        t("pve.wither_attack", IMPORTANT, "WITHER_SKELETON_SKULL|SKULL_ITEM:1", "Attaque du Wither");
        t("pve.wither_kill", CRITICAL, "NETHER_STAR", "Wither tué");
        t("pve.wither_summon", CRITICAL, "SOUL_SAND", "Wither invoqué");
        t("pve.warden_trigger", IMPORTANT, "SCULK_SHRIEKER|NOTE_BLOCK", "Warden déclenché");
        t("pve.warden_kill", CRITICAL, "ECHO_SHARD|NETHER_STAR", "Warden tué");

        // ------------------------------------------------------------- Potions et effets
        t("potion.drink", LOW, "POTION", "Potion bue");
        t("potion.splash", LOW, "SPLASH_POTION|POTION", "Potion jetable");
        t("potion.lingering", NORMAL, "LINGERING_POTION|POTION", "Potion persistante");
        t("potion.positive", LOW, "SUGAR", "Effet positif");
        t("potion.negative", LOW, "SPIDER_EYE", "Effet négatif");
        t("potion.strength", NORMAL, "BLAZE_POWDER", "Force");
        t("potion.speed", LOW, "SUGAR", "Vitesse");
        t("potion.resistance", NORMAL, "IRON_CHESTPLATE", "Résistance");
        t("potion.regeneration", NORMAL, "GHAST_TEAR", "Régénération");
        t("potion.poison", NORMAL, "SPIDER_EYE", "Poison");
        t("potion.weakness", NORMAL, "FERMENTED_SPIDER_EYE", "Faiblesse");
        t("potion.slowness", NORMAL, "SOUL_SAND", "Lenteur");
        t("potion.milk", LOW, "MILK_BUCKET", "Lait");

        // ------------------------------------------------------------- Progression
        t("game.start", CRITICAL, "LIME_WOOL|WOOL:5", "Début de la partie");
        t("game.end", CRITICAL, "RED_WOOL|WOOL:14", "Fin de la partie");
        t("game.time", NORMAL, "CLOCK|WATCH", "Temps de jeu");
        t("game.pvp_start", CRITICAL, "DIAMOND_SWORD", "Début du PvP");
        t("game.grace_end", IMPORTANT, "SHIELD|IRON_DOOR", "Fin de la période de grâce");
        t("game.episode_start", IMPORTANT, "BOOK", "Début d'épisode");
        t("game.episode_end", NORMAL, "BOOK", "Fin d'épisode");
        t("game.players_left", IMPORTANT, "PLAYER_HEAD|SKULL_ITEM:3", "Joueurs restants");
        t("game.teams_left", IMPORTANT, "WHITE_BANNER|BANNER:15", "Équipes restantes");
        t("game.half_eliminated", IMPORTANT, "BONE", "Moitié des joueurs éliminés");
        t("game.last_survivor", CRITICAL, "TOTEM_OF_UNDYING|TOTEM", "Dernier survivant");
        t("game.team_win", CRITICAL, "BEACON", "Victoire d'équipe");
        t("game.player_win", CRITICAL, "BEACON", "Victoire");
    }
}
