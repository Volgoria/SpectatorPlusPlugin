package fr.spectatorplus.event.detect;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.core.platform.Dimension;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.PlatformWorld;
import fr.spectatorplus.event.EventSettings;

/**
 * Monde et dimensions : changements de monde et portails.
 * (Zones, biomes, altitude, bordure... sont détectés par {@link PollingSignals}.)
 */
public final class WorldSignals extends Detection {

    public WorldSignals(SpectatorCore plugin) {
        super(plugin);
    }

    /** Le joueur est passé d'un monde à un autre. */
    public void worldChanged(PlatformPlayer p, PlatformWorld from, PlatformWorld to) {
        if (!tracked(p) || from == null || to == null) return;
        Dimension fe = from.getDimension(), te = to.getDimension();
        if (te == Dimension.NETHER && fe != Dimension.NETHER && on("world.nether_enter") != null) {
            fire(ev("world.nether_enter", p).data("from", from.getName()).data("to", to.getName()));
        }
        if (fe == Dimension.NETHER && te != Dimension.NETHER && on("world.nether_leave") != null) {
            fire(ev("world.nether_leave", p).data("from", from.getName()).data("to", to.getName()));
        }
        if (te == Dimension.END && fe != Dimension.END && on("world.end_enter") != null) {
            fire(ev("world.end_enter", p).data("from", from.getName()).data("to", to.getName()));
        }
        if (fe == Dimension.END && te != Dimension.END && on("world.end_leave") != null) {
            fire(ev("world.end_leave", p).data("from", from.getName()).data("to", to.getName()));
        }
        EventSettings s = on("world.change");
        if (s != null && s.accepts("worlds", to.getName())) {
            fire(ev("world.change", p).data("from", from.getName()).data("to", to.getName()));
        }
    }

    /** Passage d'un portail. Cause : NETHER_PORTAL, END_PORTAL ou END_GATEWAY (autres valeurs ignorées). */
    public void portal(PlatformPlayer p, String cause) {
        if (!tracked(p) || cause == null) return;
        String id;
        if (cause.equals("END_GATEWAY")) id = "world.end_gateway";
        else if (cause.equals("NETHER_PORTAL")) id = "world.nether_portal";
        else if (cause.equals("END_PORTAL")) id = "world.end_portal";
        else return;
        if (on(id) != null) fire(ev(id, p));
    }
}
