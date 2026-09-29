package fr.spectatorplus.mod.mixin;

import fr.spectatorplus.mod.Keys;
import fr.spectatorplus.mod.ModSignals;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bloc cassé par un joueur : le type du bloc et l'outil sont lus avant la destruction.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

    @Shadow
    protected ServerLevel level;

    @Shadow
    protected ServerPlayer player;

    @Unique
    private String sp$type;
    @Unique
    private ItemStack sp$tool;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void sp$before(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        sp$type = Keys.blockName(level.getBlockState(pos).getBlock());
        sp$tool = player.getMainHandItem().copy();
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void sp$after(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue()) && sp$type != null) ModSignals.blockBreak(player, level, pos, sp$type, sp$tool);
        sp$type = null;
        sp$tool = null;
    }
}
