package fr.spectatorplus.mod.mixin;

import fr.spectatorplus.mod.ModSignals;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bloc posé par un joueur.
 */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

    @Inject(method = "place", at = @At("RETURN"))
    private void sp$placed(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult r = cir.getReturnValue();
        if (r == InteractionResult.FAIL || r == InteractionResult.PASS) return;
        if (context.getPlayer() instanceof ServerPlayer && context.getLevel() instanceof ServerLevel) {
            ModSignals.blockPlace((ServerPlayer) context.getPlayer(), (ServerLevel) context.getLevel(), context.getClickedPos());
        }
    }
}
