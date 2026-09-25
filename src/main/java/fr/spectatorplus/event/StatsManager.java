package fr.spectatorplus.event;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Compteurs de partie par joueur (blocs minés, kills, morts, premières fois...).
 * Réinitialisés au début de chaque partie.
 */
public final class StatsManager {

    public static final class PlayerStats {
        final Map<String, Integer> counters = new HashMap<>();
        final Set<String> flags = new HashSet<>();
        final Map<UUID, Integer> killsOn = new HashMap<>();
        final Deque<Long> recentKills = new ArrayDeque<>();
        int streak;
        long lastTotem;
    }

    private final Map<UUID, PlayerStats> stats = new HashMap<>();
    private final Set<String> globalFlags = new HashSet<>();

    public PlayerStats get(UUID id) {
        PlayerStats s = stats.get(id);
        if (s == null) {
            s = new PlayerStats();
            stats.put(id, s);
        }
        return s;
    }

    public void reset() {
        stats.clear();
        globalFlags.clear();
    }

    /** @return true la première fois que ce drapeau est posé pour ce joueur */
    public boolean first(UUID id, String flag) {
        return get(id).flags.add(flag);
    }

    /** @return true la première fois que ce drapeau est posé pour tout le serveur */
    public boolean firstGlobal(String flag) {
        return globalFlags.add(flag);
    }

    public int increment(UUID id, String key, int amount) {
        PlayerStats s = get(id);
        Integer v = s.counters.get(key);
        int n = (v == null ? 0 : v) + amount;
        s.counters.put(key, n);
        return n;
    }

    public int count(UUID id, String key) {
        Integer v = get(id).counters.get(key);
        return v == null ? 0 : v;
    }

    public int killOn(UUID killer, UUID victim) {
        PlayerStats s = get(killer);
        Integer v = s.killsOn.get(victim);
        int n = (v == null ? 0 : v) + 1;
        s.killsOn.put(victim, n);
        return n;
    }

    /** Enregistre un kill et renvoie le nombre de kills dans la fenêtre donnée (ms). */
    public int recentKills(UUID killer, long windowMs) {
        PlayerStats s = get(killer);
        long now = System.currentTimeMillis();
        s.recentKills.addLast(now);
        while (!s.recentKills.isEmpty() && now - s.recentKills.peekFirst() > windowMs) s.recentKills.removeFirst();
        return s.recentKills.size();
    }

    public int incrementStreak(UUID id) {
        return ++get(id).streak;
    }

    /** Remet la série à zéro et renvoie l'ancienne valeur. */
    public int resetStreak(UUID id) {
        PlayerStats s = get(id);
        int old = s.streak;
        s.streak = 0;
        return old;
    }

    public int streak(UUID id) {
        return get(id).streak;
    }

    public void totemUsed(UUID id) {
        get(id).lastTotem = System.currentTimeMillis();
    }

    public long lastTotem(UUID id) {
        return get(id).lastTotem;
    }

    public int kills(UUID id) {
        return count(id, "kills");
    }

    public int deaths(UUID id) {
        return count(id, "deaths");
    }
}
