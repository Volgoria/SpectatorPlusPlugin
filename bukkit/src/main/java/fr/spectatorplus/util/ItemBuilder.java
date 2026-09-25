package fr.spectatorplus.util;

import fr.spectatorplus.compat.Mat;
import fr.spectatorplus.compat.Reflect;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ItemBuilder {

    private final ItemStack item;
    private final List<String> lore = new ArrayList<>();
    private String name;
    private boolean glow;

    public ItemBuilder(ItemStack base) {
        this.item = base == null ? new ItemStack(Mat.stone()) : base.clone();
    }

    public static ItemBuilder of(String spec) {
        return new ItemBuilder(Mat.parse(spec));
    }

    public static ItemBuilder of(ItemStack stack) {
        return new ItemBuilder(stack);
    }

    public ItemBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ItemBuilder lore(String... lines) {
        lore.addAll(Arrays.asList(lines));
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        if (lines != null) lore.addAll(lines);
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        this.glow = glow;
        return this;
    }

    public ItemStack build() {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        if (name != null) meta.setDisplayName(Text.color(name));
        if (!lore.isEmpty()) {
            List<String> colored = new ArrayList<>();
            for (String l : lore) {
                for (String part : l.split("\n")) colored.add(Text.color(part));
            }
            meta.setLore(colored);
        }
        try {
            meta.addItemFlags(ItemFlag.values());
        } catch (Throwable ignored) {
        }
        if (glow) {
            // UNBREAKING / DURABILITY existe sur toutes les versions, on le cache avec HIDE_ENCHANTS
            Object ench = Reflect.invokeStatic(org.bukkit.enchantments.Enchantment.class, "getByName", "DURABILITY");
            if (ench == null) ench = Reflect.getStaticField(org.bukkit.enchantments.Enchantment.class, "UNBREAKING");
            if (ench != null) Reflect.tryInvoke(meta, "addEnchant", ench, 1, true);
            Reflect.tryInvoke(meta, "setEnchantmentGlintOverride", Boolean.TRUE);
        }
        item.setItemMeta(meta);
        return item;
    }
}
