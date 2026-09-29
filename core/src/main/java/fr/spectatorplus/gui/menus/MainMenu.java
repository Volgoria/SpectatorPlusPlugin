package fr.spectatorplus.gui.menus;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.api.LeaveReason;
import fr.spectatorplus.core.platform.Click;
import fr.spectatorplus.core.platform.Icon;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.gui.Menu;
import fr.spectatorplus.spectator.SpectatorSession;

/**
 * Menu principal (étoile du Nether).
 */
public final class MainMenu extends Menu {

    public MainMenu(SpectatorCore plugin, PlatformPlayer viewer) {
        super(plugin, viewer);
    }

    @Override
    public String id() {
        return "main";
    }

    @Override
    protected String title() {
        return msg("gui.main.title");
    }

    @Override
    protected int rows() {
        return 5;
    }

    @Override
    protected void render() {
        SpectatorSession s = plugin.spectators().getSpectator(viewer);
        PlatformPlayer target = s == null ? null : s.getFollowTarget();
        set(4, Icon.skull(viewer).name("&b" + viewer.getName())
                .lore(lore("gui.main.info.lore",
                        "state", s == null ? "-" : s.getState().name(),
                        "target", target == null ? msg("placeholders.none") : target.getName(),
                        "preset", plugin.filters().get(viewer).preset))
                .build());

        set(10, item("COMPASS").name(msg("gui.main.teleport.name")).lore(lore("gui.main.teleport.lore")).build(), new ClickHandler() {
            @Override
            public void click(Click type) {
                new PlayerListMenu(plugin, viewer, PlayerListMenu.Mode.TELEPORT).open();
            }
        });
        set(11, Icon.head().name(msg("gui.main.players.name")).lore(lore("gui.main.players.lore")).build(), new ClickHandler() {
            @Override
            public void click(Click type) {
                new PlayerListMenu(plugin, viewer, PlayerListMenu.Mode.PLAYERS).open();
            }
        });
        set(12, item("ENDER_EYE|EYE_OF_ENDER").name(msg("gui.main.follow.name")).lore(lore("gui.main.follow.lore")).build(), new ClickHandler() {
            @Override
            public void click(Click type) {
                if (type.isRightClick() && s != null) {
                    plugin.spectators().stopFollowing(s, true);
                    refresh();
                } else {
                    new PlayerListMenu(plugin, viewer, PlayerListMenu.Mode.FOLLOW).open();
                }
            }
        });
        if (plugin.config().getBoolean("pov.enabled", true) && viewer.hasPermission("spectatorplus.pov")) {
            set(13, item("SPYGLASS|ENDER_PEARL").name(msg("gui.main.pov.name")).lore(lore("gui.main.pov.lore")).build(), new ClickHandler() {
                @Override
                public void click(Click type) {
                    new PlayerListMenu(plugin, viewer, PlayerListMenu.Mode.POV).open();
                }
            });
        }
        set(14, item("BOOK").name(msg("gui.main.history.name")).lore(lore("gui.main.history.lore")).build(), new ClickHandler() {
            @Override
            public void click(Click type) {
                new HistoryMenu(plugin, viewer).open();
            }
        });
        set(15, item("CHEST").name(msg("gui.main.inventories.name")).lore(lore("gui.main.inventories.lore")).build(), new ClickHandler() {
            @Override
            public void click(Click type) {
                new PlayerListMenu(plugin, viewer, PlayerListMenu.Mode.INVENTORY).open();
            }
        });
        set(16, item("HOPPER").name(msg("gui.main.filters.name")).lore(lore("gui.main.filters.lore")).build(), new ClickHandler() {
            @Override
            public void click(Click type) {
                new FiltersMenu(plugin, viewer).open();
            }
        });
        set(29, item("CLOCK|WATCH").name(msg("gui.main.info.name")).lore(lore("gui.main.info.description")).build(), new ClickHandler() {
            @Override
            public void click(Click type) {
                new GameInfoMenu(plugin, viewer).open();
            }
        });
        set(31, item("COMPARATOR|REDSTONE_COMPARATOR").name(msg("gui.main.settings.name")).lore(lore("gui.main.settings.lore")).build(), new ClickHandler() {
            @Override
            public void click(Click type) {
                new SettingsMenu(plugin, viewer).open();
            }
        });
        if (s != null && viewer.hasPermission("spectatorplus.leave")) {
            set(33, item("OAK_DOOR|WOOD_DOOR").name(msg("gui.main.leave.name")).lore(lore("gui.main.leave.lore")).build(), new ClickHandler() {
                @Override
                public void click(Click type) {
                    viewer.closeInventory();
                    plugin.spectators().leave(viewer, LeaveReason.COMMAND, false);
                }
            });
        }
        close(40);
        fill();
    }
}
