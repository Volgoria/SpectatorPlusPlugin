package fr.spectatorplus.event.detector;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.bukkit.BukkitItem;
import fr.spectatorplus.bukkit.BukkitPlayer;
import fr.spectatorplus.event.detect.Damage;
import fr.spectatorplus.event.detect.EntityInfo;
import fr.spectatorplus.event.detect.Signals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Base des détecteurs Bukkit : ils traduisent les évènements Bukkit en signaux du code commun
 * ({@link Signals}), qui contient toute la logique des évènements natifs.
 */
public abstract class Detector implements Listener {

    protected final SpectatorPlus plugin;

    protected Detector(SpectatorPlus plugin) {
        this.plugin = plugin;
    }

    protected Signals signals() {
        return plugin.core().signals();
    }

    protected BukkitPlayer w(Player p) {
        return plugin.wrap(p);
    }

    protected static BukkitItem item(ItemStack it) {
        return BukkitItem.of(it);
    }

    /** Renseigne l'attaquant de dégâts : joueur ou créature responsable (le tireur pour un projectile). */
    protected void fillAttacker(Damage d, Entity damager) {
        if (damager == null) return;
        d.directType = damager.getType().name();
        d.projectile = damager instanceof Projectile;
        Entity source = damager;
        if (damager instanceof Projectile) {
            ProjectileSource src = ((Projectile) damager).getShooter();
            if (src instanceof Entity) source = (Entity) src;
        }
        if (source instanceof Player) {
            d.attacker = w((Player) source);
        } else if (source instanceof LivingEntity) {
            d.attackerMob = entity((LivingEntity) source);
        } else if (source != damager) {
            d.directType = source.getType().name();
        }
    }

    protected static EntityInfo entity(LivingEntity e) {
        return e == null ? null : new EntityInfo(e.getUniqueId(), e.getType().name(), e.getHealth());
    }
}
