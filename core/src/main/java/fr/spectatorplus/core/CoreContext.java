package fr.spectatorplus.core;

import fr.spectatorplus.config.ConfigFiles;
import fr.spectatorplus.config.Lang;
import fr.spectatorplus.core.config.YamlConfig;
import fr.spectatorplus.core.platform.Host;
import fr.spectatorplus.event.CombatTracker;
import fr.spectatorplus.event.EventRegistry;
import fr.spectatorplus.event.StatsManager;
import fr.spectatorplus.event.ZoneManager;
import fr.spectatorplus.filter.FilterEngine;
import fr.spectatorplus.storage.Storage;

/**
 * Accès aux services communs. Implémenté par le point d'entrée de chaque plateforme
 * (classe du plugin Bukkit, du mod Fabric...), qui peut renvoyer des sous-classes spécialisées.
 */
public interface CoreContext {

    Host host();

    ConfigFiles files();

    /** config.yml */
    YamlConfig config();

    Lang messages();

    Storage storage();

    ZoneManager zones();

    StatsManager stats();

    CombatTracker combat();

    EventRegistry events();

    FilterEngine filters();
}
