package fr.spectatorplus.bukkit;

import fr.spectatorplus.compat.Compat;
import fr.spectatorplus.compat.Positions;
import fr.spectatorplus.compat.Reflect;
import fr.spectatorplus.core.platform.Dimension;
import fr.spectatorplus.core.platform.PlatformWorld;
import fr.spectatorplus.core.platform.Position;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Monde Bukkit.
 */
public final class BukkitWorld implements PlatformWorld {

    private static boolean structuresSupported = true;

    private final World world;

    public BukkitWorld(World world) {
        this.world = world;
    }

    public World handle() {
        return world;
    }

    @Override
    public String getName() {
        return world.getName();
    }

    @Override
    public Dimension getDimension() {
        return Positions.dimension(world);
    }

    @Override
    public boolean isMainWorld() {
        List<World> worlds = Bukkit.getWorlds();
        if (worlds.isEmpty()) return true;
        String main = worlds.get(0).getName();
        String n = world.getName();
        return n.equals(main) || n.equals(main + "_nether") || n.equals(main + "_the_end");
    }

    @Override
    public boolean isSolid(int x, int y, int z) {
        return world.getBlockAt(x, y, z).getType().isSolid();
    }

    @Override
    public String getBlockType(int x, int y, int z) {
        return world.getBlockAt(x, y, z).getType().name();
    }

    @Override
    public int getHighestBlockYAt(int x, int z) {
        return world.getHighestBlockYAt(x, z);
    }

    @Override
    public int getMinHeight() {
        return Compat.minHeight(world);
    }

    @Override
    public long getTime() {
        return world.getTime();
    }

    @Override
    public Position getSpawn() {
        return Positions.of(world.getSpawnLocation());
    }

    @Override
    public double getBorderSize() {
        return world.getWorldBorder().getSize();
    }

    @Override
    public double getBorderCenterX() {
        WorldBorder wb = world.getWorldBorder();
        return wb.getCenter().getX();
    }

    @Override
    public double getBorderCenterZ() {
        WorldBorder wb = world.getWorldBorder();
        return wb.getCenter().getZ();
    }

    @Override
    public List<String> getStructuresAt(Position position) {
        List<String> res = new ArrayList<>();
        if (!structuresSupported) return res;
        // 1.19+ : World#getStructures(chunkX, chunkZ)
        Object result = Reflect.invoke(world, "getStructures", position.getBlockX() >> 4, position.getBlockZ() >> 4);
        if (result == null) {
            if (!Reflect.hasMethod(world, "getStructures", 2)) structuresSupported = false;
            return res;
        }
        if (!(result instanceof Iterable)) return res;
        for (Object gs : (Iterable<?>) result) {
            Object box = Reflect.invoke(gs, "getBoundingBox");
            Object inside = Reflect.invoke(box, "contains", position.getX(), position.getY(), position.getZ());
            if (Boolean.TRUE.equals(inside)) res.add(Compat.enumName(Reflect.invoke(gs, "getStructure")).toUpperCase(Locale.ROOT));
        }
        return res;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PlatformWorld && ((PlatformWorld) o).getName().equals(world.getName());
    }

    @Override
    public int hashCode() {
        return world.getName().hashCode();
    }
}
