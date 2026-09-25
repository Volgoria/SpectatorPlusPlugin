package fr.spectatorplus.spectator;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.api.EnterReason;
import fr.spectatorplus.api.SpectatorMode;
import fr.spectatorplus.api.SpectatorState;
import fr.spectatorplus.compat.Compat;
import fr.spectatorplus.compat.DynamicEvents;
import fr.spectatorplus.compat.Reflect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Protections du mode spectateur + interactions (barre d'outils, clic sur un joueur),
 * modes semi-auto / auto (mort → spectateur), connexion / déconnexion, chat.
 */
public final class SpectatorListener implements Listener {

    private final SpectatorPlus plugin;
    private final Map<UUID, Location> pendingDeaths = new HashMap<>();

    public SpectatorListener(SpectatorPlus plugin) {
        this.plugin = plugin;
    }

    private boolean spec(Entity e) {
        return e instanceof Player && plugin.spectators().isSpectator((Player) e);
    }

    /** Évènements absents de l'API 1.8 : enregistrés dynamiquement. */
    public void registerDynamic() {
        DynamicEvents.Handler cancelIfEntitySpec = new DynamicEvents.Handler() {
            @Override
            public void handle(Event event) {
                Object e = Reflect.invoke(event, "getEntity");
                if (e instanceof Entity && spec((Entity) e)) ((Cancellable) event).setCancelled(true);
            }
        };
        DynamicEvents.Handler cancelIfPlayerSpec = new DynamicEvents.Handler() {
            @Override
            public void handle(Event event) {
                Object p = Reflect.invoke(event, "getPlayer");
                if (p instanceof Player && spec((Player) p)) ((Cancellable) event).setCancelled(true);
            }
        };
        DynamicEvents.register(plugin, "org.bukkit.event.entity.EntityPickupItemEvent", EventPriority.LOWEST, false, cancelIfEntitySpec);
        DynamicEvents.register(plugin, "org.bukkit.event.player.PlayerPickupItemEvent", EventPriority.LOWEST, false, cancelIfPlayerSpec);
        DynamicEvents.register(plugin, "org.bukkit.event.player.PlayerSwapHandItemsEvent", EventPriority.LOWEST, false, cancelIfPlayerSpec);
        DynamicEvents.register(plugin, "org.bukkit.event.player.PlayerPickupArrowEvent", EventPriority.LOWEST, false, cancelIfPlayerSpec);
        DynamicEvents.register(plugin, "org.bukkit.event.block.BlockReceiveGameEvent", EventPriority.LOWEST, false, cancelIfEntitySpec);
        DynamicEvents.register(plugin, "org.bukkit.event.player.PlayerTakeLecternBookEvent", EventPriority.LOWEST, false, cancelIfPlayerSpec);
        DynamicEvents.register(plugin, "org.bukkit.event.raid.RaidTriggerEvent", EventPriority.LOWEST, false, cancelIfPlayerSpec);
        DynamicEvents.register(plugin, "org.bukkit.event.entity.EntityPotionEffectEvent", EventPriority.LOWEST, false, new DynamicEvents.Handler() {
            @Override
            public void handle(Event event) {
                // les potions jetées ne doivent pas affecter les spectateurs
                Object e = Reflect.invoke(event, "getEntity");
                Object cause = Reflect.invoke(event, "getCause");
                if (e instanceof Player && spec((Player) e) && cause != null) {
                    String c = cause.toString();
                    if (c.equals("POTION_SPLASH") || c.equals("AREA_EFFECT_CLOUD") || c.equals("ARROW")) {
                        ((Cancellable) event).setCancelled(true);
                    }
                }
            }
        });
        // 1.12+ : le client change de langue → barre d'inventaire retraduite
        DynamicEvents.register(plugin, "org.bukkit.event.player.PlayerLocaleChangeEvent", EventPriority.MONITOR, false, new DynamicEvents.Handler() {
            @Override
            public void handle(Event event) {
                final Object p = Reflect.invoke(event, "getPlayer");
                if (!(p instanceof Player) || !spec((Player) p)) return;
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (((Player) p).isOnline()) plugin.spectators().refreshHotbar((Player) p);
                    }
                }, 2L);
            }
        });
        // 1.20+ : ProjectileHitEvent est annulable → les projectiles traversent les spectateurs
        DynamicEvents.register(plugin, "org.bukkit.event.entity.ProjectileHitEvent", EventPriority.LOWEST, false, new DynamicEvents.Handler() {
            @Override
            public void handle(Event event) {
                Object hit = Reflect.invoke(event, "getHitEntity");
                if (hit instanceof Entity && spec((Entity) hit)) Reflect.tryInvoke(event, "setCancelled", true);
            }
        });
    }

    // ------------------------------------------------------------------ blocs

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent e) {
        if (spec(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent e) {
        if (spec(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (spec(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (spec(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBed(PlayerBedEnterEvent e) {
        if (spec(e.getPlayer())) e.setCancelled(true);
    }

    // ------------------------------------------------------------------ interactions

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        SpectatorSession s = plugin.spectators().getSpectator(p);
        if (s == null) return;
        e.setCancelled(true);
        if (!Compat.isMainHand(e)) return;
        switch (e.getAction()) {
            case PHYSICAL:
                return;
            default:
                break;
        }
        String action = s.getHotbarAction(p.getInventory().getHeldItemSlot());
        if (action == null) return;
        boolean left = e.getAction().name().startsWith("LEFT");
        plugin.hotbar().use(p, s, action, left, p.isSneaking());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        SpectatorSession s = plugin.spectators().getSpectator(p);
        if (s == null) return;
        e.setCancelled(true);
        if (e instanceof PlayerInteractAtEntityEvent || !Compat.isMainHand(e)) return;
        if (!(e.getRightClicked() instanceof Player)) return;
        Player target = (Player) e.getRightClicked();
        if (plugin.spectators().isSpectator(target)) return;
        String action = s.getHotbarAction(p.getInventory().getHeldItemSlot());
        if ("follow".equals(action)) {
            plugin.spectators().follow(s, target);
            return;
        }
        String click = plugin.getConfig().getString("spectator.right-click-player", "INVENTORY").toUpperCase(Locale.ROOT);
        if (click.equals("SHEET")) s.openInspection(target);
        else if (click.equals("FOLLOW")) plugin.spectators().follow(s, target);
        else if (!click.equals("NONE")) s.openInventory(target, false);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onArmorStand(PlayerArmorStandManipulateEvent e) {
        if (spec(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onShear(PlayerShearEntityEvent e) {
        if (spec(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFish(PlayerFishEvent e) {
        if (spec(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onConsume(PlayerItemConsumeEvent e) {
        if (spec(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLaunch(ProjectileLaunchEvent e) {
        ProjectileSource shooter = e.getEntity().getShooter();
        if (shooter instanceof Player && spec((Player) shooter)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onHanging(HangingBreakByEntityEvent e) {
        if (spec(e.getRemover())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onVehicleEnter(VehicleEnterEvent e) {
        if (spec(e.getEntered())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onVehicleDamage(VehicleDamageEvent e) {
        if (spec(e.getAttacker())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onVehicleCollision(VehicleEntityCollisionEvent e) {
        if (spec(e.getEntity())) {
            e.setCancelled(true);
            e.setCollisionCancelled(true);
            e.setPickupCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onExp(PlayerExpChangeEvent e) {
        if (spec(e.getPlayer())) e.setAmount(0);
    }

    // ------------------------------------------------------------------ inventaire

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent e) {
        if (spec(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (spec(e.getWhoClicked())) e.setCancelled(true);
    }

    /**
     * Un spectateur ne peut pas ouvrir de conteneur lié au monde (coffre, machine d'un mod via une touche,
     * /enderchest d'un autre plugin...). Les menus de plugins (holder personnalisé ou null) restent autorisés.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryOpen(org.bukkit.event.inventory.InventoryOpenEvent e) {
        if (!spec(e.getPlayer()) || e.getPlayer().hasPermission("spectatorplus.bypass.inventories")) return;
        if (!plugin.getConfig().getBoolean("spectator.block-world-inventories", true)) return;
        org.bukkit.inventory.InventoryHolder holder = e.getInventory().getHolder();
        if (holder instanceof org.bukkit.block.BlockState || holder instanceof org.bukkit.block.DoubleChest
                || (holder instanceof Entity && holder != e.getPlayer())) {
            e.setCancelled(true);
            return;
        }
        String type = e.getInventory().getType().name();
        if (type.equals("ENDER_CHEST") || type.equals("MERCHANT")) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (spec(e.getWhoClicked())) e.setCancelled(true);
    }

    // ------------------------------------------------------------------ dégâts

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent e) {
        if (!spec(e.getEntity())) return;
        e.setCancelled(true);
        Player p = (Player) e.getEntity();
        p.setFireTicks(0);
        if (e.getCause() == EntityDamageEvent.DamageCause.VOID) {
            Location l = p.getLocation();
            l.setY(l.getWorld().getHighestBlockYAt(l.getBlockX(), l.getBlockZ()) + 5);
            p.teleport(l);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageByEntity(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        Entity victim = e.getEntity();
        // un spectateur ne peut infliger aucun dégât
        if (spec(damager)) {
            e.setCancelled(true);
            if (victim instanceof Player && !spec(victim) && plugin.getConfig().getBoolean("spectator.left-click-opens-sheet", true)) {
                SpectatorSession s = plugin.spectators().getSpectator((Player) damager);
                if (s != null && s.getMovementState() != SpectatorState.POV) s.openInspection((Player) victim);
            }
            return;
        }
        if (damager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Player && spec((Player) shooter)) {
                e.setCancelled(true);
                return;
            }
            // le projectile traverse le spectateur
            if (spec(victim)) {
                e.setCancelled(true);
                final Projectile proj = (Projectile) damager;
                final Vector velocity = proj.getVelocity().clone();
                if (velocity.lengthSquared() > 1.0E-4) {
                    Location through = proj.getLocation().add(velocity.clone().normalize().multiply(1.5));
                    proj.teleport(through);
                    proj.setVelocity(velocity);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTarget(EntityTargetEvent e) {
        if (spec(e.getTarget())) {
            e.setCancelled(true);
            e.setTarget(null);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCombust(EntityCombustEvent e) {
        if (spec(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFood(FoodLevelChangeEvent e) {
        if (spec(e.getEntity())) {
            e.setCancelled(true);
            ((Player) e.getEntity()).setFoodLevel(20);
        }
    }

    // ------------------------------------------------------------------ déplacements

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        SpectatorSession s = plugin.spectators().getSpectator(e.getPlayer());
        if (s == null || !s.isFrozen()) return;
        Location from = e.getFrom(), to = e.getTo();
        if (to == null) return;
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            Location back = from.clone();
            back.setYaw(to.getYaw());
            back.setPitch(to.getPitch());
            e.setTo(back);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSneak(PlayerToggleSneakEvent e) {
        SpectatorSession s = plugin.spectators().getSpectator(e.getPlayer());
        if (s != null && e.isSneaking() && s.getMovementState() == SpectatorState.POV) {
            plugin.spectators().requestPovExit(s);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPortal(PlayerPortalEvent e) {
        if (spec(e.getPlayer()) && !plugin.getConfig().getBoolean("spectator.allow-portals", false)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(final PlayerChangedWorldEvent e) {
        final Player p = e.getPlayer();
        if (!spec(p)) return;
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                SpectatorSession s = plugin.spectators().getSpectator(p);
                if (p.isOnline() && s != null && s.getMovementState() != SpectatorState.POV) {
                    p.setAllowFlight(true);
                    p.setFlying(true);
                    plugin.spectators().applyFlySpeed(p);
                }
            }
        }, 2L);
    }

    // ------------------------------------------------------------------ mort / réapparition (modes auto)

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        final Player p = e.getEntity();
        if (spec(p)) {
            e.getDrops().clear();
            e.setDroppedExp(0);
            e.setDeathMessage(null);
            e.setKeepInventory(true);
            pendingDeaths.put(p.getUniqueId(), p.getLocation());
            scheduleRespawn(p);
            return;
        }
        if (plugin.getMode() == SpectatorMode.MANUAL || !plugin.getConfig().getBoolean("auto.on-death", true)) return;
        pendingDeaths.put(p.getUniqueId(), p.getLocation());
        if (plugin.getConfig().getBoolean("auto.instant-respawn", true)) scheduleRespawn(p);
    }

    private void scheduleRespawn(final Player p) {
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (p.isOnline() && p.isDead()) Compat.respawn(p);
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {
        final Player p = e.getPlayer();
        final Location death = pendingDeaths.remove(p.getUniqueId());
        if (death == null) return;
        final boolean wasSpectator = spec(p);
        if (wasSpectator && death.getWorld() != null) {
            // un spectateur « mort » (commande /kill...) réapparaît là où il était
            Location l = death.clone();
            if (l.getY() < Compat.minHeight(l.getWorld()) + 1) {
                l.setY(l.getWorld().getHighestBlockYAt(l.getBlockX(), l.getBlockZ()) + 3);
            }
            e.setRespawnLocation(l);
        }
        final Location target = plugin.getConfig().getBoolean("auto.teleport-to-death-location", true) ? death : null;
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (!p.isOnline()) return;
                SpectatorSession s = plugin.spectators().getSpectator(p);
                if (s != null) {
                    plugin.spectators().apply(p);
                    plugin.hotbar().give(p, s);
                    return;
                }
                plugin.spectators().enter(p, EnterReason.DEATH, target, plugin.getMode() == SpectatorMode.AUTO);
            }
        }, 1L);
    }

    // ------------------------------------------------------------------ connexion

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        plugin.filters().loadPlayer(p.getUniqueId());
        plugin.spectators().handleJoin(p);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        plugin.menus().forget(p);
        plugin.spectators().handleQuit(p);
        plugin.filters().unload(p.getUniqueId());
        plugin.events().forget(p.getUniqueId());
    }

    // ------------------------------------------------------------------ chat & commandes

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        if (!plugin.getConfig().getBoolean("spectator.chat.separate", true)) return;
        Player p = e.getPlayer();
        if (!plugin.spectators().isSpectator(p.getUniqueId())) return;
        Iterator<Player> it = e.getRecipients().iterator();
        while (it.hasNext()) {
            Player r = it.next();
            if (!plugin.spectators().isSpectator(r.getUniqueId()) && !r.hasPermission("spectatorplus.chat.see")) it.remove();
        }
        e.setFormat(fr.spectatorplus.util.Text.color(plugin.getConfig().getString("spectator.chat.prefix", "&7[Spec] ")) + e.getFormat());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        if (!spec(p) || p.hasPermission("spectatorplus.bypass.commands")) return;
        String label = e.getMessage().substring(1).split(" ")[0].toLowerCase(Locale.ROOT);
        if (label.contains(":")) label = label.substring(label.indexOf(':') + 1);
        for (String blocked : plugin.getConfig().getStringList("spectator.blocked-commands")) {
            if (blocked.equalsIgnoreCase(label)) {
                e.setCancelled(true);
                plugin.messages().send(p, "errors.command-blocked");
                return;
            }
        }
    }
}
