package fr.spectatorplus.event.detect;

import java.util.UUID;

/**
 * Entité non joueur (créature) concernée par un évènement.
 */
public final class EntityInfo {

    private final UUID id;
    private final String type;
    private final double health;

    /**
     * @param type   type façon Bukkit : ZOMBIE, ENDER_DRAGON...
     * @param health vie actuelle (points)
     */
    public EntityInfo(UUID id, String type, double health) {
        this.id = id;
        this.type = type;
        this.health = health;
    }

    public UUID getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public double getHealth() {
        return health;
    }
}
