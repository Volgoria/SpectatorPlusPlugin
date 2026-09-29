package fr.spectatorplus.core.platform;

/**
 * Effet de potion actif sur un joueur.
 */
public final class EffectInfo {

    private final String name;
    private final int amplifier;
    private final int duration;

    public EffectInfo(String name, int amplifier, int duration) {
        this.name = name;
        this.amplifier = amplifier;
        this.duration = duration;
    }

    /** Nom en majuscules (SPEED, NIGHT_VISION...). */
    public String getName() {
        return name;
    }

    public int getAmplifier() {
        return amplifier;
    }

    /** Durée restante en ticks. */
    public int getDuration() {
        return duration;
    }
}
