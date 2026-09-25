package fr.spectatorplus.api;

/**
 * État courant d'un spectateur.
 */
public enum SpectatorState {
    /** Se déplace librement. */
    FREE,
    /** Suit automatiquement un joueur (caméra à la 3e personne). */
    FOLLOWING,
    /** Voit à travers les yeux d'un joueur. */
    POV,
    /** Consulte la fiche ou l'inventaire d'un joueur. */
    INSPECTING,
    /** Immobilisé par Spectator Plus. */
    FROZEN
}
