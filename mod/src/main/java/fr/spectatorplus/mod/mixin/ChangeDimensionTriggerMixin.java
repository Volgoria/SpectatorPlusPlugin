package fr.spectatorplus.mod.mixin;

import fr.spectatorplus.mod.ModSignals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.1 {
import net.minecraft.advancements.triggers.ChangeDimensionTrigger;
//?} elif >=1.21.11 {
/*import net.minecraft.advancements.criterion.ChangeDimensionTrigger;
*///?} else
/*import net.minecraft.advancements.critereon.ChangeDimensionTrigger;*/

/**
 * Changement de dimension (évènements de monde et de portail).
 */
@Mixin(ChangeDimensionTrigger.class)
public abstract class ChangeDimensionTriggerMixin {

    @Inject(method = "trigger", at = @At("HEAD"))
    private void sp$trigger(net.minecraft.server.level.ServerPlayer player, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> from, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> to, CallbackInfo ci) {
        ModSignals.worldChanged(player, fr.spectatorplus.mod.Mc.server(player).getLevel(from), fr.spectatorplus.mod.Mc.server(player).getLevel(to));
    }
}
