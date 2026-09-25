package fr.spectatorplus.event;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ZoneManager {

    private final List<Zone> zones = new ArrayList<>();

    public void load(ConfigurationSection section) {
        zones.clear();
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(id);
            if (s != null) zones.add(new Zone(id, s));
        }
    }

    public List<Zone> getZones() {
        return Collections.unmodifiableList(zones);
    }

    public Zone zoneAt(Location l) {
        for (Zone z : zones) if (z.contains(l)) return z;
        return null;
    }

    public List<Zone> zonesAt(Location l) {
        List<Zone> res = new ArrayList<>();
        for (Zone z : zones) if (z.contains(l)) res.add(z);
        return res;
    }
}
