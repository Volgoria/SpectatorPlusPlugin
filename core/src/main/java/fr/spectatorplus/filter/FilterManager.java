package fr.spectatorplus.filter;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.api.event.Importance;
import fr.spectatorplus.api.event.SpectatorEventType;
import fr.spectatorplus.api.filter.PlayerFilterMode;
import fr.spectatorplus.core.event.GameEvent;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.PlatformWorld;
import fr.spectatorplus.core.platform.Position;
import fr.spectatorplus.spectator.SpectatorSession;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Filtres des évènements appliqués aux joueurs : permissions, joueurs, mondes, conditions externes.
 * Les règles et les préférences sont dans {@link FilterEngine}.
 */
public final class FilterManager extends FilterEngine {

    /** Condition de filtrage ajoutée par un plugin / mod externe. */
    public interface Condition {
        boolean test(PlatformPlayer viewer, GameEvent event);
    }

    private final SpectatorCore plugin;
    private final Map<String, Condition> conditions = new LinkedHashMap<>();

    public FilterManager(SpectatorCore plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public Preferences get(PlatformPlayer p) {
        return get(p.getUniqueId());
    }

    protected void onPreferencesLoaded(UUID id) {
        // la langue du joueur est maintenant connue : on retraduit sa barre d'inventaire
        PlatformPlayer p = plugin.platform().getPlayer(id);
        if (p != null) plugin.spectators().refreshHotbar(p);
    }

    // ------------------------------------------------------------------ accès

    /** Le joueur a-t-il accès à cette clé de filtre (permission) ? */
    public boolean hasAccess(PlatformPlayer p, String key) {
        if (!isRestricted(key)) return true;
        return p.hasPermission("spectatorplus.filter." + key) || p.hasPermission("spectatorplus.filter.*");
    }

    /** Le joueur peut-il modifier ce filtre depuis le menu ? */
    public boolean canModify(PlatformPlayer p, String key) {
        if (!isPersonalAllowed()) return false;
        if (locked(p.getUniqueId(), key)) return false;
        return hasAccess(p, key);
    }

    // ------------------------------------------------------------------ logique de filtre

    /** Préférences effectives pour une clé : celles du joueur, ou les valeurs par défaut s'il n'y a pas accès. */
    private Preferences effective(PlatformPlayer viewer, Preferences own, String key) {
        if (!isPersonalAllowed() || !hasAccess(viewer, key)) return defaults();
        return own;
    }

    /** État d'une catégorie pour l'affichage (vert / rouge / jaune / gris). */
    public State categoryState(PlatformPlayer viewer, String category) {
        if (!canModify(viewer, category)) return State.LOCKED;
        return categoryState(get(viewer), category);
    }

    public State eventState(PlatformPlayer viewer, SpectatorEventType type) {
        if (isGloballyDisabled(type.getId()) || !plugin.events().isEnabled(type.getId())) return State.LOCKED;
        if (isForced(type.getId())) return State.LOCKED;
        if (!canModify(viewer, type.getCategory())) return State.LOCKED;
        return eventEnabled(get(viewer), type) ? State.ENABLED : State.DISABLED;
    }

    public boolean canSee(PlatformPlayer viewer, GameEvent e) {
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
        Position loc = e.getLocation();
        PlatformWorld w = loc == null || loc.getWorld() == null ? null : plugin.platform().getWorld(loc.getWorld());
        if (w != null) {
            if (!checkWorld(effective(viewer, own, "world"), w.getName().equals(viewer.getWorld().getName()),
                    w.isMainWorld(), w.getDimension())) {
                return false;
            }
            if (!checkDistance(effective(viewer, own, "distance"), viewer.getLocation(), loc)) return false;
        }
        if (e.hasDamage() && !checkDamage(effective(viewer, own, "damage"), e.getDamage(), e.getDamageType(),
                e.isPvp(), e.isCritical())) {
            return false;
        }

        for (Condition c : conditions.values()) {
            try {
                if (!c.test(viewer, e)) return false;
            } catch (Throwable t) {
                plugin.host().logger().log(Level.WARNING, "Erreur dans une condition de filtre externe", t);
            }
        }
        return true;
    }

    private boolean checkPlayers(PlatformPlayer viewer, Preferences p, GameEvent e) {
        List<UUID> involved = e.getPlayers();
        if (involved.isEmpty()) return true;
        if (!checkPlayerLists(p, viewer.getUniqueId(), involved)) return false;
        if (p.playerMode == PlayerFilterMode.FOLLOWED) {
            SpectatorSession s = plugin.spectators().getSpectator(viewer);
            PlatformPlayer target = s == null ? null : s.getFollowTarget();
            if (target != null && !involved.contains(target.getUniqueId())) return false;
        }
        if (!p.teams.isEmpty()) {
            PlatformPlayer sp = plugin.platform().getPlayer(involved.get(0));
            String team = sp == null ? null : plugin.game().getTeam(sp);
            if (team == null || !p.teams.contains(team)) return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ modifications

    public void changed(PlatformPlayer p, String filter, Object oldValue, Object newValue) {
        if (!filter.startsWith("settings")) get(p).customized = true;
        plugin.platform().api().filterChange(p, filter, String.valueOf(oldValue), String.valueOf(newValue));
    }

    public boolean isCategoryEnabled(PlatformPlayer player, String categoryId) {
        return categoryEnabled(get(player), categoryId);
    }

    public void setCategoryEnabled(PlatformPlayer player, String categoryId, boolean enabled) {
        Preferences p = get(player);
        boolean old = categoryEnabled(p, categoryId);
        enableCategory(p, categoryId, enabled);
        changed(player, "category." + categoryId, old, enabled);
    }

    public boolean isEventEnabled(PlatformPlayer player, String eventTypeId) {
        SpectatorEventType t = plugin.events().getType(eventTypeId);
        return t != null && eventEnabled(get(player), t);
    }

    public void setEventEnabled(PlatformPlayer player, String eventTypeId, boolean enabled) {
        SpectatorEventType t = plugin.events().getType(eventTypeId);
        if (t == null) return;
        Preferences p = get(player);
        boolean old = eventEnabled(p, t);
        p.events.put(t.getId(), enabled);
        if (enabled) p.categories.put(t.getCategory(), true);
        changed(player, "event." + t.getId(), old, enabled);
    }

    public String getPreset(PlatformPlayer player) {
        return get(player).preset;
    }

    public boolean setPreset(PlatformPlayer player, String preset) {
        Preset ps = preset(preset);
        if (ps == null) return false;
        Preferences p = get(player);
        String old = p.preset;
        applyPreset(p, ps);
        plugin.platform().api().filterChange(player, "preset", old, ps.getId());
        p.customized = false;
        return true;
    }

    public Collection<String> getPresets() {
        return Collections.unmodifiableSet(presets().keySet());
    }

    public Importance getMinimumImportance(PlatformPlayer player) {
        return get(player).minImportance;
    }

    public void setMinimumImportance(PlatformPlayer player, Importance importance) {
        Preferences p = get(player);
        Importance old = p.minImportance;
        p.minImportance = importance;
        changed(player, "importance", old, importance);
    }

    public PlayerFilterMode getPlayerFilterMode(PlatformPlayer player) {
        return get(player).playerMode;
    }

    public void setPlayerFilterMode(PlatformPlayer player, PlayerFilterMode mode) {
        Preferences p = get(player);
        PlayerFilterMode old = p.playerMode;
        p.playerMode = mode;
        changed(player, "player.mode", old, mode);
    }

    public void lockFilter(PlatformPlayer player, String filterKey) {
        lock(player.getUniqueId(), filterKey);
    }

    public void unlockFilter(PlatformPlayer player, String filterKey) {
        unlock(player.getUniqueId(), filterKey);
    }

    public boolean isLocked(PlatformPlayer player, String filterKey) {
        return locked(player.getUniqueId(), filterKey);
    }

    public void registerCondition(String id, Condition condition) {
        conditions.put(id, condition);
    }

    public void unregisterCondition(String id) {
        conditions.remove(id);
    }

    public void resetFilters(PlatformPlayer player) {
        Preferences old = get(player);
        Preferences fresh = freshKeepingSettings(old);
        put(player.getUniqueId(), fresh);
        plugin.platform().api().filterChange(player, "reset", old.preset, fresh.preset);
    }

    /** Réinitialisation de tous les joueurs (en ligne + stockage). */
    public void resetAll() {
        for (PlatformPlayer p : plugin.platform().getOnlinePlayers()) resetFilters(p);
    }
}
