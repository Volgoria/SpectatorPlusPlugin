package fr.spectatorplus.hook;

import fr.dalekmc.uhccore.api.UHCAPI;
import fr.dalekmc.uhccore.api.UHCPhase;
import fr.dalekmc.uhccore.api.event.UHCGameEndEvent;
import fr.dalekmc.uhccore.api.event.UHCGameStartedEvent;
import fr.dalekmc.uhccore.api.event.UHCPhaseChangeEvent;
import fr.dalekmc.uhccore.api.event.UHCPlayerEliminatedEvent;
import fr.dalekmc.uhccore.api.event.UHCScenarioStartEvent;
import fr.dalekmc.uhccore.api.event.UHCScenarioStopEvent;
import fr.dalekmc.uhccore.api.event.UHCSettingChangeEvent;
import fr.dalekmc.uhccore.api.event.UHCSpectatorEvent;
import fr.dalekmc.uhccore.api.event.UHCWinnerEvent;
import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.api.EnterReason;
import fr.spectatorplus.api.LeaveReason;
import fr.spectatorplus.api.SpectatorState;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.game.GameManager;
import fr.spectatorplus.spectator.SpectatorSession;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Intégration UHCCore : UHCCore décide qui est éliminé, Spectator Plus s'occupe des spectateurs.
 * <ul>
 *     <li>joueurs en vie, équipes et progression de la partie viennent de l'API UHCCore : seuls les joueurs
 *     en vie sont proposés aux spectateurs (téléportation, suivi, POV) ;</li>
 *     <li>un joueur que UHCCore passe spectateur (mort, élimination, connexion en cours de partie) entre dans
 *     Spectator Plus, et lui est rendu quand UHCCore le remet en jeu (revive, arrivée tardive) ou à la fin ;</li>
 *     <li>la vie des joueurs est cachée aux spectateurs quand le scénario SelfDiagnosis est actif, et laissée à
 *     TAB quand UHCCore l'affiche déjà sous les pseudos (health.below-name) ;</li>
 *     <li>le chat des éliminés est séparé et mis en forme par UHCCore (chat.spectators-separate).</li>
 * </ul>
 * Chargée seulement si UHCCore est présent (hooks.uhccore dans config.yml) : c'est la seule classe qui
 * référence l'API UHCCore.
 */
public final class UHCCoreHook implements Listener {

    private static final String SELF_DIAGNOSIS = "SelfDiagnosis";
    private static final String HEALTH_BELOW_NAME = "health.below-name";

    private final SpectatorPlus plugin;
    private final UHCAPI api;
    /** Éliminés par UHCCore dont le passage en spectateur n'a pas encore été traité. */
    private final Set<UUID> eliminated = new HashSet<>();
    /** Morts à faire entrer après leur réapparition, avec la raison. */
    private final Map<UUID, EnterReason> pendingRespawn = new HashMap<>();

