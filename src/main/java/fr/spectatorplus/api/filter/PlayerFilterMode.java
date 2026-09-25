package fr.spectatorplus.api.filter;

public enum PlayerFilterMode {
    /** Tous les joueurs. */
    ALL,
    /** Uniquement les joueurs de la whitelist. */
    WHITELIST,
    /** Tous sauf les joueurs de la blacklist. */
    BLACKLIST,
    /** Uniquement le joueur actuellement suivi. */
    FOLLOWED,
    /** Uniquement les joueurs favoris. */
    FAVORITES;

    public PlayerFilterMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
