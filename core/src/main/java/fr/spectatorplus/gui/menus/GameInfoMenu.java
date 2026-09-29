package fr.spectatorplus.gui.menus;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.core.platform.Icon;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.PlatformWorld;
import fr.spectatorplus.game.GameManager;
import fr.spectatorplus.gui.Menu;
import fr.spectatorplus.util.Text;

/**
 * Informations générales de la partie (horloge).
 */
public final class GameInfoMenu extends Menu {

    public GameInfoMenu(SpectatorCore plugin, PlatformPlayer viewer) {
        super(plugin, viewer);
    }

    @Override
    public String id() {
        return "game-info";
    }

    @Override
    protected String title() {
        return msg("gui.game-info.title");
    }

    @Override
    protected int rows() {
        return 3;
    }

    @Override
    public boolean autoRefresh() {
        return true;
    }

    @Override
    protected void render() {
        GameManager g = plugin.game();
        String yes = msg("gui.common.yes"), no = msg("gui.common.no");
        set(10, item("CLOCK|WATCH").name(msg("gui.game-info.duration"))
                .lore(lore("gui.game-info.duration-lore",
                        "running", g.isGameRunning() ? yes : no,
                        "time", Text.duration(g.getGameDuration()))).build());
        set(11, Icon.head().name(msg("gui.game-info.alive"))
                .lore(lore("gui.game-info.alive-lore",
                        "alive", g.getAlivePlayers().size(),
                        "initial", g.getInitialPlayers())).build());
        set(12, item("SKELETON_SKULL|SKULL_ITEM:0").name(msg("gui.game-info.dead"))
                .lore(lore("gui.game-info.dead-lore",
                        "eliminated", g.getEliminatedCount(),
                        "spectators", plugin.spectators().getSpectators().size())).build());
        set(13, item("WHITE_BANNER|BANNER:15").name(msg("gui.game-info.teams"))
                .lore(lore("gui.game-info.teams-lore", "teams", g.getRemainingTeams())).build());
        PlatformWorld w = viewer.getWorld();
        set(14, item("BARRIER").name(msg("gui.game-info.border"))
                .lore(lore("gui.game-info.border-lore",
                        "world", w.getName(),
                        "size", (int) w.getBorderSize(),
                        "x", (int) Math.floor(w.getBorderCenterX()),
                        "z", (int) Math.floor(w.getBorderCenterZ()))).build());
        set(15, item("BOOK").name(msg("gui.game-info.episode"))
                .lore(lore("gui.game-info.episode-lore", "episode", g.getEpisode())).build());
        set(16, item("IRON_SWORD").name(msg("gui.game-info.pvp"))
                .lore(lore("gui.game-info.pvp-lore", "pvp", g.isPvpEnabled() ? yes : no)).build());
        set(4, item("NETHER_STAR").name(msg("gui.game-info.server"))
                .lore(lore("gui.game-info.server-lore",
                        "online", plugin.platform().getOnlinePlayers().size(),
                        "tps", Text.oneDecimal(plugin.tps().current()),
                        "mode", plugin.getMode().name())).build());
        back(22, new MainMenu(plugin, viewer));
        fill();
    }
}
