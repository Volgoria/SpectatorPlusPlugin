package fr.spectatorplus.compat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Détection du logiciel serveur.
 * <ul>
 *     <li>Spigot / CraftBukkit : base, toujours supportée (1.8 → 26.x)</li>
 *     <li>Paper (PaperSpigot 1.8, Paper 1.9+) et forks (Purpur, Pufferfish...) : API Paper utilisée quand elle existe</li>
 *     <li>Serveurs hybrides (plugins Bukkit + mods) : Mohist / Youer / Arclight / Magma / Ketting / CatServer (Forge, NeoForge),
 *     Banner / Cardboard / Arclight Fabric (Fabric)</li>
 *     <li>Folia : non supporté (planificateur Bukkit indisponible)</li>
 * </ul>
 */
public final class Platform {

    public enum Loader {NONE, FORGE, NEOFORGE, FABRIC}

    private static final boolean PAPER;
    private static final boolean FOLIA;
    private static final Loader LOADER;
    private static final String HYBRID;
    private static final String SOFTWARE;

    static {
        FOLIA = Reflect.classExists("io.papermc.paper.threadedregions.RegionizedServer");
        PAPER = FOLIA
                || Reflect.classExists("io.papermc.paper.configuration.Configuration")   // 1.19+
                || Reflect.classExists("com.destroystokyo.paper.PaperConfig")            // 1.9 - 1.18
                || Reflect.classExists("org.github.paperspigot.PaperSpigotConfig");      // 1.8 PaperSpigot

        if (Reflect.classExists("net.neoforged.fml.common.Mod") || Reflect.classExists("net.neoforged.neoforge.common.NeoForge")) {
            LOADER = Loader.NEOFORGE;
        } else if (Reflect.classExists("net.minecraftforge.common.MinecraftForge")
                || Reflect.classExists("net.minecraftforge.fml.common.Mod")) {
            LOADER = Loader.FORGE;
        } else if (Reflect.classExists("net.fabricmc.loader.api.FabricLoader")) {
            LOADER = Loader.FABRIC;
        } else {
            LOADER = Loader.NONE;
        }

        String hybrid = null;
        String[][] known = {
                {"com.mohistmc.youer.Youer", "Youer"},
                {"com.mohistmc.banner.BannerMCStart", "Banner"},
                {"com.mohistmc.MohistMC", "Mohist"},
                {"io.izzel.arclight.api.Arclight", "Arclight"},
                {"io.izzel.arclight.common.mod.ArclightMod", "Arclight"},
                {"org.magmafoundation.magma.Magma", "Magma"},
                {"org.kettingpowered.ketting.core.Ketting", "Ketting"},
                {"catserver.server.CatServer", "CatServer"},
                {"org.cardboardpowered.CardboardMod", "Cardboard"},
                {"io.github.crucible.Crucible", "Crucible"},
                {"thermos.Thermos", "Thermos"}};
        for (String[] k : known) {
            if (Reflect.classExists(k[0])) {
                hybrid = k[1];
                break;
            }
        }
        if (hybrid == null && LOADER != Loader.NONE) hybrid = safeServerName();
        HYBRID = hybrid;

        String name = safeServerName();
        if (HYBRID != null) name = HYBRID + " (" + LOADER.name().charAt(0) + LOADER.name().substring(1).toLowerCase(Locale.ROOT) + ")";
        else if (FOLIA) name = "Folia";
        else if (Reflect.classExists("org.purpurmc.purpur.PurpurConfig")) name = "Purpur";
        else if (PAPER) name = "Paper";
        SOFTWARE = name;
    }

    private Platform() {
    }

    private static String safeServerName() {
        try {
            return Bukkit.getName();
        } catch (Throwable t) {
            return "Bukkit";
        }
    }

    /** Paper ou un fork de Paper (API Paper potentiellement disponible selon la version). */
    public static boolean isPaper() {
        return PAPER;
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    /** Serveur hybride : plugins Bukkit exécutés au-dessus de Forge, NeoForge ou Fabric. */
    public static boolean isHybrid() {
        return HYBRID != null;
    }

    public static Loader loader() {
        return LOADER;
    }

    public static String describe() {
        return SOFTWARE + " " + Version.asString();
    }

    /**
     * Joueurs fictifs créés par des mods (machines, tourelles...) sur les serveurs hybrides :
     * ils ne doivent ni générer d'évènements, ni compter comme joueurs en vie.
     */
    public static boolean isFakePlayer(Player p) {
        if (p == null) return true;
        if (!isHybrid()) return false;
        String cls = p.getClass().getName();
        if (cls.contains("FakePlayer")) return true;
        Object handle = Reflect.invoke(p, "getHandle");
        if (handle != null && handle.getClass().getName().contains("FakePlayer")) return true;
        try {
            return p.getAddress() == null;
        } catch (Throwable t) {
            return false;
        }
    }
}
