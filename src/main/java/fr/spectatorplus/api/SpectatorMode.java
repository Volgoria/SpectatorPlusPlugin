package fr.spectatorplus.api;

/**
 * Mode de fonctionnement du passage en spectateur.
 */
public enum SpectatorMode {
    /** Le plugin externe décide seul, via l'API, quand un joueur devient spectateur. */
    MANUAL,
    /** Spectator Plus détecte les situations, mais le SpectatorEnterEvent peut être annulé. */
    SEMI_AUTO,
    /** Spectator Plus gère entièrement les transitions ; l'annulation de l'évènement est ignorée. */
    AUTO
}
