package fr.spectatorplus.event.detect;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.core.event.GameEvent;
import fr.spectatorplus.core.platform.ItemRef;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.event.EventSettings;
import fr.spectatorplus.util.Names;

import java.util.Map;

/**
 * Crafts, objets, enchantements et potions lancées.
 */
public final class ItemSignals extends Detection {

    public ItemSignals(SpectatorCore plugin) {
        super(plugin);
    }

    /** Objet ramassé au sol. */
    public void pickup(PlatformPlayer p, ItemRef it) {
        if (!tracked(p) || empty(it)) return;
        EventSettings s = on("craft.pickup");
        if (s != null && !s.names("items").isEmpty() && s.accepts("items", it.getType())) fire(item(ev("craft.pickup", p), it));
        obtain(p, it, "pickup");
    }

    private void obtain(PlatformPlayer p, ItemRef it, String how) {
        EventSettings s = on("craft.obtain");
        if (s != null && !s.names("items").isEmpty() && s.accepts("items", it.getType())) {
            fire(item(ev("craft.obtain", p), it).data("method", how));
        }
    }

    // ------------------------------------------------------------------ crafts / objets

    /** Objet fabriqué (résultat pris dans la table de craft). */
    public void craft(PlatformPlayer p, ItemRef result) {
        if (!tracked(p) || empty(result)) return;
        String type = result.getType();
        EventSettings s = on("craft.craft");
        if (s != null && !s.names("items").isEmpty() && s.accepts("items", type)) {
            fire(item(ev("craft.craft", p), result).data("crafted_item", pretty(type)).data("crafted_amount", result.getAmount()));
        }
        s = on("craft.first_craft");
        if (s != null && s.accepts("items", type) && plugin.stats().first(p.getUniqueId(), "craft." + type)) {
            fire(item(ev("craft.first_craft", p), result).data("crafted_item", pretty(type)).data("crafted_amount", result.getAmount()));
        }
        obtain(p, result, "craft");
    }

    /** Objet sorti d'un four. */
    public void furnace(PlatformPlayer p, ItemRef it) {
        if (!tracked(p) || empty(it)) return;
        obtain(p, it, "furnace");
    }

    /** Objet jeté. */
    public void drop(PlatformPlayer p, ItemRef it) {
        if (!tracked(p) || empty(it)) return;
        EventSettings s = on("craft.drop");
        if (s != null && !s.names("items").isEmpty() && s.accepts("items", it.getType())) fire(item(ev("craft.drop", p), it));
    }

    /** Objet cassé (durabilité épuisée). */
    public void itemBreak(PlatformPlayer p, ItemRef it) {
        if (!tracked(p) || empty(it)) return;
        EventSettings s = on("craft.item_break");
        if (s != null && s.accepts("items", it.getType())) fire(item(ev("craft.item_break", p), it));
    }

    // ------------------------------------------------------------------ enchantements

    /**
     * Enchantement à la table.
     *
     * @param added enchantements ajoutés (nom → niveau)
     * @param cost  niveaux dépensés
     */
    public void enchant(PlatformPlayer p, ItemRef it, Map<String, Integer> added, int cost) {
        if (!tracked(p) || empty(it)) return;
        String type = it.getType();
        StringBuilder list = new StringBuilder();
        int best = 0;
        String bestName = "";
        for (Map.Entry<String, Integer> en : added.entrySet()) {
            if (list.length() > 0) list.append(", ");
            list.append(pretty(en.getKey())).append(' ').append(en.getValue());
            if (en.getValue() > best) {
                best = en.getValue();
                bestName = en.getKey();
            }
        }
        if (on("enchant.enchant") != null) fire(enchantEvent("enchant.enchant", p, it, list.toString(), bestName, best, cost));

        EventSettings s = on("enchant.specific");
        if (s != null && !s.names("enchantments").isEmpty()) {
            for (Map.Entry<String, Integer> en : added.entrySet()) {
                if (matchesLeveled(s.names("enchantments"), Names.enchantKeys(en.getKey()), en.getValue())) {
                    fire(enchantEvent("enchant.specific", p, it, list.toString(), en.getKey(), en.getValue(), cost));
                    break;
                }
            }
        }
        s = on("enchant.level");
        if (s != null && best >= s.integer("min-level", 4)) {
            fire(enchantEvent("enchant.level", p, it, list.toString(), bestName, best, cost));
        }
        if (isWeapon(type)) enchantCategory("enchant.weapon", p, it, added, list.toString(), cost);
        if (isArmor(type)) enchantCategory("enchant.armor", p, it, added, list.toString(), cost);
    }

    private void enchantCategory(String id, PlatformPlayer p, ItemRef it, Map<String, Integer> added, String list, int cost) {
        EventSettings s = on(id);
        if (s == null) return;
        for (Map.Entry<String, Integer> en : added.entrySet()) {
            if (matchesLeveled(s.names("enchantments"), Names.enchantKeys(en.getKey()), en.getValue())) {
                fire(enchantEvent(id, p, it, list, en.getKey(), en.getValue(), cost));
                return;
            }
        }
    }

    private GameEvent.Builder enchantEvent(String id, PlatformPlayer p, ItemRef it, String list, String ench, int level, int cost) {
        return item(ev(id, p), it)
                .data("enchanted_item", pretty(it.getType()))
                .data("enchantment", pretty(ench)).data("enchantment_level", level)
                .data("enchantments", list).data("xp_cost", cost);
    }

    /** Résultat pris dans une enclume. */
    public void anvil(PlatformPlayer p, ItemRef left, ItemRef right, ItemRef result, int cost) {
        if (!tracked(p) || empty(result)) return;
        if (on("enchant.anvil") != null) fire(item(ev("enchant.anvil", p), result).data("xp_cost", cost));
        if (empty(left) || empty(right)) return;
        if (right.getType().equals("ENCHANTED_BOOK")) {
            EventSettings s = on("enchant.book");
            if (s == null) return;
            for (Map.Entry<String, Integer> en : right.getStoredEnchantments().entrySet()) {
                if (matchesLeveled(s.names("enchantments"), Names.enchantKeys(en.getKey()), en.getValue())) {
                    fire(item(ev("enchant.book", p), result)
                            .data("enchanted_item", pretty(left.getType()))
                            .data("enchantment", pretty(en.getKey()))
                            .data("enchantment_level", en.getValue()).data("xp_cost", cost));
                    return;
                }
            }
        } else {
            EventSettings s = on("enchant.combine");
            if (s != null && s.accepts("items", left.getType())) {
                fire(item(ev("enchant.combine", p), result).data("enchanted_item", pretty(left.getType())).data("xp_cost", cost));
            }
        }
    }

    // ------------------------------------------------------------------ potions lancées

    /** Potion jetable ou persistante lancée. */
    public void potionThrow(PlatformPlayer p, ItemRef potion) {
        if (!tracked(p) || empty(potion)) return;
        String name = potion.getPotionType() == null ? "WATER" : potion.getPotionType();
        String id = potion.getType().equals("LINGERING_POTION") ? "potion.lingering" : "potion.splash";
        EventSettings s = on(id);
        if (s != null && s.accepts("potions", name)) fire(item(ev(id, p), potion).data("potion", pretty(name)));
    }
}
