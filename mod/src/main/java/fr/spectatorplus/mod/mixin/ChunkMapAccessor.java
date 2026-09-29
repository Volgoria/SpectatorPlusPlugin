package fr.spectatorplus.mod.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accès aux entités suivies par le serveur (pour cacher / montrer un joueur immédiatement).
 */
@Mixin(ChunkMap.class)
public interface ChunkMapAccessor {

    @SuppressWarnings("rawtypes")
    @Accessor("entityMap")
    Int2ObjectMap sp$entityMap();
}
