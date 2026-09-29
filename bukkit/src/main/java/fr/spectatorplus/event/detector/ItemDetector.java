package fr.spectatorplus.event.detector;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.compat.Compat;
import fr.spectatorplus.compat.DynamicEvents;
import fr.spectatorplus.compat.Mat;
import fr.spectatorplus.compat.Reflect;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Crafts, objets, enchantements et potions lancées.
 */
public final class ItemDetector extends Detector {

    public ItemDetector(SpectatorPlus plugin) {
        super(plugin);
    }

    public void registerDynamic() {
        boolean modern = DynamicEvents.register(plugin, "org.bukkit.event.entity.EntityPickupItemEvent", EventPriority.MONITOR, true, new DynamicEvents.Handler() {
            @Override
            public void handle(Event event) {
                Object entity = Reflect.invoke(event, "getEntity");
                Object item = Reflect.invoke(event, "getItem");
                if (entity instanceof Player && item instanceof Item && !((Cancellable) event).isCancelled()) {
                    signals().items().pickup(w((Player) entity), item(((Item) item).getItemStack()));
                }
            }
        });
        if (!modern) {
            DynamicEvents.register(plugin, "org.bukkit.event.player.PlayerPickupItemEvent", EventPriority.MONITOR, true, new DynamicEvents.Handler() {
                @Override
                public void handle(Event event) {
                    Object player = Reflect.invoke(event, "getPlayer");
                    Object item = Reflect.invoke(event, "getItem");
                    if (player instanceof Player && item instanceof Item && !((Cancellable) event).isCancelled()) {
                        signals().items().pickup(w((Player) player), item(((Item) item).getItemStack()));
                    }
                }
            });
        }
    }

    // ------------------------------------------------------------------ crafts / objets

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            signals().items().craft(w((Player) e.getWhoClicked()), item(e.getRecipe().getResult()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnace(FurnaceExtractEvent e) {
        signals().items().furnace(w(e.getPlayer()), item(new ItemStack(e.getItemType(), Math.max(1, e.getItemAmount()))));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        signals().items().drop(w(e.getPlayer()), item(e.getItemDrop().getItemStack()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak(PlayerItemBreakEvent e) {
        signals().items().itemBreak(w(e.getPlayer()), item(e.getBrokenItem()));
    }

    // ------------------------------------------------------------------ enchantements

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent e) {
        Map<String, Integer> added = new LinkedHashMap<>();
        for (Map.Entry<Enchantment, Integer> en : e.getEnchantsToAdd().entrySet()) {
            added.put(Compat.enchantName(en.getKey()), en.getValue());
        }
        signals().items().enchant(w(e.getEnchanter()), item(e.getItem()), added, e.getExpLevelCost());
    }

    /** Enclume : clic sur l'emplacement résultat. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnvil(InventoryClickEvent e) {
        Inventory inv = e.getInventory();
        if (inv.getType() != InventoryType.ANVIL || e.getRawSlot() != 2 || !(e.getWhoClicked() instanceof Player)) return;
        ItemStack result = e.getCurrentItem();
        if (Mat.isAir(result)) return;
        Object costObj = Reflect.invoke(inv, "getRepairCost");
        int cost = costObj instanceof Number ? ((Number) costObj).intValue() : 0;
        signals().items().anvil(w((Player) e.getWhoClicked()), item(inv.getItem(0)), item(inv.getItem(1)), item(result), cost);
    }

    // ------------------------------------------------------------------ potions lancées

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent e) {
        if (!(e.getEntity() instanceof ThrownPotion)) return;
        ThrownPotion tp = (ThrownPotion) e.getEntity();
        if (tp.getShooter() instanceof Player) signals().items().potionThrow(w((Player) tp.getShooter()), item(tp.getItem()));
    }
}
