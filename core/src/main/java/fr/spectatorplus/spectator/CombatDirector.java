package fr.spectatorplus.spectator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Suivi automatique des combats : choisit le combat à montrer à un spectateur.
 * <ul>
 *     <li>un combat est « en cours » tant que le dernier coup date de moins de {@code activeMillis} ;</li>
 *     <li>on reste sur la cible actuelle si elle se bat encore, ou si on la regarde depuis moins de
 *     {@code minWatchMillis} (pas de va-et-vient entre deux combats) ;</li>
 *     <li>sinon on passe au combat le plus récent, du côté du joueur qui a frappé en dernier.</li>
 * </ul>
 * Aucune dépendance à la plateforme : l'heure est passée en paramètre (testable).
 */
public final class CombatDirector {

    /** Combat entre deux joueurs. */
    static final class Fight {
        final UUID a;
        final UUID b;
        long lastHit;
        UUID lastAttacker;

        Fight(UUID a, UUID b) {
            this.a = a;
            this.b = b;
        }

        boolean involves(UUID id) {
            return a.equals(id) || b.equals(id);
        }

        UUID other(UUID id) {
            return a.equals(id) ? b : a;
        }
    }

    /** Joueurs acceptés comme cible (en vie, connectés). */
    public interface Eligibility {
        boolean isEligible(UUID player);
    }

    private final Map<String, Fight> fights = new HashMap<>();
    private long activeMillis = 6000;
    private long minWatchMillis = 5000;

    public void configure(long activeMillis, long minWatchMillis) {
        this.activeMillis = Math.max(1000, activeMillis);
        this.minWatchMillis = Math.max(0, minWatchMillis);
    }

    private static String key(UUID a, UUID b) {
        return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
    }

    /** Un joueur en frappe un autre. */
    public void hit(UUID attacker, UUID victim, long now) {
        String k = key(attacker, victim);
        Fight f = fights.get(k);
        if (f == null) {
            f = new Fight(attacker, victim);
            fights.put(k, f);
        }
        f.lastHit = now;
        f.lastAttacker = attacker;
    }

    /** Le joueur est mort ou parti : ses combats sont terminés. */
    public void forget(UUID player) {
        Iterator<Fight> it = fights.values().iterator();
        while (it.hasNext()) {
            if (it.next().involves(player)) it.remove();
        }
    }

    public void reset() {
        fights.clear();
    }

    /** Retire les combats terminés depuis longtemps. */
    public void purge(long now) {
        Iterator<Fight> it = fights.values().iterator();
        while (it.hasNext()) {
            if (now - it.next().lastHit > activeMillis * 10) it.remove();
        }
    }

    public boolean isFighting(UUID player, long now) {
        for (Fight f : fights.values()) {
            if (f.involves(player) && now - f.lastHit <= activeMillis) return true;
        }
        return false;
    }

    /**
     * @param current      cible actuelle du spectateur, ou null
     * @param watchedSince début du suivi de la cible actuelle (millisecondes)
     * @return le joueur à suivre, ou null pour ne rien changer
     */
    public UUID choose(UUID current, long watchedSince, long now, Eligibility eligible) {
        if (current != null && (isFighting(current, now) || now - watchedSince < minWatchMillis)) return null;
        Fight best = null;
        for (Fight f : fights.values()) {
            if (now - f.lastHit > activeMillis) continue;
            if (!eligible.isEligible(f.a) && !eligible.isEligible(f.b)) continue;
            if (best == null || f.lastHit > best.lastHit) best = f;
        }
        if (best == null) return null;
        UUID target = best.lastAttacker != null && eligible.isEligible(best.lastAttacker)
                ? best.lastAttacker : eligible.isEligible(best.a) ? best.a : best.b;
        if (!eligible.isEligible(target)) target = best.other(target);
        return target.equals(current) ? null : target;
    }

    /** Adversaire le plus récent de ce joueur dans un combat en cours, ou null. */
    public UUID opponent(UUID player, long now) {
        Fight best = null;
        for (Fight f : fights.values()) {
            if (f.involves(player) && now - f.lastHit <= activeMillis && (best == null || f.lastHit > best.lastHit)) {
                best = f;
            }
        }
        return best == null ? null : best.other(player);
    }
}
