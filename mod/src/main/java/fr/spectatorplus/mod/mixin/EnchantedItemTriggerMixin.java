package fr.spectatorplus.mod.mixin;

import fr.spectatorplus.mod.ModSignals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.1 {
import net.minecraft.advancements.triggers.EnchantedItemTrigger;
//?} elif >=1.21.11 {
/*import net.minecraft.advancements.criterion.EnchantedItemTrigger;
*///?} else
/*import net.minecraft.advancements.critereon.EnchantedItemTrigger;*/

/**
 * Objet enchanté à la table.
 */
@Mixin(EnchantedItemTrigger.class)
public abstract class EnchantedItemTriggerMixin {

    @Inject(method = "trigger", at = @At("HEAD"))
    private void sp$trigger(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.item.ItemStack stack, int levels, CallbackInfo ci) {
        ModSignals.enchanted(player, stack, levels);
    }
}
