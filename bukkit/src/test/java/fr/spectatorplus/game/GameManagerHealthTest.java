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

    @Test
    void joueurEnVieSelonLePluginDeJeu() {
        GameManager game = new GameManager(null);
        final java.util.UUID vivant = java.util.UUID.randomUUID();
        final java.util.UUID staff = java.util.UUID.randomUUID();
        game.setManagedExternally(true);
        game.setExternalGame(new GameManager.ExternalGame() {
            @Override
            public boolean isPlaying(java.util.UUID player) {
                return player.equals(vivant);
            }

            @Override
            public boolean eliminate(java.util.UUID player) {
                return false;
            }

            @Override
            public void releasePlaying(fr.spectatorplus.core.platform.PlatformPlayer player) {
            }

            @Override
            public String getPublicName(fr.spectatorplus.core.platform.PlatformPlayer player) {
                return player.getName();
            }
        });

        assertTrue(game.isAliveInGame(vivant), "en vie : ne voit pas les spectateurs");
        assertFalse(game.isAliveInGame(staff), "staff qui ne joue pas : les voit avec la permission");
    }

    @Test
    void personneEnVieSansPartie() {
        assertFalse(new GameManager(null).isAliveInGame(java.util.UUID.randomUUID()));
    }
}
