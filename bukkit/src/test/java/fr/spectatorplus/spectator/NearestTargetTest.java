package fr.spectatorplus.spectator;

import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.Position;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Clic gauche sur l'œil sans cible : suivre le joueur en vie le plus proche. */
class NearestTargetTest {

    private static final Position SPECTATOR = new Position("uhc", 0, 64, 0);

    @Test
    void choisitLePlusProcheDuMemeMonde() {
        PlatformPlayer loin = FakePlayers.at("Loin", "uhc", 300, 0);
        PlatformPlayer proche = FakePlayers.at("Proche", "uhc", 20, -10);
        PlatformPlayer nether = FakePlayers.at("Nether", "uhc_nether", 1, 1);
        assertSame(proche, SpectatorManager.nearest(SPECTATOR, Arrays.asList(loin, nether, proche)));
    }

    @Test
    void prendUnAutreMondeSiPersonneIci() {
        PlatformPlayer nether = FakePlayers.at("Nether", "uhc_nether", 1, 1);
        assertSame(nether, SpectatorManager.nearest(SPECTATOR, Collections.singletonList(nether)));
    }

    @Test
    void personneASuivre() {
        assertNull(SpectatorManager.nearest(SPECTATOR, Collections.<PlatformPlayer>emptyList()));
    }
}
