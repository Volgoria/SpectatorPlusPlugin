package fr.spectatorplus.api.events;

import fr.spectatorplus.api.event.SpectatorGameEvent;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Un évènement de partie (natif ou personnalisé) va être diffusé aux spectateurs.
 * Permet aux plugins externes de réagir aux actions détectées par Spectator Plus,
 * de modifier le message / l'importance, ou d'annuler la diffusion.
 */
public class SpectatorGameEventTriggerEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final SpectatorGameEvent gameEvent;
    private boolean cancelled;

    public SpectatorGameEventTriggerEvent(SpectatorGameEvent gameEvent, boolean async) {
        super(async);
        this.gameEvent = gameEvent;
    }

    public SpectatorGameEvent getGameEvent() {
        return gameEvent;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
