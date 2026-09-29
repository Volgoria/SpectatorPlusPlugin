package fr.spectatorplus.mod.mixin;

import fr.spectatorplus.mod.ModSignals;
import fr.spectatorplus.mod.SpectatorPlusMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mort d'un joueur (modes auto, spectateur tué par /kill...) et langue du client sur les anciennes versions.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void sp$die(DamageSource source, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        ModSignals.playerDeath(self, source);
        SpectatorPlusMod.death(self);
    }

    //? if >=1.18 <1.20.2 {
    /*@Inject(method = "updateOptions", at = @At("HEAD"))
    private void sp$language(net.minecraft.network.protocol.game.ServerboundClientInformationPacket packet, CallbackInfo ci) {
        SpectatorPlusMod.language((ServerPlayer) (Object) this, packet.language());
    }
    *///?}
}
