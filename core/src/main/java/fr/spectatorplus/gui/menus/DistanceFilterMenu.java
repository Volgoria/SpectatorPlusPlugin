package fr.spectatorplus.gui.menus;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.core.platform.Click;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.filter.Preferences;
import fr.spectatorplus.gui.Menu;

/**
 * Filtres par distance entre le spectateur et l'évènement.
 */
public final class DistanceFilterMenu extends Menu {

    public DistanceFilterMenu(SpectatorCore plugin, PlatformPlayer viewer) {
        super(plugin, viewer);
    }

    @Override
    public String id() {
        return "distance-filters";
    }

    @Override
    protected String title() {
        return msg("gui.distance-filters.title");
    }

    @Override
    protected int rows() {
        return 3;
    }

    private int step() {
        return Math.max(1, plugin.config().getInt("gui.distance-step", 25));
    }

    private int adjust(int value, Click type) {
        if (type == Click.MIDDLE || type == Click.DROP) return 0;
        int mult = type.isShiftClick() ? 4 : 1;
        int res = type.isRightClick() ? value - step() * mult : value + step() * mult;
        return Math.max(0, res);
    }

    @Override
    protected void render() {
        final Preferences p = plugin.filters().get(viewer);
        set(10, item("ENDER_PEARL").name(msg("gui.distance-filters.max.name"))
                .lore(lore("gui.distance-filters.max.lore", "value", p.maxDistance == 0 ? msg("gui.common.unlimited") : p.maxDistance))
                .lore(lore("gui.common.number-help", "step", step())).build(), new ClickHandler() {
            @Override
            public void click(Click type) {
                int old = p.maxDistance;
                p.maxDistance = adjust(old, type);
                plugin.filters().changed(viewer, "distance.max", old, p.maxDistance);
                refresh();
            }
        });
        set(11, item("SLIME_BALL").name(msg("gui.distance-filters.min.name"))
                .lore(lore("gui.distance-filters.min.lore", "value", p.minDistance))
                .lore(lore("gui.common.number-help", "step", step())).build(), new ClickHandler() {
            @Override
            public void click(Click type) {
                int old = p.minDistance;
                p.minDistance = adjust(old, type);
                plugin.filters().changed(viewer, "distance.min", old, p.minDistance);
                refresh();
            }
        });
        set(13, toggle(msg("gui.distance-filters.same-chunk"), p.sameChunk, false, null), new ClickHandler() {
            @Override
            public void click(Click type) {
                p.sameChunk = !p.sameChunk;
                plugin.filters().changed(viewer, "distance.same-chunk", !p.sameChunk, p.sameChunk);
                refresh();
            }
        });
        set(14, toggle(msg("gui.distance-filters.same-world"), p.sameWorld, false, null), new ClickHandler() {
            @Override
            public void click(Click type) {
                p.sameWorld = !p.sameWorld;
                plugin.filters().changed(viewer, "distance.same-world", !p.sameWorld, p.sameWorld);
                refresh();
            }
        });
        set(15, toggle(msg("gui.distance-filters.same-zone"), p.sameZone, plugin.zones().getZones().isEmpty(), null), new ClickHandler() {
            @Override
            public void click(Click type) {
                if (plugin.zones().getZones().isEmpty()) return;
                p.sameZone = !p.sameZone;
                plugin.filters().changed(viewer, "distance.same-zone", !p.sameZone, p.sameZone);
                refresh();
            }
        });
        back(22, new FiltersMenu(plugin, viewer));
        fill();
    }
}
