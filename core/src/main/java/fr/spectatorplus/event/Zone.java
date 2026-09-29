package fr.spectatorplus.event;

import fr.spectatorplus.core.config.ConfigSection;
import fr.spectatorplus.core.platform.Position;

/**
 * Zone rectangulaire configurée dans config.yml → zones.
 */
public final class Zone {

    private final String id;
    private final String name;
    private final String world;
    private final double minX, minY, minZ, maxX, maxY, maxZ;

    public Zone(String id, ConfigSection s) {
        this.id = id;
        this.name = s.getString("name", id);
        this.world = s.getString("world", "world");
        double x1 = s.getDouble("pos1.x"), y1 = s.getDouble("pos1.y", -1000), z1 = s.getDouble("pos1.z");
        double x2 = s.getDouble("pos2.x"), y2 = s.getDouble("pos2.y", 1000), z2 = s.getDouble("pos2.z");
        minX = Math.min(x1, x2);
        maxX = Math.max(x1, x2);
        minY = Math.min(y1, y2);
        maxY = Math.max(y1, y2);
        minZ = Math.min(z1, z2);
        maxZ = Math.max(z1, z2);
    }

    public boolean contains(Position l) {
        if (l == null || !world.equals(l.getWorld())) return false;
        return l.getX() >= minX && l.getX() <= maxX + 1 && l.getY() >= minY && l.getY() <= maxY + 1
                && l.getZ() >= minZ && l.getZ() <= maxZ + 1;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getWorld() {
        return world;
    }
}
