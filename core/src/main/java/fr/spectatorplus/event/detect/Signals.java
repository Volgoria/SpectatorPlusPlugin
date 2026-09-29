package fr.spectatorplus.event.detect;

import fr.spectatorplus.SpectatorCore;

/**
 * Point d'entrée des détections d'évènements natifs : chaque plateforme y signale ce qui se passe dans le jeu.
 */
public final class Signals {

    private final WorldSignals world;
    private final MiningSignals mining;
    private final PlayerSignals player;
    private final CombatSignals combat;
    private final ItemSignals items;
    private final PollingSignals polling;

    public Signals(SpectatorCore plugin) {
        world = new WorldSignals(plugin);
        mining = new MiningSignals(plugin);
        player = new PlayerSignals(plugin);
        combat = new CombatSignals(plugin);
        items = new ItemSignals(plugin);
        polling = new PollingSignals(plugin);
    }

    /** Démarre les détections périodiques (fins de combat, états des joueurs, bordure). */
    public void start() {
        combat.start();
        polling.start();
    }

    /** Début de partie : remise à zéro des états. */
    public void reset() {
        player.reset();
        polling.reset();
    }

    public WorldSignals world() {
        return world;
    }

    public MiningSignals mining() {
        return mining;
    }

    public PlayerSignals player() {
        return player;
    }

    public CombatSignals combat() {
        return combat;
    }

    public ItemSignals items() {
        return items;
    }

    public PollingSignals polling() {
        return polling;
    }
}
