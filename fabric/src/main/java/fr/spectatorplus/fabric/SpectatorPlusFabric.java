package fr.spectatorplus.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
//? if >=1.18 {
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//?} else {
/*import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
*///?}

/**
 * Point d'entrée du mod Fabric (server-side uniquement : les joueurs gardent un client vanilla).
 */
public class SpectatorPlusFabric implements ModInitializer {

    // SLF4J n'est fourni par Minecraft qu'à partir de 1.18 ; avant, Log4j directement
    //? if >=1.18 {
    public static final Logger LOGGER = LoggerFactory.getLogger("SpectatorPlus");
    //?} else
    /*public static final Logger LOGGER = LogManager.getLogger("SpectatorPlus");*/

    private static MinecraftServer server;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(new ServerLifecycleEvents.ServerStarted() {
            @Override
            public void onServerStarted(MinecraftServer s) {
                server = s;
                LOGGER.info("Spectator Plus (Fabric) activé.");
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(new ServerLifecycleEvents.ServerStopping() {
            @Override
            public void onServerStopping(MinecraftServer s) {
                server = null;
            }
        });
    }

    public static MinecraftServer server() {
        return server;
    }
}
