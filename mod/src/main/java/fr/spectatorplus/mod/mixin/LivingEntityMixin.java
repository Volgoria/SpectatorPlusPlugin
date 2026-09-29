package fr.spectatorplus.mod.mixin;

import fr.spectatorplus.mod.ModSignals;
import fr.spectatorplus.mod.SpectatorPlusMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Entités vivantes :
 * <ul>
 *     <li>un spectateur sans collision ne pousse personne, n'est pas poussé et n'arrête ni projectiles ni clics ;</li>
 *     <li>dégâts réellement subis (vie + absorption), morts de créatures, objets cassés (évènements natifs).</li>
 * </ul>
 * Les dégâts des joueurs passent par {@link PlayerMixin} (Player redéfinit actuallyHurt).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Unique
    private double sp$totalBefore;
    @Unique
    private double sp$healthBefore;

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void sp$pushable(CallbackInfoReturnable<Boolean> cir) {
        if (!SpectatorPlusMod.isCollidable((LivingEntity) (Object) this)) cir.setReturnValue(false);
    }

    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
    private void sp$pickable(CallbackInfoReturnable<Boolean> cir) {
        if (!SpectatorPlusMod.isCollidable((LivingEntity) (Object) this)) cir.setReturnValue(false);
    }

    @Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
    private void sp$push(CallbackInfo ci) {
        if (!SpectatorPlusMod.isCollidable((LivingEntity) (Object) this)) ci.cancel();
    }

    // ------------------------------------------------------------------ dégâts

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
        LivingEntity self = (LivingEntity) (Object) this;
        sp$healthBefore = self.getHealth();
        sp$totalBefore = self.getHealth() + self.getAbsorptionAmount();
    }

    @Unique
    private void sp$after(DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (fr.spectatorplus.mod.Mc.entityLevel(self).isClientSide()) return;
        double lost = sp$totalBefore - (self.getHealth() + self.getAbsorptionAmount());
        if (lost > 0) ModSignals.damage(self, source, sp$healthBefore, lost);
    }

    // ------------------------------------------------------------------ morts / objets cassés

    @Inject(method = "die", at = @At("HEAD"))
    private void sp$die(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!fr.spectatorplus.mod.Mc.entityLevel(self).isClientSide() && !(self instanceof ServerPlayer)) ModSignals.entityDeath(self, source);
    }

    //? if >=26.1 {
    @Inject(method = "onEquippedItemBroken", at = @At("HEAD"))
    private void sp$broken(net.minecraft.world.item.ItemStack item, EquipmentSlot slot, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer) ModSignals.itemBreak((ServerPlayer) (Object) this, item.copy());
    }
    //?} elif >=1.20.5 {
    /*@Inject(method = "onEquippedItemBroken", at = @At("HEAD"))
    private void sp$broken(net.minecraft.world.item.Item item, EquipmentSlot slot, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer) ModSignals.itemBreak((ServerPlayer) (Object) this, new net.minecraft.world.item.ItemStack(item));
    }
    *///?} else {
    /*@Inject(method = "broadcastBreakEvent(Lnet/minecraft/world/entity/EquipmentSlot;)V", at = @At("HEAD"))
    private void sp$broken(EquipmentSlot slot, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer) ModSignals.itemBreak((ServerPlayer) self, self.getItemBySlot(slot).copy());
    }
    *///?}
}
