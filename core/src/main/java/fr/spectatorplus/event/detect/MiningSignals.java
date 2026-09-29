package fr.spectatorplus.event.detect;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.core.event.GameEvent;
import fr.spectatorplus.core.platform.ItemRef;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.Position;
import fr.spectatorplus.event.EventSettings;

import java.util.UUID;

/**
 * Minage et blocs.
 */
public final class MiningSignals extends Detection {

    public MiningSignals(SpectatorCore plugin) {
        super(plugin);
    }

    private GameEvent.Builder block(String id, PlatformPlayer p, String type, Position b) {
        return ev(id, p).location(b)
                .data("block", pretty(type)).data("block_type", type)
                .data("block_x", b.getBlockX()).data("block_y", b.getBlockY()).data("block_z", b.getBlockZ());
    }

    private static boolean isDiamond(String t) {
        return t.equals("DIAMOND_ORE") || t.equals("DEEPSLATE_DIAMOND_ORE");
    }

    private static boolean isGold(String t) {
        return t.equals("GOLD_ORE") || t.equals("DEEPSLATE_GOLD_ORE") || t.equals("NETHER_GOLD_ORE");
    }

    /**
     * Bloc cassé.
     *
     * @param type  type du bloc façon Bukkit (DIAMOND_ORE)
     * @param block position du bloc
     * @param tool  objet en main principale, ou null
     */
    public void blockBreak(PlatformPlayer p, String type, Position block, ItemRef tool) {
        if (!tracked(p)) return;
        UUID id = p.getUniqueId();

        EventSettings s = on("mining.block_break");
        if (s != null && s.accepts("blocks", type)) fire(block("mining.block_break", p, type, block));

        int sameType = plugin.stats().increment(id, "block." + type, 1);
        s = on("mining.block_count");
        if (s != null && s.accepts("blocks", type) && s.ints("amounts").contains(sameType)) {
            fire(block("mining.block_count", p, type, block).data("amount", sameType).data("ore_amount", sameType));
        }

        if (isDiamond(type)) ore(p, type, block, "diamond", "mining.first_diamond", "mining.diamond_count");
        if (isGold(type)) ore(p, type, block, "gold", "mining.first_gold", "mining.gold_count");
        if (type.equals("ANCIENT_DEBRIS")) ore(p, type, block, "debris", "mining.first_debris", "mining.debris_count");

        if ((type.equals("SPAWNER") || type.equals("MOB_SPAWNER")) && on("mining.spawner_break") != null) {
            fire(block("mining.spawner_break", p, type, block));
        }
        s = on("mining.first_ore");
        if (s != null && !s.names("ores").isEmpty() && s.accepts("ores", type) && plugin.stats().first(id, "ore." + type)) {
            fire(block("mining.first_ore", p, type, block).data("ore", pretty(type)));
        }
        s = on("mining.silk_touch");
        if (s != null && s.accepts("blocks", type) && enchantLevel(tool, "SILK_TOUCH") > 0) {
            fire(block("mining.silk_touch", p, type, block));
        }
        s = on("mining.fortune");
        if (s != null && s.accepts("blocks", type)) {
            int level = enchantLevel(tool, "FORTUNE", "LOOT_BONUS_BLOCKS");
            if (level > 0 && level >= s.integer("min-level", 1)) {
                fire(block("mining.fortune", p, type, block).data("level", level).data("enchantment_level", level));
            }
        }
    }

    private void ore(PlatformPlayer p, String type, Position b, String key, String firstId, String countId) {
        UUID id = p.getUniqueId();
        int count = plugin.stats().increment(id, "ore-count." + key, 1);
        if (plugin.stats().first(id, "first." + key) && on(firstId) != null) {
            fire(block(firstId, p, type, b).data("ore", pretty(type)).data("ore_amount", 1));
        }
        EventSettings s = on(countId);
        if (s != null && s.ints("amounts").contains(count)) {
            fire(block(countId, p, type, b).data("ore", pretty(type)).data("ore_amount", count).data("amount", count));
        }
    }

    /** Bloc posé. */
    public void blockPlace(PlatformPlayer p, String type, Position block) {
        if (!tracked(p)) return;
        EventSettings s = on("mining.block_place");
        if (s != null && !s.names("blocks").isEmpty() && s.accepts("blocks", type)) fire(block("mining.block_place", p, type, block));
        s = on("mining.rare_place");
        if (s != null && !s.names("blocks").isEmpty() && s.accepts("blocks", type)) fire(block("mining.rare_place", p, type, block));
    }
}
