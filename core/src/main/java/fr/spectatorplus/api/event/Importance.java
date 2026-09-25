package fr.spectatorplus.api.event;

import java.util.Locale;

/**
 * Niveau d'importance d'un évènement.
 */
public enum Importance {
    /** Information secondaire. */
    LOW,
    /** Évènement classique. */
    NORMAL,
    /** Évènement intéressant. */
    IMPORTANT,
    /** Évènement majeur. */
    CRITICAL;

    public boolean isAtLeast(Importance other) {
        return ordinal() >= other.ordinal();
    }

    public Importance next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static Importance parse(String s, Importance def) {
        if (s == null) return def;
        try {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return def;
        }
    }
}
