package fr.spectatorplus.api.event;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Gestion des évènements de partie : enregistrement de types, déclenchement, historique.
 * Pour écouter les évènements générés, utilisez l'évènement Bukkit
 * {@link fr.spectatorplus.api.events.SpectatorGameEventTriggerEvent}.
 */
public interface EventService {

    /** Enregistre (ou remplace) une catégorie. */
    EventCategory registerCategory(EventCategory category);

    EventCategory getCategory(String id);

    Collection<EventCategory> getCategories();

    /** Enregistre un nouveau type d'évènement, sans modifier Spectator Plus. */
    SpectatorEventType registerType(SpectatorEventType type);

    void unregisterType(String id);

    SpectatorEventType getType(String id);

    Collection<SpectatorEventType> getTypes();

    List<SpectatorEventType> getTypes(String categoryId);

    /**
     * Crée un builder pour un type enregistré.
     *
     * @throws IllegalArgumentException si le type n'existe pas
     */
    SpectatorGameEvent.Builder builder(String typeId);

    /**
     * Déclenche l'évènement : filtres, messages, notifications, historique.
     * Peut être appelé depuis n'importe quel thread.
     */
    void fire(SpectatorGameEvent event);

    /** Raccourci : déclenche un évènement pour un joueur avec des données personnalisées. */
    void fire(String typeId, Player player, Map<String, ?> data);

    /** Historique des évènements de la partie (du plus ancien au plus récent). */
    List<SpectatorGameEvent> getHistory();

    void clearHistory();

    /** true si l'évènement est activé globalement dans la configuration. */
    boolean isEnabled(String typeId);
}
