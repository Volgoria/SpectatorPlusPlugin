package fr.spectatorplus.bukkit;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.api.EnterReason;
import fr.spectatorplus.compat.Compat;
import fr.spectatorplus.compat.Reflect;
import fr.spectatorplus.compat.ServerType;
import fr.spectatorplus.core.platform.ApiBridge;
import fr.spectatorplus.core.platform.Platform;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.PlatformWorld;
import fr.spectatorplus.core.platform.PlayerSnapshot;
import fr.spectatorplus.core.platform.Sender;
import fr.spectatorplus.core.platform.Task;
import fr.spectatorplus.gui.Menu;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Plateforme Bukkit (Spigot, Paper et forks, serveurs hybrides) pour le code commun.
 */
public final class BukkitPlatform implements Platform {

    private final JavaPlugin plugin;
    private final Map<UUID, BukkitPlayer> players = new ConcurrentHashMap<>();
    private final BukkitMenus menus = new BukkitMenus(this);
    private final ApiBridge api = new BukkitApiBridge();
    private final BukkitHealthDisplay healthDisplay;
    private SpectatorCore core;
    private Class<?> papi;

    public BukkitPlatform(JavaPlugin plugin) {
        this.plugin = plugin;
        this.healthDisplay = new BukkitHealthDisplay(plugin);
    }

    /** Appelé une fois le code commun créé. */
    public void attach(SpectatorCore core) {
        this.core = core;
        Bukkit.getPluginManager().registerEvents(menus, plugin);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papi = Reflect.getClass("me.clip.placeholderapi.PlaceholderAPI");
            if (papi != null) plugin.getLogger().info("PlaceholderAPI détecté : placeholders %...% activés.");
        }
    }

    BukkitHealthDisplay healthDisplay() {
        return healthDisplay;
    }

    /** À l'arrêt du plugin : les spectateurs retrouvent leur scoreboard. */
    public void shutdown() {
        healthDisplay.shutdown();
    }

    SpectatorCore core() {
        return core;
    }

    // ------------------------------------------------------------------ joueurs

    /** Joueur du code commun correspondant à un joueur Bukkit (null si null). */
    public BukkitPlayer wrap(Player p) {
        if (p == null) return null;
        BukkitPlayer w = players.get(p.getUniqueId());
        if (w == null) {
            w = new BukkitPlayer(plugin, this, p);
            players.put(p.getUniqueId(), w);
        }
        return w;
    }

    /** Expéditeur du code commun (joueur ou console). */
    public Sender sender(final CommandSender sender) {
        if (sender instanceof Player) return wrap((Player) sender);
        return new Sender() {
            @Override
            public String getName() {
                return sender.getName();
            }

            @Override
            public boolean hasPermission(String permission) {
                return sender.hasPermission(permission);
            }

            @Override
            public void sendMessage(String message) {
                sender.sendMessage(message);
            }
        };
    }

    /**
     * À la déconnexion : le cache est vidé au tick suivant, une fois que tous les listeners
     * (qui peuvent encore demander le joueur) ont été appelés.
     */
    public void forget(final UUID id) {
        menus.forget(id);
        healthDisplay.forget(id);
        runLater(new Runnable() {
            @Override
            public void run() {
                if (Bukkit.getPlayer(id) == null) players.remove(id);
            }
        }, 1L);
    }

    @Override
    public Collection<PlatformPlayer> getOnlinePlayers() {
        List<PlatformPlayer> res = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) res.add(wrap(p));
        return res;
    }

    @Override
    public PlatformPlayer getPlayer(UUID id) {
        return id == null ? null : wrap(Bukkit.getPlayer(id));
    }

    @Override
    @SuppressWarnings("deprecation")
    public PlatformPlayer findPlayer(String name) {
        Player t = Bukkit.getPlayerExact(name);
        if (t == null) t = Bukkit.getPlayer(name);
        return wrap(t);
    }

    @Override
    public List<PlatformWorld> getWorlds() {
        List<PlatformWorld> res = new ArrayList<>();
        for (World w : Bukkit.getWorlds()) res.add(new BukkitWorld(w));
        return res;
    }

    @Override
    public PlatformWorld getWorld(String name) {
        World w = name == null ? null : Bukkit.getWorld(name);
        return w == null ? null : new BukkitWorld(w);
    }

    @Override
    public Sender console() {
        return sender(Bukkit.getConsoleSender());
    }

    // ------------------------------------------------------------------ Host

    @Override
    public Logger logger() {
        return plugin.getLogger();
    }

    @Override
    public File dataFolder() {
        return plugin.getDataFolder();
    }

    @Override
    public InputStream resource(String path) {
        return plugin.getResource(path);
    }

    @Override
    public void runSync(Runnable task) {
        if (!plugin.isEnabled()) return;
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runAsync(Runnable task) {
        if (!plugin.isEnabled()) {
            task.run();
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public boolean isPrimaryThread() {
        return Bukkit.isPrimaryThread();
    }

    // ------------------------------------------------------------------ tâches

    private static Task task(final BukkitTask t) {
        return new Task() {
            @Override
            public void cancel() {
                t.cancel();
            }
        };
    }

    @Override
    public Task runTimer(Runnable task, long delay, long period) {
        return task(Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period));
    }

    @Override
    public Task runLater(Runnable task, long delay) {
        return task(Bukkit.getScheduler().runTaskLater(plugin, task, delay));
    }

    @Override
    public double[] serverTps() {
        return Compat.serverTps();
    }

    @Override
    public List<String> getDamageTypes() {
        List<String> res = new ArrayList<>();
        for (EntityDamageEvent.DamageCause c : EntityDamageEvent.DamageCause.values()) res.add(c.name());
        return res;
    }

    // ------------------------------------------------------------------ menus

    @Override
    public void openMenu(PlatformPlayer viewer, Menu menu) {
        menus.open(viewer, menu);
    }

    @Override
    public void refreshMenu(PlatformPlayer viewer, Menu menu) {
        menus.refresh(viewer, menu);
    }

    // ------------------------------------------------------------------ état des spectateurs

    @Override
    public PlayerSnapshot capture(PlatformPlayer player) {
        return SavedState.capture(BukkitPlayer.unwrap(player));
    }

    @Override
    public void saveSnapshot(File file, EnterReason reason, PlayerSnapshot snapshot) throws Exception {
        SavedState.write(file, reason.name(), (SavedState) snapshot);
    }

    @Override
    public PlayerSnapshot loadSnapshot(File file) {
        return SavedState.read(file);
    }

    // ------------------------------------------------------------------ intégrations

    @Override
    public ApiBridge api() {
        return api;
    }

    @Override
    public String externalPlaceholders(String text, UUID subject, PlatformPlayer viewer) {
        if (papi == null) return text;
        OfflinePlayer target = subject != null ? Bukkit.getOfflinePlayer(subject) : BukkitPlayer.unwrap(viewer);
        Object r = Reflect.invokeStatic(papi, "setPlaceholders", target, text);
        return r instanceof String ? (String) r : text;
    }

    @Override
    public String describe() {
        return ServerType.describe();
    }
}
