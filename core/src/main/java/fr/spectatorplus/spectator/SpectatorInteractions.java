package fr.spectatorplus.spectator;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.api.EnterReason;
import fr.spectatorplus.api.SpectatorMode;
import fr.spectatorplus.api.SpectatorState;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.PlatformWorld;
import fr.spectatorplus.core.platform.Position;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Réactions du mode spectateur aux actions du joueur, communes à toutes les plateformes :
 * barre d'inventaire, clic sur un joueur, chute dans le vide, POV, mort → spectateur (modes auto).
 * Les plateformes bloquent elles-mêmes les actions interdites puis appellent ces méthodes.
 */
public final class SpectatorInteractions {

    private final SpectatorCore plugin;
    private final Map<UUID, Position> pendingDeaths = new HashMap<>();

    public SpectatorInteractions(SpectatorCore plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ interactions

    /** Clic (gauche ou droit) avec un objet de la barre d'inventaire. */
    public void useHotbar(PlatformPlayer p, boolean leftClick) {
        SpectatorSession s = plugin.spectators().getSpectator(p);
        if (s == null) return;
        String action = s.getHotbarAction(p.getHeldSlot());
        if (action == null) return;
        plugin.hotbar().use(p, s, action, leftClick, p.isSneaking());
    }

    /** Clic droit sur un joueur : suivi (objet « follow » en main) ou action configurée. */
    public void rightClickPlayer(PlatformPlayer p, PlatformPlayer target) {
        SpectatorSession s = plugin.spectators().getSpectator(p);
        if (s == null || target == null || plugin.spectators().isSpectator(target)) return;
        String action = s.getHotbarAction(p.getHeldSlot());
        if ("follow".equals(action)) {
            plugin.spectators().follow(s, target);
            return;
        }
        String click = plugin.config().getString("spectator.right-click-player", "INVENTORY").toUpperCase(Locale.ROOT);
        if (click.equals("SHEET")) s.openInspection(target);
        else if (click.equals("FOLLOW")) plugin.spectators().follow(s, target);
        else if (!click.equals("NONE")) s.openInventory(target, false);
    }

    /** Clic gauche (attaque) sur un joueur : fiche du joueur. */
    public void leftClickPlayer(PlatformPlayer p, PlatformPlayer target) {
        if (target == null || plugin.spectators().isSpectator(target)) return;
        if (!plugin.config().getBoolean("spectator.left-click-opens-sheet", true)) return;
        SpectatorSession s = plugin.spectators().getSpectator(p);
        if (s != null && s.getMovementState() != SpectatorState.POV) s.openInspection(target);
    }

    /** Le spectateur tombe dans le vide : on le remonte au-dessus du sol. */
    public void voidDamage(PlatformPlayer p) {
        Position l = p.getLocation();
        PlatformWorld w = p.getWorld();
        p.teleport(l.withY(w.getHighestBlockYAt(l.getBlockX(), l.getBlockZ()) + 5));
    }

    /** Le joueur commence à s'accroupir (sortie du POV). */
    public void sneak(PlatformPlayer p) {
        SpectatorSession s = plugin.spectators().getSpectator(p);
        if (s != null && s.getMovementState() == SpectatorState.POV) plugin.spectators().requestPovExit(s);
    }

    /** Changement de monde : le vol est réappliqué (le serveur le réinitialise). */
    public void worldChanged(final PlatformPlayer p) {
        if (!plugin.spectators().isSpectator(p)) return;
        plugin.platform().runLater(new Runnable() {
            @Override
            public void run() {
                SpectatorSession s = plugin.spectators().getSpectator(p);
                if (p.isOnline() && s != null && s.getMovementState() != SpectatorState.POV) {
                    p.setAllowFlight(true);
                    p.setFlying(true);
                    plugin.spectators().applyFlySpeed(p);
                }
            }
        }, 2L);
    }

    /** Un spectateur gelé ne peut pas se déplacer. */
    public boolean isFrozen(PlatformPlayer p) {
        SpectatorSession s = plugin.spectators().getSpectator(p);
        return s != null && s.isFrozen();
    }

    // ------------------------------------------------------------------ mort / réapparition (modes auto)

    /**
     * Mort d'un joueur.
     *
     * @return true si la plateforme doit faire réapparaître le joueur immédiatement
     */
    public boolean death(PlatformPlayer p) {
        if (plugin.spectators().isSpectator(p)) {
            pendingDeaths.put(p.getUniqueId(), p.getLocation());
            return true;
        }
        if (plugin.getMode() == SpectatorMode.MANUAL || !plugin.config().getBoolean("auto.on-death", true)) return false;
        pendingDeaths.put(p.getUniqueId(), p.getLocation());
        return plugin.config().getBoolean("auto.instant-respawn", true);
    }

    /** Le joueur est-il en attente de réapparition après une mort gérée par Spectator Plus ? */
    public boolean hasPendingDeath(UUID id) {
        return pendingDeaths.containsKey(id);
    }

    /**
     * Réapparition. Passe le joueur en spectateur au tick suivant si sa mort a été prise en charge.
     *
     * @return le lieu de réapparition à imposer (spectateur « mort » : là où il était), ou null
     */
    public Position respawn(final PlatformPlayer p) {
        Position death = pendingDeaths.remove(p.getUniqueId());
        if (death == null) return null;
        Position override = null;
        if (plugin.spectators().isSpectator(p) && death.getWorld() != null) {
            // un spectateur « mort » (commande /kill...) réapparaît là où il était
            PlatformWorld w = plugin.platform().getWorld(death.getWorld());
            override = death;
            if (w != null && death.getY() < w.getMinHeight() + 1) {
                override = death.withY(w.getHighestBlockYAt(death.getBlockX(), death.getBlockZ()) + 3);
            }
        }
        final Position target = plugin.config().getBoolean("auto.teleport-to-death-location", true) ? death : null;
        plugin.platform().runLater(new Runnable() {
            @Override
            public void run() {
                if (!p.isOnline()) return;
                SpectatorSession s = plugin.spectators().getSpectator(p);
                if (s != null) {
                    plugin.spectators().apply(p);
                    plugin.hotbar().give(p, s);
                    return;
                }
                plugin.spectators().enter(p, EnterReason.DEATH, target, plugin.getMode() == SpectatorMode.AUTO);
            }
        }, 1L);
        return override;
    }

    public void quit(UUID id) {
        pendingDeaths.remove(id);
    }
}
