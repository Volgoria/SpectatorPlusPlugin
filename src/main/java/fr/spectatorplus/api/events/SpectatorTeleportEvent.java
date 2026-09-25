package fr.spectatorplus.api.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Un spectateur utilise le système de téléportation de Spectator Plus. Annulable.
 */
public class SpectatorTeleportEvent extends SpectatorPlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player target;
    private Location destination;

    public SpectatorTeleportEvent(Player player, Player target, Location destination) {
        super(player);
        this.target = target;
        this.destination = destination;
    }

    /** Joueur vers lequel le spectateur se téléporte (peut être null pour une position). */
    public Player getTarget() {
        return target;
    }

    public Location getDestination() {
        return destination;
    }

    public void setDestination(Location destination) {
        this.destination = destination;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
