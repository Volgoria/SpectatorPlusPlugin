package fr.spectatorplus;

import fr.spectatorplus.api.EnterReason;
import fr.spectatorplus.api.LeaveReason;
import fr.spectatorplus.api.Spectator;
import fr.spectatorplus.api.SpectatorMode;
import fr.spectatorplus.api.SpectatorPlusAPI;
import fr.spectatorplus.api.SpectatorPlusProvider;
import fr.spectatorplus.api.event.EventService;
import fr.spectatorplus.api.filter.FilterService;
import fr.spectatorplus.api.game.GameService;
import fr.spectatorplus.api.placeholder.PlaceholderService;
import fr.spectatorplus.bukkit.BukkitEventService;
import fr.spectatorplus.bukkit.BukkitFilterService;
import fr.spectatorplus.bukkit.BukkitGameService;
import fr.spectatorplus.bukkit.BukkitPlaceholderService;
import fr.spectatorplus.bukkit.BukkitPlatform;
import fr.spectatorplus.bukkit.BukkitPlayer;
import fr.spectatorplus.bukkit.BukkitSpectator;
import fr.spectatorplus.compat.PaperHooks;
import fr.spectatorplus.compat.Positions;
import fr.spectatorplus.compat.ServerType;
import fr.spectatorplus.config.Messages;
import fr.spectatorplus.core.config.YamlConfig;
import fr.spectatorplus.event.CombatTracker;
import fr.spectatorplus.event.EventManager;
import fr.spectatorplus.event.StatsManager;
import fr.spectatorplus.event.ZoneManager;
import fr.spectatorplus.event.detector.CombatDetector;
import fr.spectatorplus.event.detector.ItemDetector;
import fr.spectatorplus.event.detector.MiningDetector;
import fr.spectatorplus.event.detector.PlayerDetector;
import fr.spectatorplus.event.detector.WorldDetector;
import fr.spectatorplus.filter.FilterManager;
import fr.spectatorplus.game.GameManager;
import fr.spectatorplus.game.TpsMonitor;
import fr.spectatorplus.gui.MenuManager;
import fr.spectatorplus.placeholder.PlaceholderManager;
import fr.spectatorplus.spectator.HotbarManager;
import fr.spectatorplus.spectator.SpectatorListener;
import fr.spectatorplus.spectator.SpectatorManager;
import fr.spectatorplus.storage.Storage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spectator Plus — système spectateur avancé pour serveurs Minecraft 1.8 → 26.x.
 * Point d'entrée Bukkit : crée la plateforme Bukkit et le code commun ({@link SpectatorCore}),
 * branche les évènements du serveur et expose l'API publique.
 */
public final class SpectatorPlus extends JavaPlugin implements SpectatorPlusAPI {

    private BukkitPlatform platform;
    private SpectatorCore core;
    private BukkitEventService eventService;
    private BukkitFilterService filterService;
    private BukkitPlaceholderService placeholderService;
    private BukkitGameService gameService;

    @Override
    public void onEnable() {
        if (ServerType.isFolia()) {
            getLogger().severe("Folia n'est pas supporté (planificateur Bukkit indisponible). Utilisez Paper, Spigot ou un fork de Paper.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        platform = new BukkitPlatform(this);
        core = new SpectatorCore(platform);
        core.load();
        platform.attach(core);
        eventService = new BukkitEventService(core);
        filterService = new BukkitFilterService(core, platform);
        placeholderService = new BukkitPlaceholderService(core, platform);
        gameService = new BukkitGameService(core, platform);

        PluginManager pm = Bukkit.getPluginManager();
        SpectatorListener listener = new SpectatorListener(this);
        pm.registerEvents(listener, this);
        listener.registerDynamic();

        pm.registerEvents(new WorldDetector(this), this);
        pm.registerEvents(new MiningDetector(this), this);
        PlayerDetector playerDetector = new PlayerDetector(this);
        pm.registerEvents(playerDetector, this);
        playerDetector.registerDynamic();
        pm.registerEvents(new CombatDetector(this), this);
        ItemDetector itemDetector = new ItemDetector(this);
        pm.registerEvents(itemDetector, this);
        itemDetector.registerDynamic();

        PaperHooks paperHooks = new PaperHooks(this);
        paperHooks.register();

        PluginCommand pc = getCommand("spectatorplus");
        if (pc != null) {
            TabExecutor executor = new TabExecutor() {
                @Override
                public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                    return core.command().execute(platform.sender(sender), label, args);
                }

                @Override
                public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
                    return core.command().complete(platform.sender(sender), args);
                }
            };
            pc.setExecutor(executor);
            pc.setTabCompleter(executor);
        }

        core.enable();

        SpectatorPlusProvider.register(this);
        Bukkit.getServicesManager().register(SpectatorPlusAPI.class, this, this, ServicePriority.Normal);

        if (!paperHooks.getActive().isEmpty()) getLogger().info("API Paper utilisée : " + String.join(", ", paperHooks.getActive()));
        if (ServerType.isHybrid()) {
            getLogger().info("Serveur hybride " + ServerType.loader() + " détecté : joueurs fictifs des mods ignorés, "
                    + "inventaires de mods bloqués pour les spectateurs.");
        }
        getLogger().info("Spectator Plus " + getDescription().getVersion() + " activé (" + ServerType.describe()
                + ", mode " + core.getMode() + ", stockage " + core.storage().name() + ", "
                + core.events().getTypes().size() + " évènements).");
    }

