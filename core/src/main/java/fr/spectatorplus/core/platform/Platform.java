package fr.spectatorplus.core.platform;

import fr.spectatorplus.api.EnterReason;
import fr.spectatorplus.gui.Menu;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Serveur vu par le code commun : joueurs, mondes, tâches, menus, sauvegarde d'état.
 * Implémenté par le plugin Bukkit et par les mods.
 */
public interface Platform extends Host {

    /** Joueurs connectés (y compris les joueurs fictifs : voir {@link PlatformPlayer#isFake()}). */
    Collection<PlatformPlayer> getOnlinePlayers();

    PlatformPlayer getPlayer(UUID id);

    /** Nom exact (insensible à la casse), sinon début de nom ; null si introuvable. */
    PlatformPlayer findPlayer(String name);

    List<PlatformWorld> getWorlds();

    PlatformWorld getWorld(String name);

    Sender console();

    // ------------------------------------------------------------------ tâches

    /** Tâche répétée sur le thread principal (délais en ticks). */
    Task runTimer(Runnable task, long delay, long period);

    Task runLater(Runnable task, long delay);

    /** TPS fournis par le serveur (Paper...), ou null. */
    double[] serverTps();

    /** Types de dégâts connus, avec les noms de Bukkit (ENTITY_ATTACK, FALL...), pour le filtre de dégâts. */
    List<String> getDamageTypes();

    // ------------------------------------------------------------------ menus

    /** Ouvre (ou remplace) le menu affiché au joueur avec le contenu actuel de {@code menu}. */
    void openMenu(PlatformPlayer viewer, Menu menu);

    /** Met à jour le contenu du menu déjà ouvert. */
    void refreshMenu(PlatformPlayer viewer, Menu menu);

    // ------------------------------------------------------------------ état des spectateurs

    PlayerSnapshot capture(PlatformPlayer player);

    void saveSnapshot(File file, EnterReason reason, PlayerSnapshot snapshot) throws Exception;

    /** @return l'état sauvegardé, ou null si le fichier est illisible */
    PlayerSnapshot loadSnapshot(File file);

    // ------------------------------------------------------------------ intégrations

    ApiBridge api();

    /** Placeholders d'autres plugins (%...% de PlaceholderAPI) ; renvoie le texte tel quel s'il n'y en a pas. */
    String externalPlaceholders(String text, UUID subject, PlatformPlayer viewer);

    /** Description de la plateforme pour la console (« Paper 1.21.4 », « Fabric 1.21.1 »...). */
    String describe();
}
