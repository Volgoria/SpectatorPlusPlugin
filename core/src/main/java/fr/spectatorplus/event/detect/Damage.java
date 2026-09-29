package fr.spectatorplus.event.detect;

import fr.spectatorplus.core.platform.ItemRef;
import fr.spectatorplus.core.platform.PlatformPlayer;

/**
 * Dégâts infligés à une entité, décrits par la plateforme.
 * La victime est soit un joueur ({@link #victim}), soit une créature ({@link #victimMob}).
 */
public final class Damage {

    /** Joueur touché, ou null. */
    public PlatformPlayer victim;
    /** Créature touchée, ou null. */
    public EntityInfo victimMob;
    /** Vie de la victime avant les dégâts. */
    public double victimHealthBefore;

    /** Joueur responsable (mêlée ou projectile), ou null. */
    public PlatformPlayer attacker;
    /** Créature responsable (mêlée ou projectile), ou null. */
    public EntityInfo attackerMob;
    /** Type de l'entité qui a directement frappé (ARROW, TRIDENT, PLAYER, ZOMBIE...), ou null. */
    public String directType;
    /** L'entité qui a directement frappé est un projectile. */
    public boolean projectile;

    /** Dégâts finaux (points). */
    public double amount;
    /** Cause avec les noms de Bukkit (ENTITY_ATTACK, FALL, PROJECTILE...). */
    public String cause = "CUSTOM";
    public boolean critical;
    /** Dégâts réduits par un bouclier. */
    public boolean blocked;
    /** La victime (joueur) lève son bouclier. */
    public boolean victimBlocking;
    /** Arme en main de l'attaquant (mêlée uniquement), ou null. */
    public ItemRef weapon;
}
