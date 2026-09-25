package fr.spectatorplus.filter;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.api.Spectator;
import fr.spectatorplus.api.event.EventCategory;
import fr.spectatorplus.api.event.Importance;
import fr.spectatorplus.api.event.SpectatorEventType;
import fr.spectatorplus.api.event.SpectatorGameEvent;
import fr.spectatorplus.api.events.SpectatorFilterChangeEvent;
import fr.spectatorplus.api.filter.FilterCondition;
import fr.spectatorplus.api.filter.FilterService;
import fr.spectatorplus.api.filter.PlayerFilterMode;
import fr.spectatorplus.event.Zone;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
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
 * Filtres des évènements + préférences des joueurs.
 */
public final class FilterManager implements FilterService {

    public enum State {ENABLED, DISABLED, PARTIAL, LOCKED}

    private final SpectatorPlus plugin;
    private final Map<UUID, Preferences> prefs = new ConcurrentHashMap<>();
    private final Map<String, Preset> presets = new LinkedHashMap<>();
    private final Map<String, FilterCondition> conditions = new LinkedHashMap<>();
    private final Map<UUID, Set<String>> locks = new ConcurrentHashMap<>();
    private final Map<String, Importance> importanceOverrides = new ConcurrentHashMap<>();
    private Preferences defaults = new Preferences();
    private List<String> disabledEvents = Collections.emptyList();
    private List<String> forcedEvents = Collections.emptyList();
    private Set<String> restrictedKeys = Collections.emptySet();
    private boolean allowPersonal = true;
    private String defaultPreset = "standard";

    public FilterManager(SpectatorPlus plugin) {
        this.plugin = plugin;
    }

