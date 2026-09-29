package fr.spectatorplus.event.detector;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.compat.Compat;
import fr.spectatorplus.compat.Positions;
import fr.spectatorplus.compat.Version;
import fr.spectatorplus.event.detect.Damage;
import fr.spectatorplus.event.detect.EntityInfo;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Combat PvP, combat PvE, morts et éliminations.
 */
public final class CombatDetector extends Detector {

    public CombatDetector(SpectatorPlus plugin) {
        super(plugin);
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

    // ------------------------------------------------------------------ dégâts entre entités

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        Entity victim = e.getEntity();
        Damage d = new Damage();
        fillAttacker(d, damager);
        d.amount = e.getFinalDamage();
        d.cause = e.getCause().name();
        if (victim instanceof Player) {
            Player v = (Player) victim;
            d.victim = w(v);
            d.victimHealthBefore = v.getHealth();
            d.victimBlocking = Version.atLeast(1, 9) && v.isBlocking();
        } else if (victim instanceof LivingEntity) {
            d.victimMob = entity((LivingEntity) victim);
            d.victimHealthBefore = ((LivingEntity) victim).getHealth();
        }
        if (d.attacker != null && damager instanceof Player) {
            Player a = (Player) damager;
            d.critical = isCritical(a, damager);
            d.weapon = item(Compat.mainHand(a));
        }
        try {
            d.blocked = e.isApplicable(EntityDamageEvent.DamageModifier.BLOCKING)
                    && e.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) < 0;
        } catch (Throwable ignored) {
            // DamageModifier absent sur certaines versions
        }
        signals().combat().entityDamage(d);
    }

    // ------------------------------------------------------------------ morts

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        EntityDamageEvent last = p.getLastDamageCause();
        String cause = last == null ? "CUSTOM" : last.getCause().name();
        Entity damager = last instanceof EntityDamageByEntityEvent ? ((EntityDamageByEntityEvent) last).getDamager() : null;
        Player killer = p.getKiller();
        signals().combat().playerDeath(w(p), cause, killer == null ? null : w(killer), entity(mobOf(damager)));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        signals().combat().respawn(w(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent e) {
        LivingEntity mob = e.getEntity();
        if (mob instanceof Player) return;
        Player killer = mob.getKiller();
        EntityInfo info = new EntityInfo(mob.getUniqueId(), mob.getType().name(), mob.getHealth());
        signals().combat().entityDeath(info, killer == null ? null : w(killer));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (e.getSpawnReason().name().equals("BUILD_WITHER")) signals().combat().witherSummon(Positions.of(e.getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent e) {
        if (e.getTarget() instanceof Player) signals().combat().target(w((Player) e.getTarget()), e.getEntity().getType().name());
    }
}
