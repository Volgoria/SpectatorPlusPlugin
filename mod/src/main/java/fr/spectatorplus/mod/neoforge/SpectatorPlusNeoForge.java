package fr.spectatorplus.mod.neoforge;

//? if neoforge {
/*import fr.spectatorplus.mod.ModLogger;
import fr.spectatorplus.mod.SpectatorPlusMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.File;
import java.util.logging.Logger;

/^*
 * Point d'entrée du mod NeoForge (server-side uniquement : les joueurs gardent un client vanilla).
 * Transmet au code commun le cycle de vie du serveur, les connexions et les interactions.
 ^/
@Mod("spectatorplus")
public class SpectatorPlusNeoForge {

    private static final Logger LOGGER = ModLogger.create();

    public SpectatorPlusNeoForge(IEventBus modBus, ModContainer container) {
        final File configDir = FMLPaths.CONFIGDIR.get().resolve("spectatorplus").toFile();
        IEventBus bus = NeoForge.EVENT_BUS;

        bus.addListener((RegisterCommandsEvent e) -> SpectatorPlusMod.registerCommands(e.getDispatcher()));
        bus.addListener((ServerStartedEvent e) -> SpectatorPlusMod.start(e.getServer(), configDir, LOGGER, "NeoForge"));
        bus.addListener((ServerStoppingEvent e) -> SpectatorPlusMod.stop());
        bus.addListener((ServerTickEvent.Post e) -> SpectatorPlusMod.tick());

        bus.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer) SpectatorPlusMod.join((ServerPlayer) e.getEntity());
        });
        bus.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer) SpectatorPlusMod.quit((ServerPlayer) e.getEntity());
        });
        bus.addListener((PlayerEvent.PlayerRespawnEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer) SpectatorPlusMod.respawn((ServerPlayer) e.getEntity());
        });

        bus.addListener((PlayerInteractEvent.RightClickItem e) -> {
            if (!(e.getEntity() instanceof ServerPlayer)) return;
            ServerPlayer p = (ServerPlayer) e.getEntity();
            boolean spectator = SpectatorPlusMod.isSpectator(p);
            if (e.getHand() == InteractionHand.MAIN_HAND) SpectatorPlusMod.useItem(p, false);
            if (spectator) {
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.FAIL);
            }
        });
        bus.addListener((PlayerInteractEvent.RightClickBlock e) -> {
            if (!(e.getEntity() instanceof ServerPlayer)) return;
            ServerPlayer p = (ServerPlayer) e.getEntity();
            boolean spectator = SpectatorPlusMod.isSpectator(p);
            if (e.getHand() == InteractionHand.MAIN_HAND) SpectatorPlusMod.useItem(p, false);
            if (spectator) {
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.FAIL);
            }
        });
        bus.addListener((PlayerInteractEvent.LeftClickBlock e) -> {
            if (!(e.getEntity() instanceof ServerPlayer) || !SpectatorPlusMod.isSpectator(e.getEntity())) return;
            if (e.getHand() == InteractionHand.MAIN_HAND) SpectatorPlusMod.useItem((ServerPlayer) e.getEntity(), true);
            e.setCanceled(true);
        });
        bus.addListener((PlayerInteractEvent.EntityInteract e) -> {
            if (!(e.getEntity() instanceof ServerPlayer) || !SpectatorPlusMod.isSpectator(e.getEntity())) return;
            if (e.getHand() == InteractionHand.MAIN_HAND) SpectatorPlusMod.interactEntity((ServerPlayer) e.getEntity(), e.getTarget(), false);
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.FAIL);
        });
        bus.addListener((AttackEntityEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer && SpectatorPlusMod.interactEntity((ServerPlayer) e.getEntity(), e.getTarget(), true)) {
                e.setCanceled(true);
            }
        });
    }
}
*///?}
