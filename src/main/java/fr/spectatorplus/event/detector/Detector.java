package fr.spectatorplus.event.detector;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.api.event.SpectatorGameEvent;
import fr.spectatorplus.compat.Compat;
import fr.spectatorplus.event.EventSettings;
import fr.spectatorplus.util.Text;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base des détecteurs d'évènements natifs.
 */
public abstract class Detector implements Listener {

    protected final SpectatorPlus plugin;

    protected Detector(SpectatorPlus plugin) {
        this.plugin = plugin;
    }

    /** Réglages si l'évènement est activé, sinon null (permet de sortir vite). */
    protected EventSettings on(String id) {
        return plugin.events().isEnabled(id) ? plugin.events().settings(id) : null;
    }

    /** Le joueur doit-il générer des évènements ? (pas spectateur, pas NPC, mode de jeu suivi) */
    protected boolean tracked(Player p) {
        if (p == null || plugin.spectators().isSpectator(p) || p.hasMetadata("NPC")) return false;
        List<String> ignored = plugin.getConfig().getStringList("events.ignore-gamemodes");
        return !ignored.contains(p.getGameMode().name());
    }

    protected SpectatorGameEvent.Builder ev(String id, Player p) {
        SpectatorGameEvent.Builder b = plugin.events().builder(id);
        if (p != null) {
            Location l = p.getLocation();
            b.player(p).location(l)
                    .data("world", l.getWorld().getName())
                    .data("x", l.getBlockX()).data("y", l.getBlockY()).data("z", l.getBlockZ())
                    .data("health", Text.hearts(p.getHealth()));
            String team = plugin.game().getTeam(p);
            if (team != null) b.data("team", team);
        }
        return b;
    }

    protected void fire(SpectatorGameEvent.Builder b) {
        plugin.events().fire(b.build());
    }

    protected static String pretty(String constant) {
        return Text.pretty(constant);
    }

    /** Ajoute les placeholders {item}, {item_type}, {item_name}, {item_amount}, {item_enchantments}. */
    protected static SpectatorGameEvent.Builder item(SpectatorGameEvent.Builder b, ItemStack it) {
        if (it == null) return b;
        String type = it.getType().name();
        ItemMeta meta = it.getItemMeta();
        String name = meta != null && meta.hasDisplayName() ? meta.getDisplayName() : pretty(type);
        StringBuilder ench = new StringBuilder();
        for (Map.Entry<Enchantment, Integer> e : it.getEnchantments().entrySet()) {
            if (ench.length() > 0) ench.append(", ");
            ench.append(pretty(Compat.enchantName(e.getKey()))).append(' ').append(e.getValue());
        }
        return b.data("item", pretty(type)).data("item_type", type).data("item_name", name)
                .data("item_amount", it.getAmount()).data("item_enchantments", ench.length() == 0 ? "-" : ench.toString());
    }

    /** Niveau d'un enchantement sur un objet (0 si absent). Accepte noms legacy et modernes. */
    protected static int enchantLevel(ItemStack it, String... names) {
        if (it == null) return 0;
        for (Map.Entry<Enchantment, Integer> e : it.getEnchantments().entrySet()) {
            Set<String> keys = Compat.enchantKeys(e.getKey());
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
}
