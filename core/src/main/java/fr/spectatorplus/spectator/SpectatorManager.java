package fr.spectatorplus.spectator;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.api.EnterReason;
import fr.spectatorplus.api.LeaveReason;
import fr.spectatorplus.api.SpectatorMode;
import fr.spectatorplus.api.SpectatorState;
import fr.spectatorplus.compat.Sounds;
import fr.spectatorplus.core.platform.GameMode;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.PlatformWorld;
import fr.spectatorplus.core.platform.PlayerSnapshot;
import fr.spectatorplus.core.platform.Position;
import fr.spectatorplus.core.platform.Vector3;
import fr.spectatorplus.filter.Preferences;
import fr.spectatorplus.util.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Cœur du mode spectateur : entrée / sortie, effets, visibilité, follow, POV.
 */
public final class SpectatorManager {

    private final SpectatorCore plugin;
    // concurrente : lue depuis le thread asynchrone du chat
    private final Map<UUID, SpectatorSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> hiddenByUs = new HashMap<>();
    private final File folder;

    public SpectatorManager(SpectatorCore plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.host().dataFolder(), "spectators");
    }

    public void start() {
        // chaque tick : suivi fluide (vélocité) + passe-muraille
        plugin.platform().runTimer(new Runnable() {
            @Override
            public void run() {
                followTick();
                noclipTick();
            }
        }, 1L, 1L);
        plugin.platform().runTimer(new Runnable() {
            @Override
            public void run() {
                povTick();
                hudTick();
            }
        }, 10L, 10L);
    }

    private static boolean same(PlatformPlayer a, PlatformPlayer b) {
        return a != null && a.equals(b);
    }

    // ------------------------------------------------------------------ accès

    public boolean isSpectator(PlatformPlayer p) {
        return p != null && sessions.containsKey(p.getUniqueId());
    }

    public boolean isSpectator(UUID id) {
        return id != null && sessions.containsKey(id);
    }

    public SpectatorSession getSpectator(PlatformPlayer p) {
        return p == null ? null : sessions.get(p.getUniqueId());
    }

    public SpectatorSession getSpectator(UUID id) {
        return sessions.get(id);
    }

    public Collection<SpectatorSession> getSpectators() {
        return Collections.unmodifiableCollection(new ArrayList<>(sessions.values()));
    }

    // ------------------------------------------------------------------ entrée / sortie

    public boolean enter(PlatformPlayer p, EnterReason reason, Position location, boolean forced) {
        if (p == null || !p.isOnline() || sessions.containsKey(p.getUniqueId())) return false;
        if (!plugin.platform().api().enter(p, reason, forced) && !forced) return false;

        PlayerSnapshot state = null;
        File f = file(p.getUniqueId());
        if (f.exists()) state = plugin.platform().loadSnapshot(f);
        if (state == null) state = plugin.platform().capture(p);

        SpectatorSession session = new SpectatorSession(plugin, p.getUniqueId(), reason, state);
        sessions.put(p.getUniqueId(), session);
        persist(session);

        try {
            p.leaveVehicle();
        } catch (Throwable ignored) {
        }
        p.closeInventory();
        p.clearInventory();
        if (location != null && location.getWorld() != null) p.teleport(safe(location));
        apply(p);
        plugin.hotbar().give(p, session);
        refreshVisibility(p);

        if (reason != EnterReason.RECONNECT) plugin.messages().send(p, "spectator.enter");
        if (reason == EnterReason.DEATH || reason == EnterReason.ELIMINATION) plugin.game().onElimination(p);
        return true;
    }

    public boolean leave(PlatformPlayer p, LeaveReason reason, boolean forced) {
        SpectatorSession s = getSpectator(p);
        if (s == null) return false;
        if (!plugin.platform().api().leave(p, reason) && !forced) return false;

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

        p.setHealthDisplay(false, "");
        p.setCollidable(true);
        p.setAffectsSpawning(true);
        p.setInvulnerable(false);
        p.setCanPickupItems(true);
        p.setSleepingIgnored(false);
        p.setFireTicks(0);
        p.setFallDistance(0);
        String tp = plugin.config().getString("spectator.leave-teleport", "SAVED").toUpperCase(Locale.ROOT);
        s.saved.restore(p, tp.equals("SAVED"), true);
        if (tp.equals("SPAWN")) p.teleport(p.getWorld().getSpawn());
        refreshVisibility(p);
        if (reason != LeaveReason.PLUGIN_DISABLE) plugin.messages().send(p, "spectator.leave");
        return true;
    }

    /** Applique (ou réapplique) toutes les restrictions du mode spectateur. */
    public void apply(PlatformPlayer p) {
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
        p.setHealth(Math.min(p.getMaxHealth(), Math.max(1, p.getHealth())));
        p.setCanPickupItems(false);
        p.setSleepingIgnored(true);
        p.setCollidable(false);
        p.setAffectsSpawning(false);
        p.setInvulnerable(true);
        if (plugin.config().getBoolean("spectator.clear-effects", true)) p.clearEffects();
        if (plugin.config().getBoolean("spectator.night-vision", true)) {
            p.addEffect("NIGHT_VISION", Integer.MAX_VALUE, 0, true, false);
        }
        if (plugin.config().getBoolean("spectator.invisibility-effect", false)) {
            p.addEffect("INVISIBILITY", Integer.MAX_VALUE, 0, true, false);
        }
        applyFlySpeed(p);
        p.setHealthDisplay(plugin.config().getBoolean("spectator.health-below-name", true),
                plugin.messages().get(p, "spectator.health-title"));
    }

    /** Redonne la barre d'inventaire (ex : après un changement de langue). */
    public void refreshHotbar(PlatformPlayer p) {
        SpectatorSession s = getSpectator(p);
        if (s != null && s.state != SpectatorState.POV) plugin.hotbar().give(p, s);
    }

    public void applyFlySpeed(PlatformPlayer p) {
        SpectatorSession s = getSpectator(p);
        if (s != null && s.frozen) {
            p.setFlySpeed(0f);
            return;
        }
        int level = Math.max(1, Math.min(5, plugin.filters().get(p).flySpeed));
        float[] speeds = {0.05f, 0.1f, 0.15f, 0.2f, 0.3f};
        p.setFlySpeed(speeds[level - 1]);
    }

    private Position safe(Position loc) {
        PlatformWorld w = plugin.platform().getWorld(loc.getWorld());
        if (w == null) return loc;
        int min = w.getMinHeight();
        if (loc.getY() < min + 1) return loc.withY(w.getHighestBlockYAt(loc.getBlockX(), loc.getBlockZ()) + 2);
        return loc;
    }

    // ------------------------------------------------------------------ persistance

    private File file(UUID id) {
        return new File(folder, id + ".yml");
    }

    private void persist(SpectatorSession s) {
        try {
            plugin.platform().saveSnapshot(file(s.getUniqueId()), s.getEnterReason(), s.saved);
        } catch (Exception e) {
            plugin.host().logger().log(Level.WARNING, "Impossible de sauvegarder l'état spectateur de " + s.getUniqueId(), e);
        }
    }

    private void deleteFile(UUID id) {
        File f = file(id);
        if (f.exists() && !f.delete()) f.deleteOnExit();
    }

    public boolean hasPersistedState(UUID id) {
        return file(id).exists();
    }

    // ------------------------------------------------------------------ connexion

    public void handleJoin(PlatformPlayer p) {
        for (UUID id : sessions.keySet()) {
            PlatformPlayer sp = plugin.platform().getPlayer(id);
            if (sp != null && !same(sp, p)) applyPair(p, sp);
        }
        if (file(p.getUniqueId()).exists()) {
            enterFromFile(p);
            return;
        }
        if (plugin.getMode() != SpectatorMode.MANUAL
                && plugin.config().getBoolean("auto.join-during-game", true)
                && plugin.game().isGameRunning() && !plugin.game().isParticipant(p.getUniqueId())) {
            enter(p, EnterReason.JOIN_DURING_GAME, null, plugin.getMode() == SpectatorMode.AUTO);
        }
    }

    private void enterFromFile(PlatformPlayer p) {
        PlayerSnapshot state = plugin.platform().loadSnapshot(file(p.getUniqueId()));
        if (state == null) state = plugin.platform().capture(p);
        EnterReason reason = EnterReason.RECONNECT;
        SpectatorSession session = new SpectatorSession(plugin, p.getUniqueId(), reason, state);
        plugin.platform().api().enter(p, reason, true);
        sessions.put(p.getUniqueId(), session);
        p.clearInventory();
        apply(p);
        plugin.hotbar().give(p, session);
        refreshVisibility(p);
        plugin.messages().send(p, "spectator.restored");
    }

    public void handleQuit(PlatformPlayer p) {
        SpectatorSession s = getSpectator(p);
        if (s != null) {
            if (plugin.config().getBoolean("spectator.keep-on-quit", true)) {
                sessions.remove(p.getUniqueId());
            } else {
                leave(p, LeaveReason.QUIT, true);
            }
        }
        hiddenByUs.remove(p.getUniqueId());
    }

    /** Au démarrage (ou /reload) : restaure les spectateurs déjà connectés. */
    public void restoreOnline() {
        for (PlatformPlayer p : plugin.platform().getOnlinePlayers()) {
            if (file(p.getUniqueId()).exists() && !isSpectator(p)) enterFromFile(p);
        }
    }

    public void shutdown() {
        boolean persist = plugin.config().getBoolean("spectator.persist-on-restart", true);
        for (SpectatorSession s : new ArrayList<>(sessions.values())) {
            PlatformPlayer p = s.getPlayer();
            if (p == null) continue;
            if (s.state == SpectatorState.POV) {
                try {
                    p.setSpectatorTarget(null);
                } catch (Throwable ignored) {
                }
                p.setGameMode(GameMode.ADVENTURE);
            }
            if (!persist) leave(p, LeaveReason.PLUGIN_DISABLE, true);
            else p.setHealthDisplay(false, "");
        }
        for (Map.Entry<UUID, Set<UUID>> e : hiddenByUs.entrySet()) {
            PlatformPlayer viewer = plugin.platform().getPlayer(e.getKey());
            if (viewer == null) continue;
            for (UUID t : e.getValue()) {
                PlatformPlayer target = plugin.platform().getPlayer(t);
                if (target != null) viewer.setHidden(target, false);
            }
        }
        hiddenByUs.clear();
        sessions.clear();
    }

    // ------------------------------------------------------------------ visibilité

    public void refreshVisibility(PlatformPlayer p) {
        for (PlatformPlayer o : plugin.platform().getOnlinePlayers()) {
            if (same(o, p)) continue;
            applyPair(o, p);
            applyPair(p, o);
        }
    }

    public void refreshAllVisibility() {
        for (PlatformPlayer p : plugin.platform().getOnlinePlayers()) refreshVisibility(p);
    }

    private void applyPair(PlatformPlayer viewer, PlatformPlayer target) {
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
            viewer.setHidden(target, true);
            if (set == null) {
                set = new HashSet<>();
                hiddenByUs.put(viewer.getUniqueId(), set);
            }
            set.add(target.getUniqueId());
        } else if (!hide && hidden) {
            viewer.setHidden(target, false);
            set.remove(target.getUniqueId());
        }
    }

    /** Le joueur est-il caché à ce spectateur par Spectator Plus ? (utilisé par les mods pour le suivi réseau) */
    public boolean isHiddenFrom(UUID viewer, UUID target) {
        Set<UUID> set = hiddenByUs.get(viewer);
        return set != null && set.contains(target);
    }

    // ------------------------------------------------------------------ téléportation / follow

    public List<PlatformPlayer> targets() {
        List<PlatformPlayer> res = new ArrayList<>(plugin.game().getAlivePlayers());
        Collections.sort(res, new Comparator<PlatformPlayer>() {
            @Override
            public int compare(PlatformPlayer a, PlatformPlayer b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        return res;
    }

    public boolean teleport(SpectatorSession s, PlatformPlayer target) {
        PlatformPlayer p = s.getPlayer();
        if (p == null || target == null || !target.isOnline()) return false;
        Position dest = plugin.platform().api().teleport(p, target, behind(target, plugin.filters().get(p).followDistance));
        if (dest == null) return false;
        if (s.state == SpectatorState.POV) stopPov(s);
        p.teleport(dest);
        p.setFlying(true);
        if (plugin.filters().get(p).sounds) p.playSound(Sounds.TELEPORT, 0.6f, 1.2f);
        plugin.messages().send(p, "spectator.teleported", "target", target.getName());
        return true;
    }

    public void follow(SpectatorSession s, PlatformPlayer target) {
        PlatformPlayer p = s.getPlayer();
        if (p == null || target == null) return;
        if (same(target, p) || isSpectator(target)) {
            plugin.messages().send(p, "errors.invalid-target");
            return;
        }
        PlatformPlayer current = s.getFollowTarget();
        if (s.state == SpectatorState.FOLLOWING && current != null) {
            if (same(current, target)) return;
            if (!plugin.platform().api().followChange(p, current, target)) return;
        } else {
            if (!plugin.platform().api().followStart(p, target)) return;
        }
        if (s.state == SpectatorState.POV) exitPovMode(s, p);
        s.state = SpectatorState.FOLLOWING;
        s.target = target.getUniqueId();
        s.lastTargetPos = null;
        // une seule téléportation au départ (si loin), ensuite la caméra glisse (voir followTick)
        Position pl = p.getLocation(), tl = target.getLocation();
        if (!pl.sameWorld(tl) || pl.distanceSquared(tl) > Math.pow(plugin.filters().get(p).followDistance + 6, 2)) {
            p.teleport(behind(target, plugin.filters().get(p).followDistance));
        }
        p.setFlying(true);
        plugin.messages().send(p, "spectator.follow-start", "target", target.getName());
    }

    public void stopFollowing(SpectatorSession s, boolean message) {
        PlatformPlayer p = s.getPlayer();
        if (s.state == SpectatorState.POV) {
            stopPov(s);
            return;
        }
        if (s.state != SpectatorState.FOLLOWING) return;
        PlatformPlayer previous = s.getFollowTarget();
        s.state = SpectatorState.FREE;
        s.target = null;
        if (p != null) {
            plugin.platform().api().followStop(p, previous);
            if (message) plugin.messages().send(p, "spectator.follow-stop");
        }
    }

    /** Passe à la cible suivante (dir = 1) ou précédente (dir = -1). */
    public PlatformPlayer cycle(SpectatorSession s, int dir) {
        List<PlatformPlayer> list = targets();
        if (list.isEmpty()) return null;
        PlatformPlayer current = s.getFollowTarget();
        int idx = current == null ? -1 : list.indexOf(current);
        int next;
        if (idx < 0) next = dir > 0 ? 0 : list.size() - 1;
        else next = ((idx + dir) % list.size() + list.size()) % list.size();
        return list.get(next);
    }

    public void cycleAndFollow(SpectatorSession s, int dir) {
        PlatformPlayer p = s.getPlayer();
        PlatformPlayer t = cycle(s, dir);
        if (t == null) {
            if (p != null) plugin.messages().send(p, "errors.no-players");
            return;
        }
        if (s.state == SpectatorState.POV) startPov(s, t);
        else follow(s, t);
    }

    /** Position derrière la cible, sans entrer dans un bloc, orientée vers elle. */
    public Position behind(PlatformPlayer target, int distance) {
        Position base = target.getLocation();
        Position eye = target.getEyeLocation();
        PlatformWorld w = target.getWorld();
        Vector3 dir = base.getDirection().setY(0);
        if (dir.lengthSquared() < 1.0E-4) dir = new Vector3(0, 0, 1);
        dir.normalize();
        double up = plugin.config().getDouble("follow.height", 1.5);
        Vector3 eyeV = eye.toVector();
        Vector3 dest = eyeV.clone();
        double step = 0.5;
        for (double d = step; d <= distance; d += step) {
            Vector3 test = eyeV.clone().subtract(dir.clone().multiply(d)).add(new Vector3(0, up * d / distance, 0));
            if (w.isSolid(test.getBlockX(), test.getBlockY(), test.getBlockZ())
                    || w.isSolid(test.getBlockX(), test.getBlockY() + 1, test.getBlockZ())) {
                break;
            }
            dest = test;
        }
        dest.subtract(new Vector3(0, 1.62, 0));
        Vector3 look = eyeV.clone().subtract(dest.clone().add(new Vector3(0, 1.62, 0)));
        float yaw, pitch;
        if (look.lengthSquared() > 1.0E-4) {
            double dx = look.getX(), dy = look.getY(), dz = look.getZ();
            yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        } else {
            yaw = base.getYaw();
            pitch = base.getPitch();
        }
        return Position.of(base.getWorld(), dest, yaw, pitch);
    }

    /**
     * Suivi fluide : au lieu de téléporter le spectateur en boucle (saccadé), on lui applique
     * chaque tick une vélocité vers la position idéale. Le client interpole le mouvement,
     * et le spectateur garde le contrôle de sa caméra : il regarde librement et peut tourner
     * autour de la cible avec ses touches de déplacement.
     */
    private void followTick() {
        String style = plugin.config().getString("follow.style", "SMOOTH").toUpperCase(Locale.ROOT);
        double height = plugin.config().getDouble("follow.height", 1.5);
        double gain = Math.max(0.05, Math.min(1, plugin.config().getDouble("follow.smoothness", 0.25)));
        double maxSpeed = plugin.config().getDouble("follow.max-speed", 2.5);
        double tpDistance = plugin.config().getDouble("follow.teleport-distance", 32);
        double leash = plugin.config().getDouble("follow.leash-distance", 12);
        for (SpectatorSession s : sessions.values()) {
            if (s.state != SpectatorState.FOLLOWING || s.frozen) continue;
            PlatformPlayer p = s.getPlayer();
            if (p == null) continue;
            PlatformPlayer t = s.getFollowTarget();
            if (t == null || !t.isOnline() || isSpectator(t) || t.isDead()) {
                targetLost(s, p);
                continue;
            }
            Preferences prefs = plugin.filters().get(p);
            Position tl = t.getLocation();
            Position cur = p.getLocation();
            if (!cur.sameWorld(tl)) {
                p.teleport(behind(t, prefs.followDistance));
                s.lastTargetPos = null;
                continue;
            }
            // déplacement de la cible depuis le tick précédent (anticipation)
            Vector3 targetPos = tl.toVector();
            Vector3 motion = new Vector3(0, 0, 0);
            if (s.lastTargetPos != null && tl.getWorld().equals(s.lastTargetWorld)) {
                motion = targetPos.clone().subtract(s.lastTargetPos);
                if (motion.lengthSquared() > 25) motion = new Vector3(0, 0, 0); // la cible s'est téléportée
            }
            s.lastTargetPos = targetPos;
            s.lastTargetWorld = tl.getWorld();

            double distance = prefs.followDistance;
            if (style.equals("LEASH")) {
                if (cur.distanceSquared(tl) <= leash * leash) continue; // libre dans le rayon
                distance = leash * 0.8;
            }
            Vector3 desired = desiredPosition(style, p, t, distance, height);
            Vector3 delta = desired.clone().subtract(cur.toVector());
            if (delta.length() > tpDistance) {
                p.teleport(Position.of(cur.getWorld(), desired, cur.getYaw(), cur.getPitch()));
                continue;
            }
            Vector3 velocity = motion.add(delta.multiply(gain));
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
    private Vector3 desiredPosition(String style, PlatformPlayer p, PlatformPlayer t, double distance, double height) {
        Position tl = t.getLocation();
        Vector3 dir;
        if (style.equals("BEHIND")) {
            dir = tl.getDirection().setY(0).multiply(-1);
        } else {
            dir = p.getLocation().toVector().subtract(tl.toVector()).setY(0);
            if (dir.lengthSquared() < 0.25) dir = tl.getDirection().setY(0).multiply(-1);
        }
        if (dir.lengthSquared() < 1.0E-4) dir = new Vector3(0, 0, -1);
        dir.normalize();
        Vector3 ideal = tl.toVector().add(dir.clone().multiply(distance)).add(new Vector3(0, height, 0));
        if (noclipAllowed(p)) return ideal;
        // sans passe-muraille : on raccourcit la distance devant un mur
        PlatformWorld w = t.getWorld();
        Vector3 eye = t.getEyeLocation().toVector();
        Vector3 target = ideal.clone().add(new Vector3(0, 1.62, 0));
        Vector3 step = target.clone().subtract(eye);
        double len = step.length();
        if (len < 1.0E-3) return ideal;
        step.normalize().multiply(0.5);
        Vector3 last = eye.clone();
        Vector3 probe = eye.clone();
        for (double d = 0.5; d <= len; d += 0.5) {
            probe.add(step);
            if (w.isSolid(probe.getBlockX(), probe.getBlockY(), probe.getBlockZ())) break;
            last = probe.clone();
        }
        return last.subtract(new Vector3(0, 1.62, 0));
    }

    // ------------------------------------------------------------------ passe-muraille

    /**
     * Le mode Adventure ne permet pas de traverser les blocs (les collisions sont calculées par le client).
     * Quand le spectateur touche un bloc, il passe donc temporairement en mode Spectator vanilla,
     * puis revient en Adventure (avec sa barre d'inventaire) dès qu'il est de nouveau à l'air libre.
     */
    private boolean noclipAllowed(PlatformPlayer p) {
        return plugin.config().getBoolean("spectator.noclip.enabled", true) && plugin.filters().get(p).noclip;
    }

    private void noclipTick() {
        for (SpectatorSession s : sessions.values()) {
            PlatformPlayer p = s.getPlayer();
            if (p == null) continue;
            Vector3 pos = p.getLocation().toVector();
            Vector3 movement = s.lastPos == null ? new Vector3(0, 0, 0) : pos.clone().subtract(s.lastPos);
            if (movement.lengthSquared() > 25) movement = new Vector3(0, 0, 0);
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

    private void exitNoclip(SpectatorSession s, PlatformPlayer p) {
        s.noclip = false;
        if (p.getGameMode() == GameMode.SPECTATOR) {
            p.setGameMode(GameMode.ADVENTURE);
            p.setAllowFlight(true);
            p.setFlying(true);
            applyFlySpeed(p);
        }
    }

    /** Un bloc solide touche (ou va toucher au prochain tick) la hitbox du joueur, avec une marge. */
    private boolean nearSolid(PlatformPlayer p, Vector3 movement) {
        Position l = p.getLocation();
        PlatformWorld w = p.getWorld();
        double margin = plugin.config().getDouble("spectator.noclip.margin", 0.45);
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
                        if (w.isSolid((int) Math.floor(bx + ix * margin), (int) Math.floor(by + dy),
                                (int) Math.floor(bz + iz * margin))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void targetLost(SpectatorSession s, PlatformPlayer p) {
        if (plugin.config().getBoolean("follow.auto-next-on-target-lost", true)) {
            PlatformPlayer next = cycle(s, 1);
            if (next != null && !same(next, s.getFollowTarget())) {
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

    public void startPov(final SpectatorSession s, final PlatformPlayer target) {
        final PlatformPlayer p = s.getPlayer();
        if (p == null || target == null) return;
        if (!plugin.config().getBoolean("pov.enabled", true) || !p.hasPermission("spectatorplus.pov")) {
            plugin.messages().send(p, "errors.no-permission");
            return;
        }
        if (same(target, p) || isSpectator(target)) {
            plugin.messages().send(p, "errors.invalid-target");
            return;
        }
        PlatformPlayer current = s.getFollowTarget();
        if (current != null && !same(current, target)) {
            if (!plugin.platform().api().followChange(p, current, target)) return;
        } else if (current == null) {
            if (!plugin.platform().api().followStart(p, target)) return;
        }
        plugin.menus().close(p);
        s.state = SpectatorState.POV;
        s.target = target.getUniqueId();
        s.povExitRequested = false;
        p.teleport(target.getLocation());
        p.setGameMode(GameMode.SPECTATOR);
        plugin.platform().runLater(new Runnable() {
            @Override
            public void run() {
                if (s.state == SpectatorState.POV && p.isOnline() && target.isOnline()) p.setSpectatorTarget(target);
            }
        }, 3L);
        plugin.messages().send(p, "spectator.pov-start", "target", target.getName());
    }

    public void stopPov(SpectatorSession s) {
        PlatformPlayer p = s.getPlayer();
        if (s.state != SpectatorState.POV) return;
        PlatformPlayer previous = s.getFollowTarget();
        s.state = SpectatorState.FREE;
        s.target = null;
        if (p == null) return;
        exitPovMode(s, p);
        if (previous != null && previous.isOnline()) p.teleport(behind(previous, plugin.filters().get(p).followDistance));
        plugin.platform().api().followStop(p, previous);
        plugin.messages().send(p, "spectator.pov-stop");
    }

    private void exitPovMode(SpectatorSession s, PlatformPlayer p) {
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
            PlatformPlayer p = s.getPlayer();
            if (p == null) continue;
            PlatformPlayer t = s.getFollowTarget();
            if (s.povExitRequested) {
                stopPov(s);
                continue;
            }
            if (t == null || !t.isOnline() || isSpectator(t) || t.isDead()) {
                PlatformPlayer next = plugin.config().getBoolean("follow.auto-next-on-target-lost", true) ? cycle(s, 1) : null;
                if (next != null && !same(next, t)) {
                    startPov(s, next);
                } else {
                    stopPov(s);
                    plugin.messages().send(p, "spectator.target-lost");
                }
                continue;
            }
            UUID current = p.getSpectatorTargetId();
            if (current == null || !current.equals(t.getUniqueId())) {
                if (p.getGameMode() != GameMode.SPECTATOR) p.setGameMode(GameMode.SPECTATOR);
                Position pl = p.getLocation(), tl = t.getLocation();
                if (!pl.sameWorld(tl) || pl.distanceSquared(tl) > 4) p.teleport(tl);
                p.setSpectatorTarget(t);
            }
        }
    }

    // ------------------------------------------------------------------ HUD

    private void hudTick() {
        for (SpectatorSession s : sessions.values()) {
            PlatformPlayer p = s.getPlayer();
            if (p == null) continue;
            Preferences prefs = plugin.filters().get(p);
            if (!prefs.actionBar) continue;
            PlatformPlayer t = s.getFollowTarget();
            String text;
            if (t != null && (s.state == SpectatorState.FOLLOWING || s.state == SpectatorState.POV)) {
                Position pl = p.getLocation(), tl = t.getLocation();
                String dist = pl.sameWorld(tl) ? String.valueOf((int) pl.distance(tl)) : "?";
                text = plugin.messages().get(p, s.state == SpectatorState.POV ? "hud.pov" : "hud.following",
                        "target", t.getName(),
                        "health", Text.hearts(t.getHealth()),
                        "max_health", Text.hearts(t.getMaxHealth()),
                        "food", t.getFoodLevel(),
                        "distance", dist,
                        "world", t.getWorld().getName(),
                        "kills", plugin.stats().kills(t.getUniqueId()));
            } else if (plugin.config().getBoolean("hud.show-when-free", true)) {
                text = plugin.messages().get(p, "hud.free",
                        "alive", plugin.game().getAlivePlayers().size(),
                        "spectators", sessions.size(),
                        "time", Text.duration(plugin.game().getGameDuration()));
            } else {
                continue;
            }
            p.actionBar(plugin.placeholders().apply(text, p, null));
        }
    }
}
