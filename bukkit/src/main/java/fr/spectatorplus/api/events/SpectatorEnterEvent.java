package fr.spectatorplus.api.events;

import fr.spectatorplus.api.EnterReason;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Un joueur entre dans le système spectateur.
 * <p>
 * En mode SEMI_AUTO, annuler cet évènement empêche le passage automatique en spectateur.
 * En mode AUTO, lorsque {@link #isForced()} vaut true, l'annulation est ignorée.
 */
public class SpectatorEnterEvent extends SpectatorPlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final EnterReason reason;
    private final boolean forced;

    public SpectatorEnterEvent(Player player, EnterReason reason, boolean forced) {
        super(player);
        this.reason = reason;
        this.forced = forced;
    }

    public EnterReason getReason() {
        return reason;
    }

    /** true si l'annulation sera ignorée (mode AUTO). */
    public boolean isForced() {
        return forced;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
