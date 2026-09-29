package fr.spectatorplus.bukkit;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.compat.Version;
import fr.spectatorplus.core.platform.Click;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.gui.Menu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Affichage des menus du code commun dans des inventaires Bukkit + transmission des clics.
 * Les menus n'utilisent jamais InventoryView (devenue une interface en 1.21) : tout passe par l'InventoryHolder.
 */
public final class BukkitMenus implements Listener {

    /** Inventaire d'un menu. */
    static final class Holder implements InventoryHolder {
        final Menu menu;
        Inventory inventory;

        Holder(Menu menu) {
            this.menu = menu;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private final BukkitPlatform platform;
    private final Map<UUID, Holder> open = new HashMap<>();

    BukkitMenus(BukkitPlatform platform) {
        this.platform = platform;
    }

    private SpectatorCore core() {
        return platform.core();
    }

    void open(PlatformPlayer viewer, Menu menu) {
        Holder h = new Holder(menu);
        h.inventory = Bukkit.createInventory(h, menu.getSize(), limitTitle(menu.getTitle()));
        fill(h);
        open.put(viewer.getUniqueId(), h);
        BukkitPlayer.unwrap(viewer).openInventory(h.inventory);
    }

    void refresh(PlatformPlayer viewer, Menu menu) {
        Holder h = open.get(viewer.getUniqueId());
        if (h == null || h.menu != menu) return;
        h.inventory.clear();
        fill(h);
    }

    private static void fill(Holder h) {
        for (int i = 0; i < h.inventory.getSize(); i++) h.inventory.setItem(i, BukkitIcons.render(h.menu.getItem(i)));
    }

    /** Titre d'inventaire : limité à 32 caractères avant la 1.9. */
    private static String limitTitle(String title) {
        if (!Version.atLeast(1, 9) && title.length() > 32) return title.substring(0, 32);
        return title;
    }

    private static Holder holderOf(Inventory inv) {
        InventoryHolder h = inv == null ? null : inv.getHolder();
        return h instanceof Holder ? (Holder) h : null;
    }

    private static Click click(Object type) {
        switch (type.toString()) {
            case "LEFT":
                return Click.LEFT;
            case "RIGHT":
                return Click.RIGHT;
            case "SHIFT_LEFT":
                return Click.SHIFT_LEFT;
            case "SHIFT_RIGHT":
                return Click.SHIFT_RIGHT;
            case "MIDDLE":
                return Click.MIDDLE;
            case "DROP":
            case "CONTROL_DROP":
                return Click.DROP;
            default:
                return Click.OTHER;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        Holder h = holderOf(e.getInventory());
        if (h == null) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        int raw = e.getRawSlot();
        if (raw < 0 || raw >= e.getInventory().getSize()) return;
        core().menus().click(h.menu, raw, click(e.getClick()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (holderOf(e.getInventory()) != null) e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Holder h = holderOf(e.getInventory());
        if (h == null || !(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer();
        if (open.get(p.getUniqueId()) == h) open.remove(p.getUniqueId());
        core().menus().closed(platform.wrap(p), h.menu);
    }

    void forget(UUID id) {
        open.remove(id);
    }
}
