package fr.spectatorplus.mod.forge;

//? if forge && <1.21.6 {
/*import fr.spectatorplus.mod.SpectatorPlusMod;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import static fr.spectatorplus.mod.forge.SpectatorPlusForge.*;

/^*
 * Forge jusqu'à 1.21.5 : bus global MinecraftForge.EVENT_BUS.
 ^/
final class ForgeEvents {

    private ForgeEvents() {
    }

    static void register() {
        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.addListener((RegisterCommandsEvent e) -> SpectatorPlusMod.registerCommands(e.getDispatcher()));
        lifecycle(bus);
        tick(bus);
        bus.addListener((PlayerEvent.PlayerLoggedInEvent e) -> join(player(e)));
        bus.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> quit(player(e)));
        bus.addListener((PlayerEvent.PlayerRespawnEvent e) -> respawn(player(e)));
        bus.addListener((PlayerInteractEvent.RightClickItem e) -> {
            if (rightClick(player(e), e.getHand())) cancel(e);
        });
        bus.addListener((PlayerInteractEvent.RightClickBlock e) -> {
            if (rightClick(player(e), e.getHand())) cancel(e);
        });
        bus.addListener((PlayerInteractEvent.LeftClickBlock e) -> {
            if (leftClick(player(e), e.getHand())) e.setCanceled(true);
        });
        bus.addListener((PlayerInteractEvent.EntityInteract e) -> {
            if (interact(player(e), e.getHand(), e.getTarget())) cancel(e);
        });
        bus.addListener((AttackEntityEvent e) -> {
            if (attack(player(e), e.getTarget())) e.setCanceled(true);
        });
    }

    /^* Démarrage / arrêt du serveur (évènements FML en 1.16.5). ^/
    private static void lifecycle(IEventBus bus) {
        //? if >=1.17 {
        bus.addListener((net.minecraftforge.event.server.ServerStartedEvent e) -> SpectatorPlusMod.start(e.getServer(), configDir, LOGGER, "Forge"));
        bus.addListener((net.minecraftforge.event.server.ServerStoppingEvent e) -> SpectatorPlusMod.stop());
        //?} else {
        /^bus.addListener((net.minecraftforge.fml.event.server.FMLServerStartedEvent e) -> SpectatorPlusMod.start(e.getServer(), configDir, LOGGER, "Forge"));
        bus.addListener((net.minecraftforge.fml.event.server.FMLServerStoppingEvent e) -> SpectatorPlusMod.stop());
        ^///?}
    }

    /^* Fin de tick du serveur (évènements Pre / Post à partir de 1.20.5). ^/
    private static void tick(IEventBus bus) {
        //? if >=1.20.5 {
        bus.addListener((TickEvent.ServerTickEvent.Post e) -> SpectatorPlusMod.tick());
        //?} else {
        /^bus.addListener((TickEvent.ServerTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) SpectatorPlusMod.tick();
        });
        ^///?}
    }

    private static void cancel(PlayerInteractEvent e) {
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.FAIL);
    }

    /^* Joueur d'un évènement (getPlayer jusqu'en 1.18.2, getEntity ensuite). ^/
    private static Player player(PlayerEvent e) {
        //? if >=1.19 {
        return e.getEntity();
        //?} else
        /^return e.getPlayer();^/
    }
}
*///?}
