package fr.spectatorplus.gui.menus;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.api.filter.PlayerFilterMode;
import fr.spectatorplus.core.platform.Click;
import fr.spectatorplus.core.platform.Icon;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.filter.Preferences;
import fr.spectatorplus.gui.Menu;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Filtres par joueurs : mode, whitelist, blacklist, favoris, masquer ses propres évènements.
 */
public final class PlayerFilterMenu extends Menu {

    private static final int PER_PAGE = 36;
    private int page;

    public PlayerFilterMenu(SpectatorCore plugin, PlatformPlayer viewer) {
        super(plugin, viewer);
    }

    @Override
    public String id() {
        return "player-filters";
    }

    @Override
    protected String title() {
        return msg("gui.player-filters.title");
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void render() {
        final Preferences prefs = plugin.filters().get(viewer);
        Set<UUID> ids = new LinkedHashSet<>();
        for (PlatformPlayer p : plugin.spectators().targets()) ids.add(p.getUniqueId());
        ids.addAll(prefs.whitelist);
        ids.addAll(prefs.blacklist);
        ids.addAll(prefs.favorites);
        List<UUID> list = new ArrayList<>(ids);
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        if (page >= pages) page = pages - 1;
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < list.size(); i++) {
            final UUID id = list.get(start + i);
            PlatformPlayer online = plugin.platform().getPlayer(id);
            String name = online != null ? online.getName() : prefs.knownNames.get(id);
            if (name == null) name = id.toString().substring(0, 8);
            final String finalName = name;
            boolean wl = prefs.whitelist.contains(id), bl = prefs.blacklist.contains(id), fav = prefs.favorites.contains(id);
            Icon b = (online != null ? Icon.skull(online) : Icon.head())
                    .name("&e" + name)
                    .lore(lore("gui.player-filters.head-lore",
                            "whitelist", msg(wl ? "gui.common.yes" : "gui.common.no"),
                            "blacklist", msg(bl ? "gui.common.yes" : "gui.common.no"),
                            "favorite", msg(fav ? "gui.common.yes" : "gui.common.no")))
                    .glow(fav);
            set(i, b.build(), new ClickHandler() {
                @Override
                public void click(Click type) {
                    Set<UUID> set;
                    String key;
                    if (type.isShiftClick()) {
                        set = prefs.blacklist;
                        key = "player.blacklist";
                    } else if (type.isRightClick()) {
                        set = prefs.whitelist;
                        key = "player.whitelist";
                    } else {
                        set = prefs.favorites;
                        key = "player.favorites";
                    }
                    boolean had = set.contains(id);
                    if (had) set.remove(id);
                    else set.add(id);
                    prefs.knownNames.put(id, finalName);
                    plugin.filters().changed(viewer, key, had, !had);
                    refresh();
                }
            });
        }

        set(38, item("COMPARATOR|REDSTONE_COMPARATOR").name(msg("gui.player-filters.mode.name"))
                .lore(lore("gui.player-filters.mode.lore",
                        "mode", msg("player-mode." + prefs.playerMode.name().toLowerCase(Locale.ROOT))))
                .build(), new ClickHandler() {
            @Override
            public void click(Click type) {
                PlayerFilterMode next = prefs.playerMode.next();
                plugin.filters().setPlayerFilterMode(viewer, next);
                refresh();
            }
        });
        set(40, toggle(msg("gui.player-filters.hide-own"), prefs.hideOwn, false, null), new ClickHandler() {
            @Override
            public void click(Click type) {
                prefs.hideOwn = !prefs.hideOwn;
                plugin.filters().changed(viewer, "player.hide-own", !prefs.hideOwn, prefs.hideOwn);
                refresh();
            }
        });
        set(42, item("WHITE_BANNER|BANNER:15").name(msg("gui.player-filters.teams.name"))
                .lore(lore("gui.player-filters.teams.lore", "count", prefs.teams.size())).build(), new ClickHandler() {
            @Override
            public void click(Click type) {
                new TeamFilterMenu(plugin, viewer).open();
            }
        });
        set(44, item("BOOK").name(msg("gui.player-filters.help.name")).lore(lore("gui.player-filters.help.lore")).build());
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
        back(49, new FiltersMenu(plugin, viewer));
        fill(36, 53);
    }
}
