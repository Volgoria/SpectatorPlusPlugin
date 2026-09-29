package fr.spectatorplus.mod.mixin;

import fr.spectatorplus.mod.SpectatorPlusMod;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Un spectateur n'absorbe pas l'expérience.
 */
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void sp$pickup(Player player, CallbackInfo ci) {
        if (!SpectatorPlusMod.canPickup(player)) ci.cancel();
    }
}
