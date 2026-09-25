package fr.spectatorplus.spectator;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.api.EnterReason;
import fr.spectatorplus.api.LeaveReason;
import fr.spectatorplus.api.SpectatorState;
import fr.spectatorplus.api.events.SpectatorEnterEvent;
import fr.spectatorplus.api.events.SpectatorFollowStartEvent;
import fr.spectatorplus.api.events.SpectatorFollowStopEvent;
import fr.spectatorplus.api.events.SpectatorFollowTargetChangeEvent;
import fr.spectatorplus.api.events.SpectatorLeaveEvent;
import fr.spectatorplus.api.events.SpectatorTeleportEvent;
import fr.spectatorplus.compat.Compat;
import fr.spectatorplus.compat.Sounds;
import fr.spectatorplus.config.ConfigFiles;
import fr.spectatorplus.filter.Preferences;
import fr.spectatorplus.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Cœur du mode spectateur : entrée / sortie, effets, visibilité, follow, POV.
 */
public final class SpectatorManager {

    private final SpectatorPlus plugin;
    // concurrente : lue depuis le thread asynchrone du chat
    private final Map<UUID, SpectatorSession> sessions = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> hiddenByUs = new HashMap<>();
    private final File folder;

    public SpectatorManager(SpectatorPlus plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "spectators");
    }

    public void start() {
        // chaque tick : suivi fluide (vélocité) + passe-muraille
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                followTick();
                noclipTick();
            }
        }, 1L, 1L);
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                povTick();
                hudTick();
            }
        }, 10L, 10L);
    }

    // ------------------------------------------------------------------ accès

    public boolean isSpectator(Player p) {
        return p != null && sessions.containsKey(p.getUniqueId());
    }

    public boolean isSpectator(UUID id) {
        return id != null && sessions.containsKey(id);
    }

    public SpectatorSession getSpectator(Player p) {
        return p == null ? null : sessions.get(p.getUniqueId());
    }

    public SpectatorSession getSpectator(UUID id) {
        return sessions.get(id);
    }

    public Collection<SpectatorSession> getSpectators() {
        return Collections.unmodifiableCollection(new ArrayList<>(sessions.values()));
    }

    // ------------------------------------------------------------------ entrée / sortie

    public boolean enter(Player p, EnterReason reason, Location location, boolean forced) {
        if (p == null || !p.isOnline() || sessions.containsKey(p.getUniqueId())) return false;
        SpectatorEnterEvent ev = new SpectatorEnterEvent(p, reason, forced);
        Bukkit.getPluginManager().callEvent(ev);
        if (ev.isCancelled() && !forced) return false;

        SavedState state = null;
        File f = file(p.getUniqueId());
        if (f.exists()) state = readState(f);
        if (state == null) state = SavedState.capture(p);

        SpectatorSession session = new SpectatorSession(plugin, p.getUniqueId(), reason, state);
        sessions.put(p.getUniqueId(), session);
        persist(session);

        try {
            p.leaveVehicle();
        } catch (Throwable ignored) {
        }
        p.closeInventory();
        p.getInventory().clear();
        p.getInventory().setArmorContents(new org.bukkit.inventory.ItemStack[4]);
        if (location != null && location.getWorld() != null) p.teleport(safe(location));
        apply(p);
        plugin.hotbar().give(p, session);
        refreshVisibility(p);

        if (reason != EnterReason.RECONNECT) plugin.messages().send(p, "spectator.enter");
        if (reason == EnterReason.DEATH || reason == EnterReason.ELIMINATION) plugin.game().onElimination(p);
        return true;
    }

    public boolean leave(Player p, LeaveReason reason, boolean forced) {
        SpectatorSession s = getSpectator(p);
        if (s == null) return false;
        SpectatorLeaveEvent ev = new SpectatorLeaveEvent(p, reason);
        Bukkit.getPluginManager().callEvent(ev);
        if (ev.isCancelled() && !forced) return false;

        if (s.state == SpectatorState.POV) {
            try {
                p.setSpectatorTarget(null);
            } catch (Throwable ignored) {
            }
        }
        s.state = SpectatorState.FREE;
        s.target = null;
        plugin.menus().close(p);
        sessions.remove(p.getUniqueId());
        deleteFile(p.getUniqueId());

        Compat.setCollidable(p, true);
        Compat.setAffectsSpawning(p, true);
        Compat.setInvulnerable(p, false);
        p.setCanPickupItems(true);
        p.setSleepingIgnored(false);
        p.setFireTicks(0);
        p.setFallDistance(0);
        String tp = plugin.getConfig().getString("spectator.leave-teleport", "SAVED").toUpperCase();
        s.saved.restore(p, tp.equals("SAVED"), true);
        if (tp.equals("SPAWN")) p.teleport(p.getWorld().getSpawnLocation());
        refreshVisibility(p);
        if (reason != LeaveReason.PLUGIN_DISABLE) plugin.messages().send(p, "spectator.leave");
        return true;
    }

    /** Applique (ou réapplique) toutes les restrictions du mode spectateur. */
    public void apply(Player p) {
        SpectatorSession s = getSpectator(p);
        if (s == null) return;
        if (s.state != SpectatorState.POV) p.setGameMode(GameMode.ADVENTURE);
        s.noclip = false;
        p.setAllowFlight(true);
        p.setFlying(true);
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.setHealth(Math.min(Compat.maxHealth(p), Math.max(1, p.getHealth())));
        p.setCanPickupItems(false);
        p.setSleepingIgnored(true);
        Compat.setCollidable(p, false);
        Compat.setAffectsSpawning(p, false);
        Compat.setInvulnerable(p, true);
        if (plugin.getConfig().getBoolean("spectator.clear-effects", true)) {
            for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
        }
        if (plugin.getConfig().getBoolean("spectator.night-vision", true)) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
        }
        if (plugin.getConfig().getBoolean("spectator.invisibility-effect", false)) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true, false));
        }
        applyFlySpeed(p);
    }

    /** Redonne la barre d'inventaire (ex : après un changement de langue). */
    public void refreshHotbar(Player p) {
        SpectatorSession s = getSpectator(p);
        if (s != null && s.state != SpectatorState.POV) plugin.hotbar().give(p, s);
    }

    public void applyFlySpeed(Player p) {
        SpectatorSession s = getSpectator(p);
        if (s != null && s.frozen) {
            p.setFlySpeed(0f);
            return;
        }
        int level = Math.max(1, Math.min(5, plugin.filters().get(p).flySpeed));
        float[] speeds = {0.05f, 0.1f, 0.15f, 0.2f, 0.3f};
        p.setFlySpeed(speeds[level - 1]);
    }

    private Location safe(Location loc) {
        Location l = loc.clone();
        World w = l.getWorld();
        int min = Compat.minHeight(w);
        if (l.getY() < min + 1) {
            l.setY(w.getHighestBlockYAt(l.getBlockX(), l.getBlockZ()) + 2);
        }
        return l;
    }

    // ------------------------------------------------------------------ persistance

    private File file(UUID id) {
        return new File(folder, id + ".yml");
    }

    private void persist(SpectatorSession s) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("reason", s.getEnterReason().name());
        s.saved.save(y.createSection("state"));
        try {
            ConfigFiles.save(y, file(s.getUniqueId()));
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Impossible de sauvegarder l'état spectateur de " + s.getUniqueId(), e);
        }
    }

    private void deleteFile(UUID id) {
        File f = file(id);
        if (f.exists() && !f.delete()) f.deleteOnExit();
    }

    public boolean hasPersistedState(UUID id) {
        return file(id).exists();
    }

    /** Charge l'état sauvegardé (utilisé par enter via le fichier : on lit la section « state »). */
    static SavedState readState(File f) {
        YamlConfiguration y = ConfigFiles.read(f);
        YamlConfiguration state = new YamlConfiguration();
        if (y.isConfigurationSection("state")) {
            for (String k : y.getConfigurationSection("state").getKeys(true)) {
                state.set(k, y.get("state." + k));
            }
        }
        return SavedState.load(state);
    }

    // ------------------------------------------------------------------ connexion

    public void handleJoin(Player p) {
        for (UUID id : sessions.keySet()) {
            Player sp = Bukkit.getPlayer(id);
            if (sp != null && sp != p) applyPair(p, sp);
        }
        if (file(p.getUniqueId()).exists()) {
            enterFromFile(p);
            return;
        }
        if (plugin.getMode() != fr.spectatorplus.api.SpectatorMode.MANUAL
                && plugin.getConfig().getBoolean("auto.join-during-game", true)
                && plugin.game().isGameRunning() && !plugin.game().isParticipant(p.getUniqueId())) {
            enter(p, EnterReason.JOIN_DURING_GAME, null, plugin.getMode() == fr.spectatorplus.api.SpectatorMode.AUTO);
        }
    }

    private void enterFromFile(Player p) {
        File f = file(p.getUniqueId());
        SavedState state = readState(f);
        EnterReason reason = EnterReason.RECONNECT;
        SpectatorSession session = new SpectatorSession(plugin, p.getUniqueId(), reason, state);
        SpectatorEnterEvent ev = new SpectatorEnterEvent(p, reason, true);
        Bukkit.getPluginManager().callEvent(ev);
        sessions.put(p.getUniqueId(), session);
        p.getInventory().clear();
        p.getInventory().setArmorContents(new org.bukkit.inventory.ItemStack[4]);
        apply(p);
        plugin.hotbar().give(p, session);
        refreshVisibility(p);
        plugin.messages().send(p, "spectator.restored");
    }

    public void handleQuit(Player p) {
        SpectatorSession s = getSpectator(p);
        if (s != null) {
            if (plugin.getConfig().getBoolean("spectator.keep-on-quit", true)) {
                sessions.remove(p.getUniqueId());
            } else {
                leave(p, LeaveReason.QUIT, true);
            }
        }
        hiddenByUs.remove(p.getUniqueId());
    }

    /** Au démarrage (ou /reload) : restaure les spectateurs déjà connectés. */
    public void restoreOnline() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (file(p.getUniqueId()).exists() && !isSpectator(p)) enterFromFile(p);
        }
    }

    public void shutdown() {
        boolean persist = plugin.getConfig().getBoolean("spectator.persist-on-restart", true);
        for (SpectatorSession s : new ArrayList<>(sessions.values())) {
            Player p = s.getPlayer();
            if (p == null) continue;
            if (s.state == SpectatorState.POV) {
                try {
                    p.setSpectatorTarget(null);
                } catch (Throwable ignored) {
                }
                p.setGameMode(GameMode.ADVENTURE);
            }
            if (!persist) leave(p, LeaveReason.PLUGIN_DISABLE, true);
        }
        for (Map.Entry<UUID, Set<UUID>> e : hiddenByUs.entrySet()) {
            Player viewer = Bukkit.getPlayer(e.getKey());
            if (viewer == null) continue;
            for (UUID t : e.getValue()) {
                Player target = Bukkit.getPlayer(t);
                if (target != null) Compat.showPlayer(plugin, viewer, target);
            }
        }
        hiddenByUs.clear();
        sessions.clear();
    }

    // ------------------------------------------------------------------ visibilité

    public void refreshVisibility(Player p) {
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (o == p) continue;
            applyPair(o, p);
            applyPair(p, o);
        }
    }

    public void refreshAllVisibility() {
        for (Player p : Bukkit.getOnlinePlayers()) refreshVisibility(p);
    }

    private void applyPair(Player viewer, Player target) {
        boolean hide = false;
        if (isSpectator(target)) {
            if (isSpectator(viewer)) {
                hide = !plugin.filters().get(viewer).seeSpectators;
            } else {
                hide = !viewer.hasPermission("spectatorplus.see-spectators");
            }
        }
        Set<UUID> set = hiddenByUs.get(viewer.getUniqueId());
        boolean hidden = set != null && set.contains(target.getUniqueId());
        if (hide && !hidden) {
            Compat.hidePlayer(plugin, viewer, target);
            if (set == null) {
                set = new HashSet<>();
                hiddenByUs.put(viewer.getUniqueId(), set);
            }
            set.add(target.getUniqueId());
        } else if (!hide && hidden) {
            Compat.showPlayer(plugin, viewer, target);
            set.remove(target.getUniqueId());
        }
    }

    // ------------------------------------------------------------------ téléportation / follow

    public List<Player> targets() {
        List<Player> res = new ArrayList<>(plugin.game().getAlivePlayers());
        Collections.sort(res, new Comparator<Player>() {
            @Override
            public int compare(Player a, Player b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        return res;
    }

    public boolean teleport(SpectatorSession s, Player target) {
        Player p = s.getPlayer();
        if (p == null || target == null || !target.isOnline()) return false;
        Location dest = behind(target, plugin.filters().get(p).followDistance);
        SpectatorTeleportEvent ev = new SpectatorTeleportEvent(p, target, dest);
        Bukkit.getPluginManager().callEvent(ev);
        if (ev.isCancelled() || ev.getDestination() == null) return false;
        if (s.state == SpectatorState.POV) stopPov(s);
        p.teleport(ev.getDestination());
        p.setFlying(true);
        if (plugin.filters().get(p).sounds) Compat.playSound(p, Sounds.TELEPORT, 0.6f, 1.2f);
        plugin.messages().send(p, "spectator.teleported", "target", target.getName());
        return true;
    }

    public void follow(SpectatorSession s, Player target) {
        Player p = s.getPlayer();
        if (p == null || target == null) return;
        if (target == p || isSpectator(target)) {
            plugin.messages().send(p, "errors.invalid-target");
            return;
        }
        Player current = s.getFollowTarget();
        if (s.state == SpectatorState.FOLLOWING && current != null) {
            if (current == target) return;
            SpectatorFollowTargetChangeEvent ev = new SpectatorFollowTargetChangeEvent(p, current, target);
            Bukkit.getPluginManager().callEvent(ev);
            if (ev.isCancelled()) return;
        } else {
            SpectatorFollowStartEvent ev = new SpectatorFollowStartEvent(p, target);
            Bukkit.getPluginManager().callEvent(ev);
            if (ev.isCancelled()) return;
        }
        if (s.state == SpectatorState.POV) exitPovMode(s, p);
        s.state = SpectatorState.FOLLOWING;
        s.target = target.getUniqueId();
        s.lastTargetPos = null;
        // une seule téléportation au départ (si loin), ensuite la caméra glisse (voir followTick)
        if (!p.getWorld().equals(target.getWorld())
                || p.getLocation().distanceSquared(target.getLocation()) > Math.pow(plugin.filters().get(p).followDistance + 6, 2)) {
            p.teleport(behind(target, plugin.filters().get(p).followDistance));
        }
        p.setFlying(true);
        plugin.messages().send(p, "spectator.follow-start", "target", target.getName());
    }

    public void stopFollowing(SpectatorSession s, boolean message) {
        Player p = s.getPlayer();
        if (s.state == SpectatorState.POV) {
            stopPov(s);
            return;
        }
        if (s.state != SpectatorState.FOLLOWING) return;
        Player previous = s.getFollowTarget();
        s.state = SpectatorState.FREE;
        s.target = null;
        if (p != null) {
            Bukkit.getPluginManager().callEvent(new SpectatorFollowStopEvent(p, previous));
            if (message) plugin.messages().send(p, "spectator.follow-stop");
        }
    }

    /** Passe à la cible suivante (dir = 1) ou précédente (dir = -1). */
    public Player cycle(SpectatorSession s, int dir) {
        List<Player> list = targets();
        if (list.isEmpty()) return null;
        Player current = s.getFollowTarget();
        int idx = current == null ? -1 : list.indexOf(current);
        int next;
        if (idx < 0) next = dir > 0 ? 0 : list.size() - 1;
        else next = ((idx + dir) % list.size() + list.size()) % list.size();
        return list.get(next);
    }

    public void cycleAndFollow(SpectatorSession s, int dir) {
        Player p = s.getPlayer();
        Player t = cycle(s, dir);
        if (t == null) {
            if (p != null) plugin.messages().send(p, "errors.no-players");
            return;
        }
        if (s.state == SpectatorState.POV) startPov(s, t);
        else follow(s, t);
    }

    /** Position derrière la cible, sans entrer dans un bloc, orientée vers elle. */
    public Location behind(Player target, int distance) {
        Location base = target.getLocation();
        Location eye = target.getEyeLocation();
        Vector dir = base.getDirection().setY(0);
        if (dir.lengthSquared() < 1.0E-4) dir = new Vector(0, 0, 1);
        dir.normalize();
        double up = plugin.getConfig().getDouble("follow.height", 1.5);
        Location dest = eye.clone();
        double step = 0.5;
        for (double d = step; d <= distance; d += step) {
            Location test = eye.clone().subtract(dir.clone().multiply(d)).add(0, up * d / distance, 0);
            Block b = test.getBlock();
            if (b.getType().isSolid() || b.getRelative(0, 1, 0).getType().isSolid()) break;
            dest = test;
        }
        dest.subtract(0, 1.62, 0);
        Vector look = eye.toVector().subtract(dest.clone().add(0, 1.62, 0).toVector());
        if (look.lengthSquared() > 1.0E-4) {
            double dx = look.getX(), dy = look.getY(), dz = look.getZ();
            dest.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
            dest.setPitch((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        } else {
            dest.setYaw(base.getYaw());
            dest.setPitch(base.getPitch());
        }
        return dest;
    }

    /**
     * Suivi fluide : au lieu de téléporter le spectateur en boucle (saccadé), on lui applique
     * chaque tick une vélocité vers la position idéale. Le client interpole le mouvement,
     * et le spectateur garde le contrôle de sa caméra : il regarde librement et peut tourner
     * autour de la cible avec ses touches de déplacement.
     */
    private void followTick() {
        String style = plugin.getConfig().getString("follow.style", "SMOOTH").toUpperCase(java.util.Locale.ROOT);
        double height = plugin.getConfig().getDouble("follow.height", 1.5);
        double gain = Math.max(0.05, Math.min(1, plugin.getConfig().getDouble("follow.smoothness", 0.25)));
        double maxSpeed = plugin.getConfig().getDouble("follow.max-speed", 2.5);
        double tpDistance = plugin.getConfig().getDouble("follow.teleport-distance", 32);
        double leash = plugin.getConfig().getDouble("follow.leash-distance", 12);
        for (SpectatorSession s : sessions.values()) {
            if (s.state != SpectatorState.FOLLOWING || s.frozen) continue;
            Player p = s.getPlayer();
            if (p == null) continue;
            Player t = s.getFollowTarget();
            if (t == null || !t.isOnline() || isSpectator(t) || t.isDead()) {
                targetLost(s, p);
                continue;
            }
            Preferences prefs = plugin.filters().get(p);
            Location tl = t.getLocation();
            if (!p.getWorld().equals(t.getWorld())) {
                p.teleport(behind(t, prefs.followDistance));
                s.lastTargetPos = null;
                continue;
            }
            // déplacement de la cible depuis le tick précédent (anticipation)
            Vector targetPos = tl.toVector();
            Vector motion = new Vector(0, 0, 0);
            if (s.lastTargetPos != null && tl.getWorld().getName().equals(s.lastTargetWorld)) {
                motion = targetPos.clone().subtract(s.lastTargetPos);
                if (motion.lengthSquared() > 25) motion = new Vector(0, 0, 0); // la cible s'est téléportée
            }
            s.lastTargetPos = targetPos;
            s.lastTargetWorld = tl.getWorld().getName();

            Location cur = p.getLocation();
            double distance = prefs.followDistance;
            if (style.equals("LEASH")) {
                if (cur.distanceSquared(tl) <= leash * leash) continue; // libre dans le rayon
                distance = leash * 0.8;
            }
            Vector desired = desiredPosition(style, p, t, distance, height);
            Vector delta = desired.clone().subtract(cur.toVector());
            if (delta.length() > tpDistance) {
                p.teleport(desired.toLocation(cur.getWorld(), cur.getYaw(), cur.getPitch()));
                continue;
            }
            Vector velocity = motion.add(delta.multiply(gain));
            if (velocity.length() > maxSpeed) velocity.normalize().multiply(maxSpeed);
            if (!p.isFlying() && p.getAllowFlight()) p.setFlying(true);
            p.setVelocity(velocity);
        }
    }

    /**
     * Position idéale du spectateur.
     * SMOOTH : garde la distance en restant du côté où se trouve le spectateur (pas de balancement en combat).
     * BEHIND : toujours dans le dos de la cible.
     */
    private Vector desiredPosition(String style, Player p, Player t, double distance, double height) {
        Location tl = t.getLocation();
        Vector dir;
        if (style.equals("BEHIND")) {
            dir = tl.getDirection().setY(0).multiply(-1);
        } else {
            dir = p.getLocation().toVector().subtract(tl.toVector()).setY(0);
            if (dir.lengthSquared() < 0.25) dir = tl.getDirection().setY(0).multiply(-1);
        }
        if (dir.lengthSquared() < 1.0E-4) dir = new Vector(0, 0, -1);
        dir.normalize();
        Vector ideal = tl.toVector().add(dir.clone().multiply(distance)).add(new Vector(0, height, 0));
        if (noclipAllowed(p)) return ideal;
        // sans passe-muraille : on raccourcit la distance devant un mur
        Vector eye = t.getEyeLocation().toVector();
        Vector target = ideal.clone().add(new Vector(0, 1.62, 0));
        Vector step = target.clone().subtract(eye);
        double len = step.length();
        if (len < 1.0E-3) return ideal;
        step.normalize().multiply(0.5);
        Vector last = eye.clone();
        Vector probe = eye.clone();
        for (double d = 0.5; d <= len; d += 0.5) {
            probe.add(step);
            if (tl.getWorld().getBlockAt(probe.getBlockX(), probe.getBlockY(), probe.getBlockZ()).getType().isSolid()) break;
            last = probe.clone();
        }
        return last.subtract(new Vector(0, 1.62, 0));
    }

    // ------------------------------------------------------------------ passe-muraille

    /**
     * Le mode Adventure ne permet pas de traverser les blocs (les collisions sont calculées par le client).
     * Quand le spectateur touche un bloc, il passe donc temporairement en mode Spectator vanilla,
     * puis revient en Adventure (avec sa barre d'inventaire) dès qu'il est de nouveau à l'air libre.
     */
    private boolean noclipAllowed(Player p) {
        return plugin.getConfig().getBoolean("spectator.noclip.enabled", true) && plugin.filters().get(p).noclip;
    }

    private void noclipTick() {
        for (SpectatorSession s : sessions.values()) {
            Player p = s.getPlayer();
            if (p == null) continue;
            Vector pos = p.getLocation().toVector();
            Vector movement = s.lastPos == null ? new Vector(0, 0, 0) : pos.clone().subtract(s.lastPos);
            if (movement.lengthSquared() > 25) movement = new Vector(0, 0, 0);
            s.lastPos = pos;
            if (s.state == SpectatorState.POV) {
                s.noclip = false;
                continue;
            }
            if (s.frozen || !noclipAllowed(p)) {
                if (s.noclip) exitNoclip(s, p);
                continue;
            }
            boolean near = nearSolid(p, movement);
            if (near && !s.noclip && p.getGameMode() == GameMode.ADVENTURE) {
                s.noclip = true;
                p.setGameMode(GameMode.SPECTATOR);
            } else if (!near && s.noclip) {
                exitNoclip(s, p);
            }
        }
    }

    private void exitNoclip(SpectatorSession s, Player p) {
        s.noclip = false;
        if (p.getGameMode() == GameMode.SPECTATOR) {
            p.setGameMode(GameMode.ADVENTURE);
            p.setAllowFlight(true);
            p.setFlying(true);
            applyFlySpeed(p);
        }
    }

    /** Un bloc solide touche (ou va toucher au prochain tick) la hitbox du joueur, avec une marge. */
    private boolean nearSolid(Player p, Vector movement) {
        Location l = p.getLocation();
        World w = l.getWorld();
        double margin = plugin.getConfig().getDouble("spectator.noclip.margin", 0.45);
        boolean down = p.isSneaking() || movement.getY() < -0.05;
        double[] heights = down ? new double[]{-0.3, 0.05, 1.0, 1.85} : new double[]{0.05, 1.0, 1.85};
        for (int pass = 0; pass < 2; pass++) {
            double bx = l.getX(), by = l.getY(), bz = l.getZ();
            if (pass == 1) {
                if (movement.lengthSquared() < 1.0E-4) break;
                bx += movement.getX();
                by += movement.getY();
                bz += movement.getZ();
            }
            for (double dy : heights) {
                for (int ix = -1; ix <= 1; ix += 2) {
                    for (int iz = -1; iz <= 1; iz += 2) {
                        Block b = w.getBlockAt((int) Math.floor(bx + ix * margin), (int) Math.floor(by + dy),
                                (int) Math.floor(bz + iz * margin));
                        if (b.getType().isSolid()) return true;
                    }
                }
            }
        }
        return false;
    }

    private void targetLost(SpectatorSession s, Player p) {
        if (plugin.getConfig().getBoolean("follow.auto-next-on-target-lost", true)) {
            Player next = cycle(s, 1);
            if (next != null && next != s.getFollowTarget()) {
                s.state = SpectatorState.FREE;
                s.target = null;
                follow(s, next);
                return;
            }
        }
        stopFollowing(s, false);
        plugin.messages().send(p, "spectator.target-lost");
    }

    // ------------------------------------------------------------------ POV

    public void startPov(final SpectatorSession s, final Player target) {
        final Player p = s.getPlayer();
        if (p == null || target == null) return;
        if (!plugin.getConfig().getBoolean("pov.enabled", true) || !p.hasPermission("spectatorplus.pov")) {
            plugin.messages().send(p, "errors.no-permission");
            return;
        }
        if (target == p || isSpectator(target)) {
            plugin.messages().send(p, "errors.invalid-target");
            return;
        }
        Player current = s.getFollowTarget();
        if (current != null && current != target) {
            SpectatorFollowTargetChangeEvent ev = new SpectatorFollowTargetChangeEvent(p, current, target);
            Bukkit.getPluginManager().callEvent(ev);
            if (ev.isCancelled()) return;
        } else if (current == null) {
            SpectatorFollowStartEvent ev = new SpectatorFollowStartEvent(p, target);
            Bukkit.getPluginManager().callEvent(ev);
            if (ev.isCancelled()) return;
        }
        plugin.menus().close(p);
        s.state = SpectatorState.POV;
        s.target = target.getUniqueId();
        s.povExitRequested = false;
        p.teleport(target.getLocation());
        p.setGameMode(GameMode.SPECTATOR);
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (s.state == SpectatorState.POV && p.isOnline() && target.isOnline()) p.setSpectatorTarget(target);
            }
        }, 3L);
        plugin.messages().send(p, "spectator.pov-start", "target", target.getName());
    }

    public void stopPov(SpectatorSession s) {
        Player p = s.getPlayer();
        if (s.state != SpectatorState.POV) return;
        Player previous = s.getFollowTarget();
        s.state = SpectatorState.FREE;
        s.target = null;
        if (p == null) return;
        exitPovMode(s, p);
        if (previous != null && previous.isOnline()) p.teleport(behind(previous, plugin.filters().get(p).followDistance));
        Bukkit.getPluginManager().callEvent(new SpectatorFollowStopEvent(p, previous));
        plugin.messages().send(p, "spectator.pov-stop");
    }

    private void exitPovMode(SpectatorSession s, Player p) {
        try {
            if (p.getGameMode() == GameMode.SPECTATOR) p.setSpectatorTarget(null);
        } catch (Throwable ignored) {
        }
        s.povExitRequested = false;
        p.setGameMode(GameMode.ADVENTURE);
        apply(p);
        plugin.hotbar().give(p, s);
    }

    /** Appelé quand le joueur s'accroupit (sortie du POV en vanilla). */
    public void requestPovExit(SpectatorSession s) {
        if (s.state == SpectatorState.POV) s.povExitRequested = true;
    }

    private void povTick() {
        for (SpectatorSession s : new ArrayList<>(sessions.values())) {
            if (s.state != SpectatorState.POV) continue;
            Player p = s.getPlayer();
            if (p == null) continue;
            Player t = s.getFollowTarget();
            if (s.povExitRequested) {
                stopPov(s);
                continue;
            }
            if (t == null || !t.isOnline() || isSpectator(t) || t.isDead()) {
                Player next = plugin.getConfig().getBoolean("follow.auto-next-on-target-lost", true) ? cycle(s, 1) : null;
                if (next != null && next != t) {
                    startPov(s, next);
                } else {
                    stopPov(s);
                    plugin.messages().send(p, "spectator.target-lost");
                }
                continue;
            }
            org.bukkit.entity.Entity current = p.getSpectatorTarget();
            if (current == null || !current.getUniqueId().equals(t.getUniqueId())) {
                if (p.getGameMode() != GameMode.SPECTATOR) p.setGameMode(GameMode.SPECTATOR);
                if (!p.getWorld().equals(t.getWorld()) || p.getLocation().distanceSquared(t.getLocation()) > 4) {
                    p.teleport(t.getLocation());
                }
                p.setSpectatorTarget(t);
            }
        }
    }

    // ------------------------------------------------------------------ HUD

    private void hudTick() {
        for (SpectatorSession s : sessions.values()) {
            Player p = s.getPlayer();
            if (p == null) continue;
            Preferences prefs = plugin.filters().get(p);
            if (!prefs.actionBar) continue;
            Player t = s.getFollowTarget();
            String text;
            if (t != null && (s.state == SpectatorState.FOLLOWING || s.state == SpectatorState.POV)) {
                String dist = p.getWorld().equals(t.getWorld())
                        ? String.valueOf((int) p.getLocation().distance(t.getLocation())) : "?";
                text = plugin.messages().get(p, s.state == SpectatorState.POV ? "hud.pov" : "hud.following",
                        "target", t.getName(),
                        "health", Text.hearts(t.getHealth()),
                        "max_health", Text.hearts(Compat.maxHealth(t)),
                        "food", t.getFoodLevel(),
                        "distance", dist,
                        "world", t.getWorld().getName(),
                        "kills", plugin.stats().kills(t.getUniqueId()));
            } else if (plugin.getConfig().getBoolean("hud.show-when-free", true)) {
                text = plugin.messages().get(p, "hud.free",
                        "alive", plugin.game().getAlivePlayers().size(),
                        "spectators", sessions.size(),
                        "time", Text.duration(plugin.game().getGameDuration()));
            } else {
                continue;
            }
            Compat.actionBar(p, plugin.placeholders().apply(text, p, null));
        }
    }
}
