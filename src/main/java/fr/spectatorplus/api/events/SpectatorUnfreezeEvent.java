package fr.spectatorplus.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Un spectateur est libéré. Annulable.
 */
public class SpectatorUnfreezeEvent extends SpectatorPlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public SpectatorUnfreezeEvent(Player player) {
        super(player);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
