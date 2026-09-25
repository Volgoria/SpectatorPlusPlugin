package fr.spectatorplus.event.detector;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.api.event.SpectatorGameEvent;
import fr.spectatorplus.compat.Compat;
import fr.spectatorplus.compat.Version;
import fr.spectatorplus.event.CombatTracker;
import fr.spectatorplus.event.EventSettings;
import fr.spectatorplus.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Combat PvP, combat PvE, morts et éliminations.
 */
public final class CombatDetector extends Detector {

    private final Map<UUID, UUID> lastMobAttacker = new HashMap<>();

    public CombatDetector(SpectatorPlus plugin) {
        super(plugin);
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                for (CombatTracker.Combat c : plugin.combat().expired(combatTimeout())) combatEnd(c);
            }
        }, 20L, 20L);
    }

    private long combatTimeout() {
        return plugin.events().settings("pvp.combat_start").integer("timeout", 15) * 1000L;
    }

    private void combatEnd(CombatTracker.Combat c) {
        if (on("pvp.combat_end") == null) return;
        Player a = Bukkit.getPlayer(c.a), b = Bukkit.getPlayer(c.b);
        if (a == null) return;
        SpectatorGameEvent.Builder builder = ev("pvp.combat_end", a)
                .data("attacker", a.getName())
                .data("victim", b == null ? "?" : b.getName())
                .data("duration", Text.duration((c.lastHit - c.start) / 1000));
        if (b != null) builder.player(b);
        fire(builder);
    }

    /** Joueur responsable des dégâts (mêlée ou projectile). */
    private static Player attackerOf(Entity damager) {
        if (damager instanceof Player) return (Player) damager;
        if (damager instanceof Projectile) {
            ProjectileSource src = ((Projectile) damager).getShooter();
            if (src instanceof Player) return (Player) src;
        }
        return null;
    }

    private static LivingEntity mobOf(Entity damager) {
        if (damager instanceof Projectile) {
            ProjectileSource src = ((Projectile) damager).getShooter();
            if (src instanceof LivingEntity && !(src instanceof Player)) return (LivingEntity) src;
            return null;
        }
        return damager instanceof LivingEntity && !(damager instanceof Player) ? (LivingEntity) damager : null;
    }

    @SuppressWarnings("deprecation")
    private static boolean isCritical(Player attacker, Entity damager) {
        if (damager != attacker) return false;
        if (attacker.isOnGround() || attacker.getFallDistance() <= 0 || attacker.isInsideVehicle()) return false;
        String block = attacker.getLocation().getBlock().getType().name();
        if (block.contains("WATER") || block.contains("LADDER") || block.contains("VINE")) return false;
        for (PotionEffect e : attacker.getActivePotionEffects()) {
            if (Compat.effectKeys(e.getType()).contains("BLINDNESS")) return false;
        }
        return !(Version.atLeast(1, 9) && attacker.isSprinting());
    }

    private SpectatorGameEvent.Builder pvp(String id, Player subject, Player other, Player attacker, Player victim,
                                           double dmg, String cause, boolean crit, ItemStack weapon) {
        SpectatorGameEvent.Builder b = ev(id, subject).player(other)
                .damage(dmg, cause, true, crit)
                .data("attacker", attacker.getName())
                .data("victim", victim.getName())
                .data("damage", Text.hearts(dmg))
                .data("damage_type", pretty(cause))
                .data("weapon", weapon == null || weapon.getType().name().endsWith("AIR") ? "-" : pretty(weapon.getType().name()))
                .data("player_last_damage", Text.hearts(dmg))
                .data("player_health_after_damage", Text.hearts(Math.max(0, victim.getHealth() - dmg)));
        return b;
    }

    // ------------------------------------------------------------------ dégâts entre entités

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        Entity victimEntity = e.getEntity();
        Player attacker = attackerOf(damager);
        double dmg = e.getFinalDamage();
        String cause = e.getCause().name();

        if (victimEntity instanceof Player && attacker != null && attacker != victimEntity) {
            Player victim = (Player) victimEntity;
            if (!tracked(victim) || !tracked(attacker)) return;
            pvpHit(e, damager, attacker, victim, dmg, cause);
            return;
        }
        if (victimEntity instanceof LivingEntity && !(victimEntity instanceof Player) && attacker != null && tracked(attacker)) {
            pveAttack(attacker, (LivingEntity) victimEntity, dmg);
            return;
        }
        if (victimEntity instanceof Player && tracked((Player) victimEntity)) {
            LivingEntity mob = mobOf(damager);
            EventSettings s = on("pve.damage");
            if (mob != null && s != null && s.accepts("entities", mob.getType().name()) && dmg >= s.number("min-damage", 0)) {
                Player p = (Player) victimEntity;
                fire(ev("pve.damage", p).damage(dmg, cause, false, false)
                        .data("damage", Text.hearts(dmg)).data("damage_type", pretty(cause))
                        .data("entity", pretty(mob.getType().name())).data("entity_type", mob.getType().name())
                        .data("mob", pretty(mob.getType().name())).data("mob_damage", Text.hearts(dmg))
                        .data("mob_health", Text.hearts(mob.getHealth()))
                        .data("attacker", pretty(mob.getType().name())).data("victim", p.getName()));
            }
        }
    }

    private void pvpHit(EntityDamageByEntityEvent e, Entity damager, Player attacker, Player victim, double dmg, String cause) {
        boolean crit = isCritical(attacker, damager);
        ItemStack weapon = damager == attacker ? Compat.mainHand(attacker) : null;
        CombatTracker.HitResult hit = plugin.combat().hit(attacker.getUniqueId(), victim.getUniqueId(), combatTimeout());

        if (on("pvp.attack") != null) fire(pvp("pvp.attack", attacker, victim, attacker, victim, dmg, cause, crit, weapon));
        if (hit.firstHitEver && on("pvp.first_hit") != null) {
            fire(pvp("pvp.first_hit", attacker, victim, attacker, victim, dmg, cause, crit, weapon));
        }
        if (hit.combatStarted && on("pvp.combat_start") != null) {
            fire(pvp("pvp.combat_start", attacker, victim, attacker, victim, dmg, cause, crit, weapon));
        }
        EventSettings s = on("pvp.damage_dealt");
        if (s != null && dmg >= s.number("min-damage", 0)) {
            fire(pvp("pvp.damage_dealt", attacker, victim, attacker, victim, dmg, cause, crit, weapon));
        }
        s = on("pvp.damage_taken");
        if (s != null && dmg >= s.number("min-damage", 0)) {
            fire(pvp("pvp.damage_taken", victim, attacker, attacker, victim, dmg, cause, crit, weapon));
        }
        if (crit && on("pvp.critical") != null) fire(pvp("pvp.critical", attacker, victim, attacker, victim, dmg, cause, true, weapon));

        String dType = damager.getType().name();
        if (damager instanceof Projectile && (dType.contains("ARROW"))) {
            s = on("pvp.bow_shot");
            if (s != null && attacker.getWorld().equals(victim.getWorld())) {
                double dist = attacker.getLocation().distance(victim.getLocation());
                if (dist >= s.number("min-distance", 30)) {
                    fire(pvp("pvp.bow_shot", attacker, victim, attacker, victim, dmg, cause, crit, weapon)
                            .data("distance", (int) dist));
                }
            }
        }
        if (dType.equals("TRIDENT") && on("pvp.trident") != null) {
            fire(pvp("pvp.trident", victim, attacker, attacker, victim, dmg, cause, crit, weapon));
        }
        boolean blocked = false;
        try {
            blocked = e.isApplicable(EntityDamageEvent.DamageModifier.BLOCKING)
                    && e.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) < 0;
        } catch (Throwable ignored) {
            // DamageModifier absent sur certaines versions
        }
        if (blocked && on("pvp.shield_block") != null) {
            fire(pvp("pvp.shield_block", victim, attacker, attacker, victim, dmg, cause, crit, weapon));
        }
        if (Version.atLeast(1, 9) && victim.isBlocking() && weapon != null && weapon.getType().name().endsWith("_AXE")
                && on("pvp.shield_break") != null) {
            fire(pvp("pvp.shield_break", attacker, victim, attacker, victim, dmg, cause, crit, weapon));
        }
    }

    private void pveAttack(Player attacker, LivingEntity mob, double dmg) {
        lastMobAttacker.put(mob.getUniqueId(), attacker.getUniqueId());
        String type = mob.getType().name();
        EventSettings s = on("pve.attack");
        if (s != null && !s.names("entities").isEmpty() && s.accepts("entities", type)) fire(mob(ev("pve.attack", attacker), mob, dmg));
        if (type.equals("ENDER_DRAGON") && on("pve.dragon_attack") != null) fire(mob(ev("pve.dragon_attack", attacker), mob, dmg));
        if (type.equals("WITHER") && on("pve.wither_attack") != null) fire(mob(ev("pve.wither_attack", attacker), mob, dmg));
    }

    private static SpectatorGameEvent.Builder mob(SpectatorGameEvent.Builder b, LivingEntity mob, double dmg) {
        String type = mob.getType().name();
        return b.data("entity", pretty(type)).data("entity_type", type).data("mob", pretty(type))
                .data("mob_health", Text.hearts(Math.max(0, mob.getHealth() - dmg)))
                .data("mob_damage", Text.hearts(dmg));
    }

    // ------------------------------------------------------------------ morts de joueurs

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent e) {
        final Player p = e.getEntity();
        if (!tracked(p)) return;
        UUID id = p.getUniqueId();
        Location loc = p.getLocation();
        int deaths = plugin.stats().increment(id, "deaths", 1);

        EntityDamageEvent last = p.getLastDamageCause();
        String cause = last == null ? "CUSTOM" : last.getCause().name();
        Entity damagerEntity = last instanceof EntityDamageByEntityEvent ? ((EntityDamageByEntityEvent) last).getDamager() : null;
        LivingEntity mob = mobOf(damagerEntity);

        Player killer = p.getKiller();
        long assistWindow = plugin.events().settings("pvp.assist").integer("max-seconds", 10) * 1000L;
        if (killer == null) {
            UUID lastAttacker = plugin.combat().lastAttacker(id, assistWindow);
            if (lastAttacker != null) killer = Bukkit.getPlayer(lastAttacker);
        }
        if (killer == p) killer = null;

        String killerName = killer != null ? killer.getName() : mob != null ? pretty(mob.getType().name()) : pretty(cause);
        SpectatorGameEvent.Builder base = death("death.death", p, loc, cause, killerName);
        if (on("death.death") != null) fire(base);
        if (plugin.stats().firstGlobal("death") && on("death.first_death") != null) {
            fire(death("death.first_death", p, loc, cause, killerName));
        }
        EventSettings s = on("death.count");
        if (s != null && s.ints("amounts").contains(deaths)) fire(death("death.count", p, loc, cause, killerName).data("amount", deaths));

        // causes précises
        String causeEvent = null;
        if (cause.equals("FALL")) causeEvent = "death.fall";
        else if (cause.equals("LAVA")) causeEvent = "death.lava";
        else if (cause.equals("FIRE") || cause.equals("FIRE_TICK")) causeEvent = "death.fire";
        else if (cause.equals("BLOCK_EXPLOSION") || cause.equals("ENTITY_EXPLOSION")) causeEvent = "death.explosion";
        else if (cause.equals("VOID")) causeEvent = "death.void";
        else if (cause.equals("WORLD_BORDER") || (cause.equals("SUFFOCATION") && Compat.isOutsideBorder(loc))) causeEvent = "death.border";
        if (causeEvent != null && on(causeEvent) != null) fire(death(causeEvent, p, loc, cause, killerName));

        if (mob != null && killer == null) {
            s = on("death.mob");
            if (s != null && s.accepts("entities", mob.getType().name())) {
                fire(death("death.mob", p, loc, cause, killerName).data("entity", pretty(mob.getType().name()))
                        .data("mob", pretty(mob.getType().name())).data("entity_type", mob.getType().name()));
            }
        }
        if (killer == null) {
            s = on("death.pve");
            if (s != null && s.accepts("causes", cause)) fire(death("death.pve", p, loc, cause, killerName));
        }

        s = on("death.after_totem");
        long totem = plugin.stats().lastTotem(id);
        if (s != null && totem > 0 && System.currentTimeMillis() - totem <= s.integer("max-seconds", 30) * 1000L) {
            fire(death("death.after_totem", p, loc, cause, killerName)
                    .data("seconds", (System.currentTimeMillis() - totem) / 1000));
        }
        s = on("death.after_combat");
        long lastCombat = plugin.combat().lastCombatTime(id);
        if (s != null && p.getKiller() == null && lastCombat > 0
                && System.currentTimeMillis() - lastCombat <= s.integer("seconds", 10) * 1000L) {
            fire(death("death.after_combat", p, loc, cause, killerName)
                    .data("seconds", (System.currentTimeMillis() - lastCombat) / 1000));
        }

        // victime : fin de série
        int oldStreak = plugin.stats().resetStreak(id);
        s = on("pvp.streak_end");
        if (s != null && oldStreak >= s.integer("min-kills", 3)) {
            SpectatorGameEvent.Builder b = death("pvp.streak_end", p, loc, cause, killerName).data("streak", oldStreak);
            if (killer != null) b.player(killer);
            fire(b);
        }

        if (killer != null && tracked(killer)) kill(killer, p, loc, cause, assistWindow);

        for (CombatTracker.Combat c : plugin.combat().endAll(id)) combatEnd(c);
        plugin.combat().clearVictim(id);
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.game().checkAliveCount();
            }
        }, 5L);
    }

    private SpectatorGameEvent.Builder death(String id, Player p, Location loc, String cause, String killer) {
        return ev(id, p).location(loc)
                .data("dead_player", p.getName()).data("victim", p.getName())
                .data("killer", killer)
                .data("death_cause", pretty(cause))
                .data("death_x", loc.getBlockX()).data("death_y", loc.getBlockY()).data("death_z", loc.getBlockZ())
                .data("death_world", loc.getWorld().getName());
    }

    private void kill(Player killer, Player victim, Location loc, String cause, long assistWindow) {
        UUID k = killer.getUniqueId();
        int kills = plugin.stats().increment(k, "kills", 1);
        int streak = plugin.stats().incrementStreak(k);
        ItemStack weapon = Compat.mainHand(killer);
        String weaponName = weapon == null || weapon.getType().name().endsWith("AIR") ? "-" : pretty(weapon.getType().name());

        if (on("death.killed_by_player") != null) {
            fire(death("death.killed_by_player", victim, loc, cause, killer.getName()).player(killer)
                    .data("attacker", killer.getName()).data("weapon", weaponName).data("kills", kills));
        }
        if (plugin.stats().firstGlobal("kill") && on("pvp.first_kill") != null) {
            fire(killEvent("pvp.first_kill", killer, victim, loc, cause, weaponName).data("kills", kills));
        }
        EventSettings doubleSettings = plugin.events().settings("pvp.double_kill");
        EventSettings tripleSettings = plugin.events().settings("pvp.triple_kill");
        long window = Math.max(doubleSettings.integer("max-seconds", 10), tripleSettings.integer("max-seconds", 15)) * 1000L;
        int recent = plugin.stats().recentKills(k, window);
        if (recent == 2 && on("pvp.double_kill") != null) fire(killEvent("pvp.double_kill", killer, victim, loc, cause, weaponName));
        if (recent == 3 && on("pvp.triple_kill") != null) fire(killEvent("pvp.triple_kill", killer, victim, loc, cause, weaponName));

        EventSettings s = on("pvp.kill_streak");
        if (s != null && s.ints("amounts").contains(streak)) {
            fire(killEvent("pvp.kill_streak", killer, victim, loc, cause, weaponName).data("streak", streak).data("amount", streak));
        }
        int same = plugin.stats().killOn(k, victim.getUniqueId());
        s = on("pvp.repeat_kill");
        if (s != null && s.ints("amounts").contains(same)) {
            fire(killEvent("pvp.repeat_kill", killer, victim, loc, cause, weaponName).data("amount", same));
        }
        s = on("pvp.assist");
        if (s != null) {
            List<UUID> assists = plugin.combat().assists(victim.getUniqueId(), k, assistWindow);
            for (UUID a : assists) {
                Player ap = Bukkit.getPlayer(a);
                if (ap == null || !tracked(ap)) continue;
                fire(ev("pvp.assist", ap).player(victim).player(killer)
                        .data("assist", ap.getName()).data("killer", killer.getName())
                        .data("victim", victim.getName()).data("dead_player", victim.getName()));
            }
        }
    }

    private SpectatorGameEvent.Builder killEvent(String id, Player killer, Player victim, Location loc, String cause, String weapon) {
        return ev(id, killer).player(victim).location(loc)
                .data("killer", killer.getName()).data("attacker", killer.getName())
                .data("victim", victim.getName()).data("dead_player", victim.getName())
                .data("weapon", weapon).data("death_cause", pretty(cause))
                .data("kills", plugin.stats().kills(killer.getUniqueId()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        final Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (p.isOnline() && tracked(p) && on("death.respawn") != null) fire(ev("death.respawn", p));
            }
        }, 3L);
    }

    // ------------------------------------------------------------------ PvE : morts de mobs

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent e) {
        LivingEntity mob = e.getEntity();
        if (mob instanceof Player) return;
        Player killer = mob.getKiller();
        UUID lastAttacker = lastMobAttacker.remove(mob.getUniqueId());
        if (killer == null && lastAttacker != null) killer = Bukkit.getPlayer(lastAttacker);
        if (killer == null || !tracked(killer)) return;
        String type = mob.getType().name();
        UUID k = killer.getUniqueId();

        EventSettings s = on("pve.kill");
        if (s != null && !s.names("entities").isEmpty() && s.accepts("entities", type)) fire(mob(ev("pve.kill", killer), mob, 0));
        boolean first = plugin.stats().first(k, "mob." + type);
        s = on("pve.first_kill");
        if (s != null && first && !s.names("entities").isEmpty() && s.accepts("entities", type)) {
            fire(mob(ev("pve.first_kill", killer), mob, 0));
        }
        int count = plugin.stats().increment(k, "mob." + type, 1);
        int total = plugin.stats().increment(k, "mobs", 1);
        s = on("pve.kill_count");
        if (s != null) {
            if (s.names("entities").isEmpty()) {
                if (s.ints("amounts").contains(total)) fire(mob(ev("pve.kill_count", killer), mob, 0).data("amount", total));
            } else if (s.accepts("entities", type) && s.ints("amounts").contains(count)) {
                fire(mob(ev("pve.kill_count", killer), mob, 0).data("amount", count));
            }
        }
        s = on("pve.boss_kill");
        if (s != null && s.accepts("bosses", type)) fire(mob(ev("pve.boss_kill", killer), mob, 0));
        if (type.equals("ENDER_DRAGON") && on("pve.dragon_kill") != null) fire(mob(ev("pve.dragon_kill", killer), mob, 0));
        if (type.equals("WITHER") && on("pve.wither_kill") != null) fire(mob(ev("pve.wither_kill", killer), mob, 0));
        if (type.equals("WARDEN") && on("pve.warden_kill") != null) fire(mob(ev("pve.warden_kill", killer), mob, 0));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (!e.getSpawnReason().name().equals("BUILD_WITHER") || on("pve.wither_summon") == null) return;
        Player nearest = null;
        double best = 100;
        for (Player p : e.getLocation().getWorld().getPlayers()) {
            if (!tracked(p)) continue;
            double d = p.getLocation().distanceSquared(e.getLocation());
            if (d < best) {
                best = d;
                nearest = p;
            }
        }
        if (nearest != null) fire(ev("pve.wither_summon", nearest).location(e.getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent e) {
        if (!(e.getTarget() instanceof Player)) return;
        Player p = (Player) e.getTarget();
        if (!tracked(p)) return;
        String type = e.getEntity().getType().name();
        if (type.equals("WARDEN")) {
            if (on("pve.warden_trigger") != null) {
                fire(ev("pve.warden_trigger", p).data("entity", pretty(type)).data("mob", pretty(type)));
            }
            return;
        }
        EventSettings s = on("pve.encounter");
        if (s != null && !s.names("entities").isEmpty() && s.accepts("entities", type)) {
            fire(ev("pve.encounter", p).data("entity", pretty(type)).data("entity_type", type).data("mob", pretty(type)));
        }
    }
}
