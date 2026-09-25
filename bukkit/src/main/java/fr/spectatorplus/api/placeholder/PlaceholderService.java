package fr.spectatorplus.api.placeholder;

import fr.spectatorplus.api.event.SpectatorGameEvent;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * Placeholders utilisables sous la forme {clé} dans les messages, évènements, HUD et menus.
 */
public interface PlaceholderService {

    /** Enregistre un placeholder externe ({@code key} sans accolades). */
    void register(String key, PlaceholderResolver resolver);

    void unregister(String key);

    boolean isRegistered(String key);

    Set<String> getRegisteredKeys();

    /** Remplace tous les placeholders du texte. */
    String apply(String text, Player viewer, SpectatorGameEvent event);
}
