package fr.spectatorplus.spectator;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.api.EnterReason;
import fr.spectatorplus.api.Spectator;
import fr.spectatorplus.api.SpectatorState;
import fr.spectatorplus.api.events.SpectatorFreezeEvent;
import fr.spectatorplus.api.events.SpectatorInspectEvent;
import fr.spectatorplus.api.events.SpectatorInventoryInspectEvent;
import fr.spectatorplus.api.events.SpectatorUnfreezeEvent;
import fr.spectatorplus.gui.menus.InspectMenu;
import fr.spectatorplus.gui.menus.PlayerSheetMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SpectatorSession implements Spectator {

    private final SpectatorPlus plugin;
    private final UUID id;
    private final EnterReason reason;
    private final long since = System.currentTimeMillis();
    final SavedState saved;
    final Map<Integer, String> hotbarActions = new HashMap<>();

    SpectatorState state = SpectatorState.FREE;
    UUID target;
    boolean frozen;
    boolean povExitRequested;
    boolean inspecting;
    /** Actuellement en mode de jeu Spectator pour traverser un bloc. */
    boolean noclip;
    /** Suivi : dernière position de la cible (pour anticiper son déplacement). */
    org.bukkit.util.Vector lastTargetPos;
    String lastTargetWorld;
    /** Dernière position du spectateur (anticipation du passe-muraille). */
    org.bukkit.util.Vector lastPos;

    SpectatorSession(SpectatorPlus plugin, UUID id, EnterReason reason, SavedState saved) {
        this.plugin = plugin;
        this.id = id;
        this.reason = reason;
        this.saved = saved;
    }

    @Override
    public UUID getUniqueId() {
        return id;
    }

    @Override
    public Player getPlayer() {
        return Bukkit.getPlayer(id);
    }

    @Override
    public SpectatorState getState() {
        if (frozen) return SpectatorState.FROZEN;
        if (inspecting) return SpectatorState.INSPECTING;
        return state;
    }

    /** État hors inspection / gel (FREE, FOLLOWING, POV). */
    public SpectatorState getMovementState() {
        return state;
    }

    @Override
    public EnterReason getEnterReason() {
        return reason;
    }

    @Override
    public long getSince() {
        return since;
    }

    @Override
    public Player getFollowTarget() {
        return target == null ? null : Bukkit.getPlayer(target);
    }

    public String getHotbarAction(int slot) {
        return hotbarActions.get(slot);
    }

    public SavedState getSavedState() {
        return saved;
    }

    @Override
    public void follow(Player t) {
        plugin.spectators().follow(this, t);
    }

    @Override
    public void stopFollowing() {
        plugin.spectators().stopFollowing(this, true);
    }

    @Override
    public void startPov(Player t) {
        plugin.spectators().startPov(this, t);
    }

    @Override
    public void stopPov() {
        plugin.spectators().stopPov(this);
    }

    @Override
    public boolean teleportTo(Player t) {
        return plugin.spectators().teleport(this, t);
    }

    @Override
    public void openInspection(Player t) {
        Player p = getPlayer();
        if (p == null || t == null) return;
        SpectatorInspectEvent ev = new SpectatorInspectEvent(p, t);
        Bukkit.getPluginManager().callEvent(ev);
        if (ev.isCancelled()) return;
        new PlayerSheetMenu(plugin, p, t).open();
    }

    @Override
    public void openInventory(Player t, boolean enderChest) {
        Player p = getPlayer();
        if (p == null || t == null) return;
        if (enderChest && !p.hasPermission("spectatorplus.inspect.enderchest")) {
            plugin.messages().send(p, "errors.no-permission");
            return;
        }
        SpectatorInventoryInspectEvent ev = new SpectatorInventoryInspectEvent(p, t, enderChest);
        Bukkit.getPluginManager().callEvent(ev);
        if (ev.isCancelled()) return;
        new InspectMenu(plugin, p, t, enderChest).open();
    }

    @Override
    public boolean isFrozen() {
        return frozen;
    }

    @Override
    public void setFrozen(boolean value) {
        if (frozen == value) return;
        Player p = getPlayer();
        if (p == null) return;
        if (value) {
            SpectatorFreezeEvent ev = new SpectatorFreezeEvent(p);
            Bukkit.getPluginManager().callEvent(ev);
            if (ev.isCancelled()) return;
            plugin.spectators().stopFollowing(this, false);
            frozen = true;
            p.setFlySpeed(0f);
            plugin.messages().send(p, "spectator.frozen");
        } else {
            SpectatorUnfreezeEvent ev = new SpectatorUnfreezeEvent(p);
            Bukkit.getPluginManager().callEvent(ev);
            if (ev.isCancelled()) return;
            frozen = false;
            plugin.spectators().applyFlySpeed(p);
            plugin.messages().send(p, "spectator.unfrozen");
        }
    }

    public void setInspecting(boolean inspecting) {
        this.inspecting = inspecting;
    }
}
