package fr.spectatorplus.mod.mixin;

import fr.spectatorplus.mod.ModSignals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.1 {
import net.minecraft.advancements.triggers.ConsumeItemTrigger;
//?} elif >=1.21.11 {
/*import net.minecraft.advancements.criterion.ConsumeItemTrigger;
*///?} else
/*import net.minecraft.advancements.critereon.ConsumeItemTrigger;*/

/**
 * Objet consommé (nourriture, potion, lait).
 */
@Mixin(ConsumeItemTrigger.class)
public abstract class ConsumeItemTriggerMixin {

    @Inject(method = "trigger", at = @At("HEAD"))
    private void sp$trigger(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.item.ItemStack stack, CallbackInfo ci) {
        ModSignals.consume(player, stack);
    }
}
