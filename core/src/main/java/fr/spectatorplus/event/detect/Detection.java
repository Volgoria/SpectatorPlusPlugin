package fr.spectatorplus.event.detect;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.core.event.GameEvent;
import fr.spectatorplus.core.platform.ItemRef;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.Position;
import fr.spectatorplus.event.EventSettings;
import fr.spectatorplus.util.Names;
import fr.spectatorplus.util.Text;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base des détections d'évènements natifs, communes à toutes les plateformes.
 * Les plateformes traduisent leurs évènements (Bukkit, mixins des mods) en appels aux sous-classes.
 */
abstract class Detection {

    protected final SpectatorCore plugin;

    protected Detection(SpectatorCore plugin) {
        this.plugin = plugin;
    }

    /** Réglages si l'évènement est activé, sinon null (permet de sortir vite). */
    protected EventSettings on(String id) {
        return plugin.events().isEnabled(id) ? plugin.events().settings(id) : null;
    }

    /** Le joueur doit-il générer des évènements ? (pas spectateur, pas fictif, mode de jeu suivi) */
    protected boolean tracked(PlatformPlayer p) {
        if (p == null || plugin.spectators().isSpectator(p) || p.isFake()) return false;
        List<String> ignored = plugin.config().getStringList("events.ignore-gamemodes");
        return !ignored.contains(p.getGameMode().name());
    }

    protected GameEvent.Builder ev(String id, PlatformPlayer p) {
        GameEvent.Builder b = plugin.events().builder(id);
        if (p != null) {
            Position l = p.getLocation();
            b.player(p).location(l)
                    .data("world", l.getWorld())
                    .data("x", l.getBlockX()).data("y", l.getBlockY()).data("z", l.getBlockZ())
                    .data("health", plugin.game().hearts(p.getHealth()));
            String team = plugin.game().getTeam(p);
            if (team != null) b.data("team", team);
        }
        return b;
    }

    protected void fire(GameEvent.Builder b) {
        plugin.events().fire(b.build());
    }

    protected static String pretty(String constant) {
        return Text.pretty(constant);
    }

    /** Ajoute les placeholders {item}, {item_type}, {item_name}, {item_amount}, {item_enchantments}. */
    protected static GameEvent.Builder item(GameEvent.Builder b, ItemRef it) {
        if (it == null) return b;
        String type = it.getType();
        String name = it.getDisplayName() != null ? it.getDisplayName() : pretty(type);
        StringBuilder ench = new StringBuilder();
        for (Map.Entry<String, Integer> e : it.getEnchantments().entrySet()) {
            if (ench.length() > 0) ench.append(", ");
            ench.append(pretty(e.getKey())).append(' ').append(e.getValue());
        }
        return b.data("item", pretty(type)).data("item_type", type).data("item_name", name)
                .data("item_amount", it.getAmount()).data("item_enchantments", ench.length() == 0 ? "-" : ench.toString());
    }

    protected static boolean empty(ItemRef it) {
        return it == null || it.isEmpty();
    }

    /** Niveau d'un enchantement sur un objet (0 si absent). Accepte noms legacy et modernes. */
    protected static int enchantLevel(ItemRef it, String... names) {
        if (empty(it)) return 0;
        for (Map.Entry<String, Integer> e : it.getEnchantments().entrySet()) {
            Set<String> keys = Names.enchantKeys(e.getKey());
            for (String n : names) if (keys.contains(n)) return e.getValue();
        }
        return 0;
    }

    /**
     * Teste une entrée « NOM:niveau » d'une liste (ex : SHARPNESS:5). Liste vide = tout accepter.
     */
    protected static boolean matchesLeveled(List<String> entries, Set<String> keys, int level) {
        if (entries.isEmpty()) return true;
        for (String entry : entries) {
            String[] parts = entry.split(":");
            int min = 1;
            if (parts.length > 1) {
                try {
                    min = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException ignored) {
                }
            }
            if ((parts[0].equals("*") || keys.contains(parts[0])) && level >= min) return true;
        }
        return false;
    }

    protected static boolean isWeapon(String type) {
        return type.endsWith("_SWORD") || type.endsWith("_AXE") || type.equals("BOW") || type.equals("CROSSBOW")
                || type.equals("TRIDENT") || type.equals("MACE");
    }

    protected static boolean isArmor(String type) {
        return type.endsWith("_HELMET") || type.endsWith("_CHESTPLATE") || type.endsWith("_LEGGINGS")
                || type.endsWith("_BOOTS") || type.equals("ELYTRA");
    }

    /** Position hors de la bordure du monde. */
    protected boolean outsideBorder(Position loc) {
        fr.spectatorplus.core.platform.PlatformWorld w = plugin.platform().getWorld(loc.getWorld());
        if (w == null) return false;
        double half = w.getBorderSize() / 2.0;
        return Math.abs(loc.getX() - w.getBorderCenterX()) > half || Math.abs(loc.getZ() - w.getBorderCenterZ()) > half;
    }
}
