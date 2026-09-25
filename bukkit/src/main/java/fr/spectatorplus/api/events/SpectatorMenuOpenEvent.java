package fr.spectatorplus.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Un menu Spectator Plus est ouvert. Annulable.
 * Identifiants : main, teleport, players, follow, pov, inventories, player, inspect, enderchest,
 * history, filters, category, presets, player-filters, world-filters, distance-filters,
 * damage-filters, game-info, settings.
 */
public class SpectatorMenuOpenEvent extends SpectatorPlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final String menuId;

    public SpectatorMenuOpenEvent(Player player, String menuId) {
        super(player);
        this.menuId = menuId;
    }

    public String getMenuId() {
        return menuId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
