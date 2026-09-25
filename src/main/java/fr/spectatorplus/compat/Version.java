package fr.spectatorplus.compat;

import org.bukkit.Bukkit;

/**
 * Détection de la version du serveur.
 * Gère les anciens numéros (1.8.8, 1.21.4) comme les nouveaux (26.1, 26.2).
 */
public final class Version {

    private static final int MAJOR;
    private static final int MINOR;
    private static final int PATCH;

    static {
        int major = 1, minor = 8, patch = 0;
        try {
            String raw = Bukkit.getBukkitVersion();
            String v = raw.split("-")[0];
            String[] parts = v.split("\\.");
            major = parse(parts, 0, 1);
            minor = parse(parts, 1, 0);
            patch = parse(parts, 2, 0);
        } catch (Throwable ignored) {
        }
        MAJOR = major;
        MINOR = minor;
        PATCH = patch;
    }

    private Version() {
    }

    private static int parse(String[] parts, int index, int def) {
        if (parts.length <= index) return def;
        try {
            return Integer.parseInt(parts[index].replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static boolean atLeast(int major, int minor) {
        return atLeast(major, minor, 0);
    }

    public static boolean atLeast(int major, int minor, int patch) {
        if (MAJOR != major) return MAJOR > major;
        if (MINOR != minor) return MINOR > minor;
        return PATCH >= patch;
    }

    /** Versions antérieures à la « flattening » (1.13) : matériaux avec data values. */
    public static boolean isLegacy() {
        return !atLeast(1, 13);
    }

    public static String asString() {
        return MAJOR + "." + MINOR + (PATCH > 0 ? "." + PATCH : "");
    }
}
