package fr.spectatorplus.compat;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.api.SpectatorState;
import fr.spectatorplus.spectator.SpectatorSession;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

import java.util.ArrayList;
import java.util.List;

/**
 * Améliorations basées sur l'API Paper. Chaque évènement n'est enregistré que s'il existe
 * sur le serveur : rien en 1.8 (PaperSpigot n'a pas ces évènements), de plus en plus sur les versions récentes.
 * Sur Spigot, rien n'est enregistré et le plugin fonctionne avec ses mécanismes génériques.
 */
public final class PaperHooks {

    private final SpectatorPlus plugin;
    private final List<String> active = new ArrayList<>();

    public PaperHooks(SpectatorPlus plugin) {
        this.plugin = plugin;
    }

    private boolean spec(Object o) {
        return o instanceof Player && plugin.spectators().isSpectator((Player) o);
    }

    private void cancelIf(String className, String label, final String getter) {
        boolean ok = DynamicEvents.register(plugin, className, EventPriority.LOWEST, false, new DynamicEvents.Handler() {
            @Override
            public void handle(Event event) {
                if (spec(Reflect.invoke(event, getter))) ((Cancellable) event).setCancelled(true);
            }
        });
        if (ok) active.add(label);
    }

    public void register() {
        if (!Platform.isPaper()) return;

        // Pas de caméra vanilla (clic gauche sur une entité) pendant la traversée d'un bloc : seul le POV du plugin l'utilise
        boolean ok = DynamicEvents.register(plugin, "com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent",
                EventPriority.LOWEST, false, new DynamicEvents.Handler() {
                    @Override
                    public void handle(Event event) {
                        Object p = Reflect.invoke(event, "getPlayer");
                        if (!(p instanceof Player)) return;
                        SpectatorSession s = plugin.spectators().getSpectator((Player) p);
                        if (s != null && s.getMovementState() != SpectatorState.POV) ((Cancellable) event).setCancelled(true);
                    }
                });
        if (ok) active.add("spectate-lock");

        // Les projectiles ne heurtent plus les spectateurs (1.9 → 1.20)
        ok = DynamicEvents.register(plugin, "com.destroystokyo.paper.event.entity.ProjectileCollideEvent",
                EventPriority.LOWEST, false, new DynamicEvents.Handler() {
                    @Override
                    public void handle(Event event) {
                        Object hit = Reflect.invoke(event, "getCollidedWith");
                        if (hit instanceof Entity && spec(hit)) ((Cancellable) event).setCancelled(true);
                    }
                });
        if (ok) active.add("projectile-collide");

        // Un spectateur ne débloque pas de progrès (entrée dans un biome, une structure...)
        cancelIf("com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent", "advancements", "getPlayer");
        // Pas de pioche au clic molette (1.21.4+), pas de tonte de blocs, pas de récupération d'XP
        cancelIf("io.papermc.paper.event.player.PlayerPickItemEvent", "pick-item", "getPlayer");
        cancelIf("io.papermc.paper.event.block.PlayerShearBlockEvent", "shear-block", "getPlayer");
        cancelIf("com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent", "pickup-xp", "getPlayer");
        cancelIf("com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent", "launch", "getPlayer");
        cancelIf("io.papermc.paper.event.player.PlayerItemFrameChangeEvent", "item-frame", "getPlayer");
    }

    public List<String> getActive() {
        return active;
    }
}
