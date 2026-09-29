package fr.spectatorplus.mod;

import com.mojang.authlib.GameProfile;
import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.api.EnterReason;
import fr.spectatorplus.core.platform.ApiBridge;
import fr.spectatorplus.core.platform.Platform;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.PlatformWorld;
import fr.spectatorplus.core.platform.PlayerSnapshot;
import fr.spectatorplus.core.platform.Sender;
import fr.spectatorplus.core.platform.Task;
import fr.spectatorplus.gui.Menu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;

/**
 * Plateforme des serveurs moddés (Fabric, NeoForge, Forge) : code Minecraft commun aux loaders.
 * Le loader fournit le serveur, le dossier de configuration et transmet les évènements (voir {@link SpectatorPlusMod}).
 */
public final class ModPlatform implements Platform {

    /** Réglages du spectateur qui n'ont pas d'équivalent vanilla, lus par les mixins. */
    public static final class Flags {
        public volatile boolean pickup = true;
        public volatile boolean collidable = true;
        public volatile boolean sleepIgnored;
        public volatile boolean affectsSpawning = true;
    }

    /** Types de dégâts avec les noms de Bukkit, pour le filtre de dégâts. */
    private static final List<String> DAMAGE_TYPES = Collections.unmodifiableList(Arrays.asList(
            "CONTACT", "ENTITY_ATTACK", "ENTITY_SWEEP_ATTACK", "PROJECTILE", "SUFFOCATION", "FALL", "FIRE", "FIRE_TICK",
            "MELTING", "LAVA", "DROWNING", "BLOCK_EXPLOSION", "ENTITY_EXPLOSION", "VOID", "LIGHTNING", "SUICIDE",
            "STARVATION", "POISON", "MAGIC", "WITHER", "FALLING_BLOCK", "THORNS", "DRAGON_BREATH", "CUSTOM",
            "FLY_INTO_WALL", "HOT_FLOOR", "CAMPFIRE", "CRAMMING", "DRYOUT", "FREEZE", "SONIC_BOOM", "KILL", "WORLD_BORDER"));

