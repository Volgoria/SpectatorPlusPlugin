package fr.spectatorplus.gui.menus;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.api.event.EventCategory;
import fr.spectatorplus.api.event.Importance;
import fr.spectatorplus.filter.FilterManager;
import fr.spectatorplus.filter.Preferences;
import fr.spectatorplus.filter.Preset;
import fr.spectatorplus.gui.Menu;
import fr.spectatorplus.util.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Locale;

/**
 * Menu principal des filtres : catégories + accès aux filtres avancés.
 */
public final class FiltersMenu extends Menu {

    private static final int[] CATEGORY_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

    public FiltersMenu(SpectatorPlus plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    public String id() {
        return "filters";
    }

    @Override
    protected String title() {
        return msg("gui.filters.title");
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void render() {
        final FilterManager fm = plugin.filters();
        final Preferences prefs = fm.get(viewer);
        int i = 0;
        for (final EventCategory cat : plugin.events().getCategories()) {
            if (i >= CATEGORY_SLOTS.length) break;
            if (plugin.events().getTypes(cat.getId()).isEmpty()) continue;
            FilterManager.State state = fm.categoryState(viewer, cat.getId());
            String color = state == FilterManager.State.ENABLED ? "&a" : state == FilterManager.State.PARTIAL ? "&e"
                    : state == FilterManager.State.LOCKED ? "&7" : "&c";
            ItemBuilder b = item(cat.getIcon()).name(color + fr.spectatorplus.util.Text.strip(fr.spectatorplus.util.Text.color(plugin.messages().categoryName(lang(), cat))))
                    .lore(msg("gui.filters.state." + state.name().toLowerCase(Locale.ROOT)))
                    .lore(lore("gui.filters.category-lore"))
                    .glow(state == FilterManager.State.ENABLED);
            set(CATEGORY_SLOTS[i++], b.build(), new ClickHandler() {
                @Override
                public void click(ClickType type) {
                    if (type.isRightClick()) {
                        if (!fm.canModify(viewer, cat.getId())) {
                            plugin.messages().send(viewer, "errors.filter-locked");
                            return;
                        }
                        fm.setCategoryEnabled(viewer, cat.getId(), !fm.categoryEnabled(prefs, cat.getId()));
                        refresh();
                    } else {
                        new CategoryMenu(plugin, viewer, cat.getId()).open();
                    }
                }
            });
        }

        Preset preset = fm.preset(prefs.preset);
        String presetName = preset == null ? prefs.preset : plugin.messages().presetName(lang(), preset);
        if (prefs.customized) presetName += msg("gui.filters.customized-suffix");
        set(37, item("BOOK").name(msg("gui.filters.preset.name")).lore(lore("gui.filters.preset.lore", "preset", presetName))
                .build(), new ClickHandler() {
            @Override
            public void click(ClickType type) {
                if (!fm.canModify(viewer, "preset")) {
                    plugin.messages().send(viewer, "errors.filter-locked");
                    return;
                }
                new PresetMenu(plugin, viewer).open();
            }
        });
        set(38, item("REDSTONE_TORCH|REDSTONE_TORCH_ON").name(msg("gui.filters.importance.name"))
                .lore(lore("gui.filters.importance.lore", "importance", msg("importance." + prefs.minImportance.name())))
                .build(), new ClickHandler() {
            @Override
            public void click(ClickType type) {
                if (!fm.canModify(viewer, "importance")) {
                    plugin.messages().send(viewer, "errors.filter-locked");
                    return;
                }
                Importance next = type.isRightClick()
                        ? Importance.values()[(prefs.minImportance.ordinal() + Importance.values().length - 1) % Importance.values().length]
                        : prefs.minImportance.next();
                fm.setMinimumImportance(viewer, next);
                refresh();
            }
        });
        link(39, "PLAYER_HEAD|SKULL_ITEM:3", "gui.filters.players", "player", new PlayerFilterMenu(plugin, viewer));
        link(40, "GRASS_BLOCK|GRASS", "gui.filters.worlds", "world", new WorldFilterMenu(plugin, viewer));
        link(41, "COMPASS", "gui.filters.distance", "distance", new DistanceFilterMenu(plugin, viewer));
        link(42, "REDSTONE", "gui.filters.damage", "damage", new DamageFilterMenu(plugin, viewer));
        set(43, item("TNT").name(msg("gui.filters.reset.name")).lore(lore("gui.filters.reset.lore")).build(), new ClickHandler() {
            @Override
            public void click(ClickType type) {
                if (!type.isShiftClick()) return;
                fm.resetFilters(viewer);
                plugin.messages().send(viewer, "filters.reset");
                refresh();
            }
        });
        back(49, new MainMenu(plugin, viewer));
        close(53);
        fill();
    }

    private void link(int slot, String icon, String key, final String filterKey, final Menu menu) {
        boolean allowed = plugin.filters().canModify(viewer, filterKey);
        set(slot, item(allowed ? icon : "GRAY_STAINED_GLASS_PANE|STAINED_GLASS_PANE:7")
                .name(msg(key + ".name")).lore(lore(key + ".lore"))
                .lore(allowed ? new String[0] : new String[]{msg("gui.common.locked")}).build(), new ClickHandler() {
            @Override
            public void click(ClickType type) {
                if (!plugin.filters().canModify(viewer, filterKey)) {
                    plugin.messages().send(viewer, "errors.filter-locked");
                    return;
                }
                menu.open();
            }
        });
    }
}
