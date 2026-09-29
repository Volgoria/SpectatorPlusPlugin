package fr.spectatorplus.mod.mixin;

import fr.spectatorplus.mod.ModSignals;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dégâts réellement subis par un joueur (Player redéfinit actuallyHurt sans appeler LivingEntity).
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @Unique
    private double sp$totalBefore;
    @Unique
    private double sp$healthBefore;

    //? if >=1.21.2 {
    @Inject(method = "actuallyHurt", at = @At("HEAD"))
    private void sp$hurtHead(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount, CallbackInfo ci) {
        sp$before();
    }

    @Inject(method = "actuallyHurt", at = @At("RETURN"))
    private void sp$hurtReturn(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount, CallbackInfo ci) {
        sp$after(source);
    }
    //?} else {
    /*@Inject(method = "actuallyHurt", at = @At("HEAD"))
    private void sp$hurtHead(DamageSource source, float amount, CallbackInfo ci) {
        sp$before();
    }

    @Inject(method = "actuallyHurt", at = @At("RETURN"))
    private void sp$hurtReturn(DamageSource source, float amount, CallbackInfo ci) {
        sp$after(source);
    }
    *///?}

    @Unique
    private void sp$before() {
        Player self = (Player) (Object) this;
        sp$healthBefore = self.getHealth();
        sp$totalBefore = self.getHealth() + self.getAbsorptionAmount();
    }

    @Unique
    private void sp$after(DamageSource source) {
        Player self = (Player) (Object) this;
        if (fr.spectatorplus.mod.Mc.entityLevel(self).isClientSide()) return;
        double lost = sp$totalBefore - (self.getHealth() + self.getAbsorptionAmount());
        if (lost > 0) ModSignals.damage(self, source, sp$healthBefore, lost);
    }
}
