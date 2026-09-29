package fr.spectatorplus.core.platform;

import fr.spectatorplus.compat.Sounds;

import java.util.List;
import java.util.UUID;

/**
 * Joueur connecté, vu par le code commun.
 * <p>
 * Une instance représente un joueur par son UUID : elle reste valable après une mort ou un changement
 * de monde (sur les mods, l'entité joueur est recréée à chaque réapparition). Deux instances du même
 * joueur sont égales ({@code equals}).
 */
public interface PlatformPlayer extends Sender {

    UUID getUniqueId();

    boolean isOnline();

    /** Locale du client (« fr_fr »), ou null si inconnue. */
    String getClientLocale();

    // ------------------------------------------------------------------ messages

    /** Message cliquable (commande exécutée au clic) avec une info-bulle. */
    void sendClickable(String text, String hover, String command);

    void actionBar(String text);

    void title(String title, String subtitle, int fadeIn, int stay, int fadeOut);

    void playSound(Sounds sound, float volume, float pitch);

    /** Exécute une commande en tant que ce joueur (sans le « / »). */
    void performCommand(String command);

    // ------------------------------------------------------------------ position

    Position getLocation();

    Position getEyeLocation();

    PlatformWorld getWorld();

    boolean teleport(Position destination);

    void setVelocity(Vector3 velocity);

    void leaveVehicle();

    boolean isInsideVehicle();

    // ------------------------------------------------------------------ mode de jeu / vol

    GameMode getGameMode();

    void setGameMode(GameMode mode);

    boolean getAllowFlight();

    void setAllowFlight(boolean allow);

    boolean isFlying();

    void setFlying(boolean flying);

    float getFlySpeed();

    /** Vitesse de vol façon Bukkit (0.1 = normale, entre -1 et 1). */
    void setFlySpeed(float speed);

    float getWalkSpeed();

    void setWalkSpeed(float speed);

    // ------------------------------------------------------------------ état

    double getHealth();

    void setHealth(double health);

    double getMaxHealth();

    double getAbsorption();

    int getFoodLevel();

    void setFoodLevel(int food);

    float getSaturation();

    void setSaturation(float saturation);

    int getLevel();

    void setLevel(int level);

    float getExp();

    void setExp(float exp);

    void setFireTicks(int ticks);

    float getFallDistance();

    void setFallDistance(float distance);

    boolean isDead();

    boolean isSneaking();

    boolean isSprinting();

    boolean isOnGround();

    /** Emplacement de la barre d'action sélectionné (0-8). */
    int getHeldSlot();

    /** Équipe (scoreboard), ou null. */
    String getScoreboardTeam();

    /** Joueur fictif (NPC d'un plugin, faux joueur d'un mod) : ignoré partout. */
    boolean isFake();

    // ------------------------------------------------------------------ spectateur

    /** Caméra à la première personne (mode Spectator) ; null pour l'arrêter. */
    void setSpectatorTarget(PlatformPlayer target);

    UUID getSpectatorTargetId();

    void setCanPickupItems(boolean value);

    void setSleepingIgnored(boolean value);

    void setCollidable(boolean value);

    /** Le joueur fait-il apparaître des monstres autour de lui ? */
    void setAffectsSpawning(boolean value);

    void setInvulnerable(boolean value);

    /** Cache (ou montre) {@code target} à ce joueur. */
    void setHidden(PlatformPlayer target, boolean hidden);

    /**
     * Affiche (ou retire) les points de vie des joueurs sous leur pseudo, pour ce joueur uniquement.
     *
     * @param title unité affichée après le nombre (codes « § »)
     */
    void setHealthDisplay(boolean enabled, String title);

    // ------------------------------------------------------------------ effets

    List<EffectInfo> getEffects();

    void clearEffects();

    /** Ajoute un effet par son nom (NIGHT_VISION, INVISIBILITY...). */
    void addEffect(String name, int duration, int amplifier, boolean ambient, boolean particles);

    // ------------------------------------------------------------------ inventaire

    void closeInventory();

    /** Vide l'inventaire, l'armure et la seconde main. */
    void clearInventory();

    void setItem(int slot, Icon icon);

    void updateInventory();

    /** Inventaire principal (0-8 barre d'action, 9-35 inventaire). */
    ItemRef[] getStorageContents();

    /** Armure : bottes, jambières, plastron, casque (ordre Bukkit). */
    ItemRef[] getArmorContents();

    /** Seconde main, ou null si la version n'en a pas. */
    ItemRef getOffHand();

    ItemRef getMainHand();

    ItemRef[] getEnderChest();

    /** Nom du biome à la position du joueur (majuscules). */
    String getBiome();
}
