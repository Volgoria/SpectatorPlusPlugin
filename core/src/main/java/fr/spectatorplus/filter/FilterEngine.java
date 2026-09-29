package fr.spectatorplus.filter;

import fr.spectatorplus.api.event.Importance;
import fr.spectatorplus.api.event.SpectatorEventType;
import fr.spectatorplus.core.CoreContext;
import fr.spectatorplus.core.config.ConfigSection;
import fr.spectatorplus.core.platform.Dimension;
import fr.spectatorplus.core.platform.Position;
import fr.spectatorplus.event.Zone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Partie commune des filtres : presets, réglages admin, préférences des joueurs (chargement /
 * sauvegarde) et règles de filtrage sur des données neutres.
 * Chaque plateforme l'étend pour y brancher ses joueurs, permissions et évènements.
 */
public class FilterEngine {

    public enum State {ENABLED, DISABLED, PARTIAL, LOCKED}

    protected final CoreContext core;
    private final Map<UUID, Preferences> prefs = new ConcurrentHashMap<>();
    private final Map<String, Preset> presets = new LinkedHashMap<>();
    private final Map<UUID, Set<String>> locks = new ConcurrentHashMap<>();
    private final Map<String, Importance> importanceOverrides = new ConcurrentHashMap<>();
    private Preferences defaults = new Preferences();
    private List<String> disabledEvents = Collections.emptyList();
    private List<String> forcedEvents = Collections.emptyList();
    private Set<String> restrictedKeys = Collections.emptySet();
    private boolean allowPersonal = true;
    private String defaultPreset = "standard";

    public FilterEngine(CoreContext core) {
        this.core = core;
    }

    public void load() {
        ConfigSection cfg = core.config();
        presets.clear();
        ConfigSection ps = cfg.getConfigurationSection("presets");
        if (ps != null) {
            for (String id : ps.getKeys(false)) {
                ConfigSection s = ps.getConfigurationSection(id);
                if (s != null) presets.put(id.toLowerCase(Locale.ROOT), new Preset(id, s));
            }
        }
        disabledEvents = lower(cfg.getStringList("filters.admin.disabled-events"));
        forcedEvents = lower(cfg.getStringList("filters.admin.forced-events"));
        restrictedKeys = new HashSet<>(lower(cfg.getStringList("filters.admin.restricted-filters")));
        allowPersonal = cfg.getBoolean("filters.admin.allow-personal-filters", true);
        defaultPreset = cfg.getString("filters.admin.default-preset", "standard").toLowerCase(Locale.ROOT);

        Preferences d = new Preferences();
        d.load(cfg.getConfigurationSection("filters.defaults"));
        d.preset = presets.containsKey(defaultPreset) ? defaultPreset : "all";
        Preset p = presets.get(d.preset);
        if (p != null) d.minImportance = p.getMinImportance();
        defaults = d;
    }

    private static List<String> lower(List<String> in) {
        List<String> res = new ArrayList<>();
        for (String s : in) res.add(s.toLowerCase(Locale.ROOT));
        return res;
    }

    // ------------------------------------------------------------------ préférences

    public Preferences get(UUID id) {
        Preferences pr = prefs.get(id);
        if (pr == null) {
            pr = defaults.copy();
            prefs.put(id, pr);
        }
        return pr;
    }

    public Preferences defaults() {
        return defaults;
    }

    /** Remplace les préférences d'un joueur (réinitialisation). */
    protected void put(UUID id, Preferences p) {
        prefs.put(id, p);
    }

