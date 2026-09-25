package fr.spectatorplus.api;

public enum EnterReason {
    /** Le joueur est mort (modes SEMI_AUTO et AUTO). */
    DEATH,
    /** Le joueur a été éliminé par un plugin de jeu. */
    ELIMINATION,
    /** Le joueur a rejoint pendant une partie en cours. */
    JOIN_DURING_GAME,
    /** Restauration après une reconnexion. */
    RECONNECT,
    /** Commande /spec join. */
    COMMAND,
    /** Appel de l'API par un plugin externe. */
    API
}
