package fr.spectatorplus.mod.mixin;

import fr.spectatorplus.mod.ModSignals;
import fr.spectatorplus.mod.SpectatorPlusMod;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Actions réseau d'un spectateur : clics dans son propre inventaire, jet / échange d'objets, clic gauche dans le vide.
 * Les paquets arrivent d'abord sur le thread réseau puis sont relancés sur le thread du serveur :
 * on n'agit que sur le thread du serveur.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PacketListenerMixin {

    @Shadow
    public ServerPlayer player;

    private boolean sp$mainThread() {
        return SpectatorPlusMod.platform() != null && SpectatorPlusMod.platform().server().isSameThread();
    }

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void sp$containerClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        // les menus de Spectator Plus gèrent eux-mêmes leurs clics ; ici : l'inventaire du joueur
        if (!sp$mainThread() || player.containerMenu != player.inventoryMenu) return;
        if (SpectatorPlusMod.blockInventoryAction(player)) ci.cancel();
    }

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void sp$playerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (!sp$mainThread()) return;
        ServerboundPlayerActionPacket.Action a = packet.getAction();
        if (a == ServerboundPlayerActionPacket.Action.DROP_ITEM || a == ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS
                || a == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
            if (SpectatorPlusMod.blockInventoryAction(player)) {
                ci.cancel();
                return;
            }
            if (a != ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
                ItemStack held = player.getMainHandItem().copy();
                if (a == ServerboundPlayerActionPacket.Action.DROP_ITEM && !held.isEmpty()) held.setCount(1);
                ModSignals.drop(player, held);
            }
        }
    }

    //? if >=26.1 {
    @Inject(method = "handlePunch", at = @At("HEAD"))
    private void sp$swing(net.minecraft.network.protocol.game.ServerboundPunchPacket packet, CallbackInfo ci) {
        if (sp$mainThread()) SpectatorPlusMod.swing(player);
    }
    //?} else {
    /*@Inject(method = "handleAnimate", at = @At("HEAD"))
    private void sp$swing(net.minecraft.network.protocol.game.ServerboundSwingPacket packet, CallbackInfo ci) {
        if (sp$mainThread()) SpectatorPlusMod.swing(player);
    }
    *///?}
}