    public void loadPlayer(final UUID id) {
        core.host().runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    String data = core.storage().load(id);
                    if (data != null) {
                        prefs.put(id, Preferences.deserialize(data, defaults));
                        core.host().runSync(new Runnable() {
                            @Override
                            public void run() {
                                onPreferencesLoaded(id);
                            }
                        });
                    }
                } catch (Exception e) {
                    core.host().logger().log(Level.WARNING, "Chargement des préférences impossible pour " + id, e);
                }
            }
        });
    }

    /** Appelé sur le thread principal quand les préférences sauvegardées d'un joueur sont chargées. */
    protected void onPreferencesLoaded(UUID id) {
    }

    public void savePlayer(final UUID id, boolean async) {
        final Preferences pr = prefs.get(id);
        if (pr == null) return;
        final String data = pr.serialize();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    core.storage().save(id, data);
                } catch (Exception e) {
                    core.host().logger().log(Level.WARNING, "Sauvegarde des préférences impossible pour " + id, e);
                }
            }
        };
        if (async) core.host().runAsync(r);
        else r.run();
    }

    public void unload(UUID id) {
        savePlayer(id, true);
        prefs.remove(id);
        locks.remove(id);
    }

    public void saveAll() {
        for (UUID id : new ArrayList<>(prefs.keySet())) savePlayer(id, false);
    }

    /** Préférences réinitialisées en gardant les paramètres personnels (vitesse de vol, sons...). */
    public Preferences freshKeepingSettings(Preferences old) {
        Preferences fresh = defaults.copy();
        fresh.flySpeed = old.flySpeed;
        fresh.seeSpectators = old.seeSpectators;
        fresh.sounds = old.sounds;
        fresh.actionBar = old.actionBar;
        fresh.titles = old.titles;
        fresh.chatEvents = old.chatEvents;
        fresh.followDistance = old.followDistance;
        fresh.noclip = old.noclip;
        fresh.language = old.language;
        return fresh;
    }

    // ------------------------------------------------------------------ admin

    private static boolean matches(List<String> patterns, String id) {
        for (String p : patterns) {
            if (p.equals("*") || p.equals(id)) return true;
            if (p.endsWith("*") && id.startsWith(p.substring(0, p.length() - 1))) return true;
        }
        return false;
    }

    public boolean isGloballyDisabled(String typeId) {
        return matches(disabledEvents, typeId);
    }

    public boolean isForced(String typeId) {
        return matches(forcedEvents, typeId);
    }

    public boolean isPersonalAllowed() {
        return allowPersonal;
    }

    /** Cette clé de filtre demande-t-elle une permission (filters.admin.restricted-filters) ? */
    public boolean isRestricted(String key) {
        return restrictedKeys.contains(key);
    }

    public Importance getImportanceOverride(String typeId) {
        return importanceOverrides.get(typeId);
    }

    public void setEventImportance(String eventTypeId, Importance importance) {
        if (importance == null) importanceOverrides.remove(eventTypeId);
        else importanceOverrides.put(eventTypeId.toLowerCase(Locale.ROOT), importance);
    }

    public Map<String, Preset> presets() {
        return Collections.unmodifiableMap(presets);
    }

    public Preset preset(String id) {
        return id == null ? null : presets.get(id.toLowerCase(Locale.ROOT));
    }

    // ------------------------------------------------------------------ verrous

    public void lock(UUID player, String filterKey) {
        Set<String> l = locks.get(player);
        if (l == null) {
            l = ConcurrentHashMap.newKeySet();
            locks.put(player, l);
        }
        l.add(filterKey.toLowerCase(Locale.ROOT));
    }

    public void unlock(UUID player, String filterKey) {
        Set<String> l = locks.get(player);
        if (l != null) l.remove(filterKey.toLowerCase(Locale.ROOT));
    }

    public boolean locked(UUID player, String filterKey) {
        Set<String> l = locks.get(player);
        return l != null && (l.contains(filterKey.toLowerCase(Locale.ROOT)) || l.contains("*"));
    }

    // ------------------------------------------------------------------ règles

    public boolean categoryEnabled(Preferences p, String category) {
        Boolean b = p.categories.get(category);
        return b == null || b;
    }

    public boolean eventEnabled(Preferences p, SpectatorEventType type) {
        Boolean b = p.events.get(type.getId());
        if (b != null) return b;
        Preset preset = presets.get(p.preset);
        return preset == null || preset.includes(type.getId(), type.getCategory());
    }

    /** État d'une catégorie pour l'affichage (vert / rouge / jaune), hors verrouillage. */
    public State categoryState(Preferences p, String category) {
        if (!categoryEnabled(p, category)) return State.DISABLED;
        int on = 0, total = 0;
        for (SpectatorEventType t : core.events().getTypes(category)) {
            if (isGloballyDisabled(t.getId()) || !core.events().isEnabled(t.getId())) continue;
            total++;
            if (eventEnabled(p, t)) on++;
        }
        if (total == 0 || on == 0) return State.DISABLED;
        return on == total ? State.ENABLED : State.PARTIAL;
    }

    /** Active une catégorie ; réactiver une catégorie « vide » réactive ses évènements. */
    public void enableCategory(Preferences p, String categoryId, boolean enabled) {
        p.categories.put(categoryId, enabled);
        if (enabled) {
            boolean any = false;
            for (SpectatorEventType t : core.events().getTypes(categoryId)) if (eventEnabled(p, t)) any = true;
            if (!any) for (SpectatorEventType t : core.events().getTypes(categoryId)) p.events.put(t.getId(), true);
        }
    }

    /** Applique un preset aux préférences. */
    public void applyPreset(Preferences p, Preset ps) {
        p.preset = ps.getId();
        p.events.clear();
        p.categories.clear();
        p.minImportance = ps.getMinImportance();
    }

    /**
     * Filtre de monde.
     *
     * @param inViewerWorld l'évènement a lieu dans le monde du spectateur
     * @param mainWorld     le monde est l'un des trois mondes principaux du serveur
     */
    public boolean checkWorld(Preferences p, boolean inViewerWorld, boolean mainWorld, Dimension dimension) {
        if (p.currentWorldOnly && !inViewerWorld) return false;
        if (mainWorld) {
            switch (dimension) {
                case NETHER:
                    return p.nether;
                case END:
                    return p.end;
                default:
                    return p.overworld;
            }
        }
        if (!p.customWorlds) return false;
        // monde personnalisé : on respecte aussi le filtre de sa dimension
        switch (dimension) {
            case NETHER:
                return p.nether;
            case END:
                return p.end;
            default:
                return true;
        }
    }

    /** Filtre de distance entre le spectateur et le lieu de l'évènement. */
    public boolean checkDistance(Preferences p, Position viewer, Position loc) {
        boolean sameWorld = viewer.sameWorld(loc);
        if (p.sameWorld && !sameWorld) return false;
        if (p.sameChunk && (!sameWorld || (viewer.getBlockX() >> 4) != (loc.getBlockX() >> 4)
                || (viewer.getBlockZ() >> 4) != (loc.getBlockZ() >> 4))) {
            return false;
        }
        if (p.maxDistance > 0 && (!sameWorld || viewer.distanceSquared(loc) > (double) p.maxDistance * p.maxDistance)) return false;
        if (p.minDistance > 0 && sameWorld && viewer.distanceSquared(loc) < (double) p.minDistance * p.minDistance) return false;
        if (p.sameZone) {
            Zone a = core.zones().zoneAt(viewer), b = core.zones().zoneAt(loc);
            if (a == null || b == null || !a.getId().equals(b.getId())) return false;
        }
        return true;
    }

    /** Filtre de dégâts. */
    public boolean checkDamage(Preferences p, double damage, String damageType, boolean pvp, boolean critical) {
        if (p.minDamage > 0 && damage < p.minDamage) return false;
        if (p.maxDamage > 0 && damage > p.maxDamage) return false;
        if (!p.damageTypes.isEmpty() && (damageType == null || !p.damageTypes.contains(damageType))) {
            return false;
        }
        if (p.pvpOnly && !pvp) return false;
        if (p.pveOnly && pvp) return false;
        return !p.criticalOnly || critical;
    }

    /** Filtre de joueurs (hors mode FOLLOWED et équipes, qui dépendent de la plateforme). */
    public boolean checkPlayerLists(Preferences p, UUID viewer, List<UUID> involved) {
        if (involved.isEmpty()) return true;
        if (p.hideOwn && involved.get(0).equals(viewer)) return false;
        switch (p.playerMode) {
            case WHITELIST:
                return containsAny(p.whitelist, involved);
            case BLACKLIST:
                return !containsAny(p.blacklist, involved);
            case FAVORITES:
                return containsAny(p.favorites, involved);
            default:
                return true;
        }
    }

    private static boolean containsAny(Set<UUID> set, List<UUID> ids) {
        for (UUID u : ids) if (set.contains(u)) return true;
        return false;
    }
}
