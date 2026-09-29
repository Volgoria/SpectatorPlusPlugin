package fr.spectatorplus.core.platform;

import fr.spectatorplus.api.EnterReason;
import fr.spectatorplus.api.LeaveReason;
import fr.spectatorplus.core.event.GameEvent;

/**
 * Évènements de l'API publique déclenchés par le code commun (SpectatorEnterEvent... sur Bukkit).
 * Les méthodes booléennes renvoient false si un autre plugin a annulé l'action.
 * {@link #NONE} ne déclenche rien (plateformes sans API d'évènements).
 */
public interface ApiBridge {

    boolean enter(PlatformPlayer player, EnterReason reason, boolean forced);

    boolean leave(PlatformPlayer player, LeaveReason reason);

    boolean followStart(PlatformPlayer player, PlatformPlayer target);

    boolean followChange(PlatformPlayer player, PlatformPlayer previous, PlatformPlayer target);

    void followStop(PlatformPlayer player, PlatformPlayer previous);

    /** @return la destination (éventuellement modifiée), ou null si la téléportation est annulée */
    Position teleport(PlatformPlayer player, PlatformPlayer target, Position destination);

    boolean freeze(PlatformPlayer player);

    boolean unfreeze(PlatformPlayer player);

    boolean inspect(PlatformPlayer player, PlatformPlayer target);

    boolean inventoryInspect(PlatformPlayer player, PlatformPlayer target, boolean enderChest);

    boolean menuOpen(PlatformPlayer player, String menuId);

    void menuClose(PlatformPlayer player, String menuId);

    void filterChange(PlatformPlayer player, String filter, String oldValue, String newValue);

    /** Avant la diffusion d'un évènement de partie. */
    boolean gameEvent(GameEvent event);

    ApiBridge NONE = new ApiBridge() {
        @Override
        public boolean enter(PlatformPlayer player, EnterReason reason, boolean forced) {
            return true;
        }

        @Override
        public boolean leave(PlatformPlayer player, LeaveReason reason) {
            return true;
        }

        @Override
        public boolean followStart(PlatformPlayer player, PlatformPlayer target) {
            return true;
        }

        @Override
        public boolean followChange(PlatformPlayer player, PlatformPlayer previous, PlatformPlayer target) {
            return true;
        }

        @Override
        public void followStop(PlatformPlayer player, PlatformPlayer previous) {
        }

        @Override
        public Position teleport(PlatformPlayer player, PlatformPlayer target, Position destination) {
            return destination;
        }

        @Override
        public boolean freeze(PlatformPlayer player) {
            return true;
        }

        @Override
        public boolean unfreeze(PlatformPlayer player) {
            return true;
        }

        @Override
        public boolean inspect(PlatformPlayer player, PlatformPlayer target) {
            return true;
        }

        @Override
        public boolean inventoryInspect(PlatformPlayer player, PlatformPlayer target, boolean enderChest) {
            return true;
        }

        @Override
        public boolean menuOpen(PlatformPlayer player, String menuId) {
            return true;
        }

        @Override
        public void menuClose(PlatformPlayer player, String menuId) {
        }

        @Override
        public void filterChange(PlatformPlayer player, String filter, String oldValue, String newValue) {
        }

        @Override
        public boolean gameEvent(GameEvent event) {
            return true;
        }
    };
}