    private UHCCoreHook(SpectatorPlus plugin, UHCAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    /** @return le hook actif, ou null si l'API UHCCore n'est pas disponible */
    public static UHCCoreHook enable(SpectatorPlus plugin) {
        UHCAPI api = Bukkit.getServicesManager().load(UHCAPI.class);
        if (api == null) return null;
        UHCCoreHook hook = new UHCCoreHook(plugin, api);
        hook.register();
        return hook;
    }

    private void register() {
        GameManager game = plugin.game();
        game.setManagedExternally(true);
        game.setAlivePlayersProvider(new GameManager.AliveProvider() {
            @Override
            public Collection<PlatformPlayer> getAlivePlayers() {
                // hors partie : détection par défaut (joueurs en survie / aventure)
                if (!api.isStarted()) return null;
                List<PlatformPlayer> res = new ArrayList<>();
                for (UUID id : api.getGame().getAlivePlayers()) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) res.add(plugin.wrap(p));
                }
                return res;
            }
        });
        game.setTeamProvider(new GameManager.TeamProvider() {
            @Override
            public String getTeam(PlatformPlayer player) {
                if (!api.getTeams().isTeamMode()) return null;
                return teamName(player.getUniqueId());
            }
        });
        game.setHealthVisibility(new GameManager.HealthVisibility() {
            @Override
            public boolean isHealthVisible() {
                return !api.getScenarios().isActive(SELF_DIAGNOSIS);
            }

            @Override
            public boolean isBelowNameProvided() {
                // UHCCore l'affiche à tout le monde par TAB (health.below-name) : pas de second objectif
                return api.getSettings().getBoolean(HEALTH_BELOW_NAME) && Bukkit.getPluginManager().isPluginEnabled("TAB");
            }
        });
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Spectator Plus (re)chargé pendant une partie : on reprend là où en est UHCCore
        if (api.isStarted()) {
            game.startGame();
            if (api.getGame().isPvpEnabled()) game.startPvp();
            enterVanillaSpectators();
        }
    }

    /**
     * Joueurs éliminés que UHCCore a passés en spectateur vanilla sans prévenir (reprise d'une partie après un
     * crash, Spectator Plus chargé en cours de partie).
     */
    private void enterVanillaSpectators() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!api.getGame().isAlive(p.getUniqueId()) && p.getGameMode() == GameMode.SPECTATOR) {
                enter(p, EnterReason.RECONNECT);
            }
        }
    }

    /**
     * Nom de l'équipe UHCCore. UHCTeam est un record (Java 16+) : ce plugin, compilé pour Java 8, ne peut pas
     * référencer son type, on passe donc par la réflexion.
     */
    private String teamName(UUID id) {
        Object team = api.getTeams().getTeamOf(id).orElse(null);
        if (team == null) return null;
        try {
            return String.valueOf(team.getClass().getMethod("name").invoke(team));
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ progression de la partie

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStarted(UHCGameStartedEvent e) {
        eliminated.clear();
        plugin.game().startGame();
        plugin.platform().runLater(new Runnable() {
            @Override
            public void run() {
                if (api.isStarted()) enterVanillaSpectators();
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPhase(UHCPhaseChangeEvent e) {
        if (e.getPhase() == UHCPhase.PVP && e.isActive()) plugin.game().startPvp();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWinner(UHCWinnerEvent e) {
        if (e.getTeam() != null) {
            plugin.game().declareWinningTeam(e.getTeam());
            return;
        }
        for (UUID id : e.getWinners()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) plugin.game().declareWinner(plugin.wrap(p));
        }
    }

    /** Fin de partie : UHCCore passe ensuite tout le monde en spectateur vanilla puis ramène au lobby. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEnd(UHCGameEndEvent e) {
        plugin.game().endGame();
        for (SpectatorSession s : plugin.spectators().getSpectators()) {
            PlatformPlayer p = s.getPlayer();
            if (p != null) plugin.spectators().release(p, LeaveReason.GAME_END);
        }
        eliminated.clear();
        pendingRespawn.clear();
    }

    // ------------------------------------------------------------------ spectateurs

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEliminated(UHCPlayerEliminatedEvent e) {
        eliminated.add(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSpectator(UHCSpectatorEvent e) {
        final Player p = Bukkit.getPlayer(e.getPlayer());
        if (p == null) return;
        if (!e.isSpectating()) {
            // revive ou arrivée tardive : UHCCore a déjà remis le joueur en jeu (mode, position, kit)
            pendingRespawn.remove(p.getUniqueId());
            plugin.spectators().release(plugin.wrap(p), LeaveReason.API);
            return;
        }
        final EnterReason reason = eliminated.remove(p.getUniqueId()) ? EnterReason.ELIMINATION
                : plugin.game().wasEliminated(p.getUniqueId()) ? EnterReason.RECONNECT : EnterReason.JOIN_DURING_GAME;
        if (p.isDead()) {
            // appelé pendant PlayerDeathEvent : on attend la réapparition
            pendingRespawn.put(p.getUniqueId(), reason);
            return;
        }
        // un tick plus tard : UHCCore termine d'abord son traitement (téléportation...)
        plugin.platform().runLater(new Runnable() {
            @Override
            public void run() {
                enter(p, reason);
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        final Player p = e.getPlayer();
        final EnterReason reason = pendingRespawn.remove(p.getUniqueId());
        if (reason == null) return;
        plugin.platform().runLater(new Runnable() {
            @Override
            public void run() {
                enter(p, reason);
            }
        }, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        pendingRespawn.remove(e.getPlayer().getUniqueId());
    }

    private void enter(Player p, EnterReason reason) {
        if (!p.isOnline() || !api.isStarted() || api.getGame().isAlive(p.getUniqueId())) return;
        PlatformPlayer pp = plugin.wrap(p);
        SpectatorSession s = plugin.spectators().getSpectator(pp);
        if (s != null) {
            // déjà spectateur : UHCCore vient de le repasser en spectateur vanilla, on réapplique le mode Spectator Plus
            if (s.getMovementState() != SpectatorState.POV) {
                plugin.spectators().apply(pp);
                plugin.hotbar().give(pp, s);
            }
            return;
        }
        plugin.spectators().enter(pp, reason, null, true);
    }

    // ------------------------------------------------------------------ vie cachée (SelfDiagnosis)

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSetting(UHCSettingChangeEvent e) {
        if (HEALTH_BELOW_NAME.equals(e.getPath())) plugin.spectators().refreshHealthDisplays();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onScenarioStart(UHCScenarioStartEvent e) {
        if (SELF_DIAGNOSIS.equalsIgnoreCase(e.getScenario())) plugin.spectators().refreshHealthDisplays();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onScenarioStop(UHCScenarioStopEvent e) {
        if (SELF_DIAGNOSIS.equalsIgnoreCase(e.getScenario())) plugin.spectators().refreshHealthDisplays();
    }
}
