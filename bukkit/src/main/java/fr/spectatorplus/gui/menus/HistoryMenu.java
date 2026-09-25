package fr.spectatorplus.gui.menus;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.api.event.EventCategory;
import fr.spectatorplus.api.event.SpectatorGameEvent;
import fr.spectatorplus.gui.Menu;
import fr.spectatorplus.spectator.SpectatorSession;
import fr.spectatorplus.util.ItemBuilder;
import fr.spectatorplus.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Historique des évènements de la partie (livre).
 */
public final class HistoryMenu extends Menu {

    private static final int PER_PAGE = 45;
    private int page;
    private boolean showAll;

    public HistoryMenu(SpectatorPlus plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    public String id() {
        return "history";
    }

    @Override
    protected String title() {
        return msg("gui.history.title", "page", page + 1);
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void render() {
        List<SpectatorGameEvent> events = new ArrayList<>();
        for (SpectatorGameEvent e : plugin.events().getHistory()) {
            if (showAll || plugin.filters().canSee(viewer, e)) events.add(e);
        }
        Collections.reverse(events);
        int pages = Math.max(1, (events.size() + PER_PAGE - 1) / PER_PAGE);
        if (page >= pages) page = pages - 1;
        SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < events.size(); i++) {
            final SpectatorGameEvent e = events.get(start + i);
            EventCategory cat = plugin.events().getCategory(e.getCategory());
            ItemBuilder b = item(e.getType().getIcon())
                    .name("&f" + plugin.messages().eventName(lang(), e.getType()))
                    .lore(plugin.events().format(viewer, e, false))
                    .lore(lore("gui.history.lore",
                            "time", fmt.format(new Date(e.getTimestamp())),
                            "category", cat == null ? e.getCategory() : plugin.messages().categoryName(lang(), cat),
                            "priority", e.getImportance() == null ? "-" : e.getImportance().name()));
            final UUID subject = e.getPrimaryPlayer();
            final Location loc = e.getLocation();
            if (subject != null || loc != null) b.lore(msg("gui.history.click"));
            set(i, b.build(), new ClickHandler() {
                @Override
                public void click(ClickType type) {
                    SpectatorSession s = plugin.spectators().getSpectator(viewer);
                    if (s == null) return;
                    Player p = subject == null ? null : Bukkit.getPlayer(subject);
                    viewer.closeInventory();
                    if (type.isRightClick() && loc != null && loc.getWorld() != null) {
                        viewer.teleport(loc.add(0, 1, 0));
                    } else if (p != null && p.isOnline() && !plugin.spectators().isSpectator(p)) {
                        plugin.spectators().teleport(s, p);
                    } else if (loc != null && loc.getWorld() != null) {
                        viewer.teleport(loc.add(0, 1, 0));
                    }
                }
            });
        }
        if (events.isEmpty()) set(22, item("BARRIER").name(msg("gui.history.empty")).build());

        if (page > 0) {
            set(45, item("ARROW").name(msg("gui.common.previous")).build(), new ClickHandler() {
                @Override
                public void click(ClickType type) {
                    page--;
                    refresh();
                }
            });
        }
        if (page < pages - 1) {
            set(53, item("ARROW").name(msg("gui.common.next")).build(), new ClickHandler() {
                @Override
                public void click(ClickType type) {
                    page++;
                    refresh();
                }
            });
        }
        set(47, item("HOPPER").name(msg(showAll ? "gui.history.show-all" : "gui.history.show-filtered"))
                .lore(msg("gui.common.click-toggle")).build(), new ClickHandler() {
            @Override
            public void click(ClickType type) {
                showAll = !showAll;
                page = 0;
                refresh();
            }
        });
        set(48, item("PAPER").name(msg("gui.common.page", "page", page + 1, "pages", pages))
                .lore(msg("gui.history.count", "count", events.size())).build());
        back(49, new MainMenu(plugin, viewer));
        if (viewer.hasPermission("spectatorplus.admin")) {
            set(51, item("LAVA_BUCKET").name(msg("gui.history.clear")).build(), new ClickHandler() {
                @Override
                public void click(ClickType type) {
                    if (!type.isShiftClick()) return;
                    plugin.events().clearHistory();
                    refresh();
                }
            });
        }
        fill(45, 53);
    }

    static String strip(String s) {
        return Text.strip(Text.color(s));
    }
}
