package fr.spectatorplus.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Un spectateur change de joueur suivi. Annulable.
 */
public class SpectatorFollowTargetChangeEvent extends SpectatorPlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player from;
    private final Player to;

    public SpectatorFollowTargetChangeEvent(Player player, Player from, Player to) {
        super(player);
        this.from = from;
        this.to = to;
    }

    public Player getFrom() {
        return from;
    }

    public Player getTo() {
        return to;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
