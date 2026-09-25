package fr.spectatorplus.api;

import fr.spectatorplus.api.event.EventService;
import fr.spectatorplus.api.filter.FilterService;
import fr.spectatorplus.api.game.GameService;
import fr.spectatorplus.api.placeholder.PlaceholderService;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

/**
 * Point d'entrée de l'API Spectator Plus.
 * <pre>
 * SpectatorPlusAPI api = SpectatorPlusProvider.get();
 * api.enterSpectator(player, EnterReason.ELIMINATION);
 * </pre>
 */
public interface SpectatorPlusAPI {

    boolean isSpectator(Player player);

    boolean isSpectator(UUID id);

    Collection<? extends Spectator> getSpectators();

    /** @return le spectateur, ou null si le joueur n'est pas spectateur */
    Spectator getSpectator(Player player);

    Spectator getSpectator(UUID id);

    /**
     * Fait entrer un joueur dans le mode spectateur.
     *
     * @return false si refusé (configuration, évènement annulé, déjà spectateur)
     */
    boolean enterSpectator(Player player, EnterReason reason);

    /** Comme {@link #enterSpectator(Player, EnterReason)} mais téléporte le joueur à la position donnée. */
    boolean enterSpectator(Player player, EnterReason reason, Location location);

    boolean leaveSpectator(Player player, LeaveReason reason);

    SpectatorMode getMode();

    void setMode(SpectatorMode mode);

    EventService getEventService();

    PlaceholderService getPlaceholderService();

    FilterService getFilterService();

    GameService getGameService();
}
