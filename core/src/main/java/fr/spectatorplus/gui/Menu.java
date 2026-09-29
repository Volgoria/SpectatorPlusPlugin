package fr.spectatorplus.gui;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.compat.Sounds;
import fr.spectatorplus.core.platform.Click;
import fr.spectatorplus.core.platform.Icon;
import fr.spectatorplus.core.platform.ItemRef;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.util.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Menu graphique de base, indépendant de la plateforme : il décrit son contenu ({@link Icon}) et ses
 * actions ; la plateforme l'affiche dans un inventaire et lui transmet les clics.
 */
public abstract class Menu {

    public interface ClickHandler {
        void click(Click type);
    }

    protected final SpectatorCore plugin;
    protected final PlatformPlayer viewer;
    private final Map<Integer, ClickHandler> handlers = new HashMap<>();
    private Icon[] items;
    private String renderedTitle = "";

    protected Menu(SpectatorCore plugin, PlatformPlayer viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    /** Identifiant transmis aux évènements d'ouverture / fermeture de menu. */
    public abstract String id();

    protected abstract String title();

    protected abstract int rows();

    protected abstract void render();

    /** Rafraîchissement automatique chaque seconde (fiches joueurs, infos de partie...). */
    public boolean autoRefresh() {
        return false;
    }

    public PlatformPlayer getViewer() {
        return viewer;
    }

    // ------------------------------------------------------------------ lecture (plateformes)

    /** Nombre de cases (multiple de 9, de 9 à 54). */
    public int getSize() {
        return items == null ? Math.max(1, Math.min(6, rows())) * 9 : items.length;
    }

    /** Titre coloré (« § »). */
    public String getTitle() {
        return renderedTitle;
    }

    /** Contenu actuel ; une case null est vide. */
    public Icon getItem(int slot) {
        return items == null || slot < 0 || slot >= items.length ? null : items[slot];
    }

    // ------------------------------------------------------------------ cycle de vie

    public void open() {
        if (!viewer.isOnline()) return;
        if (!plugin.platform().api().menuOpen(viewer, id())) return;
        items = new Icon[Math.max(1, Math.min(6, rows())) * 9];
        handlers.clear();
        renderedTitle = Text.color(title());
        render();
        plugin.menus().opening(viewer, this);
        plugin.platform().openMenu(viewer, this);
        plugin.menus().opened(viewer, this);
    }

    public void refresh() {
        if (items == null) return;
        handlers.clear();
        items = new Icon[items.length];
        render();
        plugin.platform().refreshMenu(viewer, this);
    }

    public void handleClick(int slot, final Click type) {
        final ClickHandler h = handlers.get(slot);
        if (h == null) return;
        if (plugin.filters().get(viewer).sounds) viewer.playSound(Sounds.CLICK, 0.4f, 1.4f);
        // exécuté au tick suivant : ouvrir / fermer un inventaire pendant un clic n'est pas sûr
        plugin.platform().runLater(new Runnable() {
            @Override
            public void run() {
                if (viewer.isOnline()) h.click(type);
            }
        }, 1L);
    }

    public void onClose() {
    }

    // ------------------------------------------------------------------ helpers

    protected void set(int slot, Icon item, ClickHandler handler) {
        if (items == null || slot < 0 || slot >= items.length) return;
        items[slot] = item == null || item.isEmpty() ? null : item;
        if (handler != null) handlers.put(slot, handler);
        else handlers.remove(slot);
    }

    protected void set(int slot, Icon item) {
        set(slot, item, null);
    }

    /** Copie d'un objet réel (inventaire d'un joueur). */
    protected void set(int slot, ItemRef item) {
        set(slot, item == null ? null : Icon.item(item), null);
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

    protected Icon item(String spec) {
        return Icon.of(spec);
    }

    /** Remplit les emplacements vides avec la vitre de fond. */
    protected void fill() {
        fill(0, items.length - 1);
    }

    /** Remplit les emplacements vides entre from et to (inclus). */
    protected void fill(int from, int to) {
        String color = plugin.config().getString("gui.filler-color", "BLACK");
        if (color.equalsIgnoreCase("NONE")) return;
        for (int i = Math.max(0, from); i <= to && i < items.length; i++) {
            if (items[i] == null) items[i] = Icon.pane(color).name(" ");
        }
    }

    /** Bouton « retour ». */
    protected void back(int slot, final Menu parent) {
        set(slot, item("ARROW").name(msg("gui.common.back")), new ClickHandler() {
            @Override
            public void click(Click type) {
                parent.open();
            }
        });
    }

    protected void close(int slot) {
        set(slot, item("BARRIER").name(msg("gui.common.close")), new ClickHandler() {
            @Override
            public void click(Click type) {
                viewer.closeInventory();
            }
        });
    }

    /** Élément ON/OFF (vitre verte / rouge). */
    protected Icon toggle(String name, boolean on, boolean locked, List<String> extraLore) {
        Icon b = Icon.pane(locked ? "GRAY" : on ? "LIME" : "RED")
                .name((locked ? "&7" : on ? "&a" : "&c") + name);
        if (extraLore != null) b.lore(extraLore);
        b.lore(msg(locked ? "gui.common.locked" : on ? "gui.common.enabled" : "gui.common.disabled"));
        if (!locked) b.lore(msg("gui.common.click-toggle"));
        return b;
    }

    protected Menu self() {
        return this;
    }
}
