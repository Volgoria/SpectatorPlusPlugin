package fr.spectatorplus.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Un spectateur ouvre l'inventaire (ou l'ender chest) d'un joueur en consultation. Annulable.
 */
public class SpectatorInventoryInspectEvent extends SpectatorPlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player target;
    private final boolean enderChest;

    public SpectatorInventoryInspectEvent(Player player, Player target, boolean enderChest) {
        super(player);
        this.target = target;
        this.enderChest = enderChest;
    }

    public Player getTarget() {
        return target;
    }

    public boolean isEnderChest() {
        return enderChest;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
