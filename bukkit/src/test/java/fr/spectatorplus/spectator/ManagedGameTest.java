package fr.spectatorplus.spectator;

import fr.spectatorplus.api.EnterReason;
import fr.spectatorplus.api.LeaveReason;
import fr.spectatorplus.core.event.GameEvent;
import fr.spectatorplus.core.platform.GameMode;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.game.GameManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Partie gérée par un plugin de jeu (UHCCore) : un joueur en vie n'a jamais de session, un éliminé ne sort pas
 * seul du mode spectateur, les changements de mode externes sont suivis, les noms publics sont respectés.
 */
class ManagedGameTest {

    private FakeServer server;
    private SpectatorManager spectators;
    private GameManager game;
    /** Joueurs en vie pour le « plugin de jeu ». */
    private final Set<UUID> alive = new HashSet<>();
    private final Set<UUID> eliminatedByGame = new HashSet<>();
    private boolean anonymous;

    @BeforeEach
    void setUp() throws Exception {
        server = new FakeServer();
        spectators = server.core.spectators();
        game = server.core.game();
        game.setManagedExternally(true);
        game.setExternalGame(new GameManager.ExternalGame() {
            @Override
            public boolean isPlaying(UUID player) {
                return alive.contains(player);
            }

            @Override
            public boolean eliminate(UUID player) {
                if (!alive.remove(player)) return false;
                eliminatedByGame.add(player);
                return true;
            }

            @Override
            public void releasePlaying(PlatformPlayer player) {
                spectators.release(player, LeaveReason.API);
                player.setGameMode(GameMode.SURVIVAL);
            }

            @Override
            public String getPublicName(PlatformPlayer player) {
                return anonymous ? "Anonyme" : player.getName();
            }
        });
    }

    private PlatformPlayer alivePlayer(String name) {
        PlatformPlayer p = server.join(name);
        alive.add(p.getUniqueId());
        return p;
    }

    private PlatformPlayer eliminatedSpectator(String name) {
        PlatformPlayer p = server.join(name);
        p.setGameMode(GameMode.SPECTATOR);
        assertTrue(spectators.enter(p, EnterReason.ELIMINATION, null, true));
        return p;
    }

    // ------------------------------------------------------------------ SP-01

    @Test
    void joinRefuseAUnJoueurEnVie() {
        PlatformPlayer p = alivePlayer("Vivant");
        server.core.command().execute(p, "spec", new String[]{"join"});
        assertFalse(spectators.isSpectator(p));
    }

    @Test
    void eliminatePasseParLePluginDeJeu() {
        PlatformPlayer admin = alivePlayer("Host");
        PlatformPlayer p = alivePlayer("Cible");
        server.core.command().execute(admin, "spec", new String[]{"eliminate", "Cible"});
        assertTrue(eliminatedByGame.contains(p.getUniqueId()));
        // la session est ouverte par le plugin de jeu (UHCSpectatorEvent), pas directement
        assertFalse(spectators.isSpectator(p));
    }

    @Test
    void gameRefuseQuandLaPartieEstGeree() {
        PlatformPlayer admin = alivePlayer("Host");
        server.core.command().execute(admin, "spec", new String[]{"game", "start"});
        assertFalse(game.isGameRunning());
    }

    // ------------------------------------------------------------------ SP-02

    @Test
    void leaveRefuseAUnElimine() {
        PlatformPlayer p = eliminatedSpectator("Mort");
        assertEquals(SpectatorManager.LeaveRequest.ELIMINATED, spectators.requestLeave(p, true));
        assertTrue(spectators.isSpectator(p));
        server.core.command().execute(p, "spec", new String[]{"leave"});
        assertTrue(spectators.isSpectator(p));
    }

    @Test
    void leaveRendUnVivantAuPluginDeJeu() {
        PlatformPlayer p = alivePlayer("Vivant");
        // session ouverte avant le correctif (/spec join au lobby)
        assertTrue(spectators.enter(p, EnterReason.COMMAND, null, true));
        assertEquals(SpectatorManager.LeaveRequest.LEFT, spectators.requestLeave(p, false));
        assertFalse(spectators.isSpectator(p));
        assertEquals(GameMode.SURVIVAL, p.getGameMode());
    }

