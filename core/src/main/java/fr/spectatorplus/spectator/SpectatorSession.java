package fr.spectatorplus.spectator;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.api.EnterReason;
import fr.spectatorplus.api.SpectatorState;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.PlayerSnapshot;
import fr.spectatorplus.core.platform.Vector3;
import fr.spectatorplus.gui.menus.InspectMenu;
import fr.spectatorplus.gui.menus.PlayerSheetMenu;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Un joueur en mode spectateur. Exposé par l'API publique de chaque plateforme.
 */
public final class SpectatorSession {

    private final SpectatorCore plugin;
    private final UUID id;
    private final EnterReason reason;
    private final long since = System.currentTimeMillis();
    final PlayerSnapshot saved;
    final Map<Integer, String> hotbarActions = new HashMap<>();

    SpectatorState state = SpectatorState.FREE;
    UUID target;
    /** Dernier joueur atteint par le clic gauche de la boussole (joueur suivant). */
    UUID lastTeleport;
    boolean frozen;
    boolean povExitRequested;
    boolean inspecting;
    /** Actuellement en mode de jeu Spectator pour traverser un bloc. */
    boolean noclip;
    /** Suivi : dernière position de la cible (pour anticiper son déplacement). */
    Vector3 lastTargetPos;
    String lastTargetWorld;
    /** Dernière position du spectateur (anticipation du passe-muraille). */
    Vector3 lastPos;

    SpectatorSession(SpectatorCore plugin, UUID id, EnterReason reason, PlayerSnapshot saved) {
        this.plugin = plugin;
        this.id = id;
        this.reason = reason;
        this.saved = saved;
    }

    public UUID getUniqueId() {
        return id;
    }

    public PlatformPlayer getPlayer() {
        return plugin.platform().getPlayer(id);
    }

    public SpectatorState getState() {
        if (frozen) return SpectatorState.FROZEN;
        if (inspecting) return SpectatorState.INSPECTING;
        return state;
    }

    /** État hors inspection / gel (FREE, FOLLOWING, POV). */
    public SpectatorState getMovementState() {
        return state;
    }

    public EnterReason getEnterReason() {
        return reason;
    }

    public long getSince() {
        return since;
    }

    public PlatformPlayer getFollowTarget() {
        return target == null ? null : plugin.platform().getPlayer(target);
    }

    public String getHotbarAction(int slot) {
        return hotbarActions.get(slot);
    }

    public PlayerSnapshot getSavedState() {
        return saved;
    }

    public void follow(PlatformPlayer t) {
        plugin.spectators().follow(this, t);
    }

    public void stopFollowing() {
        plugin.spectators().stopFollowing(this, true);
    }

    public void startPov(PlatformPlayer t) {
        plugin.spectators().startPov(this, t);
    }

    public void stopPov() {
        plugin.spectators().stopPov(this);
    }

    public boolean teleportTo(PlatformPlayer t) {
        return plugin.spectators().teleport(this, t);
    }

    public void openInspection(PlatformPlayer t) {
        PlatformPlayer p = getPlayer();
        if (p == null || t == null) return;
        if (!plugin.platform().api().inspect(p, t)) return;
        new PlayerSheetMenu(plugin, p, t).open();
    }

    public void openInventory(PlatformPlayer t, boolean enderChest) {
        PlatformPlayer p = getPlayer();
        if (p == null || t == null) return;
        if (enderChest && !p.hasPermission("spectatorplus.inspect.enderchest")) {
            plugin.messages().send(p, "errors.no-permission");
            return;
        }
        if (!plugin.platform().api().inventoryInspect(p, t, enderChest)) return;
        new InspectMenu(plugin, p, t, enderChest).open();
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean value) {
        if (frozen == value) return;
        PlatformPlayer p = getPlayer();
        if (p == null) return;
        if (value) {
            if (!plugin.platform().api().freeze(p)) return;
            plugin.spectators().stopFollowing(this, false);
            frozen = true;
            p.setFlySpeed(0f);
            plugin.messages().send(p, "spectator.frozen");
        } else {
            if (!plugin.platform().api().unfreeze(p)) return;
            frozen = false;
            plugin.spectators().applyFlySpeed(p);
            plugin.messages().send(p, "spectator.unfrozen");
        }
    }

    public void setInspecting(boolean inspecting) {
        this.inspecting = inspecting;
    }
}
