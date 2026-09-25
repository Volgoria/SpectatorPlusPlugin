package fr.spectatorplus.gui.menus;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.filter.Preferences;
import fr.spectatorplus.gui.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

/**
 * Filtres par monde / dimension.
 */
public final class WorldFilterMenu extends Menu {

    public WorldFilterMenu(SpectatorPlus plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    public String id() {
        return "world-filters";
    }

    @Override
    protected String title() {
        return msg("gui.world-filters.title");
    }

    @Override
    protected int rows() {
        return 3;
    }

    private interface Setter {
        void set(Preferences p, boolean v);
    }

    private void option(int slot, final String key, boolean value, final Setter setter) {
        set(slot, toggle(msg("gui.world-filters." + key), value, false, null), new ClickHandler() {
            @Override
            public void click(ClickType type) {
                Preferences p = plugin.filters().get(viewer);
                boolean old = value(p, key);
                setter.set(p, !old);
                plugin.filters().changed(viewer, "world." + key, old, !old);
                refresh();
            }
        });
    }

    private static boolean value(Preferences p, String key) {
        switch (key) {
            case "overworld":
                return p.overworld;
            case "nether":
                return p.nether;
            case "end":
                return p.end;
            case "custom":
                return p.customWorlds;
            default:
                return p.currentWorldOnly;
        }
    }

    @Override
    protected void render() {
        Preferences p = plugin.filters().get(viewer);
        option(10, "overworld", p.overworld, new Setter() {
            @Override
            public void set(Preferences pr, boolean v) {
                pr.overworld = v;
            }
        });
        option(11, "nether", p.nether, new Setter() {
            @Override
            public void set(Preferences pr, boolean v) {
                pr.nether = v;
            }
        });
        option(12, "end", p.end, new Setter() {
            @Override
            public void set(Preferences pr, boolean v) {
                pr.end = v;
            }
        });
        option(13, "custom", p.customWorlds, new Setter() {
            @Override
            public void set(Preferences pr, boolean v) {
                pr.customWorlds = v;
            }
        });
        option(15, "current-only", p.currentWorldOnly, new Setter() {
            @Override
            public void set(Preferences pr, boolean v) {
                pr.currentWorldOnly = v;
            }
        });
        back(22, new FiltersMenu(plugin, viewer));
        fill();
    }
}
