package fr.spectatorplus.event.detector;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.api.event.SpectatorGameEvent;
import fr.spectatorplus.compat.Compat;
import fr.spectatorplus.compat.DynamicEvents;
import fr.spectatorplus.compat.Mat;
import fr.spectatorplus.compat.Reflect;
import fr.spectatorplus.event.EventSettings;
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
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.Set;

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
                    pickup((Player) entity, ((Item) item).getItemStack());
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
                        pickup((Player) player, ((Item) item).getItemStack());
                    }
                }
            });
        }
    }

    private void pickup(Player p, ItemStack it) {
        if (!tracked(p) || it == null) return;
        String type = it.getType().name();
        EventSettings s = on("craft.pickup");
        if (s != null && !s.names("items").isEmpty() && s.accepts("items", type)) fire(item(ev("craft.pickup", p), it));
        obtain(p, it, "pickup");
    }

    private void obtain(Player p, ItemStack it, String how) {
        EventSettings s = on("craft.obtain");
        if (s != null && !s.names("items").isEmpty() && s.accepts("items", it.getType().name())) {
            fire(item(ev("craft.obtain", p), it).data("method", how));
        }
    }

    // ------------------------------------------------------------------ crafts / objets

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        if (!tracked(p)) return;
        ItemStack result = e.getRecipe().getResult();
        if (Mat.isAir(result)) return;
        String type = result.getType().name();
        EventSettings s = on("craft.craft");
        if (s != null && !s.names("items").isEmpty() && s.accepts("items", type)) {
            fire(item(ev("craft.craft", p), result).data("crafted_item", pretty(type)).data("crafted_amount", result.getAmount()));
        }
        s = on("craft.first_craft");
        if (s != null && s.accepts("items", type) && plugin.stats().first(p.getUniqueId(), "craft." + type)) {
            fire(item(ev("craft.first_craft", p), result).data("crafted_item", pretty(type)).data("crafted_amount", result.getAmount()));
        }
        obtain(p, result, "craft");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnace(FurnaceExtractEvent e) {
        Player p = e.getPlayer();
        if (!tracked(p)) return;
        ItemStack it = new ItemStack(e.getItemType(), Math.max(1, e.getItemAmount()));
        obtain(p, it, "furnace");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        if (!tracked(p)) return;
        ItemStack it = e.getItemDrop().getItemStack();
        EventSettings s = on("craft.drop");
        if (s != null && !s.names("items").isEmpty() && s.accepts("items", it.getType().name())) fire(item(ev("craft.drop", p), it));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak(PlayerItemBreakEvent e) {
        Player p = e.getPlayer();
        if (!tracked(p)) return;
        ItemStack it = e.getBrokenItem();
        EventSettings s = on("craft.item_break");
        if (s != null && s.accepts("items", it.getType().name())) fire(item(ev("craft.item_break", p), it));
    }

    // ------------------------------------------------------------------ enchantements

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent e) {
        Player p = e.getEnchanter();
        if (!tracked(p)) return;
        ItemStack it = e.getItem();
        String type = it.getType().name();
        Map<Enchantment, Integer> added = e.getEnchantsToAdd();
        StringBuilder list = new StringBuilder();
        int best = 0;
        String bestName = "";
        for (Map.Entry<Enchantment, Integer> en : added.entrySet()) {
            String n = Compat.enchantName(en.getKey());
            if (list.length() > 0) list.append(", ");
            list.append(pretty(n)).append(' ').append(en.getValue());
            if (en.getValue() > best) {
                best = en.getValue();
                bestName = n;
            }
        }
        SpectatorGameEvent.Builder base = enchantEvent("enchant.enchant", p, it, list.toString(), bestName, best, e.getExpLevelCost());
        if (on("enchant.enchant") != null) fire(base);

        EventSettings s = on("enchant.specific");
        if (s != null && !s.names("enchantments").isEmpty()) {
            for (Map.Entry<Enchantment, Integer> en : added.entrySet()) {
                if (matchesLeveled(s.names("enchantments"), Compat.enchantKeys(en.getKey()), en.getValue())) {
                    fire(enchantEvent("enchant.specific", p, it, list.toString(), Compat.enchantName(en.getKey()), en.getValue(), e.getExpLevelCost()));
                    break;
                }
            }
        }
        s = on("enchant.level");
        if (s != null && best >= s.integer("min-level", 4)) {
            fire(enchantEvent("enchant.level", p, it, list.toString(), bestName, best, e.getExpLevelCost()));
        }
        if (isWeapon(type)) enchantCategory("enchant.weapon", p, it, added, list.toString(), e.getExpLevelCost());
        if (isArmor(type)) enchantCategory("enchant.armor", p, it, added, list.toString(), e.getExpLevelCost());
    }

    private void enchantCategory(String id, Player p, ItemStack it, Map<Enchantment, Integer> added, String list, int cost) {
        EventSettings s = on(id);
        if (s == null) return;
        for (Map.Entry<Enchantment, Integer> en : added.entrySet()) {
            if (matchesLeveled(s.names("enchantments"), Compat.enchantKeys(en.getKey()), en.getValue())) {
                fire(enchantEvent(id, p, it, list, Compat.enchantName(en.getKey()), en.getValue(), cost));
                return;
            }
        }
    }

    private SpectatorGameEvent.Builder enchantEvent(String id, Player p, ItemStack it, String list, String ench, int level, int cost) {
        return item(ev(id, p), it)
                .data("enchanted_item", pretty(it.getType().name()))
                .data("enchantment", pretty(ench)).data("enchantment_level", level)
                .data("enchantments", list).data("xp_cost", cost);
    }

    /** Enclume : clic sur l'emplacement résultat. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnvil(InventoryClickEvent e) {
        Inventory inv = e.getInventory();
        if (inv.getType() != InventoryType.ANVIL || e.getRawSlot() != 2 || !(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack result = e.getCurrentItem();
        if (!tracked(p) || Mat.isAir(result)) return;
        ItemStack left = inv.getItem(0), right = inv.getItem(1);
        Object costObj = Reflect.invoke(inv, "getRepairCost");
        int cost = costObj instanceof Number ? ((Number) costObj).intValue() : 0;

        if (on("enchant.anvil") != null) fire(item(ev("enchant.anvil", p), result).data("xp_cost", cost));
        if (Mat.isAir(left) || Mat.isAir(right)) return;
        if (right.getType().name().equals("ENCHANTED_BOOK")) {
            EventSettings s = on("enchant.book");
            if (s == null) return;
            ItemMeta meta = right.getItemMeta();
            if (!(meta instanceof EnchantmentStorageMeta)) return;
            for (Map.Entry<Enchantment, Integer> en : ((EnchantmentStorageMeta) meta).getStoredEnchants().entrySet()) {
                Set<String> keys = Compat.enchantKeys(en.getKey());
                if (matchesLeveled(s.names("enchantments"), keys, en.getValue())) {
                    fire(item(ev("enchant.book", p), result)
                            .data("enchanted_item", pretty(left.getType().name()))
                            .data("enchantment", pretty(Compat.enchantName(en.getKey())))
                            .data("enchantment_level", en.getValue()).data("xp_cost", cost));
                    return;
                }
            }
        } else {
            EventSettings s = on("enchant.combine");
            if (s != null && s.accepts("items", left.getType().name())) {
                fire(item(ev("enchant.combine", p), result).data("enchanted_item", pretty(left.getType().name())).data("xp_cost", cost));
            }
        }
    }

    // ------------------------------------------------------------------ potions lancées

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent e) {
        if (!(e.getEntity() instanceof ThrownPotion)) return;
        ThrownPotion tp = (ThrownPotion) e.getEntity();
        if (!(tp.getShooter() instanceof Player)) return;
        Player p = (Player) tp.getShooter();
        if (!tracked(p)) return;
        ItemStack it = tp.getItem();
        String potion = Compat.potionName(it);
        boolean lingering = it != null && it.getType().name().equals("LINGERING_POTION");
        String id = lingering ? "potion.lingering" : "potion.splash";
        EventSettings s = on(id);
        if (s != null && s.accepts("potions", potion)) fire(item(ev(id, p), it).data("potion", pretty(potion)));
    }
}
