package fr.spectatorplus.gui.menus;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.compat.Compat;
import fr.spectatorplus.gui.Menu;
import fr.spectatorplus.spectator.SpectatorSession;
import fr.spectatorplus.util.ItemBuilder;
import fr.spectatorplus.util.Text;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

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

    public PlayerListMenu(SpectatorPlus plugin, Player viewer, Mode mode) {
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
        List<Player> players = plugin.spectators().targets();
        int pages = Math.max(1, (players.size() + PER_PAGE - 1) / PER_PAGE);
        if (page >= pages) page = pages - 1;
        SpectatorSession s = plugin.spectators().getSpectator(viewer);
        Player current = s == null ? null : s.getFollowTarget();

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < players.size(); i++) {
            final Player target = players.get(start + i);
            set(i, head(target, target == current), new ClickHandler() {
                @Override
                public void click(ClickType type) {
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
        set(47, item("PAPER").name(msg("gui.common.page", "page", page + 1, "pages", pages))
                .lore(msg("gui.players.count", "count", players.size())).build());
        back(49, new MainMenu(plugin, viewer));
        fill();
    }

    private ItemBuilder headBuilder(Player target) {
        Location l = target.getLocation();
        String dist = viewer.getWorld().equals(target.getWorld())
                ? String.valueOf((int) viewer.getLocation().distance(l)) : "-";
        String team = plugin.game().getTeam(target);
        return new ItemBuilder(Compat.skull(target))
                .name(msg("gui.players.name", "player", target.getName()))
                .lore(lore("gui.players.lore",
                        "health", Text.hearts(target.getHealth()),
                        "max_health", Text.hearts(Compat.maxHealth(target)),
                        "food", target.getFoodLevel(),
                        "world", target.getWorld().getName(),
                        "x", l.getBlockX(), "y", l.getBlockY(), "z", l.getBlockZ(),
                        "distance", dist,
                        "team", team == null ? msg("placeholders.none") : team,
                        "kills", plugin.stats().kills(target.getUniqueId()),
                        "level", target.getLevel()));
    }

    private org.bukkit.inventory.ItemStack head(Player target, boolean followed) {
        ItemBuilder b = headBuilder(target);
        if (followed) b.lore(msg("gui.players.followed"));
        b.lore(lore("gui.players.click." + mode.name().toLowerCase(Locale.ROOT)));
        return b.glow(followed).build();
    }

    private void clicked(Player target, ClickType type) {
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
