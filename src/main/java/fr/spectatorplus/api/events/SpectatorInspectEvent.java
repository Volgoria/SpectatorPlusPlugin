package fr.spectatorplus.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Un spectateur commence à inspecter un joueur (ouverture de sa fiche). Annulable.
 */
public class SpectatorInspectEvent extends SpectatorPlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player target;

    public SpectatorInspectEvent(Player player, Player target) {
        super(player);
        this.target = target;
    }

    public Player getTarget() {
        return target;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
