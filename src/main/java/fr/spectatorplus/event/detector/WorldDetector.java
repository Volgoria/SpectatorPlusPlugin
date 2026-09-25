package fr.spectatorplus.event.detector;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.event.EventSettings;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Monde et dimensions : changements de monde et portails.
 * (Zones, biomes, altitude, bordure... sont détectés par {@link PollingDetector}.)
 */
public final class WorldDetector extends Detector {

    public WorldDetector(SpectatorPlus plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        if (!tracked(p)) return;
        World from = e.getFrom();
        World to = p.getWorld();
        World.Environment fe = from.getEnvironment(), te = to.getEnvironment();

        if (te == World.Environment.NETHER && fe != World.Environment.NETHER && on("world.nether_enter") != null) {
            fire(ev("world.nether_enter", p).data("from", from.getName()).data("to", to.getName()));
        }
        if (fe == World.Environment.NETHER && te != World.Environment.NETHER && on("world.nether_leave") != null) {
            fire(ev("world.nether_leave", p).data("from", from.getName()).data("to", to.getName()));
        }
        if (te == World.Environment.THE_END && fe != World.Environment.THE_END && on("world.end_enter") != null) {
            fire(ev("world.end_enter", p).data("from", from.getName()).data("to", to.getName()));
        }
        if (fe == World.Environment.THE_END && te != World.Environment.THE_END && on("world.end_leave") != null) {
            fire(ev("world.end_leave", p).data("from", from.getName()).data("to", to.getName()));
        }
        EventSettings s = on("world.change");
        if (s != null && s.accepts("worlds", to.getName())) {
            fire(ev("world.change", p).data("from", from.getName()).data("to", to.getName()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent e) {
        portal(e.getPlayer(), e.getCause());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        portal(e.getPlayer(), e.getCause());
    }

    private void portal(Player p, PlayerTeleportEvent.TeleportCause cause) {
        if (!tracked(p) || cause == null) return;
        String c = cause.name();
        String id;
        if (c.equals("END_GATEWAY")) id = "world.end_gateway";
        else if (c.equals("NETHER_PORTAL")) id = "world.nether_portal";
        else if (c.equals("END_PORTAL")) id = "world.end_portal";
        else return;
        if (on(id) != null) fire(ev(id, p));
    }
}
