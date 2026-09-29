package fr.spectatorplus.mod.mixin;

import fr.spectatorplus.mod.ModSignals;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Résultat d'une enclume pris par le joueur (les deux objets d'entrée sont encore présents).
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Shadow
    @Final
    private DataSlot cost;

    //? if >=1.17 {
    @Inject(method = "onTake", at = @At("HEAD"))
    private void sp$take(Player player, ItemStack stack, CallbackInfo ci) {
        sp$anvil(player, stack);
    }
    //?} else {
    /*@Inject(method = "onTake", at = @At("HEAD"))
    private void sp$take(Player player, ItemStack stack, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<ItemStack> cir) {
        sp$anvil(player, stack);
    }
    *///?}

    @org.spongepowered.asm.mixin.Unique
    private void sp$anvil(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer)) return;
        ItemCombinerMenu menu = (ItemCombinerMenu) (Object) this;
        ModSignals.anvil((ServerPlayer) player, menu.getSlot(0).getItem().copy(), menu.getSlot(1).getItem().copy(), stack.copy(), cost.get());
    }
}
