package fr.spectatorplus.mod.mixin;

import net.minecraft.server.level.ServerPlayer;

/**
 * Méthodes ajoutées à ChunkMap.TrackedEntity (classe non publique) par {@link TrackedEntityMixin}.
 */
public interface TrackedEntityAccess {

    void sp$removePlayer(ServerPlayer player);

    void sp$updatePlayer(ServerPlayer player);
}
