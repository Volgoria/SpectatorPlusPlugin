package fr.spectatorplus.core.platform;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Services fournis par la plateforme (plugin Bukkit, mod Fabric, NeoForge...) au code commun.
 */
public interface Host {

    Logger logger();

    /** Dossier de configuration (plugins/SpectatorPlus, config/spectatorplus...). */
    File dataFolder();

    /** Ressource embarquée dans le jar, ou null. */
    InputStream resource(String path);

    /** Exécute la tâche sur le thread principal du serveur (au prochain tick si on n'y est pas). */
    void runSync(Runnable task);

    /** Exécute la tâche hors du thread principal. */
    void runAsync(Runnable task);

    boolean isPrimaryThread();
}
