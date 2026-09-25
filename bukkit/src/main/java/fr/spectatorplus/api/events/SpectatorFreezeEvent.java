package fr.spectatorplus.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Un spectateur est immobilisé par Spectator Plus. Annulable.
 */
public class SpectatorFreezeEvent extends SpectatorPlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public SpectatorFreezeEvent(Player player) {
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
