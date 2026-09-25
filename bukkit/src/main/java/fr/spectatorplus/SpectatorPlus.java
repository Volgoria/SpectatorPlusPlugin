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
import fr.spectatorplus.command.SpectatorCommand;
import fr.spectatorplus.compat.PaperHooks;
import fr.spectatorplus.compat.Platform;
import fr.spectatorplus.config.ConfigFiles;
import fr.spectatorplus.config.Messages;
import fr.spectatorplus.event.CombatTracker;
import fr.spectatorplus.event.EventManager;
import fr.spectatorplus.event.StatsManager;
import fr.spectatorplus.event.ZoneManager;
import fr.spectatorplus.event.detector.CombatDetector;
import fr.spectatorplus.event.detector.ItemDetector;
import fr.spectatorplus.event.detector.MiningDetector;
import fr.spectatorplus.event.detector.PlayerDetector;
import fr.spectatorplus.event.detector.PollingDetector;
import fr.spectatorplus.event.detector.WorldDetector;
import fr.spectatorplus.filter.FilterManager;
import fr.spectatorplus.game.GameManager;
import fr.spectatorplus.game.TpsMonitor;
import fr.spectatorplus.gui.MenuManager;
import fr.spectatorplus.placeholder.PlaceholderManager;
import fr.spectatorplus.spectator.HotbarManager;
import fr.spectatorplus.spectator.SpectatorListener;
import fr.spectatorplus.spectator.SpectatorManager;
import fr.spectatorplus.spectator.SpectatorSession;
import fr.spectatorplus.storage.SqlStorage;
import fr.spectatorplus.storage.Storage;
import fr.spectatorplus.storage.YamlStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Spectator Plus — système spectateur avancé pour serveurs Minecraft 1.8 → 26.x.
 */
public final class SpectatorPlus extends JavaPlugin implements SpectatorPlusAPI {

    private ConfigFiles files;
    private Messages messages;
    private Storage storage;
    private SpectatorMode mode = SpectatorMode.AUTO;

    private ZoneManager zones;
    private StatsManager stats;
    private CombatTracker combat;
    private EventManager events;
    private FilterManager filters;
    private PlaceholderManager placeholders;
    private GameManager game;
    private SpectatorManager spectators;
    private HotbarManager hotbar;
    private MenuManager menus;
    private TpsMonitor tps;
    private PlayerDetector playerDetector;
    private PollingDetector pollingDetector;

