package fr.spectatorplus.event.detect;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.core.event.GameEvent;
import fr.spectatorplus.core.platform.EffectInfo;
import fr.spectatorplus.core.platform.ItemRef;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.PlatformWorld;
import fr.spectatorplus.core.platform.Position;
import fr.spectatorplus.event.EventSettings;
import fr.spectatorplus.event.Zone;
import fr.spectatorplus.util.Text;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Détections sans évènement du jeu : analyse périodique de l'état des joueurs.
 * (zones, coordonnées, biomes, altitude, spawn, bordure, effets, absorption, vie, armure, objets, AFK, structures)
 */
public final class PollingSignals extends Detection {

    private static final Set<String> POSITIVE = new HashSet<>(Arrays.asList(
            "SPEED", "HASTE", "STRENGTH", "INSTANT_HEALTH", "JUMP_BOOST", "REGENERATION", "RESISTANCE",
            "FIRE_RESISTANCE", "WATER_BREATHING", "INVISIBILITY", "NIGHT_VISION", "HEALTH_BOOST", "ABSORPTION",
            "SATURATION", "LUCK", "SLOW_FALLING", "CONDUIT_POWER", "DOLPHINS_GRACE", "HERO_OF_THE_VILLAGE"));

    private static final class State {
        boolean initialized;
        String world;
        Set<String> zones = new HashSet<>();
        String biome;
        double y;
        double spawnDistance = -1;
        boolean outsideBorder;
        Map<String, Integer> effects = new HashMap<>();
        double absorption;
        double health;
        String[] armor = new String[4];
        String fullSet;
        Position lastLocation;
        long lastMove = System.currentTimeMillis();
        boolean afk;
        int tick;
    }

    private static final class BorderState {
        double size = -1;
        double centerX, centerZ;
        boolean moving;
        int stable;
    }

    private final Map<UUID, State> states = new HashMap<>();
    private final Map<String, BorderState> borders = new HashMap<>();

    public PollingSignals(SpectatorCore plugin) {
        super(plugin);
    }

    public void start() {
        plugin.platform().runTimer(new Runnable() {
            @Override
            public void run() {
                for (PlatformPlayer p : plugin.platform().getOnlinePlayers()) {
                    if (!tracked(p)) {
                        states.remove(p.getUniqueId());
                        continue;
                    }
                    try {
                        poll(p);
                    } catch (Throwable t) {
                        plugin.host().logger().log(Level.FINE, "Polling error", t);
                    }
                }
            }
        }, 20L, 10L);
        plugin.platform().runTimer(new Runnable() {
            @Override
            public void run() {
                for (PlatformWorld w : plugin.platform().getWorlds()) border(w);
            }
        }, 40L, 20L);
    }

    public void reset() {
        states.clear();
    }

    public void quit(UUID id) {
        states.remove(id);
    }

    private void poll(PlatformPlayer p) {
        State st = states.get(p.getUniqueId());
        if (st == null) {
            st = new State();
            states.put(p.getUniqueId(), st);
        }
        Position loc = p.getLocation();
        st.tick++;
        boolean first = !st.initialized;
        if (st.world != null && !st.world.equals(loc.getWorld())) {
            // changement de monde : on repart de zéro pour la position (pas d'évènements de franchissement)
            st.spawnDistance = -1;
            st.y = loc.getY();
        }
        st.world = loc.getWorld();

        if (!first) {
            zones(p, st, loc);
            coordinates(p, loc);
            biome(p, st);
            altitude(p, st, loc);
            spawn(p, st, loc);
            borderCross(p, st, loc);
            effects(p, st);
            absorption(p, st);
            health(p, st);
            armor(p, st);
            amounts(p);
            afk(p, st, loc);
            if (st.tick % 10 == 0) structures(p, loc);
        } else {
            for (Zone z : plugin.zones().zonesAt(loc)) st.zones.add(z.getId());
            st.biome = p.getBiome();
            st.outsideBorder = outsideBorder(loc);
            st.effects = effectMap(p);
            st.absorption = p.getAbsorption();
            st.health = p.getHealth();
            st.armor = armorNames(p);
            st.fullSet = fullSet(st.armor);
            st.initialized = true;
        }
        st.y = loc.getY();
        st.lastLocation = loc;
    }

    // ------------------------------------------------------------------ position

