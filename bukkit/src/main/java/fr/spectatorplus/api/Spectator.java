package fr.spectatorplus.api;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Un joueur actuellement géré par le système spectateur de Spectator Plus.
 */
public interface Spectator {

    UUID getUniqueId();

    /** Joueur Bukkit (null s'il est hors ligne). */
    Player getPlayer();

    SpectatorState getState();

    EnterReason getEnterReason();

    /** Horodatage (ms) d'entrée en mode spectateur. */
    long getSince();

    /** Joueur actuellement suivi (follow ou POV), ou null. */
    Player getFollowTarget();

    /** Démarre (ou change) le suivi d'un joueur. */
    void follow(Player target);

    /** Arrête le suivi (et le POV). */
    void stopFollowing();

    /** Passe en vue à la première personne du joueur ciblé. */
    void startPov(Player target);

    void stopPov();

    /** Téléporte le spectateur vers un joueur. */
    boolean teleportTo(Player target);

    /** Ouvre la fiche d'inspection d'un joueur. */
    void openInspection(Player target);

    /** Ouvre l'inventaire (ou l'ender chest) d'un joueur en lecture seule. */
    void openInventory(Player target, boolean enderChest);

    boolean isFrozen();

    void setFrozen(boolean frozen);
}
