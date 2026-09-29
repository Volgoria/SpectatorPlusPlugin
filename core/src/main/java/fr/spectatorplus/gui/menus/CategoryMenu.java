package fr.spectatorplus.gui.menus;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.api.event.EventCategory;
import fr.spectatorplus.api.event.SpectatorEventType;
import fr.spectatorplus.core.platform.Click;
import fr.spectatorplus.core.platform.Icon;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.filter.FilterManager;
import fr.spectatorplus.gui.Menu;
import fr.spectatorplus.util.Text;

import java.util.List;
import java.util.Locale;

/**
 * Évènements d'une catégorie : activation individuelle.
 */
public final class CategoryMenu extends Menu {

    private static final int PER_PAGE = 45;
    private final String category;
    private int page;

    public CategoryMenu(SpectatorCore plugin, PlatformPlayer viewer, String category) {
        super(plugin, viewer);
        this.category = category;
    }

    @Override
    public String id() {
        return "category";
    }

    @Override
    protected String title() {
        EventCategory c = plugin.events().getCategory(category);
        return msg("gui.category.title", "category", c == null ? category : Text.strip(Text.color(plugin.messages().categoryName(lang(), c))));
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void render() {
        final FilterManager fm = plugin.filters();
        List<SpectatorEventType> types = plugin.events().getTypes(category);
        int pages = Math.max(1, (types.size() + PER_PAGE - 1) / PER_PAGE);
        if (page >= pages) page = pages - 1;
        final boolean editable = fm.canModify(viewer, category);
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < types.size(); i++) {
            final SpectatorEventType t = types.get(start + i);
            final FilterManager.State state = fm.eventState(viewer, t);
            String color = state == FilterManager.State.ENABLED ? "LIME" : state == FilterManager.State.LOCKED ? "GRAY" : "RED";
            String reason = "";
            if (fm.isForced(t.getId())) reason = msg("gui.category.forced");
            else if (fm.isGloballyDisabled(t.getId()) || !plugin.events().isEnabled(t.getId())) reason = msg("gui.category.disabled-admin");
            Icon b = Icon.pane(color)
                    .name((state == FilterManager.State.ENABLED ? "&a" : state == FilterManager.State.LOCKED ? "&7" : "&c") + Text.strip(Text.color(plugin.messages().eventName(lang(), t))))
                    .lore(lore("gui.category.event-lore", "id", t.getId(),
                            "priority", msg("importance." + plugin.events().importanceOf(t.getId()).name())))
                    .lore(msg("gui.filters.state." + state.name().toLowerCase(Locale.ROOT)));
            if (!reason.isEmpty()) b.lore(reason);
            else if (state != FilterManager.State.LOCKED) b.lore(msg("gui.common.click-toggle"));
            set(i, b.build(), new ClickHandler() {
                @Override
                public void click(Click type) {
                    if (state == FilterManager.State.LOCKED) {
                        plugin.messages().send(viewer, "errors.filter-locked");
                        return;
                    }
                    fm.setEventEnabled(viewer, t.getId(), state != FilterManager.State.ENABLED);
                    refresh();
                }
            });
        }
        if (page > 0) {
            set(45, item("ARROW").name(msg("gui.common.previous")).build(), new ClickHandler() {
                @Override
                public void click(Click type) {
                    page--;
                    refresh();
                }
            });
        }
        if (page < pages - 1) {
            set(53, item("ARROW").name(msg("gui.common.next")).build(), new ClickHandler() {
                @Override
                public void click(Click type) {
                    page++;
                    refresh();
                }
            });
        }
        if (editable) {
            final List<SpectatorEventType> all = types;
            set(47, item("LIME_WOOL|WOOL:5").name(msg("gui.category.enable-all")).build(), new ClickHandler() {
                @Override
                public void click(Click type) {
                    for (SpectatorEventType t : all) fm.get(viewer).events.put(t.getId(), true);
                    fm.setCategoryEnabled(viewer, category, true);
                    refresh();
                }
            });
            set(51, item("RED_WOOL|WOOL:14").name(msg("gui.category.disable-all")).build(), new ClickHandler() {
                @Override
                public void click(Click type) {
                    for (SpectatorEventType t : all) fm.get(viewer).events.put(t.getId(), false);
                    fm.changed(viewer, "category." + category, true, false);
                    refresh();
                }
            });
        }
        back(49, new FiltersMenu(plugin, viewer));
        fill(45, 53);
    }
}
