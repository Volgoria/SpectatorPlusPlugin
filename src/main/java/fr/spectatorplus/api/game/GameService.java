package fr.spectatorplus.api.game;

import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Progression de la partie. Spectator Plus ne peut pas deviner le début d'une partie,
 * le début du PvP, les équipes ou le vainqueur : le plugin de jeu les communique ici.
 */
public interface GameService {

    void startGame();

    void endGame();

    boolean isGameRunning();

    /** Durée de la partie en secondes (0 si aucune partie). */
    long getGameDuration();

    void startPvp();

    boolean isPvpEnabled();

    void endGracePeriod();

    void startEpisode(int episode);

    void endEpisode(int episode);

    int getEpisode();

    void declareWinner(Player player);

    void declareWinningTeam(String team);

    /** Élimine un joueur : évènement « éliminé » + passage en spectateur. */
    void eliminate(Player player);

    Collection<Player> getAlivePlayers();

    /** Remplace la détection par défaut des joueurs en vie. */
    void setAlivePlayersProvider(AlivePlayersProvider provider);

    /** Équipe d'un joueur (null si aucune). */
    String getTeam(Player player);

    /** Remplace la détection par défaut des équipes (scoreboard). */
    void setTeamProvider(TeamProvider provider);

    int getRemainingTeams();

    interface AlivePlayersProvider {
        Collection<Player> getAlivePlayers();
    }

    interface TeamProvider {
        String getTeam(Player player);
    }
}
