package fr.spectatorplus.api.filter;

import fr.spectatorplus.api.event.SpectatorGameEvent;
import org.bukkit.entity.Player;

/**
 * Condition de filtrage personnalisée, ajoutée par un plugin externe.
 * Toutes les conditions enregistrées doivent renvoyer true pour qu'un évènement soit affiché.
 */
public interface FilterCondition {

    boolean test(Player viewer, SpectatorGameEvent event);
}
