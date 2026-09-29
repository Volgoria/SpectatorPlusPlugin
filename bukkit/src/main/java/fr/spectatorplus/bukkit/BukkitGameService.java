package fr.spectatorplus.bukkit;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.api.game.GameService;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.game.GameManager;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * API publique de la partie (Bukkit) → {@link GameManager}.
 */
public final class BukkitGameService implements GameService {

    private final SpectatorCore core;
    private final BukkitPlatform platform;

    public BukkitGameService(SpectatorCore core, BukkitPlatform platform) {
        this.core = core;
        this.platform = platform;
    }

    private GameManager g() {
        return core.game();
    }

    @Override
    public void startGame() {
        g().startGame();
    }

    @Override
    public void endGame() {
        g().endGame();
    }

    @Override
    public boolean isGameRunning() {
        return g().isGameRunning();
    }

    @Override
    public long getGameDuration() {
        return g().getGameDuration();
    }

    @Override
    public void startPvp() {
        g().startPvp();
    }

    @Override
    public boolean isPvpEnabled() {
        return g().isPvpEnabled();
    }

    @Override
    public void endGracePeriod() {
        g().endGracePeriod();
    }

    @Override
    public void startEpisode(int episode) {
        g().startEpisode(episode);
    }

    @Override
    public void endEpisode(int episode) {
        g().endEpisode(episode);
    }

    @Override
    public int getEpisode() {
        return g().getEpisode();
    }

    @Override
    public void declareWinner(Player player) {
        g().declareWinner(platform.wrap(player));
    }

    @Override
    public void declareWinningTeam(String team) {
        g().declareWinningTeam(team);
    }

    @Override
    public void eliminate(Player player) {
        g().eliminate(platform.wrap(player));
    }

    @Override
    public Collection<Player> getAlivePlayers() {
        List<Player> res = new ArrayList<>();
        for (PlatformPlayer p : g().getAlivePlayers()) res.add(BukkitPlayer.unwrap(p));
        return res;
    }

    @Override
    public void setAlivePlayersProvider(final AlivePlayersProvider provider) {
        if (provider == null) {
            g().setAlivePlayersProvider(null);
            return;
        }
        g().setAlivePlayersProvider(new GameManager.AliveProvider() {
            @Override
            public Collection<PlatformPlayer> getAlivePlayers() {
                Collection<Player> c = provider.getAlivePlayers();
                if (c == null) return null;
                List<PlatformPlayer> res = new ArrayList<>();
                for (Player p : c) res.add(platform.wrap(p));
                return res;
            }
        });
    }

    @Override
    public String getTeam(Player player) {
        return g().getTeam(platform.wrap(player));
    }

    @Override
    public void setTeamProvider(final TeamProvider provider) {
        if (provider == null) {
            g().setTeamProvider(null);
            return;
        }
        g().setTeamProvider(new GameManager.TeamProvider() {
            @Override
            public String getTeam(PlatformPlayer player) {
                return provider.getTeam(BukkitPlayer.unwrap(player));
            }
        });
    }

    @Override
    public int getRemainingTeams() {
        return g().getRemainingTeams();
    }
}