    @Override
    public void onEnable() {
        if (Platform.isFolia()) {
            getLogger().severe("Folia n'est pas supporté (planificateur Bukkit indisponible). Utilisez Paper, Spigot ou un fork de Paper.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        files = new ConfigFiles(this);
        files.load();
        messages = new Messages(this);
        messages.load();
        readMode();
        initStorage();

        zones = new ZoneManager();
        zones.load(getConfig().getConfigurationSection("zones"));
        stats = new StatsManager();
        combat = new CombatTracker();
        placeholders = new PlaceholderManager(this);
        events = new EventManager(this);
        filters = new FilterManager(this);
        game = new GameManager(this);
        spectators = new SpectatorManager(this);
        hotbar = new HotbarManager(this);
        menus = new MenuManager(this);
        tps = new TpsMonitor();

        events.load();
        filters.load();
        placeholders.hookPlaceholderApi();

        PluginManager pm = Bukkit.getPluginManager();
        SpectatorListener listener = new SpectatorListener(this);
        pm.registerEvents(listener, this);
        listener.registerDynamic();
        pm.registerEvents(menus, this);

        pm.registerEvents(new WorldDetector(this), this);
        pm.registerEvents(new MiningDetector(this), this);
        playerDetector = new PlayerDetector(this);
        pm.registerEvents(playerDetector, this);
        playerDetector.registerDynamic();
        CombatDetector combatDetector = new CombatDetector(this);
        pm.registerEvents(combatDetector, this);
        combatDetector.start();
        ItemDetector itemDetector = new ItemDetector(this);
        pm.registerEvents(itemDetector, this);
        itemDetector.registerDynamic();
        pollingDetector = new PollingDetector(this);
        pm.registerEvents(pollingDetector, this);
        pollingDetector.start();

        PaperHooks paperHooks = new PaperHooks(this);
        paperHooks.register();

        SpectatorCommand command = new SpectatorCommand(this);
        PluginCommand pc = getCommand("spectatorplus");
        if (pc != null) {
            pc.setExecutor(command);
            pc.setTabCompleter(command);
        }

        spectators.start();
        menus.start();
        game.start();
        tps.start(this);

        for (Player p : Bukkit.getOnlinePlayers()) filters.loadPlayer(p.getUniqueId());
        spectators.restoreOnline();

        SpectatorPlusProvider.register(this);
        Bukkit.getServicesManager().register(SpectatorPlusAPI.class, this, this, ServicePriority.Normal);

        if (!paperHooks.getActive().isEmpty()) getLogger().info("API Paper utilisée : " + String.join(", ", paperHooks.getActive()));
        if (Platform.isHybrid()) {
            getLogger().info("Serveur hybride " + Platform.loader() + " détecté : joueurs fictifs des mods ignorés, "
                    + "inventaires de mods bloqués pour les spectateurs.");
        }
        getLogger().info("Spectator Plus " + getDescription().getVersion() + " activé (" + Platform.describe()
                + ", mode " + mode + ", stockage " + storage.name() + ", " + events.getTypes().size() + " évènements).");
    }

    @Override
    public void onDisable() {
        try {
            if (menus != null) menus.closeAll();
            if (spectators != null) spectators.shutdown();
            if (filters != null) filters.saveAll();
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "Erreur à la désactivation", t);
        }
        if (storage != null) storage.close();
        Bukkit.getServicesManager().unregisterAll(this);
        SpectatorPlusProvider.register(null);
    }

    /** /spec reload */
    public void reload() {
        filters.saveAll();
        files.load();
        messages.load();
        readMode();
        zones.load(getConfig().getConfigurationSection("zones"));
        events.load();
        filters.load();
        for (SpectatorSession s : spectators.getSpectators()) {
            Player p = s.getPlayer();
            if (p != null) {
                spectators.apply(p);
                hotbar.give(p, s);
            }
        }
    }

    private void readMode() {
        try {
            mode = SpectatorMode.valueOf(getConfig().getString("mode", "AUTO").toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            getLogger().warning("Mode invalide dans config.yml, AUTO utilisé.");
            mode = SpectatorMode.AUTO;
        }
    }

    private void initStorage() {
        String type = getConfig().getString("storage.type", "YAML").toUpperCase(Locale.ROOT);
        Storage s;
        if (type.equals("SQLITE")) s = new SqlStorage(false, getConfig().getConfigurationSection("storage.sqlite"), getDataFolder());
        else if (type.equals("MYSQL")) s = new SqlStorage(true, getConfig().getConfigurationSection("storage.mysql"), getDataFolder());
        else s = new YamlStorage(getDataFolder());
        try {
            s.init();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Stockage " + type + " indisponible, utilisation du YAML.", e);
            s = new YamlStorage(getDataFolder());
            try {
                s.init();
            } catch (Exception ignored) {
            }
        }
        storage = s;
    }

    // ------------------------------------------------------------------ accès internes

    @Override
    public FileConfiguration getConfig() {
        return files == null ? super.getConfig() : files.config();
    }

    public ConfigFiles files() {
        return files;
    }

    public Messages messages() {
        return messages;
    }

    public Storage storage() {
        return storage;
    }

    public ZoneManager zones() {
        return zones;
    }

    public StatsManager stats() {
        return stats;
    }

    public CombatTracker combat() {
        return combat;
    }

    public EventManager events() {
        return events;
    }

    public FilterManager filters() {
        return filters;
    }

    public PlaceholderManager placeholders() {
        return placeholders;
    }

    public GameManager game() {
        return game;
    }

    public SpectatorManager spectators() {
        return spectators;
    }

    public HotbarManager hotbar() {
        return hotbar;
    }

    public MenuManager menus() {
        return menus;
    }

    public TpsMonitor tps() {
        return tps;
    }

    /** Remise à zéro des compteurs de partie (appelé au début d'une partie). */
    public void resetGameTracking() {
        if (playerDetector != null) playerDetector.reset();
        if (pollingDetector != null) pollingDetector.reset();
    }

    // ------------------------------------------------------------------ API

    @Override
    public boolean isSpectator(Player player) {
        return spectators.isSpectator(player);
    }

    @Override
    public boolean isSpectator(UUID id) {
        return spectators.isSpectator(id);
    }

    @Override
    public Collection<? extends Spectator> getSpectators() {
        return spectators.getSpectators();
    }

    @Override
    public Spectator getSpectator(Player player) {
        return spectators.getSpectator(player);
    }

    @Override
    public Spectator getSpectator(UUID id) {
        return spectators.getSpectator(id);
    }

    @Override
    public boolean enterSpectator(Player player, EnterReason reason) {
        return enterSpectator(player, reason, null);
    }

    @Override
    public boolean enterSpectator(Player player, EnterReason reason, Location location) {
        if (!getConfig().getBoolean("api.allow-enter-leave", true)) return false;
        return spectators.enter(player, reason == null ? EnterReason.API : reason, location, mode == SpectatorMode.AUTO);
    }

    @Override
    public boolean leaveSpectator(Player player, LeaveReason reason) {
        if (!getConfig().getBoolean("api.allow-enter-leave", true)) return false;
        return spectators.leave(player, reason == null ? LeaveReason.API : reason, false);
    }

    @Override
    public SpectatorMode getMode() {
        return mode;
    }

    @Override
    public void setMode(SpectatorMode mode) {
        this.mode = mode;
    }

    @Override
    public EventService getEventService() {
        return events;
    }

    @Override
    public PlaceholderService getPlaceholderService() {
        return placeholders;
    }

    @Override
    public FilterService getFilterService() {
        return filters;
    }

    @Override
    public GameService getGameService() {
        return game;
    }
}
