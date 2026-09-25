package fr.spectatorplus.gui.menus;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.filter.Preferences;
import fr.spectatorplus.gui.Menu;
import fr.spectatorplus.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.ClickType;

/**
 * Filtres spécifiques aux évènements de dégâts.
 */
public final class DamageFilterMenu extends Menu {

    private boolean typesPage;

    public DamageFilterMenu(SpectatorPlus plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    public String id() {
        return "damage-filters";
    }

    @Override
    protected String title() {
        return msg(typesPage ? "gui.damage-filters.types-title" : "gui.damage-filters.title");
    }

    @Override
    protected int rows() {
        return typesPage ? 6 : 3;
    }

    private static double adjust(double value, ClickType type) {
        if (type == ClickType.MIDDLE || type == ClickType.DROP) return 0;
        double step = type.isShiftClick() ? 5 : 1;
        return Math.max(0, type.isRightClick() ? value - step : value + step);
    }

    @Override
    protected void render() {
        final Preferences p = plugin.filters().get(viewer);
        if (typesPage) {
            renderTypes(p);
            return;
        }
        set(10, item("REDSTONE").name(msg("gui.damage-filters.min.name"))
                .lore(lore("gui.damage-filters.min.lore", "value", Text.oneDecimal(p.minDamage)))
                .lore(lore("gui.common.number-help", "step", 1)).build(), new ClickHandler() {
            @Override
            public void click(ClickType type) {
                double old = p.minDamage;
                p.minDamage = adjust(old, type);
                plugin.filters().changed(viewer, "damage.min", old, p.minDamage);
                refresh();
            }
        });
        set(11, item("REDSTONE_BLOCK").name(msg("gui.damage-filters.max.name"))
                .lore(lore("gui.damage-filters.max.lore", "value", p.maxDamage == 0 ? msg("gui.common.unlimited") : Text.oneDecimal(p.maxDamage)))
                .lore(lore("gui.common.number-help", "step", 1)).build(), new ClickHandler() {
            @Override
            public void click(ClickType type) {
                double old = p.maxDamage;
                p.maxDamage = adjust(old, type);
                plugin.filters().changed(viewer, "damage.max", old, p.maxDamage);
                refresh();
            }
        });
        set(12, toggle(msg("gui.damage-filters.pvp-only"), p.pvpOnly, false, null), new ClickHandler() {
            @Override
            public void click(ClickType type) {
                p.pvpOnly = !p.pvpOnly;
                if (p.pvpOnly) p.pveOnly = false;
                plugin.filters().changed(viewer, "damage.pvp-only", !p.pvpOnly, p.pvpOnly);
                refresh();
            }
        });
        set(13, toggle(msg("gui.damage-filters.pve-only"), p.pveOnly, false, null), new ClickHandler() {
            @Override
            public void click(ClickType type) {
                p.pveOnly = !p.pveOnly;
                if (p.pveOnly) p.pvpOnly = false;
                plugin.filters().changed(viewer, "damage.pve-only", !p.pveOnly, p.pveOnly);
                refresh();
            }
        });
        set(14, toggle(msg("gui.damage-filters.critical-only"), p.criticalOnly, false, null), new ClickHandler() {
            @Override
            public void click(ClickType type) {
                p.criticalOnly = !p.criticalOnly;
                plugin.filters().changed(viewer, "damage.critical-only", !p.criticalOnly, p.criticalOnly);
                refresh();
            }
        });
        set(16, item("BLAZE_POWDER").name(msg("gui.damage-filters.types.name"))
                .lore(lore("gui.damage-filters.types.lore", "count", p.damageTypes.isEmpty()
                        ? msg("gui.common.all") : String.valueOf(p.damageTypes.size()))).build(), new ClickHandler() {
            @Override
            public void click(ClickType type) {
                typesPage = true;
                open();
            }
        });
        back(22, new FiltersMenu(plugin, viewer));
        fill();
    }

    private void renderTypes(final Preferences p) {
        int slot = 0;
        for (EntityDamageEvent.DamageCause cause : EntityDamageEvent.DamageCause.values()) {
            if (slot >= 45) break;
            final String name = cause.name();
            final boolean on = p.damageTypes.contains(name);
            set(slot++, toggle(Text.pretty(name), on, false, null), new ClickHandler() {
                @Override
                public void click(ClickType type) {
                    if (on) p.damageTypes.remove(name);
                    else p.damageTypes.add(name);
                    plugin.filters().changed(viewer, "damage.types", on, !on);
                    refresh();
                }
            });
        }
        set(47, item("PAPER").name(msg("gui.damage-filters.types-info")).build());
        set(51, item("LAVA_BUCKET").name(msg("gui.damage-filters.types-clear")).build(), new ClickHandler() {
            @Override
            public void click(ClickType type) {
                p.damageTypes.clear();
                plugin.filters().changed(viewer, "damage.types", "", "");
                refresh();
            }
        });
        set(49, item("ARROW").name(msg("gui.common.back")).build(), new ClickHandler() {
            @Override
            public void click(ClickType type) {
                typesPage = false;
                open();
            }
        });
        fill(45, 53);
    }
}
