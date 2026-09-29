package fr.spectatorplus.mod;

import fr.spectatorplus.core.platform.Dimension;
import fr.spectatorplus.core.platform.PlatformWorld;
import fr.spectatorplus.core.platform.Position;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dimension d'un serveur moddé. Son nom est l'identifiant de la dimension (« minecraft:overworld »).
 */
public final class ModWorld implements PlatformWorld {

    private final ServerLevel level;

    public ModWorld(ServerLevel level) {
        this.level = level;
    }

    public ServerLevel handle() {
        return level;
    }

    @Override
    public String getName() {
        return Mc.worldName(level);
    }

    @Override
    public Dimension getDimension() {
        if (level.dimension() == Level.NETHER) return Dimension.NETHER;
        if (level.dimension() == Level.END) return Dimension.END;
        String name = getName();
        if (name.contains("nether")) return Dimension.NETHER;
        if (name.endsWith("end")) return Dimension.END;
        return Dimension.OVERWORLD;
    }

    @Override
    public boolean isMainWorld() {
        return level.dimension() == Level.OVERWORLD || level.dimension() == Level.NETHER || level.dimension() == Level.END;
    }

    @Override
    public boolean isSolid(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    @Override
    public String getBlockType(int x, int y, int z) {
        return Keys.blockName(level.getBlockState(new BlockPos(x, y, z)).getBlock());
    }

    @Override
    public int getHighestBlockYAt(int x, int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
    }

    @Override
    public int getMinHeight() {
        return Mc.minHeight(level);
    }

    @Override
    public long getTime() {
        //? if >=26.1 {
        return level.getDefaultClockTime() % 24000L;
        //?} else
        /*return level.getDayTime() % 24000L;*/
    }

    @Override
    public Position getSpawn() {
        BlockPos p = Mc.spawn(level);
        return new Position(getName(), p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
    }

    @Override
    public double getBorderSize() {
        return level.getWorldBorder().getSize();
    }

    @Override
    public double getBorderCenterX() {
        return level.getWorldBorder().getCenterX();
    }

    @Override
    public double getBorderCenterZ() {
        return level.getWorldBorder().getCenterZ();
    }

    @Override
    public List<String> getStructuresAt(Position position) {
        //? if >=1.19.3 {
        List<String> res = new ArrayList<>();
        BlockPos pos = new BlockPos(position.getBlockX(), position.getBlockY(), position.getBlockZ());
        for (net.minecraft.world.level.levelgen.structure.Structure s : level.structureManager().getAllStructuresAt(pos).keySet()) {
            //? if >=26.1 {
            boolean inside = level.structureManager().getStructureWithPieceAt(pos.getX(), pos.getY(), pos.getZ(), s).isValid();
            //?} else
            /*boolean inside = level.structureManager().getStructureWithPieceAt(pos, s).isValid();*/
            if (!inside) continue;
            //? if >=1.21.2 {
            Identifier id = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getKey(s);
            //?} else
            /*Identifier id = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getKey(s);*/
            if (id != null) res.add(id.getPath().toUpperCase(Locale.ROOT));
        }
        return res;
        //?} else
        /*return new ArrayList<>();*/
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PlatformWorld && ((PlatformWorld) o).getName().equals(getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }
}
