package fr.spectatorplus.util;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Noms d'enchantements et d'effets : les anciens noms Bukkit (DAMAGE_ALL, INCREASE_DAMAGE...) et les noms
 * modernes (SHARPNESS, STRENGTH...) sont acceptés partout dans la configuration.
 */
public final class Names {

    private static final Map<String, String> EFFECT_ALIASES = new HashMap<>();
    private static final Map<String, String> ENCHANT_ALIASES = new HashMap<>();

    static {
        String[][] effects = {
                {"INCREASE_DAMAGE", "STRENGTH"}, {"DAMAGE_RESISTANCE", "RESISTANCE"}, {"SLOW", "SLOWNESS"},
                {"FAST_DIGGING", "HASTE"}, {"SLOW_DIGGING", "MINING_FATIGUE"}, {"JUMP", "JUMP_BOOST"},
                {"HEAL", "INSTANT_HEALTH"}, {"HARM", "INSTANT_DAMAGE"}, {"CONFUSION", "NAUSEA"}};
        for (String[] e : effects) {
            EFFECT_ALIASES.put(e[0], e[1]);
            EFFECT_ALIASES.put(e[1], e[0]);
        }
        String[][] enchants = {
                {"DAMAGE_ALL", "SHARPNESS"}, {"DAMAGE_UNDEAD", "SMITE"}, {"DAMAGE_ARTHROPODS", "BANE_OF_ARTHROPODS"},
                {"ARROW_DAMAGE", "POWER"}, {"ARROW_FIRE", "FLAME"}, {"ARROW_INFINITE", "INFINITY"},
                {"ARROW_KNOCKBACK", "PUNCH"}, {"PROTECTION_ENVIRONMENTAL", "PROTECTION"},
                {"PROTECTION_FIRE", "FIRE_PROTECTION"}, {"PROTECTION_FALL", "FEATHER_FALLING"},
                {"PROTECTION_EXPLOSIONS", "BLAST_PROTECTION"}, {"PROTECTION_PROJECTILE", "PROJECTILE_PROTECTION"},
                {"DIG_SPEED", "EFFICIENCY"}, {"LOOT_BONUS_BLOCKS", "FORTUNE"}, {"LOOT_BONUS_MOBS", "LOOTING"},
                {"DURABILITY", "UNBREAKING"}, {"OXYGEN", "RESPIRATION"}, {"WATER_WORKER", "AQUA_AFFINITY"},
                {"LUCK", "LUCK_OF_THE_SEA"}, {"SWEEPING_EDGE", "SWEEPING"}};
        for (String[] e : enchants) {
            ENCHANT_ALIASES.put(e[0], e[1]);
            ENCHANT_ALIASES.put(e[1], e[0]);
        }
    }

    private Names() {
    }

    /** Nom d'enchantement + son alias (ancien / moderne). */
    public static Set<String> enchantKeys(String name) {
        return keys(name, ENCHANT_ALIASES);
    }

    /** Nom d'effet + son alias (ancien / moderne). */
    public static Set<String> effectKeys(String name) {
        return keys(name, EFFECT_ALIASES);
    }

    private static Set<String> keys(String name, Map<String, String> aliases) {
        Set<String> res = new LinkedHashSet<>();
        if (name == null) return res;
        String n = name.toUpperCase(Locale.ROOT);
        res.add(n);
        String alias = aliases.get(n);
        if (alias != null) res.add(alias);
        return res;
    }
}
