package fr.spectatorplus.mod.mixin;

import fr.spectatorplus.mod.ModSignals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.1 {
import net.minecraft.advancements.triggers.UsedTotemTrigger;
//?} elif >=1.21.11 {
/*import net.minecraft.advancements.criterion.UsedTotemTrigger;
*///?} else
/*import net.minecraft.advancements.critereon.UsedTotemTrigger;*/

/**
 * Totem d'immortalité utilisé.
 */
@Mixin(UsedTotemTrigger.class)
public abstract class UsedTotemTriggerMixin {

    @Inject(method = "trigger", at = @At("HEAD"))
    private void sp$trigger(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.item.ItemStack stack, CallbackInfo ci) {
        ModSignals.totem(player);
    }
}
