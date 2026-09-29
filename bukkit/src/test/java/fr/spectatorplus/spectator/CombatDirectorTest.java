package fr.spectatorplus.spectator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Suivi automatique des combats : choix du combat montré au spectateur. */
class CombatDirectorTest {

    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();
    private final UUID carl = UUID.randomUUID();
    private final UUID dana = UUID.randomUUID();
    private final Set<UUID> alive = new HashSet<>(Arrays.asList(alice, bob, carl, dana));
    private final CombatDirector.Eligibility eligible = new CombatDirector.Eligibility() {
        @Override
        public boolean isEligible(UUID player) {
            return alive.contains(player);
        }
    };
    private CombatDirector director;

    @BeforeEach
    void setUp() {
        director = new CombatDirector();
        director.configure(6000, 5000);
    }

    @Test
    void aucunCombatAucunChangement() {
        assertNull(director.choose(null, 0, 10_000, eligible));
        assertNull(director.choose(alice, 0, 10_000, eligible));
    }

    @Test
    void suitLAttaquantDuCombatEnCours() {
        director.hit(alice, bob, 1000);
        assertEquals(alice, director.choose(null, 0, 2000, eligible));
        assertEquals(bob, director.opponent(alice, 2000));
    }

    @Test
    void resteSurUneCibleQuiSeBatEncore() {
        director.hit(alice, bob, 5000);
        director.hit(carl, dana, 9000);
        assertNull(director.choose(alice, 0, 9500, eligible), "Alice se bat encore (dernier coup il y a 4,5 s, fenêtre de 6 s)");
        assertEquals(carl, director.choose(alice, 0, 12_000, eligible), "combat d'Alice fini : on passe à Carl");
    }

    @Test
    void pasDAllerRetourAvantLeTempsMinimal() {
        director.hit(carl, dana, 9000);
        assertNull(director.choose(alice, 8000, 10_000, eligible), "Alice regardée depuis 2 s seulement");
        assertEquals(carl, director.choose(alice, 4000, 10_000, eligible));
    }

    @Test
    void combatLePlusRecent() {
        director.hit(alice, bob, 1000);
        director.hit(dana, carl, 3000);
        assertEquals(dana, director.choose(null, 0, 4000, eligible));
    }

    @Test
    void combatTerminéIgnoré() {
        director.hit(alice, bob, 1000);
        assertNull(director.choose(null, 0, 8000, eligible));
    }

    @Test
    void attaquantMortOnSuitLAutre() {
        director.hit(alice, bob, 1000);
        alive.remove(alice);
        assertEquals(bob, director.choose(null, 0, 2000, eligible));
        director.forget(alice);
        assertNull(director.choose(null, 0, 2000, eligible));
    }

    @Test
    void dejaSurLaBonneCible() {
        director.hit(alice, bob, 1000);
        assertNull(director.choose(alice, 0, 20_000, eligible), "combat fini, rien d'autre : on reste");
        director.hit(bob, alice, 20_000);
        assertNull(director.choose(bob, 0, 20_500, eligible));
    }
}
