package fr.spectatorplus.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vie cachée par un plugin de jeu (UHCCore et SelfDiagnosis) : plus aucune valeur ne doit être affichée.
 */
class GameManagerHealthTest {

    @Test
    void vieAfficheeSansPluginDeJeu() {
        GameManager game = new GameManager(null);
        assertTrue(game.isHealthVisible());
        assertEquals("7.5", game.hearts(15));
    }

    @Test
    void vieCacheeParLePluginDeJeu() {
        GameManager game = new GameManager(null);
        final boolean[] visible = {false};
        game.setHealthVisibility(new GameManager.HealthVisibility() {
            @Override
            public boolean isHealthVisible() {
                return visible[0];
            }
        });
        assertFalse(game.isHealthVisible());
        assertEquals("?", game.hearts(15));
        visible[0] = true;
        assertEquals("7.5", game.hearts(15));
    }

    @Test
    void gestionExterneDesactiveeParDefaut() {
        GameManager game = new GameManager(null);
        assertFalse(game.isManagedExternally());
        game.setManagedExternally(true);
        assertTrue(game.isManagedExternally());
    }
}
