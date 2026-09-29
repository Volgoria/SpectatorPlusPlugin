package fr.spectatorplus.event.detect;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.core.event.GameEvent;
import fr.spectatorplus.core.platform.ItemRef;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.Position;
import fr.spectatorplus.event.CombatTracker;
import fr.spectatorplus.event.EventSettings;
import fr.spectatorplus.util.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Combat PvP, combat PvE, morts et éliminations.
 */
public final class CombatSignals extends Detection {

    private final Map<UUID, UUID> lastMobAttacker = new HashMap<>();

    public CombatSignals(SpectatorCore plugin) {
        super(plugin);
    }

    public void start() {
        plugin.platform().runTimer(new Runnable() {
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
        PlatformPlayer a = plugin.platform().getPlayer(c.a), b = plugin.platform().getPlayer(c.b);
        if (a == null) return;
        GameEvent.Builder builder = ev("pvp.combat_end", a)
                .data("attacker", a.getName())
                .data("victim", b == null ? "?" : b.getName())
                .data("duration", Text.duration((c.lastHit - c.start) / 1000));
        if (b != null) builder.player(b);
        fire(builder);
    }

    private GameEvent.Builder pvp(String id, PlatformPlayer subject, PlatformPlayer other, PlatformPlayer attacker,
                                  PlatformPlayer victim, double dmg, String cause, boolean crit, ItemRef weapon, double victimHealth) {
        return ev(id, subject).player(other)
                .damage(dmg, cause, true, crit)
                .data("attacker", attacker.getName())
                .data("victim", victim.getName())
                .data("damage", Text.hearts(dmg))
                .data("damage_type", pretty(cause))
                .data("weapon", empty(weapon) ? "-" : pretty(weapon.getType()))
                .data("player_last_damage", Text.hearts(dmg))
                .data("player_health_after_damage", plugin.game().hearts(Math.max(0, victimHealth - dmg)));
    }

    // ------------------------------------------------------------------ dégâts entre entités

    /** Dégâts causés par une entité (joueur, créature, projectile). */
    public void entityDamage(Damage d) {
        PlatformPlayer attacker = d.attacker;
        double dmg = d.amount;
        String cause = d.cause;
        if (d.victim != null && attacker != null && !attacker.equals(d.victim)) {
            if (!tracked(d.victim) || !tracked(attacker)) return;
            pvpHit(d);
            return;
        }
        if (d.victimMob != null && attacker != null && tracked(attacker)) {
            pveAttack(attacker, d.victimMob, dmg);
            return;
        }
        if (d.victim != null && tracked(d.victim)) {
            EntityInfo mob = d.attackerMob;
            EventSettings s = on("pve.damage");
            if (mob != null && s != null && s.accepts("entities", mob.getType()) && dmg >= s.number("min-damage", 0)) {
                PlatformPlayer p = d.victim;
                fire(ev("pve.damage", p).damage(dmg, cause, false, false)
                        .data("damage", Text.hearts(dmg)).data("damage_type", pretty(cause))
                        .data("entity", pretty(mob.getType())).data("entity_type", mob.getType())
                        .data("mob", pretty(mob.getType())).data("mob_damage", Text.hearts(dmg))
                        .data("mob_health", Text.hearts(mob.getHealth()))
                        .data("attacker", pretty(mob.getType())).data("victim", p.getName()));
            }
        }
    }

    private void pvpHit(Damage d) {
        PlatformPlayer attacker = d.attacker, victim = d.victim;
        double dmg = d.amount, hp = d.victimHealthBefore;
        String cause = d.cause;
        boolean crit = d.critical;
        ItemRef weapon = d.weapon;
        CombatTracker.HitResult hit = plugin.combat().hit(attacker.getUniqueId(), victim.getUniqueId(), combatTimeout());
        plugin.spectators().combatHit(attacker.getUniqueId(), victim.getUniqueId());

        if (on("pvp.attack") != null) fire(pvp("pvp.attack", attacker, victim, attacker, victim, dmg, cause, crit, weapon, hp));
        if (hit.firstHitEver && on("pvp.first_hit") != null) {
            fire(pvp("pvp.first_hit", attacker, victim, attacker, victim, dmg, cause, crit, weapon, hp));
        }
        if (hit.combatStarted && on("pvp.combat_start") != null) {
            fire(pvp("pvp.combat_start", attacker, victim, attacker, victim, dmg, cause, crit, weapon, hp));
        }
        EventSettings s = on("pvp.damage_dealt");
        if (s != null && dmg >= s.number("min-damage", 0)) {
            fire(pvp("pvp.damage_dealt", attacker, victim, attacker, victim, dmg, cause, crit, weapon, hp));
        }
        s = on("pvp.damage_taken");
        if (s != null && dmg >= s.number("min-damage", 0)) {
            fire(pvp("pvp.damage_taken", victim, attacker, attacker, victim, dmg, cause, crit, weapon, hp));
        }
        if (crit && on("pvp.critical") != null) fire(pvp("pvp.critical", attacker, victim, attacker, victim, dmg, cause, true, weapon, hp));

        String dType = d.directType == null ? "" : d.directType;
        if (d.projectile && dType.contains("ARROW")) {
            s = on("pvp.bow_shot");
            Position al = attacker.getLocation(), vl = victim.getLocation();
            if (s != null && al.sameWorld(vl)) {
                double dist = al.distance(vl);
                if (dist >= s.number("min-distance", 30)) {
                    fire(pvp("pvp.bow_shot", attacker, victim, attacker, victim, dmg, cause, crit, weapon, hp)
                            .data("distance", (int) dist));
                }
            }
        }
        if (dType.equals("TRIDENT") && on("pvp.trident") != null) {
            fire(pvp("pvp.trident", victim, attacker, attacker, victim, dmg, cause, crit, weapon, hp));
        }
        if (d.blocked && on("pvp.shield_block") != null) {
            fire(pvp("pvp.shield_block", victim, attacker, attacker, victim, dmg, cause, crit, weapon, hp));
        }
        if (d.victimBlocking && !empty(weapon) && weapon.getType().endsWith("_AXE") && on("pvp.shield_break") != null) {
            fire(pvp("pvp.shield_break", attacker, victim, attacker, victim, dmg, cause, crit, weapon, hp));
        }
    }

    private void pveAttack(PlatformPlayer attacker, EntityInfo mob, double dmg) {
        lastMobAttacker.put(mob.getId(), attacker.getUniqueId());
        String type = mob.getType();
        EventSettings s = on("pve.attack");
        if (s != null && !s.names("entities").isEmpty() && s.accepts("entities", type)) fire(mob(ev("pve.attack", attacker), mob, dmg));
        if (type.equals("ENDER_DRAGON") && on("pve.dragon_attack") != null) fire(mob(ev("pve.dragon_attack", attacker), mob, dmg));
        if (type.equals("WITHER") && on("pve.wither_attack") != null) fire(mob(ev("pve.wither_attack", attacker), mob, dmg));
    }

    private static GameEvent.Builder mob(GameEvent.Builder b, EntityInfo mob, double dmg) {
        String type = mob.getType();
        return b.data("entity", pretty(type)).data("entity_type", type).data("mob", pretty(type))
                .data("mob_health", Text.hearts(Math.max(0, mob.getHealth() - dmg)))
                .data("mob_damage", Text.hearts(dmg));
    }

    // ------------------------------------------------------------------ morts de joueurs

    /**
     * Mort d'un joueur.
     *
     * @param cause        dernière cause de dégâts (noms Bukkit), null = CUSTOM
     * @param directKiller joueur tueur reconnu par le jeu, ou null
     * @param mob          créature responsable, ou null
     */
    public void playerDeath(PlatformPlayer p, String cause, PlatformPlayer directKiller, EntityInfo mob) {
        if (!tracked(p)) return;
        if (cause == null) cause = "CUSTOM";
        UUID id = p.getUniqueId();
        Position loc = p.getLocation();
        int deaths = plugin.stats().increment(id, "deaths", 1);

        PlatformPlayer killer = directKiller;
        long assistWindow = plugin.events().settings("pvp.assist").integer("max-seconds", 10) * 1000L;
        if (killer == null) {
            UUID lastAttacker = plugin.combat().lastAttacker(id, assistWindow);
            if (lastAttacker != null) killer = plugin.platform().getPlayer(lastAttacker);
        }
        if (p.equals(killer)) killer = null;

        String killerName = killer != null ? killer.getName() : mob != null ? pretty(mob.getType()) : pretty(cause);
        if (on("death.death") != null) fire(death("death.death", p, loc, cause, killerName));
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
        else if (cause.equals("WORLD_BORDER") || (cause.equals("SUFFOCATION") && outsideBorder(loc))) causeEvent = "death.border";
        if (causeEvent != null && on(causeEvent) != null) fire(death(causeEvent, p, loc, cause, killerName));

        if (mob != null && killer == null) {
            s = on("death.mob");
            if (s != null && s.accepts("entities", mob.getType())) {
                fire(death("death.mob", p, loc, cause, killerName).data("entity", pretty(mob.getType()))
                        .data("mob", pretty(mob.getType())).data("entity_type", mob.getType()));
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
        if (s != null && directKiller == null && lastCombat > 0
                && System.currentTimeMillis() - lastCombat <= s.integer("seconds", 10) * 1000L) {
            fire(death("death.after_combat", p, loc, cause, killerName)
                    .data("seconds", (System.currentTimeMillis() - lastCombat) / 1000));
        }

        // victime : fin de série
        int oldStreak = plugin.stats().resetStreak(id);
        s = on("pvp.streak_end");
        if (s != null && oldStreak >= s.integer("min-kills", 3)) {
            GameEvent.Builder b = death("pvp.streak_end", p, loc, cause, killerName).data("streak", oldStreak);
            if (killer != null) b.player(killer);
            fire(b);
        }

        if (killer != null && tracked(killer)) kill(killer, p, loc, cause, assistWindow);

        for (CombatTracker.Combat c : plugin.combat().endAll(id)) combatEnd(c);
        plugin.combat().clearVictim(id);
        plugin.spectators().combatOver(id);
        plugin.platform().runLater(new Runnable() {
            @Override
            public void run() {
                plugin.game().checkAliveCount();
            }
        }, 5L);
    }

    private GameEvent.Builder death(String id, PlatformPlayer p, Position loc, String cause, String killer) {
        return ev(id, p).location(loc)
                .data("dead_player", p.getName()).data("victim", p.getName())
                .data("killer", killer)
                .data("death_cause", pretty(cause))
                .data("death_x", loc.getBlockX()).data("death_y", loc.getBlockY()).data("death_z", loc.getBlockZ())
                .data("death_world", loc.getWorld());
    }

    private void kill(PlatformPlayer killer, PlatformPlayer victim, Position loc, String cause, long assistWindow) {
        UUID k = killer.getUniqueId();
        int kills = plugin.stats().increment(k, "kills", 1);
        int streak = plugin.stats().incrementStreak(k);
        ItemRef weapon = killer.getMainHand();
        String weaponName = empty(weapon) ? "-" : pretty(weapon.getType());

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
                PlatformPlayer ap = plugin.platform().getPlayer(a);
                if (ap == null || !tracked(ap)) continue;
                fire(ev("pvp.assist", ap).player(victim).player(killer)
                        .data("assist", ap.getName()).data("killer", killer.getName())
                        .data("victim", victim.getName()).data("dead_player", victim.getName()));
            }
        }
    }

    private GameEvent.Builder killEvent(String id, PlatformPlayer killer, PlatformPlayer victim, Position loc, String cause, String weapon) {
        return ev(id, killer).player(victim).location(loc)
                .data("killer", killer.getName()).data("attacker", killer.getName())
                .data("victim", victim.getName()).data("dead_player", victim.getName())
                .data("weapon", weapon).data("death_cause", pretty(cause))
                .data("kills", plugin.stats().kills(killer.getUniqueId()));
    }

    /** Réapparition d'un joueur. */
    public void respawn(final PlatformPlayer p) {
        plugin.platform().runLater(new Runnable() {
            @Override
            public void run() {
                if (p.isOnline() && tracked(p) && on("death.respawn") != null) fire(ev("death.respawn", p));
            }
        }, 3L);
    }

    // ------------------------------------------------------------------ PvE : morts de mobs

    /** Mort d'une créature. {@code killer} : joueur reconnu par le jeu, ou null. */
    public void entityDeath(EntityInfo mob, PlatformPlayer killer) {
        UUID lastAttacker = lastMobAttacker.remove(mob.getId());
        if (killer == null && lastAttacker != null) killer = plugin.platform().getPlayer(lastAttacker);
        if (killer == null || !tracked(killer)) return;
        String type = mob.getType();
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

    /** Wither construit : attribué au joueur suivi le plus proche (10 blocs). */
    public void witherSummon(Position loc) {
        if (on("pve.wither_summon") == null) return;
        PlatformPlayer nearest = null;
        double best = 100;
        for (PlatformPlayer p : plugin.platform().getOnlinePlayers()) {
            if (!tracked(p)) continue;
            Position pl = p.getLocation();
            if (!pl.sameWorld(loc)) continue;
            double d = pl.distanceSquared(loc);
            if (d < best) {
                best = d;
                nearest = p;
            }
        }
        if (nearest != null) fire(ev("pve.wither_summon", nearest).location(loc));
    }

    /** Une créature prend un joueur pour cible. */
    public void target(PlatformPlayer p, String mobType) {
        if (!tracked(p)) return;
        if (mobType.equals("WARDEN")) {
            if (on("pve.warden_trigger") != null) {
                fire(ev("pve.warden_trigger", p).data("entity", pretty(mobType)).data("mob", pretty(mobType)));
            }
            return;
        }
        EventSettings s = on("pve.encounter");
        if (s != null && !s.names("entities").isEmpty() && s.accepts("entities", mobType)) {
            fire(ev("pve.encounter", p).data("entity", pretty(mobType)).data("entity_type", mobType).data("mob", pretty(mobType)));
        }
    }
}