    private void zones(PlatformPlayer p, State st, Position loc) {
        Set<String> now = new HashSet<>();
        for (Zone z : plugin.zones().zonesAt(loc)) {
            now.add(z.getId());
            if (!st.zones.contains(z.getId())) {
                EventSettings s = on("world.zone_enter");
                if (s != null && s.accepts("zones", z.getId())) {
                    fire(ev("world.zone_enter", p).data("zone", z.getName()).data("zone_name", z.getName())
                            .data("zone_id", z.getId()).data("zone_action", "@placeholders.zone-enter"));
                }
            }
        }
        for (String old : st.zones) {
            if (now.contains(old)) continue;
            EventSettings s = on("world.zone_leave");
            if (s == null || !s.accepts("zones", old)) continue;
            String name = old;
            for (Zone z : plugin.zones().getZones()) if (z.getId().equals(old)) name = z.getName();
            fire(ev("world.zone_leave", p).data("zone", name).data("zone_name", name)
                    .data("zone_id", old).data("zone_action", "@placeholders.zone-leave"));
        }
        st.zones = now;
    }

    private void coordinates(PlatformPlayer p, Position loc) {
        EventSettings s = on("world.coordinate");
        if (s == null) return;
        List<Map<?, ?>> targets = s.section().getMapList("targets");
        for (int i = 0; i < targets.size(); i++) {
            Map<?, ?> t = targets.get(i);
            Object world = t.get("world");
            if (world != null && !world.toString().equals(loc.getWorld())) continue;
            double radius = num(t.get("radius"), 5);
            double dx = loc.getX() - num(t.get("x"), 0), dz = loc.getZ() - num(t.get("z"), 0);
            double dy = t.containsKey("y") ? loc.getY() - num(t.get("y"), 0) : 0;
            if (dx * dx + dy * dy + dz * dz > radius * radius) continue;
            String name = t.containsKey("name") ? t.get("name").toString() : "#" + (i + 1);
            if (plugin.stats().first(p.getUniqueId(), "coord." + i)) {
                fire(ev("world.coordinate", p).data("target", name).data("coordinate", name));
            }
        }
    }

