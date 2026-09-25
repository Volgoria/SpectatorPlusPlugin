package fr.spectatorplus.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Un spectateur arrête de suivre un joueur. Non annulable.
 */
public class SpectatorFollowStopEvent extends SpectatorPlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player previousTarget;

    public SpectatorFollowStopEvent(Player player, Player previousTarget) {
        super(player);
        this.previousTarget = previousTarget;
    }

    /** Ancienne cible (peut être null si elle s'est déconnectée). */
    public Player getPreviousTarget() {
        return previousTarget;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
