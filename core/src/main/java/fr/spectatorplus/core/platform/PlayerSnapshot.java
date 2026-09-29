package fr.spectatorplus.core.platform;

/**
 * État d'un joueur avant son passage en spectateur (inventaire, mode de jeu, position, vie, effets...),
 * restauré à la sortie. Créé et sauvegardé par la plateforme (format natif des objets).
 */
public interface PlayerSnapshot {

    /**
     * @param teleport         remettre le joueur à sa position sauvegardée
     * @param restoreInventory rendre l'inventaire sauvegardé
     */
    void restore(PlatformPlayer player, boolean teleport, boolean restoreInventory);

    /**
     * Rend uniquement l'inventaire sauvegardé (armure et seconde main comprises), sans toucher au reste.
     *
     * @return false si la plateforme ne sait pas le faire
     */
    default boolean restoreInventory(PlatformPlayer player) {
        return false;
    }

    /** Position sauvegardée, ou null. */
    Position getLocation();
}