    // ------------------------------------------------------------------ SP-03

    @Test
    void passageEnSurvieExterneLibereLaSession() {
        PlatformPlayer p = eliminatedSpectator("Mort");
        assertEquals(GameMode.ADVENTURE, p.getGameMode());
        p.setGameMode(GameMode.SURVIVAL); // /gamemode survival
        assertFalse(spectators.isSpectator(p));
    }

    @Test
    void passageEnSpectateurExterneActiveLePasseMuraille() {
        PlatformPlayer p = eliminatedSpectator("Mort");
        SpectatorSession s = spectators.getSpectator(p);
        assertFalse(s.noclip);
        p.setGameMode(GameMode.SPECTATOR); // /uhc spectate, /gamemode spectator
        assertTrue(spectators.isSpectator(p));
        assertTrue(s.noclip, "le passe-muraille doit ramener le joueur en aventure à l'air libre");
    }

    @Test
    void changementsDeModeDeSpectatorPlusIgnores() {
        PlatformPlayer p = eliminatedSpectator("Mort");
        // apply() repasse en aventure : changement interne, la session reste
        p.setGameMode(GameMode.SPECTATOR);
        spectators.apply(p);
        assertTrue(spectators.isSpectator(p));
        assertEquals(GameMode.ADVENTURE, p.getGameMode());
        assertFalse(spectators.isChangingGameMode(p.getUniqueId()));
    }

    // ------------------------------------------------------------------ SP-07

    @Test
    void releaseGardeLesEffetsDuPluginDeJeu() {
        PlatformPlayer p = eliminatedSpectator("Mort");
        assertTrue(FakeServer.hasEffect(p, "NIGHT_VISION"));
        // revive : UHCCore remet le glowing de phase avant de rendre la main
        p.addEffect("GLOWING", 20 * 60, 0, false, false);
        spectators.release(p, LeaveReason.API);
        assertTrue(FakeServer.hasEffect(p, "GLOWING"));
        assertFalse(FakeServer.hasEffect(p, "NIGHT_VISION"));
    }

    // ------------------------------------------------------------------ SP-08

    @Test
    void nomsPublicsAvecAnonymous() {
        PlatformPlayer p = alivePlayer("VraiPseudo");
        assertEquals("VraiPseudo", game.publicName(p));
        anonymous = true;
        assertEquals("Anonyme", game.publicName(p));
        GameEvent e = server.core.events().builder("death.eliminated").player(p).build();
        assertEquals("Anonyme", e.get("player"));
        assertEquals("Anonyme", e.getPlayerName(p.getUniqueId()));
    }

    // ------------------------------------------------------------------ SP-09 et compteurs

    @Test
    void eliminationHorsLigneAnnonceeUneSeuleFois() {
        UUID id = UUID.randomUUID();
        game.onElimination(id, "Absent");
        game.onElimination(id, "Absent"); // reconnexion : pas de nouvelle annonce
        assertEquals(1, game.getEliminatedCount());
        assertTrue(game.wasEliminated(id));
        int announces = 0;
        List<GameEvent> history = server.core.events().getHistory();
        for (GameEvent e : history) {
            if (e.getTypeId().equals("death.eliminated") && "Absent".equals(e.get("dead_player"))) announces++;
        }
        assertEquals(1, announces);
        GameEvent last = history.get(history.size() - 1);
        assertNotNull(last.getPlayers());
    }

    @Test
    void reviveRetireLeJoueurDesElimines() {
        PlatformPlayer p = eliminatedSpectator("Mort");
        assertTrue(game.wasEliminated(p.getUniqueId()));
        assertEquals(1, game.getEliminatedCount());
        spectators.release(p, LeaveReason.API);
        game.onRevive(p.getUniqueId());
        assertFalse(game.wasEliminated(p.getUniqueId()));
        assertEquals(0, game.getEliminatedCount());
        // nouvelle mort après le revive : de nouveau annoncée
        p.setGameMode(GameMode.SPECTATOR);
        assertTrue(spectators.enter(p, EnterReason.ELIMINATION, null, true));
        assertEquals(1, game.getEliminatedCount());
    }
}
