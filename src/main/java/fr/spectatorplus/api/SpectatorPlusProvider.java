package fr.spectatorplus.api;

/**
 * Accès statique à l'API.
 */
public final class SpectatorPlusProvider {

    private static SpectatorPlusAPI instance;

    private SpectatorPlusProvider() {
    }

    /**
     * @throws IllegalStateException si Spectator Plus n'est pas activé
     */
    public static SpectatorPlusAPI get() {
        if (instance == null) throw new IllegalStateException("SpectatorPlus is not enabled");
        return instance;
    }

    public static boolean isAvailable() {
        return instance != null;
    }

    /** Usage interne. */
    public static void register(SpectatorPlusAPI api) {
        instance = api;
    }
}
