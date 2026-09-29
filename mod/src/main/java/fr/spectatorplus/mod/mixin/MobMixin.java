package fr.spectatorplus.mod.mixin;

import fr.spectatorplus.mod.ModSignals;
import fr.spectatorplus.mod.SpectatorPlusMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Les monstres ne ciblent pas les spectateurs ; une nouvelle cible joueur déclenche l'évènement de rencontre.
 */
@Mixin(Mob.class)
public abstract class MobMixin {

    @Shadow
    public abstract LivingEntity getTarget();

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void sp$target(LivingEntity target, CallbackInfo ci) {
        if (target == null) return;
        if (SpectatorPlusMod.isSpectator(target)) {
            ci.cancel();
            return;
        }
        if (target != getTarget()) ModSignals.target((Mob) (Object) this, target);
    }
}
