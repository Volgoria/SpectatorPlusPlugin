package fr.spectatorplus.mod.forge;

//? if forge {
/*import fr.spectatorplus.mod.ModLogger;
import fr.spectatorplus.mod.SpectatorPlusMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.util.logging.Logger;

/^*
 * Point d'entrée du mod Forge (server-side uniquement : les joueurs gardent un client vanilla).
 * Transmet au code commun le cycle de vie du serveur, les connexions et les interactions ;
 * l'enregistrement auprès du bus d'évènements dépend de la version (voir ForgeEvents).
 ^/
@Mod("spectatorplus")
public class SpectatorPlusForge {

    static final Logger LOGGER = ModLogger.create();
    static File configDir;

    public SpectatorPlusForge() {
        configDir = FMLPaths.CONFIGDIR.get().resolve("spectatorplus").toFile();
        ForgeEvents.register();
    }

    static ServerPlayer server(Player p) {
        return p instanceof ServerPlayer ? (ServerPlayer) p : null;
    }

    /^* Clic droit (objet ou bloc) : true pour annuler (spectateur). ^/
    static boolean rightClick(Player player, InteractionHand hand) {
        ServerPlayer p = server(player);
        if (p == null) return false;
        boolean spectator = SpectatorPlusMod.isSpectator(p);
        if (hand == InteractionHand.MAIN_HAND) SpectatorPlusMod.useItem(p, false);
        return spectator;
    }

    /^* Clic gauche sur un bloc : true pour annuler (spectateur). ^/
    static boolean leftClick(Player player, InteractionHand hand) {
        ServerPlayer p = server(player);
        if (p == null || !SpectatorPlusMod.isSpectator(p)) return false;
        if (hand == InteractionHand.MAIN_HAND) SpectatorPlusMod.useItem(p, true);
        return true;
    }

    /^* Clic droit sur une entité : true pour annuler (spectateur). ^/
    static boolean interact(Player player, InteractionHand hand, Entity target) {
        ServerPlayer p = server(player);
        if (p == null || !SpectatorPlusMod.isSpectator(p)) return false;
        if (hand == InteractionHand.MAIN_HAND) SpectatorPlusMod.interactEntity(p, target, false);
        return true;
    }

    /^* Attaque d'une entité : true pour annuler (spectateur). ^/
    static boolean attack(Player player, Entity target) {
        ServerPlayer p = server(player);
        return p != null && SpectatorPlusMod.interactEntity(p, target, true);
    }

    static void join(Player p) {
        if (server(p) != null) SpectatorPlusMod.join(server(p));
    }

    static void quit(Player p) {
        if (server(p) != null) SpectatorPlusMod.quit(server(p));
    }

    static void respawn(Player p) {
        if (server(p) != null) SpectatorPlusMod.respawn(server(p));
    }
}
*///?}
