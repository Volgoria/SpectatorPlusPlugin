package fr.spectatorplus.mod;

import fr.spectatorplus.mod.mixin.ChunkMapAccessor;
import fr.spectatorplus.mod.mixin.TrackedEntityAccess;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Joueurs cachés à d'autres joueurs (équivalent de Player#hidePlayer de Bukkit) : l'entité cachée
 * n'est plus envoyée au client qui ne doit pas la voir (voir le mixin sur ChunkMap.TrackedEntity).
 */
public final class ModVisibility {

    private final ModPlatform platform;
    private final Map<UUID, Set<UUID>> hidden = new ConcurrentHashMap<>();

    ModVisibility(ModPlatform platform) {
        this.platform = platform;
    }

    public boolean isHidden(UUID viewer, UUID target) {
        Set<UUID> set = hidden.get(viewer);
        return set != null && set.contains(target);
    }

    void setHidden(ServerPlayer viewer, ServerPlayer target, boolean hide) {
        if (viewer == null || target == null || viewer == target) return;
        Set<UUID> set = hidden.get(viewer.getUUID());
        if (hide) {
            if (set == null) {
                set = ConcurrentHashMap.newKeySet();
                hidden.put(viewer.getUUID(), set);
            }
            set.add(target.getUUID());
        } else if (set != null) {
            set.remove(target.getUUID());
        }
        // mise à jour immédiate du suivi réseau de l'entité
        Object tracked = ((ChunkMapAccessor) Mc.level(target).getChunkSource().chunkMap).sp$entityMap().get(target.getId());
        if (tracked instanceof TrackedEntityAccess) {
            if (hide) ((TrackedEntityAccess) tracked).sp$removePlayer(viewer);
            else ((TrackedEntityAccess) tracked).sp$updatePlayer(viewer);
        }
    }

    void forget(UUID id) {
        hidden.remove(id);
        for (Set<UUID> set : hidden.values()) set.remove(id);
    }
}
