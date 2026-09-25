package fr.spectatorplus.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Suivi des combats joueur contre joueur (qui a frappé qui et quand).
 */
public final class CombatTracker {

    public static final class Combat {
        public final UUID a;
        public final UUID b;
        public final long start;
        public long lastHit;

        Combat(UUID a, UUID b, long now) {
            this.a = a;
            this.b = b;
            this.start = now;
            this.lastHit = now;
        }
    }

    public static final class HitResult {
        public boolean firstHitEver;
        public boolean combatStarted;
    }

    private final Map<UUID, Map<UUID, Long>> damagers = new HashMap<>();
    private final Map<String, Combat> combats = new HashMap<>();
    private final Set<String> pairsEver = new HashSet<>();
    private final Map<UUID, Long> lastCombat = new HashMap<>();

    private static String key(UUID a, UUID b) {
        return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
    }

    public HitResult hit(UUID attacker, UUID victim, long combatTimeoutMs) {
        long now = System.currentTimeMillis();
        HitResult r = new HitResult();
        Map<UUID, Long> m = damagers.get(victim);
        if (m == null) {
            m = new HashMap<>();
            damagers.put(victim, m);
        }
        m.put(attacker, now);
        String k = key(attacker, victim);
        r.firstHitEver = pairsEver.add(k);
        Combat c = combats.get(k);
        if (c == null || now - c.lastHit > combatTimeoutMs) {
            combats.put(k, new Combat(attacker, victim, now));
            r.combatStarted = true;
        } else {
            c.lastHit = now;
        }
        lastCombat.put(attacker, now);
        lastCombat.put(victim, now);
        return r;
    }

    /** Dernier joueur ayant frappé la victime dans le délai, ou null. */
    public UUID lastAttacker(UUID victim, long withinMs) {
        Map<UUID, Long> m = damagers.get(victim);
        if (m == null) return null;
        long now = System.currentTimeMillis();
        UUID best = null;
        long bestTime = 0;
        for (Map.Entry<UUID, Long> e : m.entrySet()) {
            if (now - e.getValue() <= withinMs && e.getValue() > bestTime) {
                best = e.getKey();
                bestTime = e.getValue();
            }
        }
        return best;
    }

    /** Joueurs (hors tueur) ayant frappé la victime dans le délai. */
    public List<UUID> assists(UUID victim, UUID killer, long withinMs) {
        List<UUID> res = new ArrayList<>();
        Map<UUID, Long> m = damagers.get(victim);
        if (m == null) return res;
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> e : m.entrySet()) {
            if (!e.getKey().equals(killer) && now - e.getValue() <= withinMs) res.add(e.getKey());
        }
        return res;
    }

    public long lastCombatTime(UUID player) {
        Long l = lastCombat.get(player);
        return l == null ? 0 : l;
    }

    /** Retire et renvoie les combats terminés. */
    public List<Combat> expired(long timeoutMs) {
        List<Combat> res = new ArrayList<>();
        long now = System.currentTimeMillis();
        Iterator<Combat> it = combats.values().iterator();
        while (it.hasNext()) {
            Combat c = it.next();
            if (now - c.lastHit > timeoutMs) {
                res.add(c);
                it.remove();
            }
        }
        return res;
    }

    /** Termine tous les combats impliquant ce joueur (mort, déconnexion). */
    public List<Combat> endAll(UUID player) {
        List<Combat> res = new ArrayList<>();
        Iterator<Combat> it = combats.values().iterator();
        while (it.hasNext()) {
            Combat c = it.next();
            if (c.a.equals(player) || c.b.equals(player)) {
                res.add(c);
                it.remove();
            }
        }
        return res;
    }

    public void clearVictim(UUID victim) {
        damagers.remove(victim);
    }

    public void reset() {
        damagers.clear();
        combats.clear();
        pairsEver.clear();
        lastCombat.clear();
    }
}
