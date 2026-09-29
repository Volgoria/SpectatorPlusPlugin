package fr.spectatorplus.mod.forge;

//? if forge && >=1.21.6 {
/*import fr.spectatorplus.mod.SpectatorPlusMod;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

import static fr.spectatorplus.mod.forge.SpectatorPlusForge.*;

/^*
 * Forge 1.21.6+ (EventBus 7) : un bus par évènement ; pour les évènements annulables,
 * l'écouteur renvoie true pour annuler.
 ^/
final class ForgeEvents {

    private ForgeEvents() {
    }

    static void register() {
        RegisterCommandsEvent.BUS.addListener(e -> SpectatorPlusMod.registerCommands(e.getDispatcher()));
        ServerStartedEvent.BUS.addListener(e -> SpectatorPlusMod.start(e.getServer(), configDir, LOGGER, "Forge"));
        ServerStoppingEvent.BUS.addListener(e -> SpectatorPlusMod.stop());
        TickEvent.ServerTickEvent.Post.BUS.addListener(e -> SpectatorPlusMod.tick());
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(e -> join(e.getEntity()));
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(e -> quit(e.getEntity()));
        PlayerEvent.PlayerRespawnEvent.BUS.addListener(e -> respawn(e.getEntity()));
        PlayerInteractEvent.RightClickItem.BUS.addListener(e -> {
            return rightClick(e.getEntity(), e.getHand());
        });
        PlayerInteractEvent.RightClickBlock.BUS.addListener(e -> {
            return rightClick(e.getEntity(), e.getHand());
        });
        PlayerInteractEvent.LeftClickBlock.BUS.addListener(e -> {
            return leftClick(e.getEntity(), e.getHand());
        });
        // 26.x : seul l'évènement « Specific » (avec le point visé) existe encore
        //? if >=26.1 {
        PlayerInteractEvent.EntityInteractSpecific.BUS.addListener(e -> {
            return interact(e.getEntity(), e.getHand(), e.getTarget());
        });
        //?} else {
        /^PlayerInteractEvent.EntityInteract.BUS.addListener(e -> {
            return interact(e.getEntity(), e.getHand(), e.getTarget());
        });
        ^///?}
        AttackEntityEvent.BUS.addListener(e -> {
            return attack(e.getEntity(), e.getTarget());
        });
    }
}
*///?}
