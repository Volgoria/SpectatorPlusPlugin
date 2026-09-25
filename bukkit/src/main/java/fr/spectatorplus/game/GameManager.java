package fr.spectatorplus.game;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.api.EnterReason;
import fr.spectatorplus.api.LeaveReason;
import fr.spectatorplus.api.event.SpectatorGameEvent;
import fr.spectatorplus.api.game.GameService;
import fr.spectatorplus.compat.Compat;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Progression de la partie (déclenchée par l'API ou par /spec game ...).
 */
public final class GameManager implements GameService {

    private final SpectatorPlus plugin;
    private AlivePlayersProvider aliveProvider;
    private TeamProvider teamProvider;

    private boolean running;
    private long startTime;
    private boolean pvp;
    private int episode;
    private int initialPlayers;
    private boolean halfFired;
    private boolean lastSurvivorFired;
    private int lastAliveCount = -1;
    private int lastTeamCount = -1;
    private final Set<Integer> minutesFired = new HashSet<>();
    private final Set<UUID> eliminated = new HashSet<>();
    private final Set<UUID> participants = new HashSet<>();

    public GameManager(SpectatorPlus plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                tick();
            }
        }, 20L, 20L);
    }

    private void tick() {
        if (!running) return;
        long minutes = getGameDuration() / 60;
        for (Integer m : plugin.events().settings("game.time").ints("minutes")) {
            if (minutes >= m && minutesFired.add(m)) {
                fire(plugin.events().builder("game.time").data("minutes", m).data("time", m + " min"));
            }
        }
    }

    private void fire(SpectatorGameEvent.Builder b) {
        plugin.events().fire(b.build());
    }

    @Override
    public void startGame() {
        running = true;
        startTime = System.currentTimeMillis();
        episode = 0;
        pvp = false;
        halfFired = false;
        lastSurvivorFired = false;
        minutesFired.clear();
        eliminated.clear();
        plugin.stats().reset();
        plugin.combat().reset();
        plugin.resetGameTracking();
        plugin.events().clearHistory();
        participants.clear();
        for (Player p : getAlivePlayers()) participants.add(p.getUniqueId());
        initialPlayers = participants.size();
        lastAliveCount = initialPlayers;
        lastTeamCount = getRemainingTeams();
        fire(plugin.events().builder("game.start").data("players", initialPlayers));
    }

    @Override
    public void endGame() {
        if (!running) return;
        fire(plugin.events().builder("game.end").data("time", fr.spectatorplus.util.Text.duration(getGameDuration())));
        running = false;
        if (plugin.getConfig().getBoolean("game.release-spectators-on-end", false)) {
            for (Player p : new ArrayList<>(Bukkit.getOnlinePlayers())) {
                if (plugin.spectators().isSpectator(p)) plugin.spectators().leave(p, LeaveReason.GAME_END, true);
            }
        }
    }

    @Override
    public boolean isGameRunning() {
        return running;
    }

    @Override
    public long getGameDuration() {
        return running ? (System.currentTimeMillis() - startTime) / 1000 : 0;
    }

    @Override
    public void startPvp() {
        pvp = true;
        fire(plugin.events().builder("game.pvp_start"));
    }

    @Override
    public boolean isPvpEnabled() {
        return pvp;
    }

    @Override
    public void endGracePeriod() {
        fire(plugin.events().builder("game.grace_end"));
    }

    @Override
    public void startEpisode(int episode) {
        this.episode = episode;
        fire(plugin.events().builder("game.episode_start").data("episode", episode));
    }

    @Override
    public void endEpisode(int episode) {
        fire(plugin.events().builder("game.episode_end").data("episode", episode));
    }

    @Override
    public int getEpisode() {
        return episode;
    }

    @Override
    public void declareWinner(Player player) {
        fire(plugin.events().builder("game.player_win").player(player));
    }

    @Override
    public void declareWinningTeam(String team) {
        fire(plugin.events().builder("game.team_win").data("team", team));
    }

    @Override
    public void eliminate(Player player) {
        if (plugin.spectators().isSpectator(player)) return;
        plugin.spectators().enter(player, EnterReason.ELIMINATION, null, true);
    }

    /** Appelé lorsqu'un joueur est éliminé (mort → spectateur ou API). */
    public void onElimination(Player player) {
        eliminated.add(player.getUniqueId());
        fire(plugin.events().builder("death.eliminated").player(player)
                .data("dead_player", player.getName())
                .data("alive", getAlivePlayers().size()));
        checkAliveCount();
    }

    /** Joueur présent (en vie) au lancement de la partie. */
    public boolean isParticipant(UUID id) {
        return participants.contains(id);
    }

    public boolean wasEliminated(UUID id) {
        return eliminated.contains(id);
    }

    /** Vérifie le nombre de joueurs/équipes restants (appelé après chaque mort, élimination, déconnexion). */
    public void checkAliveCount() {
        Collection<Player> alive = getAlivePlayers();
        int count = alive.size();
        boolean active = running || plugin.getConfig().getBoolean("game.detect-without-game", false);
        if (count != lastAliveCount) {
            if (active && lastAliveCount >= 0 && count < lastAliveCount) {
                for (Integer n : plugin.events().settings("game.players_left").ints("amounts")) {
                    if (count == n) fire(plugin.events().builder("game.players_left").data("players", count).data("amount", count));
                }
                for (Integer n : plugin.events().settings("death.players_left").ints("amounts")) {
                    if (count == n) fire(plugin.events().builder("death.players_left").data("players", count).data("amount", count));
                }
            }
            lastAliveCount = count;
        }
        if (running && !halfFired && initialPlayers >= 2 && count <= initialPlayers / 2) {
            halfFired = true;
            fire(plugin.events().builder("game.half_eliminated").data("players", count).data("initial", initialPlayers));
        }
        if (count == 1 && !lastSurvivorFired && active) {
            lastSurvivorFired = true;
            Player last = alive.iterator().next();
            fire(plugin.events().builder("game.last_survivor").player(last));
        }
        if (count > 1) lastSurvivorFired = false;

        int teams = getRemainingTeams();
        if (teams != lastTeamCount) {
            if (active && lastTeamCount >= 0 && teams < lastTeamCount && teams > 0) {
                for (Integer n : plugin.events().settings("game.teams_left").ints("amounts")) {
                    if (teams == n) fire(plugin.events().builder("game.teams_left").data("teams", teams).data("amount", teams));
                }
            }
            lastTeamCount = teams;
        }
    }

    @Override
    public Collection<Player> getAlivePlayers() {
        if (aliveProvider != null) {
            Collection<Player> c = aliveProvider.getAlivePlayers();
            if (c != null) return c;
        }
        List<Player> res = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.spectators().isSpectator(p) || p.hasMetadata("NPC") || Compat.isFake(p)) continue;
            GameMode gm = p.getGameMode();
            if (gm == GameMode.SURVIVAL || gm == GameMode.ADVENTURE) res.add(p);
        }
        return res;
    }

    @Override
    public void setAlivePlayersProvider(AlivePlayersProvider provider) {
        this.aliveProvider = provider;
    }

    @Override
    public String getTeam(Player player) {
        if (teamProvider != null) return teamProvider.getTeam(player);
        return Compat.scoreboardTeam(player);
    }

    @Override
    public void setTeamProvider(TeamProvider provider) {
        this.teamProvider = provider;
    }

    @Override
    public int getRemainingTeams() {
        Set<String> teams = new HashSet<>();
        for (Player p : getAlivePlayers()) {
            String t = getTeam(p);
            if (t != null) teams.add(t);
        }
        return teams.size();
    }

    public int getInitialPlayers() {
        return initialPlayers;
    }

    public int getEliminatedCount() {
        return eliminated.size();
    }
}
