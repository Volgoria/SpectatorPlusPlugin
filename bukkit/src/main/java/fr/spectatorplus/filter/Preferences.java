package fr.spectatorplus.filter;

import fr.spectatorplus.api.event.Importance;
import fr.spectatorplus.api.filter.PlayerFilterMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Préférences d'un joueur : filtres d'évènements + paramètres personnels.
 * Sérialisées en YAML (fichier ou colonne SQL).
 */
public final class Preferences {

    // --- preset / catégories / évènements
    public String preset = "standard";
    public boolean customized;
    public final Map<String, Boolean> categories = new HashMap<>();
    public final Map<String, Boolean> events = new HashMap<>();
    public Importance minImportance = Importance.LOW;

    // --- joueurs / équipes
    public PlayerFilterMode playerMode = PlayerFilterMode.ALL;
    public final Set<UUID> whitelist = new HashSet<>();
    public final Set<UUID> blacklist = new HashSet<>();
    public final Set<UUID> favorites = new HashSet<>();
    public final Map<UUID, String> knownNames = new HashMap<>();
    public boolean hideOwn;
    public final Set<String> teams = new HashSet<>();

    // --- mondes
    public boolean overworld = true;
    public boolean nether = true;
    public boolean end = true;
    public boolean customWorlds = true;
    public boolean currentWorldOnly;

    // --- distance
    public int maxDistance;
    public int minDistance;
    public boolean sameChunk;
    public boolean sameWorld;
    public boolean sameZone;

    // --- dégâts
    public double minDamage;
    public double maxDamage;
    public final Set<String> damageTypes = new HashSet<>();
    public boolean pvpOnly;
    public boolean pveOnly;
    public boolean criticalOnly;

    // --- temporel
    public int rateLimit = 6;
    public boolean showTime;

    // --- paramètres personnels
    public int flySpeed = 2;
    public boolean seeSpectators = true;
    public boolean sounds = true;
    public boolean actionBar = true;
    public boolean titles = true;
    public boolean chatEvents = true;
    public int followDistance = 4;
    public boolean noclip = true;
    /** Code de langue (fr, en...) ou « auto » (langue du client / du serveur). */
    public String language = "auto";