    @Override
    public void onDisable() {
        if (core != null) core.disable();
        if (platform != null) platform.shutdown();
        Bukkit.getServicesManager().unregisterAll(this);
        SpectatorPlusProvider.register(null);
    }

    // ------------------------------------------------------------------ accès internes (listeners, détecteurs)

    public SpectatorCore core() {
        return core;
    }

    public BukkitPlatform platform() {
        return platform;
    }

    /** Joueur du code commun. */
    public BukkitPlayer wrap(Player p) {
        return platform.wrap(p);
    }

    public void reload() {
        core.reload();
    }

    public YamlConfig config() {
        return core.config();
    }

    public Messages messages() {
        return core.messages();
    }

    public Storage storage() {
        return core.storage();
    }

    public ZoneManager zones() {
        return core.zones();
    }

    public StatsManager stats() {
        return core.stats();
    }

    public CombatTracker combat() {
        return core.combat();
    }

    public EventManager events() {
        return core.events();
    }

    public FilterManager filters() {
        return core.filters();
    }

    public PlaceholderManager placeholders() {
        return core.placeholders();
    }

    public GameManager game() {
        return core.game();
    }

    public SpectatorManager spectators() {
        return core.spectators();
    }

    public HotbarManager hotbar() {
        return core.hotbar();
    }

    public MenuManager menus() {
        return core.menus();
    }

    public TpsMonitor tps() {
        return core.tps();
    }

    // ------------------------------------------------------------------ API

    @Override
    public boolean isSpectator(Player player) {
        return player != null && core.spectators().isSpectator(player.getUniqueId());
    }

    @Override
    public boolean isSpectator(UUID id) {
        return core.spectators().isSpectator(id);
    }

    @Override
    public Collection<? extends Spectator> getSpectators() {
        return BukkitSpectator.all(platform, core.spectators().getSpectators());
    }

    @Override
    public Spectator getSpectator(Player player) {
        return player == null ? null : getSpectator(player.getUniqueId());
    }

    @Override
    public Spectator getSpectator(UUID id) {
        return BukkitSpectator.of(platform, core.spectators().getSpectator(id));
    }

    @Override
    public boolean enterSpectator(Player player, EnterReason reason) {
        return enterSpectator(player, reason, null);
    }

    @Override
    public boolean enterSpectator(Player player, EnterReason reason, Location location) {
        if (!config().getBoolean("api.allow-enter-leave", true)) return false;
        return core.spectators().enter(wrap(player), reason == null ? EnterReason.API : reason, Positions.of(location),
                core.getMode() == SpectatorMode.AUTO);
    }

    @Override
    public boolean leaveSpectator(Player player, LeaveReason reason) {
        if (!config().getBoolean("api.allow-enter-leave", true)) return false;
        return core.spectators().leave(wrap(player), reason == null ? LeaveReason.API : reason, false);
    }

    @Override
    public SpectatorMode getMode() {
        return core.getMode();
    }

    @Override
    public void setMode(SpectatorMode mode) {
        core.setMode(mode);
    }

    @Override
    public EventService getEventService() {
        return eventService;
    }

    @Override
    public PlaceholderService getPlaceholderService() {
        return placeholderService;
    }

    @Override
    public FilterService getFilterService() {
        return filterService;
    }

    @Override
    public GameService getGameService() {
        return gameService;
    }
}
