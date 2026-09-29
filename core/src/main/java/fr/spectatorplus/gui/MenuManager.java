package fr.spectatorplus.gui;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.core.platform.Click;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.spectator.SpectatorSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Suivi des menus ouverts. Les plateformes lui transmettent les clics et les fermetures.
 */
public final class MenuManager {

    private final SpectatorCore plugin;
    private final Map<UUID, Menu> open = new HashMap<>();
    private final Map<UUID, Menu> switching = new HashMap<>();

    public MenuManager(SpectatorCore plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.platform().runTimer(new Runnable() {
            @Override
            public void run() {
                for (Menu m : new ArrayList<>(open.values())) {
                    if (m.autoRefresh() && m.getViewer().isOnline()) m.refresh();
                }
            }
        }, 20L, 20L);
    }

    void opening(PlatformPlayer p, Menu menu) {
        switching.put(p.getUniqueId(), menu);
    }

    void opened(PlatformPlayer p, Menu menu) {
        switching.remove(p.getUniqueId());
        open.put(p.getUniqueId(), menu);
        SpectatorSession s = plugin.spectators().getSpectator(p);
        if (s != null) s.setInspecting(menu.id().equals("inspect") || menu.id().equals("player") || menu.id().equals("enderchest"));
    }

    public Menu getOpen(PlatformPlayer p) {
        return open.get(p.getUniqueId());
    }

    /** Le joueur est-il en train de passer d'un menu à un autre ? */
    public boolean isSwitching(PlatformPlayer p) {
        return switching.containsKey(p.getUniqueId());
    }

    public void close(PlatformPlayer p) {
        if (open.containsKey(p.getUniqueId())) p.closeInventory();
    }

    public void forget(PlatformPlayer p) {
        open.remove(p.getUniqueId());
        switching.remove(p.getUniqueId());
    }

    public void closeAll() {
        for (UUID id : new ArrayList<>(open.keySet())) {
            PlatformPlayer p = plugin.platform().getPlayer(id);
            if (p != null) p.closeInventory();
        }
        open.clear();
    }

    /** Clic dans un menu (case du menu uniquement). */
    public void click(Menu menu, int slot, Click type) {
        if (slot < 0 || slot >= menu.getSize()) return;
        try {
            menu.handleClick(slot, type);
        } catch (Throwable t) {
            plugin.host().logger().log(Level.WARNING, "Erreur dans le menu " + menu.id(), t);
        }
    }

    /** Fermeture de l'inventaire d'un menu. */
    public void closed(PlatformPlayer p, Menu menu) {
        menu.onClose();
        plugin.platform().api().menuClose(p, menu.id());
        if (open.get(p.getUniqueId()) == menu) open.remove(p.getUniqueId());
        if (!switching.containsKey(p.getUniqueId())) {
            SpectatorSession s = plugin.spectators().getSpectator(p);
            if (s != null) s.setInspecting(false);
        }
    }
}
