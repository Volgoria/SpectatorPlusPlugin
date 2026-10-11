package fr.spectatorplus.game;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.api.EnterReason;
import fr.spectatorplus.api.LeaveReason;
import fr.spectatorplus.core.event.GameEvent;
import fr.spectatorplus.core.platform.GameMode;
import fr.spectatorplus.core.platform.Icon;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.util.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Progression de la partie (déclenchée par l'API ou par /spec game ...).
 */
public final class GameManager {

    /** Liste des joueurs en vie fournie par un plugin / mod de jeu (UHC...). */
    public interface AliveProvider {
        /** @return les joueurs en vie, ou null pour utiliser la détection par défaut */
        Collection<PlatformPlayer> getAlivePlayers();
    }

    /** Équipe d'un joueur fournie par un plugin / mod de jeu. */
    public interface TeamProvider {
        /** @return le nom de l'équipe, ou null */
        String getTeam(PlatformPlayer player);
    }

    /** Le plugin de jeu peut cacher la vie des joueurs (scénario où personne ne connaît sa vie...). */
    public interface HealthVisibility {
        boolean isHealthVisible();

        /** Le plugin de jeu affiche déjà la vie sous les pseudos à tout le monde : Spectator Plus n'ajoute pas la sienne. */
        default boolean isBelowNameProvided() {
            return false;
        }
    }

    /**
     * Plugin de jeu qui gère lui-même la partie (UHCCore) : vies, éliminations et noms publics passent par lui.
     */
    public interface ExternalGame {
        /** @return vrai si le joueur fait partie de la partie et n'est pas éliminé (lobby compris) */
        boolean isPlaying(UUID player);

        /** Élimine le joueur par le plugin de jeu. @return faux si le plugin de jeu refuse */
        boolean eliminate(UUID player);

        /** Rend un joueur encore en jeu (session ouverte par erreur) au plugin de jeu : sortie du mode spectateur. */
        void releasePlaying(PlatformPlayer player);

        /** Nom montré aux autres (« Anonyme » avec le scénario Anonymous...). */
        String getPublicName(PlatformPlayer player);
    }

    private final SpectatorCore plugin;
    private ExternalGame externalGame;
    private AliveProvider aliveProvider;
    private TeamProvider teamProvider;
    private HealthVisibility healthVisibility;
    /** Un plugin de jeu gère lui-même les morts, reconnexions et retours en jeu (voir {@link #setManagedExternally}). */
    private boolean managedExternally;

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

    public GameManager(SpectatorCore plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.platform().runTimer(new Runnable() {
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

    private void fire(GameEvent.Builder b) {
        plugin.events().fire(b.build());
    }

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
        plugin.spectators().resetCombats();
        plugin.resetGameTracking();
        plugin.events().clearHistory();
        participants.clear();
        for (PlatformPlayer p : getAlivePlayers()) participants.add(p.getUniqueId());
        initialPlayers = participants.size();
        lastAliveCount = initialPlayers;
        lastTeamCount = getRemainingTeams();
        fire(plugin.events().builder("game.start").data("players", initialPlayers));
    }

    public void endGame() {
        if (!running) return;
        fire(plugin.events().builder("game.end").data("time", Text.duration(getGameDuration())));
        running = false;
        if (plugin.config().getBoolean("game.release-spectators-on-end", false)) {
            for (PlatformPlayer p : new ArrayList<>(plugin.platform().getOnlinePlayers())) {
                if (plugin.spectators().isSpectator(p)) plugin.spectators().leave(p, LeaveReason.GAME_END, true);
            }
        }
    }

    public boolean isGameRunning() {
        return running;
    }

    public long getGameDuration() {
        return running ? (System.currentTimeMillis() - startTime) / 1000 : 0;
    }

    public void startPvp() {
        pvp = true;
        fire(plugin.events().builder("game.pvp_start"));
    }

    public boolean isPvpEnabled() {
        return pvp;
    }

    public void endGracePeriod() {
        fire(plugin.events().builder("game.grace_end"));
    }

    public void startEpisode(int episode) {
        this.episode = episode;
        fire(plugin.events().builder("game.episode_start").data("episode", episode));
    }

    public void endEpisode(int episode) {
        fire(plugin.events().builder("game.episode_end").data("episode", episode));
    }

    public int getEpisode() {
        return episode;
    }

    public void declareWinner(PlatformPlayer player) {
        fire(plugin.events().builder("game.player_win").player(player));
    }

    public void declareWinningTeam(String team) {
        fire(plugin.events().builder("game.team_win").data("team", team));
    }

    /**
     * Élimine un joueur. Quand un plugin de jeu gère la partie, c'est lui qui élimine (et fait entrer le joueur en
     * spectateur) : une session ouverte directement laisserait le joueur « en vie » pour lui.
     *
     * @return faux si le plugin de jeu refuse ou si le joueur est déjà spectateur
     */
    public boolean eliminate(PlatformPlayer player) {
        if (externalGame != null) return externalGame.eliminate(player.getUniqueId());
        if (plugin.spectators().isSpectator(player)) return false;
        return plugin.spectators().enter(player, EnterReason.ELIMINATION, null, true);
    }

    /** Appelé lorsqu'un joueur est éliminé (mort → spectateur ou API). */
    public void onElimination(PlatformPlayer player) {
        onElimination(player.getUniqueId(), publicName(player));
    }

    /** Élimination d'un joueur éventuellement hors ligne : annoncée une seule fois. */
    public void onElimination(UUID id, String name) {
        if (!eliminated.add(id)) return;
        fire(plugin.events().builder("death.eliminated").player(id, name)
                .data("dead_player", name)
                .data("alive", getAlivePlayers().size()));
        checkAliveCount();
    }

    /** Joueur remis en jeu (revive) : il n'est plus compté parmi les éliminés. */
    public void onRevive(UUID id) {
        if (!eliminated.remove(id)) return;
        if (running) participants.add(id);
        checkAliveCount();
    }

    /** Joueur présent (en vie) au lancement de la partie. */
    public boolean isParticipant(UUID id) {
        return participants.contains(id);
    }

    public boolean wasEliminated(UUID id) {
        return eliminated.contains(id);
    }

    /** Le joueur est-il en vie dans une partie en cours (UHCCore ou partie interne) ? */
    public boolean isAliveInGame(UUID id) {
        if (managedExternally) return isPlayingExternally(id);
        return running && participants.contains(id) && !eliminated.contains(id);
    }

    /** Vérifie le nombre de joueurs/équipes restants (appelé après chaque mort, élimination, déconnexion). */
    public void checkAliveCount() {
        Collection<PlatformPlayer> alive = getAlivePlayers();
        int count = alive.size();
        boolean active = running || plugin.config().getBoolean("game.detect-without-game", false);
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
            PlatformPlayer last = alive.iterator().next();
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

    public Collection<PlatformPlayer> getAlivePlayers() {
        if (aliveProvider != null) {
            Collection<PlatformPlayer> c = aliveProvider.getAlivePlayers();
            if (c != null) return c;
        }
        List<PlatformPlayer> res = new ArrayList<>();
        for (PlatformPlayer p : plugin.platform().getOnlinePlayers()) {
            if (plugin.spectators().isSpectator(p) || p.isFake()) continue;
            GameMode gm = p.getGameMode();
            if (gm == GameMode.SURVIVAL || gm == GameMode.ADVENTURE) res.add(p);
        }
        return res;
    }

    public void setAlivePlayersProvider(AliveProvider provider) {
        this.aliveProvider = provider;
    }

    public String getTeam(PlatformPlayer player) {
        if (teamProvider != null) return teamProvider.getTeam(player);
        return player.getScoreboardTeam();
    }

    public void setTeamProvider(TeamProvider provider) {
        this.teamProvider = provider;
    }

    public void setHealthVisibility(HealthVisibility visibility) {
        this.healthVisibility = visibility;
    }

    public boolean isBelowNameProvided() {
        return healthVisibility != null && healthVisibility.isBelowNameProvided();
    }

    public boolean isHealthVisible() {
        return healthVisibility == null || healthVisibility.isHealthVisible();
    }

    /** Vie d'un joueur en cœurs pour l'affichage, « ? » si le plugin de jeu la cache. */
    public String hearts(double health) {
        return isHealthVisible() ? Text.hearts(health) : "?";
    }

    /**
     * Un plugin de jeu (UHCCore...) décide seul qui est spectateur : Spectator Plus ne fait plus entrer les joueurs
     * à leur mort ou à leur connexion, et ne garde pas les spectateurs d'une connexion ou d'un redémarrage à l'autre.
     */
    public void setManagedExternally(boolean managed) {
        this.managedExternally = managed;
    }

    public void setExternalGame(ExternalGame game) {
        this.externalGame = game;
    }

    public ExternalGame getExternalGame() {
        return externalGame;
    }

    /** Le joueur est encore en jeu pour le plugin de jeu (faux sans plugin de jeu). */
    public boolean isPlayingExternally(UUID id) {
        return managedExternally && externalGame != null && externalGame.isPlaying(id);
    }

    /** Tête du joueur dans les menus : neutre quand le plugin de jeu cache son identité (skin compris). */
    public Icon publicSkull(PlatformPlayer player) {
        return publicName(player).equals(player.getName()) ? Icon.skull(player) : Icon.head();
    }

    /** Nom montré aux spectateurs : celui du plugin de jeu (anonymat), sinon le pseudo. */
    public String publicName(PlatformPlayer player) {
        if (player == null) return "?";
        if (externalGame != null) {
            try {
                String name = externalGame.getPublicName(player);
                if (name != null && !name.isEmpty()) return name;
            } catch (RuntimeException ignored) {
                // plugin de jeu en cours de rechargement : pseudo
            }
        }
        return player.getName();
    }

    public boolean isManagedExternally() {
        return managedExternally;
    }

    public int getRemainingTeams() {
        Set<String> teams = new HashSet<>();
        for (PlatformPlayer p : getAlivePlayers()) {
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
