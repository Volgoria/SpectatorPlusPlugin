package fr.spectatorplus.mod.mixin;

import fr.spectatorplus.mod.ModSignals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.1 {
import net.minecraft.advancements.triggers.SummonedEntityTrigger;
//?} elif >=1.21.11 {
/*import net.minecraft.advancements.criterion.SummonedEntityTrigger;
*///?} else
/*import net.minecraft.advancements.critereon.SummonedEntityTrigger;*/

/**
 * Entité invoquée par un joueur (wither).
 */
@Mixin(SummonedEntityTrigger.class)
public abstract class SummonedEntityTriggerMixin {

    @Inject(method = "trigger", at = @At("HEAD"))
    private void sp$trigger(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.entity.Entity entity, CallbackInfo ci) {
        ModSignals.summoned(entity);
    }
}