    public void load() {
        ConfigurationSection cfg = plugin.getConfig();
        presets.clear();
        ConfigurationSection ps = plugin.files().config().getConfigurationSection("presets");
        if (ps != null) {
            for (String id : ps.getKeys(false)) {
                ConfigurationSection s = ps.getConfigurationSection(id);
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

    public Preferences get(Player p) {
        return get(p.getUniqueId());
    }

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

    public void loadPlayer(final UUID id) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    String data = plugin.storage().load(id);
                    if (data != null) {
                        prefs.put(id, Preferences.deserialize(data, defaults));
                        // la langue du joueur est maintenant connue : on retraduit sa barre d'inventaire
                        Bukkit.getScheduler().runTask(plugin, new Runnable() {
                            @Override
                            public void run() {
                                Player p = Bukkit.getPlayer(id);
                                if (p != null) plugin.spectators().refreshHotbar(p);
                            }
                        });
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Chargement des préférences impossible pour " + id, e);
                }
            }
        });
    }

    public void savePlayer(final UUID id, boolean async) {
        final Preferences pr = prefs.get(id);
        if (pr == null) return;
        final String data = pr.serialize();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    plugin.storage().save(id, data);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Sauvegarde des préférences impossible pour " + id, e);
                }
            }
        };
        if (async) Bukkit.getScheduler().runTaskAsynchronously(plugin, r);
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

    /** Le joueur a-t-il accès à cette clé de filtre (permission) ? */
    public boolean hasAccess(Player p, String key) {
        if (!restrictedKeys.contains(key)) return true;
        return p.hasPermission("spectatorplus.filter." + key) || p.hasPermission("spectatorplus.filter.*");
    }

    /** Le joueur peut-il modifier ce filtre depuis le menu ? */
    public boolean canModify(Player p, String key) {
        if (!allowPersonal) return false;
        Set<String> l = locks.get(p.getUniqueId());
        if (l != null && (l.contains(key) || l.contains("*"))) return false;
        return hasAccess(p, key);
    }

    public Importance getImportanceOverride(String typeId) {
        return importanceOverrides.get(typeId);
    }

    public Map<String, Preset> presets() {
        return Collections.unmodifiableMap(presets);
    }

    public Preset preset(String id) {
        return id == null ? null : presets.get(id.toLowerCase(Locale.ROOT));
    }

    // ------------------------------------------------------------------ logique de filtre

    /** Préférences effectives pour une clé : celles du joueur, ou les valeurs par défaut s'il n'y a pas accès. */
    private Preferences effective(Player viewer, Preferences own, String key) {
        if (!allowPersonal || !hasAccess(viewer, key)) return defaults;
        return own;
    }

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

    /** État d'une catégorie pour l'affichage (vert / rouge / jaune / gris). */
    public State categoryState(Player viewer, String category) {
        if (!canModify(viewer, category)) return State.LOCKED;
        Preferences p = get(viewer);
        if (!categoryEnabled(p, category)) return State.DISABLED;
        int on = 0, total = 0;
        for (SpectatorEventType t : plugin.events().getTypes(category)) {
            if (isGloballyDisabled(t.getId()) || !plugin.events().isEnabled(t.getId())) continue;
            total++;
            if (eventEnabled(p, t)) on++;
        }
        if (total == 0 || on == 0) return State.DISABLED;
        return on == total ? State.ENABLED : State.PARTIAL;
    }

    public State eventState(Player viewer, SpectatorEventType type) {
        if (isGloballyDisabled(type.getId()) || !plugin.events().isEnabled(type.getId())) return State.LOCKED;
        if (isForced(type.getId())) return State.LOCKED;
        if (!canModify(viewer, type.getCategory())) return State.LOCKED;
        return eventEnabled(get(viewer), type) ? State.ENABLED : State.DISABLED;
    }

    @Override
    public boolean canSee(Player viewer, SpectatorGameEvent e) {
        if (viewer == null) return true;
        if (isForced(e.getTypeId())) return true;
        Preferences own = get(viewer);
        String cat = e.getCategory();

        Preferences pc = effective(viewer, own, cat);
        if (!categoryEnabled(pc, cat) || !eventEnabled(pc, e.getType())) return false;

        Preferences pi = effective(viewer, own, "importance");
        Importance imp = e.getImportance() == null ? Importance.NORMAL : e.getImportance();
        if (!imp.isAtLeast(pi.minImportance)) return false;

        if (!checkPlayers(viewer, effective(viewer, own, "player"), e)) return false;
        Location loc = e.getLocation();
        if (loc != null && loc.getWorld() != null) {
            if (!checkWorld(viewer, effective(viewer, own, "world"), loc)) return false;
            if (!checkDistance(viewer, effective(viewer, own, "distance"), loc)) return false;
        }
        if (e.hasDamage() && !checkDamage(effective(viewer, own, "damage"), e)) return false;

        for (FilterCondition c : conditions.values()) {
            try {
                if (!c.test(viewer, e)) return false;
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "Erreur dans une condition de filtre externe", t);
            }
        }
        return true;
    }

    private boolean checkPlayers(Player viewer, Preferences p, SpectatorGameEvent e) {
        List<UUID> involved = e.getPlayers();
        if (involved.isEmpty()) return true;
        UUID subject = involved.get(0);
        if (p.hideOwn && subject.equals(viewer.getUniqueId())) return false;
        switch (p.playerMode) {
            case WHITELIST:
                if (!containsAny(p.whitelist, involved)) return false;
                break;
            case BLACKLIST:
                if (containsAny(p.blacklist, involved)) return false;
                break;
            case FAVORITES:
                if (!containsAny(p.favorites, involved)) return false;
                break;
            case FOLLOWED:
                Spectator s = plugin.spectators().getSpectator(viewer);
                Player target = s == null ? null : s.getFollowTarget();
                if (target != null && !involved.contains(target.getUniqueId())) return false;
                break;
            default:
                break;
        }
        if (!p.teams.isEmpty()) {
            Player sp = Bukkit.getPlayer(subject);
            String team = sp == null ? null : plugin.game().getTeam(sp);
            if (team == null || !p.teams.contains(team)) return false;
        }
        return true;
    }

    private static boolean containsAny(Set<UUID> set, List<UUID> ids) {
        for (UUID u : ids) if (set.contains(u)) return true;
        return false;
    }

    private boolean checkWorld(Player viewer, Preferences p, Location loc) {
        World w = loc.getWorld();
        if (p.currentWorldOnly && !w.equals(viewer.getWorld())) return false;
        if (isMainWorld(w)) {
            switch (w.getEnvironment()) {
                case NETHER:
                    return p.nether;
                case THE_END:
                    return p.end;
                default:
                    return p.overworld;
            }
        }
        if (!p.customWorlds) return false;
        // monde personnalisé : on respecte aussi le filtre de sa dimension
        switch (w.getEnvironment()) {
            case NETHER:
                return p.nether;
            case THE_END:
                return p.end;
            default:
                return true;
        }
    }

    private boolean isMainWorld(World w) {
        List<World> worlds = Bukkit.getWorlds();
        if (worlds.isEmpty()) return true;
        String main = worlds.get(0).getName();
        String n = w.getName();
        return n.equals(main) || n.equals(main + "_nether") || n.equals(main + "_the_end");
    }

    private boolean checkDistance(Player viewer, Preferences p, Location loc) {
        Location v = viewer.getLocation();
        boolean sameWorld = v.getWorld().equals(loc.getWorld());
        if (p.sameWorld && !sameWorld) return false;
        if (p.sameChunk && (!sameWorld || (v.getBlockX() >> 4) != (loc.getBlockX() >> 4)
                || (v.getBlockZ() >> 4) != (loc.getBlockZ() >> 4))) {
            return false;
        }
        if (p.maxDistance > 0 && (!sameWorld || v.distanceSquared(loc) > (double) p.maxDistance * p.maxDistance)) return false;
        if (p.minDistance > 0 && sameWorld && v.distanceSquared(loc) < (double) p.minDistance * p.minDistance) return false;
        if (p.sameZone) {
            Zone a = plugin.zones().zoneAt(v), b = plugin.zones().zoneAt(loc);
            if (a == null || b == null || !a.getId().equals(b.getId())) return false;
        }
        return true;
    }

    private boolean checkDamage(Preferences p, SpectatorGameEvent e) {
        double dmg = e.getDamage();
        if (p.minDamage > 0 && dmg < p.minDamage) return false;
        if (p.maxDamage > 0 && dmg > p.maxDamage) return false;
        if (!p.damageTypes.isEmpty() && (e.getDamageType() == null || !p.damageTypes.contains(e.getDamageType()))) {
            return false;
        }
        if (p.pvpOnly && !e.isPvp()) return false;
        if (p.pveOnly && e.isPvp()) return false;
        return !p.criticalOnly || e.isCritical();
    }

    // ------------------------------------------------------------------ modifications

    public void changed(Player p, String filter, Object oldValue, Object newValue) {
        if (!filter.startsWith("settings")) get(p).customized = true;
        Bukkit.getPluginManager().callEvent(new SpectatorFilterChangeEvent(p, filter,
                String.valueOf(oldValue), String.valueOf(newValue)));
    }

    @Override
    public boolean isCategoryEnabled(Player player, String categoryId) {
        return categoryEnabled(get(player), categoryId);
    }

    @Override
    public void setCategoryEnabled(Player player, String categoryId, boolean enabled) {
        Preferences p = get(player);
        boolean old = categoryEnabled(p, categoryId);
        p.categories.put(categoryId, enabled);
        if (enabled) {
            // réactiver une catégorie « vide » réactive ses évènements
            boolean any = false;
            for (SpectatorEventType t : plugin.events().getTypes(categoryId)) if (eventEnabled(p, t)) any = true;
            if (!any) for (SpectatorEventType t : plugin.events().getTypes(categoryId)) p.events.put(t.getId(), true);
        }
        changed(player, "category." + categoryId, old, enabled);
    }

    @Override
    public boolean isEventEnabled(Player player, String eventTypeId) {
        SpectatorEventType t = plugin.events().getType(eventTypeId);
        return t != null && eventEnabled(get(player), t);
    }

    @Override
    public void setEventEnabled(Player player, String eventTypeId, boolean enabled) {
        SpectatorEventType t = plugin.events().getType(eventTypeId);
        if (t == null) return;
        Preferences p = get(player);
        boolean old = eventEnabled(p, t);
        p.events.put(t.getId(), enabled);
        if (enabled) p.categories.put(t.getCategory(), true);
        changed(player, "event." + t.getId(), old, enabled);
    }

    @Override
    public String getPreset(Player player) {
        return get(player).preset;
    }

    @Override
    public boolean setPreset(Player player, String preset) {
        Preset ps = preset(preset);
        if (ps == null) return false;
        Preferences p = get(player);
        String old = p.preset;
        p.preset = ps.getId();
        p.events.clear();
        p.categories.clear();
        p.minImportance = ps.getMinImportance();
        Bukkit.getPluginManager().callEvent(new SpectatorFilterChangeEvent(player, "preset", old, ps.getId()));
        p.customized = false;
        return true;
    }

    @Override
    public Collection<String> getPresets() {
        return Collections.unmodifiableSet(presets.keySet());
    }

    @Override
    public Importance getMinimumImportance(Player player) {
        return get(player).minImportance;
    }

    @Override
    public void setMinimumImportance(Player player, Importance importance) {
        Preferences p = get(player);
        Importance old = p.minImportance;
        p.minImportance = importance;
        changed(player, "importance", old, importance);
    }

    @Override
    public PlayerFilterMode getPlayerFilterMode(Player player) {
        return get(player).playerMode;
    }

    @Override
    public void setPlayerFilterMode(Player player, PlayerFilterMode mode) {
        Preferences p = get(player);
        PlayerFilterMode old = p.playerMode;
        p.playerMode = mode;
        changed(player, "player.mode", old, mode);
    }

    @Override
    public void lockFilter(Player player, String filterKey) {
        Set<String> l = locks.get(player.getUniqueId());
        if (l == null) {
            l = ConcurrentHashMap.newKeySet();
            locks.put(player.getUniqueId(), l);
        }
        l.add(filterKey.toLowerCase(Locale.ROOT));
    }

    @Override
    public void unlockFilter(Player player, String filterKey) {
        Set<String> l = locks.get(player.getUniqueId());
        if (l != null) l.remove(filterKey.toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean isLocked(Player player, String filterKey) {
        Set<String> l = locks.get(player.getUniqueId());
        return l != null && (l.contains(filterKey.toLowerCase(Locale.ROOT)) || l.contains("*"));
    }

    @Override
    public void setEventImportance(String eventTypeId, Importance importance) {
        if (importance == null) importanceOverrides.remove(eventTypeId);
        else importanceOverrides.put(eventTypeId.toLowerCase(Locale.ROOT), importance);
    }

    @Override
    public void registerCondition(String id, FilterCondition condition) {
        conditions.put(id, condition);
    }

    @Override
    public void unregisterCondition(String id) {
        conditions.remove(id);
    }

    @Override
    public void registerCategory(EventCategory category) {
        plugin.events().registerCategory(category);
    }

    @Override
    public void resetFilters(Player player) {
        Preferences old = get(player);
        Preferences fresh = defaults.copy();
        // on garde les paramètres personnels (vitesse de vol, sons...)
        fresh.flySpeed = old.flySpeed;
        fresh.seeSpectators = old.seeSpectators;
        fresh.sounds = old.sounds;
        fresh.actionBar = old.actionBar;
        fresh.titles = old.titles;
        fresh.chatEvents = old.chatEvents;
        fresh.followDistance = old.followDistance;
        fresh.noclip = old.noclip;
        fresh.language = old.language;
        prefs.put(player.getUniqueId(), fresh);
        Bukkit.getPluginManager().callEvent(new SpectatorFilterChangeEvent(player, "reset", old.preset, fresh.preset));
    }

    /** Réinitialisation de tous les joueurs (en ligne + stockage). */
    public void resetAll() {
        for (Player p : Bukkit.getOnlinePlayers()) resetFilters(p);
    }
}
