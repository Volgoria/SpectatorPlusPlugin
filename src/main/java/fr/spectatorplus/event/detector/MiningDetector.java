package fr.spectatorplus.event.detector;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.api.event.SpectatorGameEvent;
import fr.spectatorplus.compat.Compat;
import fr.spectatorplus.event.EventSettings;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Minage et blocs.
 */
public final class MiningDetector extends Detector {

    public MiningDetector(SpectatorPlus plugin) {
        super(plugin);
    }

    private SpectatorGameEvent.Builder block(String id, Player p, Block b) {
        String type = b.getType().name();
        return ev(id, p).location(b.getLocation())
                .data("block", pretty(type)).data("block_type", type)
                .data("block_x", b.getX()).data("block_y", b.getY()).data("block_z", b.getZ());
    }

    private static boolean isDiamond(String t) {
        return t.equals("DIAMOND_ORE") || t.equals("DEEPSLATE_DIAMOND_ORE");
    }

    private static boolean isGold(String t) {
        return t.equals("GOLD_ORE") || t.equals("DEEPSLATE_GOLD_ORE") || t.equals("NETHER_GOLD_ORE");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (!tracked(p)) return;
        Block b = e.getBlock();
        String type = b.getType().name();
        UUID id = p.getUniqueId();
        ItemStack tool = Compat.mainHand(p);

        EventSettings s = on("mining.block_break");
        if (s != null && s.accepts("blocks", type)) fire(block("mining.block_break", p, b));

        int sameType = plugin.stats().increment(id, "block." + type, 1);
        s = on("mining.block_count");
        if (s != null && s.accepts("blocks", type) && s.ints("amounts").contains(sameType)) {
            fire(block("mining.block_count", p, b).data("amount", sameType).data("ore_amount", sameType));
        }

        if (isDiamond(type)) ore(p, b, "diamond", "mining.first_diamond", "mining.diamond_count");
        if (isGold(type)) ore(p, b, "gold", "mining.first_gold", "mining.gold_count");
        if (type.equals("ANCIENT_DEBRIS")) ore(p, b, "debris", "mining.first_debris", "mining.debris_count");

        if ((type.equals("SPAWNER") || type.equals("MOB_SPAWNER")) && on("mining.spawner_break") != null) {
            fire(block("mining.spawner_break", p, b));
        }
        s = on("mining.first_ore");
        if (s != null && !s.names("ores").isEmpty() && s.accepts("ores", type) && plugin.stats().first(id, "ore." + type)) {
            fire(block("mining.first_ore", p, b).data("ore", pretty(type)));
        }
        s = on("mining.silk_touch");
        if (s != null && s.accepts("blocks", type) && enchantLevel(tool, "SILK_TOUCH") > 0) {
            fire(block("mining.silk_touch", p, b));
        }
        s = on("mining.fortune");
        if (s != null && s.accepts("blocks", type)) {
            int level = enchantLevel(tool, "FORTUNE", "LOOT_BONUS_BLOCKS");
            if (level > 0 && level >= s.integer("min-level", 1)) {
                fire(block("mining.fortune", p, b).data("level", level).data("enchantment_level", level));
            }
        }
    }

    private void ore(Player p, Block b, String key, String firstId, String countId) {
        UUID id = p.getUniqueId();
        int count = plugin.stats().increment(id, "ore-count." + key, 1);
        if (plugin.stats().first(id, "first." + key) && on(firstId) != null) {
            fire(block(firstId, p, b).data("ore", pretty(b.getType().name())).data("ore_amount", 1));
        }
        EventSettings s = on(countId);
        if (s != null && s.ints("amounts").contains(count)) {
            fire(block(countId, p, b).data("ore", pretty(b.getType().name())).data("ore_amount", count).data("amount", count));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (!tracked(p)) return;
        Block b = e.getBlockPlaced();
        String type = b.getType().name();
        EventSettings s = on("mining.block_place");
        if (s != null && !s.names("blocks").isEmpty() && s.accepts("blocks", type)) fire(block("mining.block_place", p, b));
        s = on("mining.rare_place");
        if (s != null && !s.names("blocks").isEmpty() && s.accepts("blocks", type)) fire(block("mining.rare_place", p, b));
    }
}
