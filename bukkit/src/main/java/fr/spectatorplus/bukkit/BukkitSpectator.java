package fr.spectatorplus.bukkit;

import fr.spectatorplus.api.EnterReason;
import fr.spectatorplus.api.Spectator;
import fr.spectatorplus.api.SpectatorState;
import fr.spectatorplus.spectator.SpectatorSession;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spectateur exposé par l'API publique Bukkit.
 */
public final class BukkitSpectator implements Spectator {

    private final BukkitPlatform platform;
    private final SpectatorSession session;

    BukkitSpectator(BukkitPlatform platform, SpectatorSession session) {
        this.platform = platform;
        this.session = session;
    }

    public static BukkitSpectator of(BukkitPlatform platform, SpectatorSession session) {
        return session == null ? null : new BukkitSpectator(platform, session);
    }

    public static List<BukkitSpectator> all(BukkitPlatform platform, Collection<SpectatorSession> sessions) {
        List<BukkitSpectator> res = new ArrayList<>();
        for (SpectatorSession s : sessions) res.add(new BukkitSpectator(platform, s));
        return res;
    }

    @Override
    public UUID getUniqueId() {
        return session.getUniqueId();
    }

    @Override
    public Player getPlayer() {
        return BukkitPlayer.unwrap(session.getPlayer());
    }

    @Override
    public SpectatorState getState() {
        return session.getState();
    }

    @Override
    public EnterReason getEnterReason() {
        return session.getEnterReason();
    }

    @Override
    public long getSince() {
        return session.getSince();
    }

    @Override
    public Player getFollowTarget() {
        return BukkitPlayer.unwrap(session.getFollowTarget());
    }

    @Override
    public void follow(Player target) {
        session.follow(platform.wrap(target));
    }

    @Override
    public void stopFollowing() {
        session.stopFollowing();
    }

    @Override
    public void startPov(Player target) {
        session.startPov(platform.wrap(target));
    }

    @Override
    public void stopPov() {
        session.stopPov();
    }

    @Override
    public boolean teleportTo(Player target) {
        return session.teleportTo(platform.wrap(target));
    }

    @Override
    public void openInspection(Player target) {
        session.openInspection(platform.wrap(target));
    }

    @Override
    public void openInventory(Player target, boolean enderChest) {
        session.openInventory(platform.wrap(target), enderChest);
    }

    @Override
    public boolean isFrozen() {
        return session.isFrozen();
    }

    @Override
    public void setFrozen(boolean frozen) {
        session.setFrozen(frozen);
    }
}