    private final MinecraftServer server;
    private final File dataFolder;
    private final Logger logger;
    private final String loaderName;
    private final Map<UUID, ModPlayer> players = new ConcurrentHashMap<>();
    private final Map<UUID, Flags> flags = new ConcurrentHashMap<>();
    private final Map<UUID, String> languages = new ConcurrentHashMap<>();
    private final ModMenus menus = new ModMenus(this);
    private final ModVisibility visibility = new ModVisibility(this);
    private final ModHealthDisplay healthDisplay = new ModHealthDisplay(this);
    private final ModPermissions permissions = new ModPermissions();
    private final List<ScheduledTask> tasks = new ArrayList<>();
    private final ConcurrentLinkedQueue<Runnable> syncQueue = new ConcurrentLinkedQueue<>();
    private final ExecutorService async = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "SpectatorPlus-IO");
            t.setDaemon(true);
            return t;
        }
    });
    private SpectatorCore core;
    private long tick;

    public ModPlatform(MinecraftServer server, File dataFolder, Logger logger, String loaderName) {
        this.server = server;
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.loaderName = loaderName;
        permissions.load(logger);
    }

    void attach(SpectatorCore core) {
        this.core = core;
    }

    public SpectatorCore core() {
        return core;
    }

    public MinecraftServer server() {
        return server;
    }

    public ModPermissions permissions() {
        return permissions;
    }

    public ModVisibility visibility() {
        return visibility;
    }

    public ModHealthDisplay healthDisplay() {
        return healthDisplay;
    }

    public Flags flags(UUID id) {
        Flags f = flags.get(id);
        if (f == null) {
            f = new Flags();
            flags.put(id, f);
        }
        return f;
    }

    /** Réglages d'un joueur s'il en a (null sinon) : utilisé par les mixins. */
    public Flags flagsOrNull(UUID id) {
        return flags.get(id);
    }

    /** Langue envoyée par le client (versions sans ClientInformation). */
    public void setLanguage(UUID id, String language) {
        if (language != null) languages.put(id, language);
    }

    public String language(UUID id) {
        return languages.get(id);
    }

    // ------------------------------------------------------------------ joueurs

    public ModPlayer wrap(ServerPlayer p) {
        if (p == null) return null;
        ModPlayer w = players.get(p.getUUID());
        if (w == null) {
            w = new ModPlayer(this, p);
            players.put(p.getUUID(), w);
        }
        return w;
    }

    public void forget(UUID id) {
        players.remove(id);
        flags.remove(id);
        languages.remove(id);
        menus.forget(id);
        visibility.forget(id);
        healthDisplay.forget(id);
    }

    /** Profil complet (avec skin) d'un joueur connecté, ou null. */
    GameProfile profile(UUID id) {
        ServerPlayer p = id == null ? null : server.getPlayerList().getPlayer(id);
        return p == null ? null : p.getGameProfile();
    }

    public static boolean isFakePlayer(ServerPlayer p) {
        return p.getClass().getName().contains("FakePlayer") || p.connection == null;
    }

    /** Expéditeur d'une commande (joueur ou console / bloc de commande). */
    public Sender sender(final CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer) return wrap((ServerPlayer) source.getEntity());
        return new Sender() {
            @Override
            public String getName() {
                return source.getTextName();
            }

            @Override
            public boolean hasPermission(String permission) {
                return permissions.has(source, permission);
            }

            @Override
            public void sendMessage(String message) {
                //? if >=1.19 {
                source.sendSystemMessage(Texts.of(message));
                //?} else
                /*source.sendSuccess(Texts.of(message), false);*/
            }
        };
    }

    @Override
    public Collection<PlatformPlayer> getOnlinePlayers() {
        List<PlatformPlayer> res = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) res.add(wrap(p));
        return res;
    }

    @Override
    public PlatformPlayer getPlayer(UUID id) {
        return id == null ? null : wrap(server.getPlayerList().getPlayer(id));
    }

    @Override
    public PlatformPlayer findPlayer(String name) {
        ServerPlayer exact = server.getPlayerList().getPlayerByName(name);
        if (exact != null) return wrap(exact);
        String lower = name.toLowerCase(Locale.ROOT);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.getScoreboardName().toLowerCase(Locale.ROOT).startsWith(lower)) return wrap(p);
        }
        return null;
    }

    @Override
    public List<PlatformWorld> getWorlds() {
        List<PlatformWorld> res = new ArrayList<>();
        for (ServerLevel l : server.getAllLevels()) res.add(new ModWorld(l));
        return res;
    }

    @Override
    public PlatformWorld getWorld(String name) {
        ServerLevel l = level(name);
        return l == null ? null : new ModWorld(l);
    }

    public ServerLevel level(String name) {
        if (name == null) return null;
        for (ServerLevel l : server.getAllLevels()) if (Mc.worldName(l).equals(name)) return l;
        return null;
    }

    @Override
    public Sender console() {
        return sender(server.createCommandSourceStack());
    }

    // ------------------------------------------------------------------ Host

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public File dataFolder() {
        return dataFolder;
    }

    @Override
    public InputStream resource(String path) {
        return ModPlatform.class.getClassLoader().getResourceAsStream(path);
    }

    @Override
    public void runSync(Runnable task) {
        syncQueue.add(task);
    }

    @Override
    public void runAsync(Runnable task) {
        if (async.isShutdown()) {
            task.run();
            return;
        }
        async.execute(task);
    }

    @Override
    public boolean isPrimaryThread() {
        return server.isSameThread();
    }

    // ------------------------------------------------------------------ tâches

    private final class ScheduledTask implements Task {
        final Runnable runnable;
        final long period;
        long next;
        boolean cancelled;

        ScheduledTask(Runnable runnable, long delay, long period) {
            this.runnable = runnable;
            this.period = period;
            this.next = tick + Math.max(1, delay);
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }

    @Override
    public Task runTimer(Runnable task, long delay, long period) {
        ScheduledTask t = new ScheduledTask(task, delay, Math.max(1, period));
        tasks.add(t);
        return t;
    }

    @Override
    public Task runLater(Runnable task, long delay) {
        ScheduledTask t = new ScheduledTask(task, delay, 0);
        tasks.add(t);
        return t;
    }

    /** Appelé à la fin de chaque tick du serveur. */
    public void tick() {
        tick++;
        Runnable r;
        while ((r = syncQueue.poll()) != null) run(r);
        for (ScheduledTask t : new ArrayList<>(tasks)) {
            if (t.cancelled || t.next > tick) continue;
            run(t.runnable);
            if (t.period > 0) t.next = tick + t.period;
            else t.cancelled = true;
        }
        if (tick % 10 == 0) healthDisplay.tick();
        Iterator<ScheduledTask> it = tasks.iterator();
        while (it.hasNext()) if (it.next().cancelled) it.remove();
    }

    private void run(Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            logger.log(java.util.logging.Level.WARNING, "Erreur dans une tâche de Spectator Plus", t);
        }
    }

    public void shutdown() {
        async.shutdown();
        tasks.clear();
    }

    @Override
    public double[] serverTps() {
        return null;
    }

    @Override
    public List<String> getDamageTypes() {
        return DAMAGE_TYPES;
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
        return ModSnapshot.capture((ModPlayer) player);
    }

    @Override
    public void saveSnapshot(File file, EnterReason reason, PlayerSnapshot snapshot) throws Exception {
        ((ModSnapshot) snapshot).write(file, reason.name(), server);
    }

    @Override
    public PlayerSnapshot loadSnapshot(File file) {
        return ModSnapshot.read(file, server);
    }

    // ------------------------------------------------------------------ intégrations

    @Override
    public ApiBridge api() {
        return ApiBridge.NONE;
    }

    @Override
    public String externalPlaceholders(String text, UUID subject, PlatformPlayer viewer) {
        return text;
    }

    @Override
    public String describe() {
        return loaderName + " " + server.getServerVersion();
    }
}
