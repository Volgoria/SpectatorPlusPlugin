package fr.spectatorplus.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Les filtres d'un spectateur ont été modifiés. Non annulable (déclenché après la modification).
 * <p>
 * Exemples de clés : {@code category.pvp}, {@code event.pvp.first_kill}, {@code preset},
 * {@code importance}, {@code player.mode}, {@code reset}.
 */
public class SpectatorFilterChangeEvent extends SpectatorPlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final String filter;
    private final String oldValue;
    private final String newValue;

    public SpectatorFilterChangeEvent(Player player, String filter, String oldValue, String newValue) {
        super(player);
        this.filter = filter;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getFilter() {
        return filter;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
