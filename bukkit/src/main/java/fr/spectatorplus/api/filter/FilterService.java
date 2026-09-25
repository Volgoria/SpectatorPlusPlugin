package fr.spectatorplus.api.filter;

import fr.spectatorplus.api.event.EventCategory;
import fr.spectatorplus.api.event.Importance;
import fr.spectatorplus.api.event.SpectatorGameEvent;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Filtres d'évènements des spectateurs.
 * <p>
 * Clés de filtres utilisées pour les verrous et permissions ({@code spectatorplus.filter.<clé>}) :
 * les identifiants de catégories (world, mining, player, pvp, death, craft, enchant, pve, potion, game, custom)
 * ainsi que {@code player} (filtres joueurs), {@code world} (filtres mondes), {@code distance},
 * {@code damage}, {@code importance} et {@code preset}.
 * Toute modification déclenche un {@link fr.spectatorplus.api.events.SpectatorFilterChangeEvent}.
 */
public interface FilterService {

    boolean isCategoryEnabled(Player player, String categoryId);

    void setCategoryEnabled(Player player, String categoryId, boolean enabled);

    boolean isEventEnabled(Player player, String eventTypeId);

    void setEventEnabled(Player player, String eventTypeId, boolean enabled);

    String getPreset(Player player);

    /** @return false si le preset n'existe pas */
    boolean setPreset(Player player, String preset);

    Collection<String> getPresets();

    Importance getMinimumImportance(Player player);

    void setMinimumImportance(Player player, Importance importance);

    PlayerFilterMode getPlayerFilterMode(Player player);

    void setPlayerFilterMode(Player player, PlayerFilterMode mode);

    /** Empêche le joueur de modifier un filtre (verrou non persistant). */
    void lockFilter(Player player, String filterKey);

    void unlockFilter(Player player, String filterKey);

    boolean isLocked(Player player, String filterKey);

    /** Change l'importance d'un type d'évènement pour tout le serveur. */
    void setEventImportance(String eventTypeId, Importance importance);

    /** Ajoute une condition de filtrage personnalisée. */
    void registerCondition(String id, FilterCondition condition);

    void unregisterCondition(String id);

    /** Ajoute une catégorie de filtres (identique à EventService#registerCategory). */
    void registerCategory(EventCategory category);

    /** true si l'évènement passe tous les filtres du joueur. */
    boolean canSee(Player viewer, SpectatorGameEvent event);

    /** Réinitialise les filtres d'un joueur au preset par défaut. */
    void resetFilters(Player player);
}
