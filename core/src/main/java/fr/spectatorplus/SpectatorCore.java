package fr.spectatorplus;

import fr.spectatorplus.api.SpectatorMode;
import fr.spectatorplus.command.SpectatorCommand;
import fr.spectatorplus.config.ConfigFiles;
import fr.spectatorplus.config.Messages;
import fr.spectatorplus.core.CoreContext;
import fr.spectatorplus.core.config.YamlConfig;
import fr.spectatorplus.core.platform.Host;
import fr.spectatorplus.core.platform.Platform;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.event.CombatTracker;
import fr.spectatorplus.event.EventManager;
import fr.spectatorplus.event.StatsManager;
import fr.spectatorplus.event.ZoneManager;
import fr.spectatorplus.event.detect.Signals;
import fr.spectatorplus.filter.FilterManager;
import fr.spectatorplus.game.GameManager;
import fr.spectatorplus.game.TpsMonitor;
import fr.spectatorplus.gui.MenuManager;
import fr.spectatorplus.placeholder.PlaceholderManager;
import fr.spectatorplus.spectator.HotbarManager;
import fr.spectatorplus.spectator.SpectatorInteractions;
import fr.spectatorplus.spectator.SpectatorManager;
import fr.spectatorplus.spectator.SpectatorSession;
import fr.spectatorplus.storage.SqlStorage;
import fr.spectatorplus.storage.Storage;
import fr.spectatorplus.storage.YamlStorage;

import java.util.Locale;
import java.util.logging.Level;

/**
 * Spectator Plus, partie commune à toutes les plateformes : crée et relie tous les services.
 * Le point d'entrée de chaque plateforme (plugin Bukkit, mod Fabric...) crée une instance avec sa
 * {@link Platform}, appelle {@link #enable()} au démarrage et {@link #disable()} à l'arrêt, puis lui
 * transmet les évènements du jeu.
 */
public final class SpectatorCore implements CoreContext {

    private final Platform platform;

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
    private SpectatorInteractions interactions;
    private MenuManager menus;
    private TpsMonitor tps;
    private Signals signals;
    private SpectatorCommand command;

    public SpectatorCore(Platform platform) {
        this.platform = platform;
    }

    /** Chargement de la configuration et création des services (sans démarrer les tâches). */
    public void load() {
        files = new ConfigFiles(platform);
        files.load();
        messages = new Messages(this);
        messages.load();
        readMode();
        initStorage();

        zones = new ZoneManager();
        zones.load(config().getConfigurationSection("zones"));
        stats = new StatsManager();
        combat = new CombatTracker();
        placeholders = new PlaceholderManager(this);
        events = new EventManager(this);
        filters = new FilterManager(this);
        game = new GameManager(this);
        spectators = new SpectatorManager(this);
        hotbar = new HotbarManager(this);
        interactions = new SpectatorInteractions(this);
        menus = new MenuManager(this);
        tps = new TpsMonitor();
        command = new SpectatorCommand(this);
        signals = new Signals(this);

        events.load();
        filters.load();
    }

    /** Démarrage des tâches et restauration des joueurs déjà connectés (après {@link #load()}). */
    public void enable() {
        spectators.start();
        menus.start();
        game.start();
        tps.start(platform);
        signals.start();
        for (PlatformPlayer p : platform.getOnlinePlayers()) filters.loadPlayer(p.getUniqueId());
        spectators.restoreOnline();
    }

    public void disable() {
        try {
            if (menus != null) menus.closeAll();
            if (spectators != null) spectators.shutdown();
            if (filters != null) filters.saveAll();
        } catch (Throwable t) {
            platform.logger().log(Level.WARNING, "Erreur à la désactivation", t);
        }
        if (storage != null) storage.close();
    }

    /** /spec reload */
    public void reload() {
        filters.saveAll();
        files.load();
        messages.load();
        readMode();
        zones.load(config().getConfigurationSection("zones"));
        events.load();
        filters.load();
        for (SpectatorSession s : spectators.getSpectators()) {
            PlatformPlayer p = s.getPlayer();
            if (p != null) {
                spectators.apply(p);
                hotbar.give(p, s);
            }
        }
    }

    private void readMode() {
        try {
            mode = SpectatorMode.valueOf(config().getString("mode", "AUTO").toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            platform.logger().warning("Mode invalide dans config.yml, AUTO utilisé.");
            mode = SpectatorMode.AUTO;
        }
    }

    private void initStorage() {
        String type = config().getString("storage.type", "YAML").toUpperCase(Locale.ROOT);
        Storage s;
        if (type.equals("SQLITE")) s = new SqlStorage(false, config().getConfigurationSection("storage.sqlite"), platform.dataFolder());
        else if (type.equals("MYSQL")) s = new SqlStorage(true, config().getConfigurationSection("storage.mysql"), platform.dataFolder());
        else s = new YamlStorage(platform.dataFolder());
        try {
            s.init();
        } catch (Exception e) {
            platform.logger().log(Level.SEVERE, "Stockage " + type + " indisponible, utilisation du YAML.", e);
            s = new YamlStorage(platform.dataFolder());
            try {
                s.init();
            } catch (Exception ignored) {
            }
        }
        storage = s;
    }

    /** Remise à zéro des compteurs de partie (appelé au début d'une partie). */
    public void resetGameTracking() {
        signals.reset();
    }

    // ------------------------------------------------------------------ accès

    public Platform platform() {
        return platform;
    }

    @Override
    public Host host() {
        return platform;
    }

    @Override
    public ConfigFiles files() {
        return files;
    }

    @Override
    public YamlConfig config() {
        return files.config();
    }

    @Override
    public Messages messages() {
        return messages;
    }

    @Override
    public Storage storage() {
        return storage;
    }

    @Override
    public ZoneManager zones() {
        return zones;
    }

    @Override
    public StatsManager stats() {
        return stats;
    }

    @Override
    public CombatTracker combat() {
        return combat;
    }

    @Override
    public EventManager events() {
        return events;
    }

    @Override
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

    public SpectatorInteractions interactions() {
        return interactions;
    }

    public MenuManager menus() {
        return menus;
    }

    public TpsMonitor tps() {
        return tps;
    }

    /** Détections d'évènements natifs (appelées par les plateformes). */
    public Signals signals() {
        return signals;
    }

    public SpectatorCommand command() {
        return command;
    }

    public SpectatorMode getMode() {
        return mode;
    }

    public void setMode(SpectatorMode mode) {
        this.mode = mode;
    }
}
