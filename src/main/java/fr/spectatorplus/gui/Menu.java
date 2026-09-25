package fr.spectatorplus.gui;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.api.events.SpectatorMenuOpenEvent;
import fr.spectatorplus.compat.Compat;
import fr.spectatorplus.compat.Mat;
import fr.spectatorplus.compat.Sounds;
import fr.spectatorplus.util.ItemBuilder;
import fr.spectatorplus.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Menu graphique de base. Les menus n'utilisent jamais InventoryView
 * (devenue une interface en 1.21) : tout passe par l'InventoryHolder.
 */
public abstract class Menu implements InventoryHolder {

    public interface ClickHandler {
        void click(ClickType type);
    }

    protected final SpectatorPlus plugin;
    protected final Player viewer;
    private final Map<Integer, ClickHandler> handlers = new HashMap<>();
    private Inventory inventory;

    protected Menu(SpectatorPlus plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    /** Identifiant transmis aux évènements SpectatorMenuOpen/CloseEvent. */
    public abstract String id();

    protected abstract String title();

    protected abstract int rows();

    protected abstract void render();

    /** Rafraîchissement automatique chaque seconde (fiches joueurs, infos de partie...). */
    public boolean autoRefresh() {
        return false;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getViewer() {
        return viewer;
    }

    public void open() {
        if (!viewer.isOnline()) return;
        SpectatorMenuOpenEvent ev = new SpectatorMenuOpenEvent(viewer, id());
        Bukkit.getPluginManager().callEvent(ev);
        if (ev.isCancelled()) return;
        inventory = Bukkit.createInventory(this, Math.max(1, Math.min(6, rows())) * 9, Text.title(title()));
        render();
        plugin.menus().opening(viewer, this);
        viewer.openInventory(inventory);
        plugin.menus().opened(viewer, this);
    }

    public void refresh() {
        if (inventory == null) return;
        handlers.clear();
        inventory.clear();
        render();
    }

    public void handleClick(int slot, final ClickType type) {
        final ClickHandler h = handlers.get(slot);
        if (h == null) return;
        if (plugin.filters().get(viewer).sounds) Compat.playSound(viewer, Sounds.CLICK, 0.4f, 1.4f);
        // exécuté au tick suivant : ouvrir / fermer un inventaire pendant un clic n'est pas sûr
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (viewer.isOnline()) h.click(type);
            }
        });
    }

    public void onClose() {
    }

    // ------------------------------------------------------------------ helpers

    protected void set(int slot, ItemStack item, ClickHandler handler) {
        if (slot < 0 || slot >= inventory.getSize()) return;
        inventory.setItem(slot, item);
        if (handler != null) handlers.put(slot, handler);
        else handlers.remove(slot);
    }

    protected void set(int slot, ItemStack item) {
        set(slot, item, null);
    }

    /** Langue du joueur qui regarde le menu. */
    protected String lang() {
        return plugin.messages().lang(viewer);
    }

    protected String msg(String key, Object... replacements) {
        return plugin.messages().get(viewer, key, replacements);
    }

    protected List<String> lore(String key, Object... replacements) {
        return plugin.messages().list(viewer, key, replacements);
    }

    protected ItemBuilder item(String spec) {
        return ItemBuilder.of(spec);
    }

    /** Remplit les emplacements vides avec la vitre de fond. */
    protected void fill() {
        fill(0, inventory.getSize() - 1);
    }

    /** Remplit les emplacements vides entre from et to (inclus). */
    protected void fill(int from, int to) {
        String color = plugin.getConfig().getString("gui.filler-color", "BLACK");
        if (color.equalsIgnoreCase("NONE")) return;
        ItemStack filler = new ItemBuilder(Mat.pane(color)).name(" ").build();
        for (int i = Math.max(0, from); i <= to && i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) inventory.setItem(i, filler);
        }
    }

    /** Bouton « retour ». */
    protected void back(int slot, final Menu parent) {
        set(slot, item("ARROW").name(msg("gui.common.back")).build(), new ClickHandler() {
            @Override
            public void click(ClickType type) {
                parent.open();
            }
        });
    }

    protected void close(int slot) {
        set(slot, item("BARRIER").name(msg("gui.common.close")).build(), new ClickHandler() {
            @Override
            public void click(ClickType type) {
                viewer.closeInventory();
            }
        });
    }

    /** Élément ON/OFF (vitre verte / rouge). */
    protected ItemStack toggle(String name, boolean on, boolean locked, List<String> extraLore) {
        ItemBuilder b = new ItemBuilder(Mat.pane(locked ? "GRAY" : on ? "LIME" : "RED"))
                .name((locked ? "&7" : on ? "&a" : "&c") + name);
        if (extraLore != null) b.lore(extraLore);
        b.lore(msg(locked ? "gui.common.locked" : on ? "gui.common.enabled" : "gui.common.disabled"));
        if (!locked) b.lore(msg("gui.common.click-toggle"));
        return b.build();
    }

    protected Menu self() {
        return this;
    }
}
