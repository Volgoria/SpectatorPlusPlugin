package fr.spectatorplus.bukkit;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.api.event.EventCategory;
import fr.spectatorplus.api.event.Importance;
import fr.spectatorplus.api.event.SpectatorGameEvent;
import fr.spectatorplus.api.filter.FilterCondition;
import fr.spectatorplus.api.filter.FilterService;
import fr.spectatorplus.api.filter.PlayerFilterMode;
import fr.spectatorplus.core.event.GameEvent;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.filter.FilterManager;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * API publique des filtres (Bukkit) → {@link FilterManager}.
 */
public final class BukkitFilterService implements FilterService {

    private final SpectatorCore core;
    private final BukkitPlatform platform;

    public BukkitFilterService(SpectatorCore core, BukkitPlatform platform) {
        this.core = core;
        this.platform = platform;
    }

    private FilterManager f() {
        return core.filters();
    }

    private PlatformPlayer w(Player p) {
        return platform.wrap(p);
    }

    @Override
    public boolean isCategoryEnabled(Player player, String categoryId) {
        return f().isCategoryEnabled(w(player), categoryId);
    }

    @Override
    public void setCategoryEnabled(Player player, String categoryId, boolean enabled) {
        f().setCategoryEnabled(w(player), categoryId, enabled);
    }

    @Override
    public boolean isEventEnabled(Player player, String eventTypeId) {
        return f().isEventEnabled(w(player), eventTypeId);
    }

    @Override
    public void setEventEnabled(Player player, String eventTypeId, boolean enabled) {
        f().setEventEnabled(w(player), eventTypeId, enabled);
    }

    @Override
    public String getPreset(Player player) {
        return f().getPreset(w(player));
    }

    @Override
    public boolean setPreset(Player player, String preset) {
        return f().setPreset(w(player), preset);
    }

    @Override
    public Collection<String> getPresets() {
        return f().getPresets();
    }

    @Override
    public Importance getMinimumImportance(Player player) {
        return f().getMinimumImportance(w(player));
    }

    @Override
    public void setMinimumImportance(Player player, Importance importance) {
        f().setMinimumImportance(w(player), importance);
    }

    @Override
    public PlayerFilterMode getPlayerFilterMode(Player player) {
        return f().getPlayerFilterMode(w(player));
    }

    @Override
    public void setPlayerFilterMode(Player player, PlayerFilterMode mode) {
        f().setPlayerFilterMode(w(player), mode);
    }

    @Override
    public void lockFilter(Player player, String filterKey) {
        f().lockFilter(w(player), filterKey);
    }

    @Override
    public void unlockFilter(Player player, String filterKey) {
        f().unlockFilter(w(player), filterKey);
    }

    @Override
    public boolean isLocked(Player player, String filterKey) {
        return f().isLocked(w(player), filterKey);
    }

    @Override
    public void setEventImportance(String eventTypeId, Importance importance) {
        f().setEventImportance(eventTypeId, importance);
    }

    @Override
    public void registerCondition(String id, final FilterCondition condition) {
        f().registerCondition(id, new FilterManager.Condition() {
            @Override
            public boolean test(PlatformPlayer viewer, GameEvent event) {
                return condition.test(BukkitPlayer.unwrap(viewer), SpectatorGameEvent.wrap(event));
            }
        });
    }

    @Override
    public void unregisterCondition(String id) {
        f().unregisterCondition(id);
    }

    @Override
    public void registerCategory(EventCategory category) {
        core.events().registerCategory(category);
    }

    @Override
    public boolean canSee(Player viewer, SpectatorGameEvent event) {
        return f().canSee(w(viewer), event.unwrap());
    }

    @Override
    public void resetFilters(Player player) {
        f().resetFilters(w(player));
    }
}
