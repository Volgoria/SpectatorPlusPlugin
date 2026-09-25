package fr.spectatorplus.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Un menu Spectator Plus est fermé. Non annulable.
 */
public class SpectatorMenuCloseEvent extends SpectatorPlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final String menuId;

    public SpectatorMenuCloseEvent(Player player, String menuId) {
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
