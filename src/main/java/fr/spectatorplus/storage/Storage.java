package fr.spectatorplus.storage;

import java.util.UUID;

/**
 * Stockage des préférences joueurs. Les appels sont faits hors du thread principal.
 */
public interface Storage {

    void init() throws Exception;

    /** @return les données sérialisées, ou null si le joueur n'a pas encore de préférences */
    String load(UUID player) throws Exception;

    void save(UUID player, String data) throws Exception;

    void delete(UUID player) throws Exception;

    void close();

    String name();
}
