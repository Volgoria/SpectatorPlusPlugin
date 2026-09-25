package fr.spectatorplus.event.detector;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.compat.Compat;
import fr.spectatorplus.compat.DynamicEvents;
import fr.spectatorplus.compat.Mat;
import fr.spectatorplus.compat.Reflect;
import fr.spectatorplus.event.EventSettings;
import fr.spectatorplus.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Évènements liés aux joueurs (dégâts, consommables, XP, connexion...).
 * Les seuils de vie, effets, armures et AFK sont gérés par {@link PollingDetector}.
 */
public final class PlayerDetector extends Detector {

    private final Set<UUID> leftOnce = new HashSet<>();

    public PlayerDetector(SpectatorPlus plugin) {
        super(plugin);
    }

    public void registerDynamic() {
        DynamicEvents.register(plugin, "org.bukkit.event.entity.EntityResurrectEvent", EventPriority.MONITOR, true, new DynamicEvents.Handler() {
            @Override
            public void handle(Event event) {
                if (((Cancellable) event).isCancelled()) return;
                Object entity = Reflect.invoke(event, "getEntity");
                if (!(entity instanceof Player) || !tracked((Player) entity)) return;
                Player p = (Player) entity;
                plugin.stats().totemUsed(p.getUniqueId());
                if (on("player.totem") != null) fire(ev("player.totem", p));
            }
        });
    }

    // ------------------------------------------------------------------ dégâts

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        if (!tracked(p)) return;
        double dmg = e.getFinalDamage();
        if (dmg <= 0) return;
        String cause = e.getCause().name();
        boolean pvp = false;
        String attacker = null;
        if (e instanceof EntityDamageByEntityEvent) {
            Entity d = ((EntityDamageByEntityEvent) e).getDamager();
            if (d instanceof Projectile) {
                ProjectileSource src = ((Projectile) d).getShooter();
                if (src instanceof Entity) d = (Entity) src;
            }
            if (d instanceof Player) {
                pvp = true;
                attacker = ((Player) d).getName();
            } else if (d != null) {
                attacker = pretty(d.getType().name());
            }
        }
        double after = Math.max(0, p.getHealth() - dmg);
        EventSettings s = on("player.damage");
        if (s != null && s.accepts("causes", cause) && dmg >= s.number("min-damage", 0)) {
            fire(ev("player.damage", p).damage(dmg, cause, pvp, false)
                    .data("damage", Text.hearts(dmg)).data("damage_type", pretty(cause))
                    .data("attacker", attacker == null ? pretty(cause) : attacker)
                    .data("player_last_damage", Text.hearts(dmg))
                    .data("player_health_after_damage", Text.hearts(after)));
        }
        s = on("player.big_damage");
        if (s != null && dmg >= s.number("hearts", 4) * 2) {
            fire(ev("player.big_damage", p).damage(dmg, cause, pvp, false)
                    .data("damage", Text.hearts(dmg)).data("damage_type", pretty(cause))
                    .data("attacker", attacker == null ? pretty(cause) : attacker)
                    .data("player_last_damage", Text.hearts(dmg))
                    .data("player_health_after_damage", Text.hearts(after)));
        }
    }

    // ------------------------------------------------------------------ consommables

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();
        if (!tracked(p)) return;
        ItemStack it = e.getItem();
        String type = it.getType().name();
        boolean enchantedApple = type.equals("ENCHANTED_GOLDEN_APPLE") || (type.equals("GOLDEN_APPLE") && fr.spectatorplus.compat.Version.isLegacy() && it.getDurability() == 1);
        if (enchantedApple) {
            if (on("player.enchanted_golden_apple") != null) fire(item(ev("player.enchanted_golden_apple", p), it));
        } else if (type.equals("GOLDEN_APPLE")) {
            if (on("player.golden_apple") != null) fire(item(ev("player.golden_apple", p), it));
        } else if (type.equals("POTION")) {
            String potion = Compat.potionName(it);
            EventSettings s = on("player.potion");
            if (s != null && s.accepts("potions", potion)) fire(item(ev("player.potion", p), it).data("potion", pretty(potion)));
            s = on("potion.drink");
            if (s != null && s.accepts("potions", potion)) fire(item(ev("potion.drink", p), it).data("potion", pretty(potion)));
        } else if (type.equals("MILK_BUCKET")) {
            if (on("potion.milk") != null && !p.getActivePotionEffects().isEmpty()) fire(ev("potion.milk", p));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLevel(PlayerLevelChangeEvent e) {
        Player p = e.getPlayer();
        EventSettings s = on("player.xp_level");
        if (s == null || !tracked(p)) return;
        for (Integer lvl : s.ints("levels")) {
            if (e.getOldLevel() < lvl && e.getNewLevel() >= lvl) {
                fire(ev("player.xp_level", p).data("level", lvl));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.useItemInHand() == Event.Result.DENY || !Compat.isMainHand(e)) return;
        Player p = e.getPlayer();
        ItemStack it = e.getItem();
        if (Mat.isAir(it) || !tracked(p)) return;
        String type = it.getType().name();
        EventSettings s = on("player.item_use");
        if (s != null && !s.names("items").isEmpty() && s.accepts("items", type)) fire(item(ev("player.item_use", p), it));
        s = on("craft.item_use");
        if (s != null && !s.names("items").isEmpty() && s.accepts("items", type)) fire(item(ev("craft.item_use", p), it));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent e) {
        ProjectileSource src = e.getEntity().getShooter();
        if (!(src instanceof Player) || !tracked((Player) src)) return;
        if (e.getEntity().getType().name().equals("ENDER_PEARL") && on("player.ender_pearl") != null) {
            fire(ev("player.ender_pearl", (Player) src));
        }
    }

    // ------------------------------------------------------------------ connexion

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent e) {
        final Player p = e.getPlayer();
        // un tick plus tard : le passage éventuel en spectateur a eu lieu
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (!p.isOnline() || !tracked(p)) return;
                boolean again = leftOnce.contains(p.getUniqueId());
                String id = again ? "player.reconnect" : "player.join";
                if (on(id) != null) fire(ev(id, p));
                plugin.game().checkAliveCount();
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        final Player p = e.getPlayer();
        if (tracked(p)) {
            leftOnce.add(p.getUniqueId());
            if (on("player.quit") != null) fire(ev("player.quit", p));
        }
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.game().checkAliveCount();
            }
        }, 1L);
    }

    public void reset() {
        leftOnce.clear();
    }
}
