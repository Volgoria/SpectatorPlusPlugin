package fr.spectatorplus.event.detector;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.compat.Compat;
import fr.spectatorplus.compat.Mat;
import fr.spectatorplus.compat.Reflect;
import fr.spectatorplus.event.EventSettings;
import fr.spectatorplus.event.Zone;
import fr.spectatorplus.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Détections qui n'ont pas d'évènement Bukkit : analyse périodique de l'état des joueurs.
 * (zones, coordonnées, biomes, altitude, spawn, bordure, effets, absorption, vie, armure, objets, AFK, structures)
 */
public final class PollingDetector extends Detector {

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
        Location lastLocation;
        long lastMove = System.currentTimeMillis();
        boolean afk;
        int tick;
    }

    private static final class BorderState {
        double size = -1;
        boolean moving;
        int stable;
    }

    private final Map<UUID, State> states = new HashMap<>();
    private final Map<String, BorderState> borders = new HashMap<>();
    private boolean structuresSupported = true;

    public PollingDetector(SpectatorPlus plugin) {
        super(plugin);
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!tracked(p)) {
                        states.remove(p.getUniqueId());
                        continue;
                    }
                    try {
                        poll(p);
                    } catch (Throwable t) {
                        plugin.getLogger().log(java.util.logging.Level.FINE, "Polling error", t);
                    }
                }
            }
        }, 20L, 10L);
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                for (World w : Bukkit.getWorlds()) border(w);
            }
        }, 40L, 20L);
    }

    public void reset() {
        states.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        states.remove(e.getPlayer().getUniqueId());
    }

    private void poll(Player p) {
        State st = states.get(p.getUniqueId());
        if (st == null) {
            st = new State();
            states.put(p.getUniqueId(), st);
        }
        Location loc = p.getLocation();
        st.tick++;
        boolean first = !st.initialized;
        if (st.world != null && !st.world.equals(loc.getWorld().getName())) {
            // changement de monde : on repart de zéro pour la position (pas d'évènements de franchissement)
            st.spawnDistance = -1;
            st.y = loc.getY();
        }
        st.world = loc.getWorld().getName();

        if (!first) {
            zones(p, st, loc);
            coordinates(p, loc);
            biome(p, st, loc);
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
            st.biome = Compat.biomeName(loc);
            st.outsideBorder = Compat.isOutsideBorder(loc);
            st.effects = effectMap(p);
            st.absorption = Compat.absorption(p);
            st.health = p.getHealth();
            st.armor = armorNames(p);
            st.fullSet = fullSet(st.armor);
            st.initialized = true;
        }
        st.y = loc.getY();
        st.lastLocation = loc;
    }

    // ------------------------------------------------------------------ position

    private void zones(Player p, State st, Location loc) {
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

    private void coordinates(Player p, Location loc) {
        EventSettings s = on("world.coordinate");
        if (s == null) return;
        List<Map<?, ?>> targets = s.section().getMapList("targets");
        for (int i = 0; i < targets.size(); i++) {
            Map<?, ?> t = targets.get(i);
            Object world = t.get("world");
            if (world != null && !world.toString().equals(loc.getWorld().getName())) continue;
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

    private void biome(Player p, State st, Location loc) {
        if (st.tick % 4 != 0) return;
        String biome = Compat.biomeName(loc);
        if (biome.equals(st.biome)) return;
        st.biome = biome;
        EventSettings s = on("world.biome");
        if (s != null && !s.names("biomes").isEmpty() && s.accepts("biomes", biome)
                && plugin.stats().first(p.getUniqueId(), "biome." + biome)) {
            fire(ev("world.biome", p).data("biome", pretty(biome)));
        }
    }

    private void altitude(Player p, State st, Location loc) {
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

    private void spawn(Player p, State st, Location loc) {
        Location spawn = loc.getWorld().getSpawnLocation();
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

    private void borderCross(Player p, State st, Location loc) {
        boolean outside = Compat.isOutsideBorder(loc);
        if (outside && !st.outsideBorder && on("world.border_cross") != null) {
            fire(ev("world.border_cross", p).data("size", (int) loc.getWorld().getWorldBorder().getSize()));
        }
        st.outsideBorder = outside;
    }

    private void border(World w) {
        WorldBorder wb = w.getWorldBorder();
        double size = wb.getSize();
        BorderState bs = borders.get(w.getName());
        if (bs == null) {
            bs = new BorderState();
            bs.size = size;
            borders.put(w.getName(), bs);
            return;
        }
        boolean changed = Math.abs(size - bs.size) > 0.001;
        if (changed && !bs.moving) {
            bs.moving = true;
            if (on("world.border_start") != null) {
                fire(plugin.events().builder("world.border_start").location(wb.getCenter())
                        .data("world", w.getName()).data("size", (int) size)
                        .data("direction", size < bs.size ? "@placeholders.border-shrink" : "@placeholders.border-grow"));
            }
        }
        if (changed) {
            bs.stable = 0;
            EventSettings s = on("world.border_size");
            if (s != null) {
                for (Integer t : s.ints("sizes")) {
                    if ((bs.size > t && size <= t) || (bs.size < t && size >= t)) {
                        fire(plugin.events().builder("world.border_size").location(wb.getCenter())
                                .data("world", w.getName()).data("size", t));
                    }
                }
            }
        } else if (bs.moving && ++bs.stable >= 2) {
            bs.moving = false;
            if (on("world.border_stop") != null) {
                fire(plugin.events().builder("world.border_stop").location(wb.getCenter())
                        .data("world", w.getName()).data("size", (int) size));
            }
        }
        bs.size = size;
    }

    // ------------------------------------------------------------------ effets / vie

    private static Map<String, Integer> effectMap(Player p) {
        Map<String, Integer> res = new HashMap<>();
        for (PotionEffect e : p.getActivePotionEffects()) res.put(Compat.effectName(e.getType()), e.getAmplifier());
        return res;
    }

    private void effects(Player p, State st) {
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

    private void effectGained(Player p, String effect, int level) {
        PotionEffect pe = null;
        for (PotionEffect e : p.getActivePotionEffects()) {
            if (Compat.effectName(e.getType()).equals(effect)) pe = e;
        }
        String duration = pe == null ? "?" : pe.getDuration() < 0 ? "∞" : Text.duration(pe.getDuration() / 20);
        EventSettings s = on("player.effect_gain");
        if (s != null && !s.names("effects").isEmpty() && s.accepts("effects", effect)) {
            fire(effectEvent("player.effect_gain", p, effect, level, duration));
        }
        String cat = POSITIVE.contains(effect) ? "potion.positive" : "potion.negative";
        s = on(cat);
        if (s != null && matchesLeveled(s.names("effects"), java.util.Collections.singleton(effect), level)) {
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

    private fr.spectatorplus.api.event.SpectatorGameEvent.Builder effectEvent(String id, Player p, String effect, int level, String duration) {
        return ev(id, p).data("effect", pretty(effect)).data("effect_level", level).data("effect_duration", duration)
                .data("level", level);
    }

    private void absorption(Player p, State st) {
        double now = Compat.absorption(p);
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

    private void health(Player p, State st) {
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

    private static String[] armorNames(Player p) {
        ItemStack[] armor = p.getInventory().getArmorContents();
        String[] res = new String[4];
        for (int i = 0; i < 4 && i < armor.length; i++) res[i] = Mat.isAir(armor[i]) ? null : armor[i].getType().name();
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

    private void armor(Player p, State st) {
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

    private void amounts(Player p) {
        EventSettings s = on("craft.amount");
        if (s == null) return;
        List<String> entries = s.names("items");
        if (entries.isEmpty()) return;
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack it : Compat.storageContents(p)) {
            if (Mat.isAir(it)) continue;
            String t = it.getType().name();
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

    private void afk(Player p, State st, Location loc) {
        Location last = st.lastLocation;
        boolean moved = last == null || !last.getWorld().equals(loc.getWorld())
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

    // ------------------------------------------------------------------ structures (1.19+)

    private void structures(Player p, Location loc) {
        if (!structuresSupported) return;
        EventSettings s = on("world.structure");
        if (s == null) return;
        Object result = Reflect.invoke(loc.getWorld(), "getStructures", loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        if (result == null) {
            if (!Reflect.hasMethod(loc.getWorld(), "getStructures", 2)) structuresSupported = false;
            return;
        }
        if (!(result instanceof Iterable)) return;
        for (Object gs : (Iterable<?>) result) {
            Object box = Reflect.invoke(gs, "getBoundingBox");
            Object inside = Reflect.invoke(box, "contains", loc.getX(), loc.getY(), loc.getZ());
            if (!Boolean.TRUE.equals(inside)) continue;
            Object structure = Reflect.invoke(gs, "getStructure");
            String name = Compat.enumName(structure).toUpperCase(Locale.ROOT);
            if (!s.accepts("structures", name)) continue;
            if (plugin.stats().first(p.getUniqueId(), "structure." + name)) {
                fire(ev("world.structure", p).data("structure", pretty(name)));
            }
        }
    }
}
