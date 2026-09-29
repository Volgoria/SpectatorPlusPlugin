package fr.spectatorplus.compat;

import fr.spectatorplus.core.platform.Dimension;
import fr.spectatorplus.core.platform.Position;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Conversion des positions et mondes Bukkit vers les types du code commun, et inversement.
 */
public final class Positions {

    private Positions() {
    }

    public static Position of(Location l) {
        if (l == null) return null;
        return new Position(l.getWorld() == null ? null : l.getWorld().getName(),
                l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
    }

    /** Location Bukkit, ou null si le monde n'est pas chargé. */
    public static Location toLocation(Position p) {
        if (p == null || p.getWorld() == null) return null;
        World w = Bukkit.getWorld(p.getWorld());
        return w == null ? null : new Location(w, p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
    }

    public static Dimension dimension(World w) {
        switch (w.getEnvironment()) {
            case NETHER:
                return Dimension.NETHER;
            case THE_END:
                return Dimension.END;
            default:
                return Dimension.OVERWORLD;
        }
    }
}
