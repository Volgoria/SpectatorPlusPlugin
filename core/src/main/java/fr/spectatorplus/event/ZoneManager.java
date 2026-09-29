package fr.spectatorplus.event;

import fr.spectatorplus.core.config.ConfigSection;
import fr.spectatorplus.core.platform.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ZoneManager {

    private final List<Zone> zones = new ArrayList<>();

    public void load(ConfigSection section) {
        zones.clear();
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            ConfigSection s = section.getConfigurationSection(id);
            if (s != null) zones.add(new Zone(id, s));
        }
    }

    public List<Zone> getZones() {
        return Collections.unmodifiableList(zones);
    }

    public Zone zoneAt(Position l) {
        for (Zone z : zones) if (z.contains(l)) return z;
        return null;
    }

    public List<Zone> zonesAt(Position l) {
        List<Zone> res = new ArrayList<>();
        for (Zone z : zones) if (z.contains(l)) res.add(z);
        return res;
    }
}