    private static double num(Object o, double def) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        try {
            return o == null ? def : Double.parseDouble(o.toString());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private void biome(PlatformPlayer p, State st) {
        if (st.tick % 4 != 0) return;
        String biome = p.getBiome();
        if (biome.equals(st.biome)) return;
        st.biome = biome;
        EventSettings s = on("world.biome");
        if (s != null && !s.names("biomes").isEmpty() && s.accepts("biomes", biome)
                && plugin.stats().first(p.getUniqueId(), "biome." + biome)) {
            fire(ev("world.biome", p).data("biome", pretty(biome)));
        }
    }

    private void altitude(PlatformPlayer p, State st, Position loc) {
        EventSettings s = on("world.altitude");
        if (s != null) {
            for (Integer y : s.ints("heights")) {
                if (st.y < y && loc.getY() >= y) fire(ev("world.altitude", p).data("altitude", y).data("value", y));
            }
        }
        s = on("world.depth");
        if (s != null) {
            for (Integer y : s.ints("depths")) {
                if (st.y > y && loc.getY() <= y) fire(ev("world.depth", p).data("depth", y).data("value", y));
            }
        }
    }

    private void spawn(PlatformPlayer p, State st, Position loc) {
        Position spawn = p.getWorld().getSpawn();
        double dx = loc.getX() - spawn.getX(), dz = loc.getZ() - spawn.getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        double prev = st.spawnDistance;
        st.spawnDistance = d;
        if (prev < 0) return;
        EventSettings s = on("world.spawn_away");
        if (s != null) {
            for (Integer t : s.ints("distances")) {
                if (prev < t && d >= t) fire(ev("world.spawn_away", p).data("distance", t));
            }
        }
        s = on("world.spawn_approach");
        if (s != null) {
            for (Integer t : s.ints("distances")) {
                if (prev > t && d <= t) fire(ev("world.spawn_approach", p).data("distance", t));
            }
        }
    }

    private void borderCross(PlatformPlayer p, State st, Position loc) {
        boolean outside = outsideBorder(loc);
        if (outside && !st.outsideBorder && on("world.border_cross") != null) {
            fire(ev("world.border_cross", p).data("size", (int) p.getWorld().getBorderSize()));
        }
        st.outsideBorder = outside;
    }

    private GameEvent.Builder borderEvent(String id, PlatformWorld w, BorderState bs) {
        return plugin.events().builder(id).location(new Position(w.getName(), bs.centerX, 0, bs.centerZ))
                .data("world", w.getName());
    }

    private void border(PlatformWorld w) {
        double size = w.getBorderSize();
        BorderState bs = borders.get(w.getName());
        if (bs == null) {
            bs = new BorderState();
            bs.size = size;
            borders.put(w.getName(), bs);
            return;
        }
        bs.centerX = w.getBorderCenterX();
        bs.centerZ = w.getBorderCenterZ();
        boolean changed = Math.abs(size - bs.size) > 0.001;
        if (changed && !bs.moving) {
            bs.moving = true;
            if (on("world.border_start") != null) {
                fire(borderEvent("world.border_start", w, bs).data("size", (int) size)
                        .data("direction", size < bs.size ? "@placeholders.border-shrink" : "@placeholders.border-grow"));
            }
        }
        if (changed) {
            bs.stable = 0;
            EventSettings s = on("world.border_size");
            if (s != null) {
                for (Integer t : s.ints("sizes")) {
                    if ((bs.size > t && size <= t) || (bs.size < t && size >= t)) {
                        fire(borderEvent("world.border_size", w, bs).data("size", t));
                    }
                }
            }
        } else if (bs.moving && ++bs.stable >= 2) {
            bs.moving = false;
            if (on("world.border_stop") != null) fire(borderEvent("world.border_stop", w, bs).data("size", (int) size));
        }
        bs.size = size;
    }

    // ------------------------------------------------------------------ effets / vie

    private static Map<String, Integer> effectMap(PlatformPlayer p) {
        Map<String, Integer> res = new HashMap<>();
        for (EffectInfo e : p.getEffects()) res.put(e.getName(), e.getAmplifier());
        return res;
    }

    private void effects(PlatformPlayer p, State st) {
        Map<String, Integer> now = effectMap(p);
        for (Map.Entry<String, Integer> e : now.entrySet()) {
            Integer old = st.effects.get(e.getKey());
            if (old == null || e.getValue() > old) effectGained(p, e.getKey(), e.getValue() + 1);
        }
        for (String old : st.effects.keySet()) {
            if (!now.containsKey(old)) {
                EventSettings s = on("player.effect_lose");
                if (s != null && !s.names("effects").isEmpty() && s.accepts("effects", old)) {
                    fire(ev("player.effect_lose", p).data("effect", pretty(old)));
                }
            }
        }
        st.effects = now;
    }

    private void effectGained(PlatformPlayer p, String effect, int level) {
        EffectInfo pe = null;
        for (EffectInfo e : p.getEffects()) if (e.getName().equals(effect)) pe = e;
        String duration = pe == null ? "?" : pe.getDuration() < 0 ? "∞" : Text.duration(pe.getDuration() / 20);
        EventSettings s = on("player.effect_gain");
        if (s != null && !s.names("effects").isEmpty() && s.accepts("effects", effect)) {
            fire(effectEvent("player.effect_gain", p, effect, level, duration));
        }
        String cat = POSITIVE.contains(effect) ? "potion.positive" : "potion.negative";
        s = on(cat);
        if (s != null && matchesLeveled(s.names("effects"), Collections.singleton(effect), level)) {
            fire(effectEvent(cat, p, effect, level, duration));
        }
        String specific = null;
        switch (effect) {
            case "STRENGTH":
                specific = "potion.strength";
                break;
            case "SPEED":
                specific = "potion.speed";
                break;
            case "RESISTANCE":
                specific = "potion.resistance";
                break;
            case "REGENERATION":
                specific = "potion.regeneration";
                break;
            case "POISON":
                specific = "potion.poison";
                break;
            case "WEAKNESS":
                specific = "potion.weakness";
                break;
            case "SLOWNESS":
                specific = "potion.slowness";
                break;
            default:
                break;
        }
        if (specific != null) {
            s = on(specific);
            if (s != null && level >= s.integer("min-level", 1)) fire(effectEvent(specific, p, effect, level, duration));
        }
    }

    private GameEvent.Builder effectEvent(String id, PlatformPlayer p, String effect, int level, String duration) {
        return ev(id, p).data("effect", pretty(effect)).data("effect_level", level).data("effect_duration", duration)
                .data("level", level);
    }

    private void absorption(PlatformPlayer p, State st) {
        double now = p.getAbsorption();
        if (st.absorption <= 0 && now > 0) {
            EventSettings s = on("player.absorption_gain");
            if (s != null && now / 2.0 >= s.number("min-hearts", 0)) {
                fire(ev("player.absorption_gain", p).data("hearts", Text.hearts(now)).data("absorption", Text.hearts(now)));
            }
        } else if (st.absorption > 0 && now <= 0 && on("player.absorption_lose") != null) {
            fire(ev("player.absorption_lose", p));
        }
        st.absorption = now;
    }

    private void health(PlatformPlayer p, State st) {
        double prev = st.health / 2.0, now = p.getHealth() / 2.0;
        st.health = p.getHealth();
        if (p.isDead() || now <= 0) return;
        EventSettings s = on("player.low_health");
        if (s != null) {
            for (Double t : s.doubles("hearts")) {
                if (prev >= t && now < t) fire(ev("player.low_health", p).data("hearts", Text.oneDecimal(now)).data("threshold", t));
            }
        }
        s = on("player.health_recovered");
        if (s != null) {
            for (Double t : s.doubles("hearts")) {
                if (prev < t && now >= t) fire(ev("player.health_recovered", p).data("hearts", Text.oneDecimal(now)).data("threshold", t));
            }
        }
    }

    // ------------------------------------------------------------------ armure / objets

    private static String[] armorNames(PlatformPlayer p) {
        ItemRef[] armor = p.getArmorContents();
        String[] res = new String[4];
        for (int i = 0; i < 4 && i < armor.length; i++) res[i] = empty(armor[i]) ? null : armor[i].getType();
        return res;
    }

    private static String fullSet(String[] armor) {
        String prefix = null;
        for (String a : armor) {
            if (a == null || a.indexOf('_') < 0) return null;
            String pre = a.substring(0, a.lastIndexOf('_'));
            if (prefix == null) prefix = pre;
            else if (!prefix.equals(pre)) return null;
        }
        return prefix;
    }

    private void armor(PlatformPlayer p, State st) {
        String[] now = armorNames(p);
        EventSettings s = on("player.armor_equip");
        for (int i = 0; i < 4; i++) {
            if (now[i] != null && !now[i].equals(st.armor[i]) && s != null && !s.names("items").isEmpty() && s.accepts("items", now[i])) {
                fire(ev("player.armor_equip", p).data("item", pretty(now[i])).data("item_type", now[i]));
            }
        }
        String set = fullSet(now);
        if (set != null && !set.equals(st.fullSet)) {
            s = on("player.armor_full");
            if (s != null && s.accepts("types", set)) fire(ev("player.armor_full", p).data("armor", pretty(set)).data("item", pretty(set)));
        }
        st.armor = now;
        st.fullSet = set;
    }

    private void amounts(PlatformPlayer p) {
        EventSettings s = on("craft.amount");
        if (s == null) return;
        List<String> entries = s.names("items");
        if (entries.isEmpty()) return;
        Map<String, Integer> counts = new HashMap<>();
        for (ItemRef it : p.getStorageContents()) {
            if (empty(it)) continue;
            String t = it.getType();
            Integer c = counts.get(t);
            counts.put(t, (c == null ? 0 : c) + it.getAmount());
        }
        for (String entry : entries) {
            String[] parts = entry.split(":");
            if (parts.length < 2) continue;
            int amount;
            try {
                amount = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                continue;
            }
            Integer c = counts.get(parts[0]);
            if (c != null && c >= amount && plugin.stats().first(p.getUniqueId(), "amount." + entry)) {
                fire(ev("craft.amount", p).data("item", pretty(parts[0])).data("item_type", parts[0])
                        .data("item_amount", c).data("amount", amount));
            }
        }
    }

    // ------------------------------------------------------------------ AFK

    private void afk(PlatformPlayer p, State st, Position loc) {
        Position last = st.lastLocation;
        boolean moved = last == null || !last.sameWorld(loc)
                || last.distanceSquared(loc) > 0.01 || last.getYaw() != loc.getYaw() || last.getPitch() != loc.getPitch();
        long now = System.currentTimeMillis();
        if (moved) {
            st.lastMove = now;
            if (st.afk) {
                st.afk = false;
                if (on("player.afk_back") != null) fire(ev("player.afk_back", p));
            }
            return;
        }
        EventSettings s = on("player.afk");
        int seconds = plugin.events().settings("player.afk").integer("seconds", 120);
        if (!st.afk && now - st.lastMove >= seconds * 1000L) {
            st.afk = true;
            if (s != null) fire(ev("player.afk", p).data("seconds", seconds).data("duration", Text.duration(seconds)));
        }
    }

    // ------------------------------------------------------------------ structures

    private void structures(PlatformPlayer p, Position loc) {
        EventSettings s = on("world.structure");
        if (s == null) return;
        for (String name : p.getWorld().getStructuresAt(loc)) {
            if (!s.accepts("structures", name)) continue;
            if (plugin.stats().first(p.getUniqueId(), "structure." + name)) {
                fire(ev("world.structure", p).data("structure", pretty(name)));
            }
        }
    }
}
