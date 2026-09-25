package fr.spectatorplus.api.placeholder;

import fr.spectatorplus.api.event.SpectatorGameEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Contexte de résolution d'un placeholder.
 */
public final class PlaceholderContext {

    private final Player viewer;
    private final SpectatorGameEvent event;
    private final UUID subject;

    public PlaceholderContext(Player viewer, SpectatorGameEvent event, UUID subject) {
        this.viewer = viewer;
        this.event = event;
        this.subject = subject;
    }

    /** Joueur qui lit le message (spectateur), peut être null. */
    public Player getViewer() {
        return viewer;
    }

    /** Évènement en cours d'affichage, peut être null. */
    public SpectatorGameEvent getEvent() {
        return event;
    }

    /** Joueur principal concerné (joueur de l'évènement), peut être null. */
    public UUID getSubjectId() {
        return subject;
    }

    public Player getSubject() {
        return subject == null ? null : Bukkit.getPlayer(subject);
    }
}
