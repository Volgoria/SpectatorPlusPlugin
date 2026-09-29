package fr.spectatorplus.bukkit;

import fr.spectatorplus.compat.Compat;
import fr.spectatorplus.compat.Mat;
import fr.spectatorplus.compat.Version;
import fr.spectatorplus.core.platform.ItemRef;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Copie d'un ItemStack Bukkit.
 */
public final class BukkitItem implements ItemRef {

    private final ItemStack stack;

    private BukkitItem(ItemStack stack) {
        this.stack = stack;
    }

    /** Copie de l'objet ; null si l'objet est null. */
    public static BukkitItem of(ItemStack stack) {
        return stack == null ? null : new BukkitItem(stack.clone());
    }

    public static BukkitItem[] of(ItemStack[] stacks) {
        BukkitItem[] res = new BukkitItem[stacks == null ? 0 : stacks.length];
        for (int i = 0; i < res.length; i++) res[i] = of(stacks[i]);
        return res;
    }

    public ItemStack stack() {
        return stack;
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getType() {
        String type = stack.getType().name();
        // 1.8 - 1.12 : la pomme d'or enchantée est une GOLDEN_APPLE de valeur 1
        if (type.equals("GOLDEN_APPLE") && Version.isLegacy() && stack.getDurability() == 1) return "ENCHANTED_GOLDEN_APPLE";
        return type;
    }

    @Override
    public boolean isEmpty() {
        return Mat.isAir(stack);
    }

    @Override
    public int getAmount() {
        return stack.getAmount();
    }

    @Override
    public String getDisplayName() {
        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.hasDisplayName() ? meta.getDisplayName() : null;
    }

    @Override
    public Map<String, Integer> getEnchantments() {
        Map<String, Integer> res = new LinkedHashMap<>();
        for (Map.Entry<Enchantment, Integer> e : stack.getEnchantments().entrySet()) {
            res.put(Compat.enchantName(e.getKey()), e.getValue());
        }
        return res;
    }

    @Override
    public Map<String, Integer> getStoredEnchantments() {
        Map<String, Integer> res = new LinkedHashMap<>();
        ItemMeta meta = stack.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta) {
            for (Map.Entry<Enchantment, Integer> e : ((EnchantmentStorageMeta) meta).getStoredEnchants().entrySet()) {
                res.put(Compat.enchantName(e.getKey()), e.getValue());
            }
        }
        return res;
    }

    @Override
    public String getPotionType() {
        String type = stack.getType().name();
        if (!type.endsWith("POTION")) return null;
        return Compat.potionName(stack);
    }

    @Override
    public Object handle() {
        return stack;
    }
}
