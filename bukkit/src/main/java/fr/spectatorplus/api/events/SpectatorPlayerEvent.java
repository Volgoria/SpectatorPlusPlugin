package fr.spectatorplus.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

/**
 * Base des évènements Bukkit de Spectator Plus concernant un spectateur.
 */
public abstract class SpectatorPlayerEvent extends Event implements Cancellable {

    private final Player player;
    private boolean cancelled;

    protected SpectatorPlayerEvent(Player player) {
        this.player = player;
    }

    /** Le spectateur concerné. */
    public Player getPlayer() {
        return player;
    }

    /** Certains évènements ne sont pas annulables : l'appel est alors ignoré (voir la javadoc de chaque classe). */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
