package fr.spectatorplus.mod.fabric;

//? if fabric {
import fr.spectatorplus.mod.ModLogger;
import fr.spectatorplus.mod.SpectatorPlusMod;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

import java.io.File;
import java.util.logging.Logger;

/**
 * Point d'entrée du mod Fabric (server-side uniquement : les joueurs gardent un client vanilla).
 * Transmet au code commun le cycle de vie du serveur, les connexions et les interactions.
 */
public class SpectatorPlusFabric implements ModInitializer {

    private static final Logger LOGGER = ModLogger.create();

    @Override
    public void onInitialize() {
        final File configDir = FabricLoader.getInstance().getConfigDir().resolve("spectatorplus").toFile();

        //? if >=1.19 {
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> SpectatorPlusMod.registerCommands(dispatcher));
        //?} else {
        /*net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback.EVENT.register(
                (dispatcher, dedicated) -> SpectatorPlusMod.registerCommands(dispatcher));
        *///?}

        ServerLifecycleEvents.SERVER_STARTED.register(server -> SpectatorPlusMod.start(server, configDir, LOGGER, "Fabric"));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> SpectatorPlusMod.stop());
        ServerTickEvents.END_SERVER_TICK.register(server -> SpectatorPlusMod.tick());

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SpectatorPlusMod.join(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SpectatorPlusMod.quit(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> SpectatorPlusMod.respawn(newPlayer));

        //? if >=1.21.2 {
        UseItemCallback.EVENT.register((player, level, hand) ->
                main(player, hand) && SpectatorPlusMod.useItem((ServerPlayer) player, false) ? InteractionResult.FAIL : InteractionResult.PASS);
        //?} else {
        /*UseItemCallback.EVENT.register((player, level, hand) ->
                main(player, hand) && SpectatorPlusMod.useItem((ServerPlayer) player, false)
                        ? net.minecraft.world.InteractionResultHolder.fail(player.getItemInHand(hand))
                        : net.minecraft.world.InteractionResultHolder.pass(player.getItemInHand(hand)));
        *///?}
        UseBlockCallback.EVENT.register((player, level, hand, hit) ->
                block(player, hand, false) ? InteractionResult.FAIL : InteractionResult.PASS);
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
                block(player, hand, true) ? InteractionResult.FAIL : InteractionResult.PASS);
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
                player instanceof ServerPlayer && SpectatorPlusMod.isSpectator(player)
                        && (hand != InteractionHand.MAIN_HAND || SpectatorPlusMod.interactEntity((ServerPlayer) player, entity, false))
                        ? InteractionResult.FAIL : InteractionResult.PASS);
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
                player instanceof ServerPlayer && SpectatorPlusMod.interactEntity((ServerPlayer) player, entity, true)
                        ? InteractionResult.FAIL : InteractionResult.PASS);
    }

    private static boolean main(Player player, InteractionHand hand) {
        return player instanceof ServerPlayer && hand == InteractionHand.MAIN_HAND;
    }

    /**
     * Clic sur un bloc : bloqué pour un spectateur (les deux mains), action de la barre avec la main principale ;
     * pour les autres joueurs, un clic droit avec un objet est signalé (évènement d'utilisation d'objet).
     */
    private static boolean block(Player player, InteractionHand hand, boolean left) {
        if (!(player instanceof ServerPlayer)) return false;
        if (!SpectatorPlusMod.isSpectator(player)) {
            if (!left && hand == InteractionHand.MAIN_HAND) SpectatorPlusMod.useItem((ServerPlayer) player, false);
            return false;
        }
        if (hand == InteractionHand.MAIN_HAND) SpectatorPlusMod.useItem((ServerPlayer) player, left);
        return true;
    }
}
//?}
