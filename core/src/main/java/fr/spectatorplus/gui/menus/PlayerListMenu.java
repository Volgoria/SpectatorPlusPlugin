package fr.spectatorplus.gui.menus;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.core.platform.Click;
import fr.spectatorplus.core.platform.Icon;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.Position;
import fr.spectatorplus.gui.Menu;
import fr.spectatorplus.spectator.SpectatorSession;
import fr.spectatorplus.util.Text;

import java.util.List;
import java.util.Locale;

/**
 * Liste paginée des joueurs en vie. Le comportement du clic dépend du mode.
 */
public final class PlayerListMenu extends Menu {

    public enum Mode {TELEPORT, PLAYERS, FOLLOW, POV, INVENTORY}

    private static final int PER_PAGE = 45;
    private final Mode mode;
    private int page;

    public PlayerListMenu(SpectatorCore plugin, PlatformPlayer viewer, Mode mode) {
        super(plugin, viewer);
        this.mode = mode;
    }

    @Override
    public String id() {
        return mode == Mode.INVENTORY ? "inventories" : mode.name().toLowerCase(Locale.ROOT);
    }

    @Override
    protected String title() {
        return msg("gui.players.title." + mode.name().toLowerCase(Locale.ROOT), "page", page + 1);
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    public boolean autoRefresh() {
        return true;
    }

    @Override
    protected void render() {
        List<PlatformPlayer> players = plugin.spectators().targets();
        int pages = Math.max(1, (players.size() + PER_PAGE - 1) / PER_PAGE);
        if (page >= pages) page = pages - 1;
        SpectatorSession s = plugin.spectators().getSpectator(viewer);
        PlatformPlayer current = s == null ? null : s.getFollowTarget();

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < players.size(); i++) {
            final PlatformPlayer target = players.get(start + i);
            set(i, head(target, target.equals(current)), new ClickHandler() {
                @Override
                public void click(Click type) {
                    clicked(target, type);
                }
            });
        }
        if (players.isEmpty()) {
            set(22, item("BARRIER").name(msg("gui.players.empty")).build());
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
        set(47, item("PAPER").name(msg("gui.common.page", "page", page + 1, "pages", pages))
                .lore(msg("gui.players.count", "count", players.size())).build());
        back(49, new MainMenu(plugin, viewer));
        fill();
    }

    private Icon headBuilder(PlatformPlayer target) {
        Position l = target.getLocation();
        String dist = viewer.getLocation().sameWorld(l)
                ? String.valueOf((int) viewer.getLocation().distance(l)) : "-";
        String team = plugin.game().getTeam(target);
        return Icon.skull(target)
                .name(msg("gui.players.name", "player", target.getName()))
                .lore(lore("gui.players.lore",
                        "health", plugin.game().hearts(target.getHealth()),
                        "max_health", plugin.game().hearts(target.getMaxHealth()),
                        "food", target.getFoodLevel(),
                        "world", target.getWorld().getName(),
                        "x", l.getBlockX(), "y", l.getBlockY(), "z", l.getBlockZ(),
                        "distance", dist,
                        "team", team == null ? msg("placeholders.none") : team,
                        "kills", plugin.stats().kills(target.getUniqueId()),
                        "level", target.getLevel()));
    }

    private Icon head(PlatformPlayer target, boolean followed) {
        Icon b = headBuilder(target);
        if (followed) b.lore(msg("gui.players.followed"));
        b.lore(lore("gui.players.click." + mode.name().toLowerCase(Locale.ROOT)));
        return b.glow(followed).build();
    }

    private void clicked(PlatformPlayer target, Click type) {
        if (!target.isOnline()) {
            refresh();
            return;
        }
        SpectatorSession s = plugin.spectators().getSpectator(viewer);
        switch (mode) {
            case TELEPORT:
                if (s == null) return;
                viewer.closeInventory();
                plugin.spectators().teleport(s, target);
                break;
            case FOLLOW:
                if (s == null) return;
                viewer.closeInventory();
                plugin.spectators().follow(s, target);
                break;
            case POV:
                if (s == null) return;
                plugin.spectators().startPov(s, target);
                break;
            case INVENTORY:
                if (s == null) return;
                s.openInventory(target, type.isRightClick() && viewer.hasPermission("spectatorplus.inspect.enderchest"));
                break;
            default:
                if (s != null) s.openInspection(target);
                else new PlayerSheetMenu(plugin, viewer, target).open();
                break;
        }
    }
}
