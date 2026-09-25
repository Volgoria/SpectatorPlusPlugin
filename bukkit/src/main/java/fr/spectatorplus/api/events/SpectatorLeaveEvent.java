package fr.spectatorplus.api.events;

import fr.spectatorplus.api.LeaveReason;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Un joueur quitte le système spectateur. Annulable sauf pour {@link LeaveReason#PLUGIN_DISABLE}.
 */
public class SpectatorLeaveEvent extends SpectatorPlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final LeaveReason reason;

    public SpectatorLeaveEvent(Player player, LeaveReason reason) {
        super(player);
        this.reason = reason;
    }

    public LeaveReason getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
