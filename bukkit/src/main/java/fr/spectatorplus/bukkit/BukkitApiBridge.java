package fr.spectatorplus.bukkit;

import fr.spectatorplus.api.EnterReason;
import fr.spectatorplus.api.LeaveReason;
import fr.spectatorplus.api.event.SpectatorGameEvent;
import fr.spectatorplus.api.events.SpectatorEnterEvent;
import fr.spectatorplus.api.events.SpectatorFilterChangeEvent;
import fr.spectatorplus.api.events.SpectatorFollowStartEvent;
import fr.spectatorplus.api.events.SpectatorFollowStopEvent;
import fr.spectatorplus.api.events.SpectatorFollowTargetChangeEvent;
import fr.spectatorplus.api.events.SpectatorFreezeEvent;
import fr.spectatorplus.api.events.SpectatorGameEventTriggerEvent;
import fr.spectatorplus.api.events.SpectatorInspectEvent;
import fr.spectatorplus.api.events.SpectatorInventoryInspectEvent;
import fr.spectatorplus.api.events.SpectatorLeaveEvent;
import fr.spectatorplus.api.events.SpectatorMenuCloseEvent;
import fr.spectatorplus.api.events.SpectatorMenuOpenEvent;
import fr.spectatorplus.api.events.SpectatorTeleportEvent;
import fr.spectatorplus.api.events.SpectatorUnfreezeEvent;
import fr.spectatorplus.compat.Positions;
import fr.spectatorplus.core.event.GameEvent;
import fr.spectatorplus.core.platform.ApiBridge;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.Position;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

import static fr.spectatorplus.bukkit.BukkitPlayer.unwrap;

/**
 * Déclenche les évènements Bukkit de l'API publique (fr.spectatorplus.api.events).
 */
final class BukkitApiBridge implements ApiBridge {

    /** @return true si l'évènement n'a pas été annulé */
    private static boolean call(Event e) {
        Bukkit.getPluginManager().callEvent(e);
        return !(e instanceof Cancellable) || !((Cancellable) e).isCancelled();
    }

    @Override
    public boolean enter(PlatformPlayer player, EnterReason reason, boolean forced) {
        return call(new SpectatorEnterEvent(unwrap(player), reason, forced));
    }

    @Override
    public boolean leave(PlatformPlayer player, LeaveReason reason) {
        return call(new SpectatorLeaveEvent(unwrap(player), reason));
    }

    @Override
    public boolean followStart(PlatformPlayer player, PlatformPlayer target) {
        return call(new SpectatorFollowStartEvent(unwrap(player), unwrap(target)));
    }

    @Override
    public boolean followChange(PlatformPlayer player, PlatformPlayer previous, PlatformPlayer target) {
        return call(new SpectatorFollowTargetChangeEvent(unwrap(player), unwrap(previous), unwrap(target)));
    }

    @Override
    public void followStop(PlatformPlayer player, PlatformPlayer previous) {
        call(new SpectatorFollowStopEvent(unwrap(player), unwrap(previous)));
    }

    @Override
    public Position teleport(PlatformPlayer player, PlatformPlayer target, Position destination) {
        SpectatorTeleportEvent ev = new SpectatorTeleportEvent(unwrap(player), unwrap(target), Positions.toLocation(destination));
        if (!call(ev) || ev.getDestination() == null) return null;
        return Positions.of(ev.getDestination());
    }

    @Override
    public boolean freeze(PlatformPlayer player) {
        return call(new SpectatorFreezeEvent(unwrap(player)));
    }

    @Override
    public boolean unfreeze(PlatformPlayer player) {
        return call(new SpectatorUnfreezeEvent(unwrap(player)));
    }

    @Override
    public boolean inspect(PlatformPlayer player, PlatformPlayer target) {
        return call(new SpectatorInspectEvent(unwrap(player), unwrap(target)));
    }

    @Override
    public boolean inventoryInspect(PlatformPlayer player, PlatformPlayer target, boolean enderChest) {
        return call(new SpectatorInventoryInspectEvent(unwrap(player), unwrap(target), enderChest));
    }

    @Override
    public boolean menuOpen(PlatformPlayer player, String menuId) {
        return call(new SpectatorMenuOpenEvent(unwrap(player), menuId));
    }

    @Override
    public void menuClose(PlatformPlayer player, String menuId) {
        call(new SpectatorMenuCloseEvent(unwrap(player), menuId));
    }

    @Override
    public void filterChange(PlatformPlayer player, String filter, String oldValue, String newValue) {
        call(new SpectatorFilterChangeEvent(unwrap(player), filter, oldValue, newValue));
    }

    @Override
    public boolean gameEvent(GameEvent event) {
        return call(new SpectatorGameEventTriggerEvent(SpectatorGameEvent.wrap(event), false));
    }
}