    public Preferences copy() {
        // passage par le texte : les Map deviennent de vraies sections YAML
        Preferences p = new Preferences();
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(serialize());
        } catch (Exception ignored) {
        }
        p.load(y);
        return p;
    }

    public YamlConfiguration toYaml() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("preset", preset);
        y.set("customized", customized);
        y.set("categories", new LinkedHashMap<>(categories));
        Map<String, Boolean> ev = new LinkedHashMap<>();
        // les ids contiennent des points : on les encode pour ne pas créer de sous-sections
        for (Map.Entry<String, Boolean> e : events.entrySet()) ev.put(e.getKey().replace('.', '~'), e.getValue());
        y.set("events", ev);
        y.set("min-importance", minImportance.name());
        y.set("players.mode", playerMode.name());
        y.set("players.whitelist", uuids(whitelist));
        y.set("players.blacklist", uuids(blacklist));
        y.set("players.favorites", uuids(favorites));
        Map<String, String> names = new LinkedHashMap<>();
        for (Map.Entry<UUID, String> e : knownNames.entrySet()) names.put(e.getKey().toString(), e.getValue());
        y.set("players.names", names);
        y.set("players.hide-own", hideOwn);
        y.set("teams", new ArrayList<>(teams));
        y.set("worlds.overworld", overworld);
        y.set("worlds.nether", nether);
        y.set("worlds.end", end);
        y.set("worlds.custom", customWorlds);
        y.set("worlds.current-only", currentWorldOnly);
        y.set("distance.max", maxDistance);
        y.set("distance.min", minDistance);
        y.set("distance.same-chunk", sameChunk);
        y.set("distance.same-world", sameWorld);
        y.set("distance.same-zone", sameZone);
        y.set("damage.min", minDamage);
        y.set("damage.max", maxDamage);
        y.set("damage.types", new ArrayList<>(damageTypes));
        y.set("damage.pvp-only", pvpOnly);
        y.set("damage.pve-only", pveOnly);
        y.set("damage.critical-only", criticalOnly);
        y.set("time.rate-limit", rateLimit);
        y.set("time.show-time", showTime);
        y.set("settings.fly-speed", flySpeed);
        y.set("settings.see-spectators", seeSpectators);
        y.set("settings.sounds", sounds);
        y.set("settings.action-bar", actionBar);
        y.set("settings.titles", titles);
        y.set("settings.chat-events", chatEvents);
        y.set("settings.follow-distance", followDistance);
        y.set("settings.noclip", noclip);
        y.set("settings.language", language);
        return y;
    }

    public void load(ConfigurationSection y) {
        if (y == null) return;
        preset = y.getString("preset", preset);
        customized = y.getBoolean("customized", customized);
        ConfigurationSection cats = y.getConfigurationSection("categories");
        if (cats != null) {
            categories.clear();
            for (String k : cats.getKeys(false)) categories.put(k, cats.getBoolean(k));
        }
        ConfigurationSection ev = y.getConfigurationSection("events");
        if (ev != null) {
            events.clear();
            for (String k : ev.getKeys(false)) events.put(k.replace('~', '.'), ev.getBoolean(k));
        }
        minImportance = Importance.parse(y.getString("min-importance"), minImportance);
        try {
            playerMode = PlayerFilterMode.valueOf(y.getString("players.mode", playerMode.name()));
        } catch (IllegalArgumentException ignored) {
        }
        readUuids(y.getStringList("players.whitelist"), whitelist);
        readUuids(y.getStringList("players.blacklist"), blacklist);
        readUuids(y.getStringList("players.favorites"), favorites);
        ConfigurationSection names = y.getConfigurationSection("players.names");
        if (names != null) {
            for (String k : names.getKeys(false)) {
                try {
                    knownNames.put(UUID.fromString(k), names.getString(k));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        hideOwn = y.getBoolean("players.hide-own", hideOwn);
        if (y.isList("teams")) {
            teams.clear();
            teams.addAll(y.getStringList("teams"));
        }
        overworld = y.getBoolean("worlds.overworld", overworld);
        nether = y.getBoolean("worlds.nether", nether);
        end = y.getBoolean("worlds.end", end);
        customWorlds = y.getBoolean("worlds.custom", customWorlds);
        currentWorldOnly = y.getBoolean("worlds.current-only", currentWorldOnly);
        maxDistance = y.getInt("distance.max", maxDistance);
        minDistance = y.getInt("distance.min", minDistance);
        sameChunk = y.getBoolean("distance.same-chunk", sameChunk);
        sameWorld = y.getBoolean("distance.same-world", sameWorld);
        sameZone = y.getBoolean("distance.same-zone", sameZone);
        minDamage = y.getDouble("damage.min", minDamage);
        maxDamage = y.getDouble("damage.max", maxDamage);
        if (y.isList("damage.types")) {
            damageTypes.clear();
            damageTypes.addAll(y.getStringList("damage.types"));
        }
        pvpOnly = y.getBoolean("damage.pvp-only", pvpOnly);
        pveOnly = y.getBoolean("damage.pve-only", pveOnly);
        criticalOnly = y.getBoolean("damage.critical-only", criticalOnly);
        rateLimit = y.getInt("time.rate-limit", rateLimit);
        showTime = y.getBoolean("time.show-time", showTime);
        flySpeed = y.getInt("settings.fly-speed", flySpeed);
        seeSpectators = y.getBoolean("settings.see-spectators", seeSpectators);
        sounds = y.getBoolean("settings.sounds", sounds);
        actionBar = y.getBoolean("settings.action-bar", actionBar);
        titles = y.getBoolean("settings.titles", titles);
        chatEvents = y.getBoolean("settings.chat-events", chatEvents);
        followDistance = y.getInt("settings.follow-distance", followDistance);
        noclip = y.getBoolean("settings.noclip", noclip);
        language = y.getString("settings.language", language);
    }

    public String serialize() {
        return toYaml().saveToString();
    }

    public static Preferences deserialize(String data, Preferences defaults) {
        Preferences p = defaults.copy();
        p.categories.clear();
        p.events.clear();
        if (data == null || data.isEmpty()) return p;
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(data);
            p.load(y);
        } catch (Exception ignored) {
        }
        return p;
    }

    private static List<String> uuids(Set<UUID> set) {
        List<String> res = new ArrayList<>();
        for (UUID u : set) res.add(u.toString());
        return res;
    }

    private static void readUuids(List<String> list, Set<UUID> into) {
        if (list == null || list.isEmpty()) return;
        into.clear();
        for (String s : list) {
            try {
                into.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}
