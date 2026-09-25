package fr.spectatorplus.gui;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.api.events.SpectatorMenuCloseEvent;
import fr.spectatorplus.spectator.SpectatorSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Distribution des clics + suivi des menus ouverts.
 */
public final class MenuManager implements Listener {

    private final SpectatorPlus plugin;
    private final Map<UUID, Menu> open = new HashMap<>();
    private final Map<UUID, Menu> switching = new HashMap<>();

    public MenuManager(SpectatorPlus plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                for (Menu m : new ArrayList<>(open.values())) {
                    if (m.autoRefresh() && m.getViewer().isOnline()) m.refresh();
                }
            }
        }, 20L, 20L);
    }

    void opening(Player p, Menu menu) {
        switching.put(p.getUniqueId(), menu);
    }

    void opened(Player p, Menu menu) {
        switching.remove(p.getUniqueId());
        open.put(p.getUniqueId(), menu);
        SpectatorSession s = plugin.spectators().getSpectator(p);
        if (s != null) s.setInspecting(menu.id().equals("inspect") || menu.id().equals("player") || menu.id().equals("enderchest"));
    }

    public Menu getOpen(Player p) {
        return open.get(p.getUniqueId());
    }

    public void close(Player p) {
        if (open.containsKey(p.getUniqueId())) p.closeInventory();
    }

    public void forget(Player p) {
        open.remove(p.getUniqueId());
        switching.remove(p.getUniqueId());
    }

    public void closeAll() {
        for (UUID id : new ArrayList<>(open.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.closeInventory();
        }
        open.clear();
    }

    private static Menu menuOf(InventoryHolder holder) {
        return holder instanceof Menu ? (Menu) holder : null;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        Menu menu = menuOf(e.getInventory().getHolder());
        if (menu == null) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        int raw = e.getRawSlot();
        if (raw < 0 || raw >= e.getInventory().getSize()) return;
        try {
            menu.handleClick(raw, e.getClick());
        } catch (Throwable t) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Erreur dans le menu " + menu.id(), t);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (menuOf(e.getInventory().getHolder()) != null) e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Menu menu = menuOf(e.getInventory().getHolder());
        if (menu == null || !(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer();
        menu.onClose();
        Bukkit.getPluginManager().callEvent(new SpectatorMenuCloseEvent(p, menu.id()));
        if (open.get(p.getUniqueId()) == menu) open.remove(p.getUniqueId());
        if (!switching.containsKey(p.getUniqueId())) {
            SpectatorSession s = plugin.spectators().getSpectator(p);
            if (s != null) s.setInspecting(false);
        }
    }
}
