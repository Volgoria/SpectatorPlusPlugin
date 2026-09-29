package fr.spectatorplus.mod.mixin;

import fr.spectatorplus.mod.ModSignals;
import fr.spectatorplus.mod.SpectatorPlusMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Objets au sol : un spectateur ne les ramasse pas ; un joueur qui en ramasse déclenche l'évènement.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Unique
    private ItemStack sp$before;

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void sp$pickup(Player player, CallbackInfo ci) {
        if (!SpectatorPlusMod.canPickup(player)) {
            ci.cancel();
            return;
        }
        sp$before = player instanceof ServerPlayer ? ((ItemEntity) (Object) this).getItem().copy() : null;
    }

    @Inject(method = "playerTouch", at = @At("RETURN"))
    private void sp$picked(Player player, CallbackInfo ci) {
        ItemStack before = sp$before;
        sp$before = null;
        if (before == null || before.isEmpty()) return;
        ItemEntity self = (ItemEntity) (Object) this;
        // ramassé en entier : l'entité disparaît (le jeu remet le nombre d'origine pour l'animation)
        int picked = !self.isAlive() ? before.getCount() : before.getCount() - self.getItem().getCount();
        if (picked <= 0) return;
        ItemStack stack = before.copy();
        stack.setCount(picked);
        ModSignals.pickup((ServerPlayer) player, stack);
    }
}
