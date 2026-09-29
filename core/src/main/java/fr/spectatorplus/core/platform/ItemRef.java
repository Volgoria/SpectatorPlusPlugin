package fr.spectatorplus.core.platform;

import java.util.Map;

/**
 * Copie d'un objet réel du jeu (inventaire d'un joueur), manipulée sans connaître son type natif.
 * La plateforme sait l'afficher tel quel dans un menu.
 */
public interface ItemRef {

    /** Type en majuscules façon Bukkit (DIAMOND_SWORD). */
    String getType();

    boolean isEmpty();

    int getAmount();

    /** Nom personnalisé (avec couleurs), ou null. */
    String getDisplayName();

    /** Enchantements : nom (SHARPNESS, legacy ou moderne selon la plateforme) → niveau. */
    Map<String, Integer> getEnchantments();

    /** Enchantements stockés (livre enchanté) : nom → niveau. */
    Map<String, Integer> getStoredEnchantments();

    /** Type de base d'une potion (SWIFTNESS, STRENGTH...), ou null si ce n'est pas une potion. */
    String getPotionType();

    /** Objet natif (ItemStack Bukkit, ItemStack Minecraft...). */
    Object handle();
}
