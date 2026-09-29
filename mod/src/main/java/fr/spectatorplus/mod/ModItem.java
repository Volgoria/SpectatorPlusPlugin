package fr.spectatorplus.mod;

import fr.spectatorplus.core.platform.ItemRef;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Copie d'un ItemStack Minecraft.
 */
public final class ModItem implements ItemRef {

    private final ItemStack stack;

    private ModItem(ItemStack stack) {
        this.stack = stack;
    }

    public static ModItem of(ItemStack stack) {
        return new ModItem(stack == null ? ItemStack.EMPTY : stack.copy());
    }

    public static ModItem[] of(ItemStack[] stacks) {
        ModItem[] res = new ModItem[stacks.length];
        for (int i = 0; i < stacks.length; i++) res[i] = of(stacks[i]);
        return res;
    }

    @Override
    public String getType() {
        return ModItems.bukkitName(stack.getItem());
    }

    @Override
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    @Override
    public int getAmount() {
        return stack.getCount();
    }

    @Override
    public String getDisplayName() {
        return ModItems.displayName(stack);
    }

    @Override
    public Map<String, Integer> getEnchantments() {
        return ModItems.enchantments(stack);
    }

    @Override
    public Map<String, Integer> getStoredEnchantments() {
        return ModItems.storedEnchantments(stack);
    }

    @Override
    public String getPotionType() {
        return ModItems.potionType(stack);
    }

    @Override
    public Object handle() {
        return stack;
    }
}
