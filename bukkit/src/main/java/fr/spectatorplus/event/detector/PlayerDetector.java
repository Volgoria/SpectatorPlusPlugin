package fr.spectatorplus.event.detector;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.compat.Compat;
import fr.spectatorplus.compat.DynamicEvents;
import fr.spectatorplus.compat.Mat;
import fr.spectatorplus.compat.Reflect;
import fr.spectatorplus.event.detect.Damage;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Évènements liés aux joueurs (dégâts, consommables, XP, connexion...).
 */
public final class PlayerDetector extends Detector {

    public PlayerDetector(SpectatorPlus plugin) {
        super(plugin);
    }

    public void registerDynamic() {
        DynamicEvents.register(plugin, "org.bukkit.event.entity.EntityResurrectEvent", EventPriority.MONITOR, true, new DynamicEvents.Handler() {
            @Override
            public void handle(Event event) {
                if (((Cancellable) event).isCancelled()) return;
                Object entity = Reflect.invoke(event, "getEntity");
                if (entity instanceof Player) signals().player().totem(w((Player) entity));
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        Damage d = new Damage();
        d.victim = w(p);
        d.victimHealthBefore = p.getHealth();
        d.amount = e.getFinalDamage();
        d.cause = e.getCause().name();
        if (e instanceof EntityDamageByEntityEvent) fillAttacker(d, ((EntityDamageByEntityEvent) e).getDamager());
        signals().player().damage(d);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();
        signals().player().consume(w(p), item(e.getItem()), !p.getActivePotionEffects().isEmpty());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLevel(PlayerLevelChangeEvent e) {
        signals().player().levelChange(w(e.getPlayer()), e.getOldLevel(), e.getNewLevel());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.useItemInHand() == Event.Result.DENY || !Compat.isMainHand(e) || Mat.isAir(e.getItem())) return;
        signals().player().itemUse(w(e.getPlayer()), item(e.getItem()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent e) {
        ProjectileSource src = e.getEntity().getShooter();
        if (src instanceof Player) signals().player().projectileLaunch(w((Player) src), e.getEntity().getType().name());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        signals().player().join(w(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        signals().player().quit(w(e.getPlayer()));
        signals().polling().quit(e.getPlayer().getUniqueId());
    }
}
