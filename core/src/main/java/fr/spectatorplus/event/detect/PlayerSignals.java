package fr.spectatorplus.event.detect;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.core.platform.ItemRef;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.event.EventSettings;
import fr.spectatorplus.util.Text;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Évènements liés aux joueurs (dégâts, consommables, XP, connexion...).
 * Les seuils de vie, effets, armures et AFK sont gérés par {@link PollingSignals}.
 */
public final class PlayerSignals extends Detection {

    private final Set<UUID> leftOnce = new HashSet<>();

    public PlayerSignals(SpectatorCore plugin) {
        super(plugin);
    }

    /** Totem d'immortalité utilisé. */
    public void totem(PlatformPlayer p) {
        if (!tracked(p)) return;
        plugin.stats().totemUsed(p.getUniqueId());
        if (on("player.totem") != null) fire(ev("player.totem", p));
    }

    /** Dégâts subis par un joueur (toutes causes). */
    public void damage(Damage d) {
        PlatformPlayer p = d.victim;
        if (p == null || !tracked(p)) return;
        double dmg = d.amount;
        if (dmg <= 0) return;
        String cause = d.cause;
        boolean pvp = d.attacker != null;
        String attacker = d.attacker != null ? d.attacker.getName()
                : d.attackerMob != null ? pretty(d.attackerMob.getType())
                : d.directType != null ? pretty(d.directType) : null;
        double after = Math.max(0, d.victimHealthBefore - dmg);
        EventSettings s = on("player.damage");
        if (s != null && s.accepts("causes", cause) && dmg >= s.number("min-damage", 0)) {
            fire(ev("player.damage", p).damage(dmg, cause, pvp, false)
                    .data("damage", Text.hearts(dmg)).data("damage_type", pretty(cause))
                    .data("attacker", attacker == null ? pretty(cause) : attacker)
                    .data("player_last_damage", Text.hearts(dmg))
                    .data("player_health_after_damage", Text.hearts(after)));
        }
        s = on("player.big_damage");
        if (s != null && dmg >= s.number("hearts", 4) * 2) {
            fire(ev("player.big_damage", p).damage(dmg, cause, pvp, false)
                    .data("damage", Text.hearts(dmg)).data("damage_type", pretty(cause))
                    .data("attacker", attacker == null ? pretty(cause) : attacker)
                    .data("player_last_damage", Text.hearts(dmg))
                    .data("player_health_after_damage", Text.hearts(after)));
        }
    }

    /**
     * Objet consommé (nourriture, potion, lait).
     *
     * @param hadEffects le joueur avait des effets avant de consommer (pour le lait)
     */
    public void consume(PlatformPlayer p, ItemRef it, boolean hadEffects) {
        if (!tracked(p) || empty(it)) return;
        String type = it.getType();
        if (type.equals("ENCHANTED_GOLDEN_APPLE")) {
            if (on("player.enchanted_golden_apple") != null) fire(item(ev("player.enchanted_golden_apple", p), it));
        } else if (type.equals("GOLDEN_APPLE")) {
            if (on("player.golden_apple") != null) fire(item(ev("player.golden_apple", p), it));
        } else if (type.equals("POTION")) {
            String potion = it.getPotionType() == null ? "WATER" : it.getPotionType();
            EventSettings s = on("player.potion");
            if (s != null && s.accepts("potions", potion)) fire(item(ev("player.potion", p), it).data("potion", pretty(potion)));
            s = on("potion.drink");
            if (s != null && s.accepts("potions", potion)) fire(item(ev("potion.drink", p), it).data("potion", pretty(potion)));
        } else if (type.equals("MILK_BUCKET")) {
            if (on("potion.milk") != null && hadEffects) fire(ev("potion.milk", p));
        }
    }

    /** Changement de niveau d'expérience. */
    public void levelChange(PlatformPlayer p, int oldLevel, int newLevel) {
        EventSettings s = on("player.xp_level");
        if (s == null || !tracked(p)) return;
        for (Integer lvl : s.ints("levels")) {
            if (oldLevel < lvl && newLevel >= lvl) fire(ev("player.xp_level", p).data("level", lvl));
        }
    }

    /** Clic droit avec un objet en main principale. */
    public void itemUse(PlatformPlayer p, ItemRef it) {
        if (empty(it) || !tracked(p)) return;
        String type = it.getType();
        EventSettings s = on("player.item_use");
        if (s != null && !s.names("items").isEmpty() && s.accepts("items", type)) fire(item(ev("player.item_use", p), it));
        s = on("craft.item_use");
        if (s != null && !s.names("items").isEmpty() && s.accepts("items", type)) fire(item(ev("craft.item_use", p), it));
    }

    /** Projectile lancé par un joueur (type façon Bukkit : ENDER_PEARL, ARROW...). */
    public void projectileLaunch(PlatformPlayer p, String projectileType) {
        if (!tracked(p)) return;
        if ("ENDER_PEARL".equals(projectileType) && on("player.ender_pearl") != null) fire(ev("player.ender_pearl", p));
    }

    // ------------------------------------------------------------------ connexion

    public void join(final PlatformPlayer p) {
        // un tick plus tard : le passage éventuel en spectateur a eu lieu
        plugin.platform().runLater(new Runnable() {
            @Override
            public void run() {
                if (!p.isOnline() || !tracked(p)) return;
                boolean again = leftOnce.contains(p.getUniqueId());
                String id = again ? "player.reconnect" : "player.join";
                if (on(id) != null) fire(ev(id, p));
                plugin.game().checkAliveCount();
            }
        }, 2L);
    }

    public void quit(PlatformPlayer p) {
        if (tracked(p)) {
            leftOnce.add(p.getUniqueId());
            if (on("player.quit") != null) fire(ev("player.quit", p));
        }
        plugin.platform().runLater(new Runnable() {
            @Override
            public void run() {
                plugin.game().checkAliveCount();
            }
        }, 1L);
    }

    public void reset() {
        leftOnce.clear();
    }
}
