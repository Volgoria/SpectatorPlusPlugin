package fr.spectatorplus.core.platform;

/**
 * Monde / dimension d'un serveur.
 */
public interface PlatformWorld {

    /** Nom (Bukkit : nom du dossier ; mods : identifiant de dimension, ex « minecraft:overworld »). */
    String getName();

    Dimension getDimension();

    /** L'un des trois mondes principaux du serveur (monde, nether, end), par opposition aux mondes ajoutés. */
    boolean isMainWorld();

    /** Bloc solide (pour le suivi et le passe-muraille). */
    boolean isSolid(int x, int y, int z);

    /** Nom du bloc, en majuscules façon Bukkit (DIAMOND_ORE). */
    String getBlockType(int x, int y, int z);

    int getHighestBlockYAt(int x, int z);

    int getMinHeight();

    /** Heure du monde (0-24000). */
    long getTime();

    Position getSpawn();

    /** Taille de la bordure du monde (diamètre en blocs). */
    double getBorderSize();

    double getBorderCenterX();

    double getBorderCenterZ();

    /** Structures générées qui contiennent cette position (VILLAGE, STRONGHOLD...) ; liste vide si non supporté. */
    java.util.List<String> getStructuresAt(Position position);
}
