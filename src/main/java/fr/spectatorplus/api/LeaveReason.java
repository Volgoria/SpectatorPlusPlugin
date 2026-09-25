package fr.spectatorplus.api;

public enum LeaveReason {
    /** Commande /spec leave. */
    COMMAND,
    /** Appel de l'API par un plugin externe. */
    API,
    /** Fin de partie (si game.release-spectators-on-end est activé). */
    GAME_END,
    /** Déconnexion (si spectator.keep-on-quit est désactivé). */
    QUIT,
    /** Désactivation du plugin (si spectator.persist-on-restart est désactivé). */
    PLUGIN_DISABLE
}
