package fr.spectatorplus.mod.mixin;

import fr.spectatorplus.mod.SpectatorPlusMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Un joueur caché à un autre (spectateur) ne lui est plus envoyé.
 */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin implements TrackedEntityAccess {

    @Shadow
    @Final
    Entity entity;

    @Shadow
    public abstract void removePlayer(ServerPlayer player);

    @Shadow
    public abstract void updatePlayer(ServerPlayer player);

    @Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
    private void sp$hide(ServerPlayer viewer, CallbackInfo ci) {
        if (entity instanceof ServerPlayer && SpectatorPlusMod.isHiddenFrom(viewer, (ServerPlayer) entity)) {
            removePlayer(viewer);
            ci.cancel();
        }
    }

    @Override
    public void sp$removePlayer(ServerPlayer player) {
        removePlayer(player);
    }

    @Override
    public void sp$updatePlayer(ServerPlayer player) {
        updatePlayer(player);
    }
}
