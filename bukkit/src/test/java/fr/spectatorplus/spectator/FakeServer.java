package fr.spectatorplus.spectator;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.core.platform.EffectInfo;
import fr.spectatorplus.core.platform.GameMode;
import fr.spectatorplus.core.platform.ItemRef;
import fr.spectatorplus.core.platform.Platform;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.PlatformWorld;
import fr.spectatorplus.core.platform.PlayerSnapshot;
import fr.spectatorplus.core.platform.Position;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Serveur de test : une plateforme minimale (mondes vides, tâches différées exécutées à la demande) et des joueurs
 * qui retiennent leur mode de jeu et leurs effets. Comme sur Bukkit, un changement de mode de jeu prévient
 * Spectator Plus (PlayerGameModeChangeEvent → SpectatorManager#externalGameModeChange).
 */
final class FakeServer {

    final Map<UUID, PlatformPlayer> players = new LinkedHashMap<>();
    final List<Runnable> later = new ArrayList<>();
    final SpectatorCore core;

    FakeServer() throws Exception {
        final File folder = Files.createTempDirectory("spectatorplus-test").toFile();
        folder.deleteOnExit();
        final PlatformWorld world = (PlatformWorld) proxy(PlatformWorld.class, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method m, Object[] args) {
                if (m.getName().equals("getName")) return "world";
                if (m.getName().equals("getSpawn")) return new Position("world", 0, 64, 0);
                return defaultValue(m);
            }
        });
        Platform platform = (Platform) proxy(Platform.class, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method m, Object[] args) {
                switch (m.getName()) {
                    case "logger":
                        return Logger.getLogger("SpectatorPlusTest");
                    case "dataFolder":
                        return folder;
                    case "resource":
                        return SpectatorCore.class.getClassLoader().getResourceAsStream((String) args[0]);
                    case "runSync":
                        ((Runnable) args[0]).run();
                        return null;
                    case "isPrimaryThread":
                        return true;
                    case "getOnlinePlayers":
                        return new ArrayList<>(players.values());
                    case "getPlayer":
                        return players.get(args[0]);
                    case "findPlayer":
                        for (PlatformPlayer pl : players.values()) {
                            if (pl.getName().equalsIgnoreCase((String) args[0])) return pl;
                        }
                        return null;
                    case "getWorld":
                        return world;
                    case "getWorlds":
                        return Collections.singletonList(world);
                    case "runLater":
                        later.add((Runnable) args[0]);
                        return null;
                    case "capture":
                        return proxy(PlayerSnapshot.class, new InvocationHandler() {
                            @Override
                            public Object invoke(Object proxy, Method m, Object[] args) {
                                return defaultValue(m);
                            }
                        });
                    case "api":
                        return proxy(fr.spectatorplus.core.platform.ApiBridge.class, new InvocationHandler() {
                            @Override
                            public Object invoke(Object proxy, Method m, Object[] args) {
                                if (m.getReturnType() == boolean.class) return true;
                                if (m.getName().equals("teleport")) return args[2];
                                return null;
                            }
                        });
                    case "externalPlaceholders":
                        return args[0];
                    case "serverTps":
                        return new double[]{20, 20, 20};
                    default:
                        return defaultValue(m);
                }
            }
        });
        core = new SpectatorCore(platform);
        core.load();
    }

    /** Exécute les tâches différées (runLater). */
    void tick() {
        List<Runnable> tasks = new ArrayList<>(later);
        later.clear();
        for (Runnable r : tasks) r.run();
    }

    /** Joueur connecté, en survie. */
    PlatformPlayer join(final String name) {
        final UUID id = UUID.nameUUIDFromBytes(name.getBytes());
        final GameMode[] mode = {GameMode.SURVIVAL};
        final List<EffectInfo> effects = new ArrayList<>();
        final boolean[] flags = new boolean[2]; // allowFlight, flying
        PlatformPlayer p = (PlatformPlayer) proxy(PlatformPlayer.class, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method m, Object[] args) {
                switch (m.getName()) {
                    case "getUniqueId":
                        return id;
                    case "getName":
                    case "toString":
                        return name;
                    case "equals":
                        return proxy == args[0];
                    case "hashCode":
                        return id.hashCode();
                    case "isOnline":
                    case "hasPermission":
                        return true;
                    case "getGameMode":
                        return mode[0];
                    case "setGameMode": {
                        GameMode next = (GameMode) args[0];
                        if (next == mode[0]) return null;
                        // comme Bukkit : l'évènement de changement est appelé avant le changement
                        if (core.spectators().isSpectator(id)) core.spectators().externalGameModeChange((PlatformPlayer) proxy, next);
                        mode[0] = next;
                        return null;
                    }
                    case "getEffects":
                        return new ArrayList<>(effects);
                    case "clearEffects":
                        effects.clear();
                        return null;
                    case "addEffect":
                        removeEffect(effects, (String) args[0]);
                        effects.add(new EffectInfo((String) args[0], (Integer) args[2], (Integer) args[1]));
                        return null;
                    case "removeEffect":
                        removeEffect(effects, (String) args[0]);
                        return null;
                    case "setAllowFlight":
                        flags[0] = (Boolean) args[0];
                        return null;
                    case "getAllowFlight":
                        return flags[0];
                    case "setFlying":
                        flags[1] = (Boolean) args[0];
                        return null;
                    case "isFlying":
                        return flags[1];
                    case "getLocation":
                    case "getEyeLocation":
                        return new Position("world", 0, 64, 0);
                    case "getWorld":
                        return core.platform().getWorld("world");
                    case "getStorageContents":
                        return new ItemRef[36];
                    case "getArmorContents":
                        return new ItemRef[4];
                    case "getHealth":
                    case "getMaxHealth":
                        return 20.0;
                    default:
                        return defaultValue(m);
                }
            }
        });
        players.put(id, p);
        core.filters().loadPlayer(id);
        return p;
    }

    private static void removeEffect(List<EffectInfo> effects, String name) {
        for (int i = effects.size() - 1; i >= 0; i--) {
            if (effects.get(i).getName().equals(name)) effects.remove(i);
        }
    }

    static boolean hasEffect(PlatformPlayer p, String name) {
        for (EffectInfo e : p.getEffects()) {
            if (e.getName().equals(name)) return true;
        }
        return false;
    }

    private static Object proxy(Class<?> type, InvocationHandler handler) {
        return Proxy.newProxyInstance(FakeServer.class.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Object defaultValue(Method m) {
        Class<?> t = m.getReturnType();
        if (t == boolean.class) return false;
        if (t == int.class) return 0;
        if (t == long.class) return 0L;
        if (t == double.class) return 0.0;
        if (t == float.class) return 0f;
        if (t == String.class) return "";
        if (t == List.class) return new ArrayList<>();
        return null;
    }
}
