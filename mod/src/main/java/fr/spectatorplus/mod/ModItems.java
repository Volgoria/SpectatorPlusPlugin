package fr.spectatorplus.mod;

import com.mojang.authlib.GameProfile;
import fr.spectatorplus.core.platform.Icon;
import fr.spectatorplus.util.Text;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemLore;
//?}

/**
 * Objets : registre, noms / descriptions, têtes de joueur, brillance, rendu des {@link Icon}.
 */
public final class ModItems {

    private ModItems() {
    }

    // ------------------------------------------------------------------ registre

    /** « diamond_sword » pour les objets vanilla, « mod:objet » pour ceux d'un mod. */
    public static String id(Item item) {
        Identifier key = registry().getKey(item);
        return key.getNamespace().equals("minecraft") ? key.getPath() : key.toString();
    }

    /** Nom façon Bukkit : DIAMOND_SWORD (ou MOD_OBJET pour un objet de mod). */
    public static String bukkitName(Item item) {
        return id(item).replace(':', '_').toUpperCase(Locale.ROOT);
    }

    /** Objet par identifiant (« DIAMOND_SWORD », « minecraft:diamond_sword »...), ou null. */
    public static Item byName(String name) {
        if (name == null || name.isEmpty()) return null;
        Identifier id = Identifier.tryParse(name.trim().toLowerCase(Locale.ROOT));
        if (id == null || !registry().containsKey(id)) return null;
        //? if >=1.21.2 {
        return registry().getValue(id);
        //?} else
        /*return registry().get(id);*/
    }

    //? if >=1.19.3 {
    private static net.minecraft.core.DefaultedRegistry<Item> registry() {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM;
    }
    //?} else {
    /*private static net.minecraft.core.DefaultedRegistry<Item> registry() {
        return net.minecraft.core.Registry.ITEM;
    }
    *///?}

    // ------------------------------------------------------------------ lecture

    public static String displayName(ItemStack stack) {
        //? if >=1.20.5 {
        Component c = stack.get(DataComponents.CUSTOM_NAME);
        return c == null ? null : c.getString();
        //?} else
        /*return stack.hasCustomHoverName() ? stack.getHoverName().getString() : null;*/
    }

    /** Enchantements : nom en majuscules (SHARPNESS) → niveau. */
    public static Map<String, Integer> enchantments(ItemStack stack) {
        Map<String, Integer> res = new LinkedHashMap<>();
        //? if >=1.20.5 {
        for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment>> e
                : stack.getEnchantments().entrySet()) {
            e.getKey().unwrapKey().ifPresent(k -> res.put(Keys.path(k).toUpperCase(Locale.ROOT), e.getIntValue()));
        }
        //?} else {
        /*for (Map.Entry<net.minecraft.world.item.enchantment.Enchantment, Integer> e
                : net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(stack).entrySet()) {
            Identifier key = Keys.enchantment(e.getKey());
            if (key != null) res.put(key.getPath().toUpperCase(Locale.ROOT), e.getValue());
        }
        *///?}
        return res;
    }

    /** Enchantements stockés d'un livre enchanté. */
    public static Map<String, Integer> storedEnchantments(ItemStack stack) {
        //? if >=1.20.5 {
        Map<String, Integer> res = new LinkedHashMap<>();
        net.minecraft.world.item.enchantment.ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored != null) {
            for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment>> e
                    : stored.entrySet()) {
                e.getKey().unwrapKey().ifPresent(k -> res.put(Keys.path(k).toUpperCase(Locale.ROOT), e.getIntValue()));
            }
        }
        return res;
        //?} else
        /*return enchantments(stack);*/
    }

    /** Type de base d'une potion (SWIFTNESS...), ou null si l'objet n'est pas une potion. */
    public static String potionType(ItemStack stack) {
        if (!bukkitName(stack.getItem()).endsWith("POTION")) return null;
        //? if >=1.20.5 {
        net.minecraft.world.item.alchemy.PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null || !contents.potion().isPresent()) return "WATER";
        return contents.potion().get().unwrapKey().map(Keys::path).orElse("water").toUpperCase(Locale.ROOT);
        //?} elif >=1.19.3 {
        /*Identifier id = net.minecraft.core.registries.BuiltInRegistries.POTION.getKey(net.minecraft.world.item.alchemy.PotionUtils.getPotion(stack));
        return id == null ? "WATER" : id.getPath().toUpperCase(Locale.ROOT);
        *///?} else {
        /*Identifier id = net.minecraft.core.Registry.POTION.getKey(net.minecraft.world.item.alchemy.PotionUtils.getPotion(stack));
        return id == null ? "WATER" : id.getPath().toUpperCase(Locale.ROOT);
        *///?}
    }

    // ------------------------------------------------------------------ écriture

    public static void setName(ItemStack stack, String legacy) {
        Component name = Texts.item(Text.color(legacy));
        //? if >=1.20.5 {
        stack.set(DataComponents.CUSTOM_NAME, name);
        //?} else
        /*stack.setHoverName(name);*/
    }

    public static void setLore(ItemStack stack, List<String> lines) {
        List<Component> lore = new ArrayList<>();
        for (String l : lines) {
            for (String part : l.split("\n")) lore.add(Texts.item(Text.color(part)));
        }
        //? if >=1.20.5 {
        stack.set(DataComponents.LORE, new ItemLore(lore));
        //?} else {
        /*net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (Component c : lore) list.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(c)));
        stack.getOrCreateTagElement("display").put("Lore", list);
        *///?}
    }

    /** Masque les lignes techniques (attributs, enchantements...) comme les ItemFlag de Bukkit. */
    public static void hideDetails(ItemStack stack) {
        //? if >=1.21.5 {
        java.util.LinkedHashSet<net.minecraft.core.component.DataComponentType<?>> hidden = new java.util.LinkedHashSet<>();
        hidden.add(DataComponents.ATTRIBUTE_MODIFIERS);
        hidden.add(DataComponents.ENCHANTMENTS);
        hidden.add(DataComponents.STORED_ENCHANTMENTS);
        hidden.add(DataComponents.POTION_CONTENTS);
        hidden.add(DataComponents.UNBREAKABLE);
        stack.set(DataComponents.TOOLTIP_DISPLAY, new net.minecraft.world.item.component.TooltipDisplay(false, hidden));
        //?} elif >=1.20.5 {
        /*stack.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, net.minecraft.util.Unit.INSTANCE);
        *///?} else
        /*stack.getOrCreateTag().putInt("HideFlags", 127);*/
    }

    public static void glow(ItemStack stack) {
        //? if >=1.20.5 {
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, Boolean.TRUE);
        //?} else {
        /*net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        list.add(new net.minecraft.nbt.CompoundTag());
        stack.getOrCreateTag().put("Enchantments", list);
        *///?}
    }

    /** Tête avec le skin d'un joueur (profil complet s'il est connecté, sinon par UUID / nom). */
    public static ItemStack head(GameProfile profile, UUID id, String name) {
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        //? if >=1.21.9 {
        stack.set(DataComponents.PROFILE, profile != null
                ? net.minecraft.world.item.component.ResolvableProfile.createResolved(profile)
                : id != null ? net.minecraft.world.item.component.ResolvableProfile.createUnresolved(id)
                : net.minecraft.world.item.component.ResolvableProfile.createUnresolved(name));
        //?} elif >=1.20.5 {
        /*GameProfile gp = profile != null ? profile : new GameProfile(id == null ? net.minecraft.Util.NIL_UUID : id, name == null ? "" : name);
        stack.set(DataComponents.PROFILE, new net.minecraft.world.item.component.ResolvableProfile(gp));
        *///?} else {
        /*if (profile != null) {
            stack.getOrCreateTag().put("SkullOwner", net.minecraft.nbt.NbtUtils.writeGameProfile(new net.minecraft.nbt.CompoundTag(), profile));
        } else if (name != null) {
            stack.getOrCreateTag().putString("SkullOwner", name);
        }
        *///?}
        return stack;
    }

    // ------------------------------------------------------------------ icônes du code commun

    public static ItemStack render(Icon icon, ModPlatform platform) {
        if (icon == null || icon.isEmpty()) return ItemStack.EMPTY;
        ItemStack stack;
        switch (icon.getKind()) {
            case PANE:
                stack = colored(icon.getSpec(), "_stained_glass_pane", Items.GLASS_PANE);
                break;
            case WOOL:
                stack = colored(icon.getSpec(), "_wool", Items.STONE);
                break;
            case HEAD:
                stack = new ItemStack(Items.PLAYER_HEAD);
                break;
            case SKULL:
                stack = head(platform.profile(icon.getOwner()), icon.getOwner(), icon.getOwnerName());
                break;
            case ITEM:
                stack = ((ItemStack) icon.getItem().handle()).copy();
                if (icon.getName() == null && icon.getLore().isEmpty() && !icon.isGlow()) return stack;
                break;
            default:
                stack = new ItemStack(parse(icon.getSpec()));
                break;
        }
        if (icon.getName() != null) setName(stack, icon.getName());
        if (!icon.getLore().isEmpty()) setLore(stack, icon.getLore());
        if (icon.getAmount() > 1) stack.setCount(icon.getAmount());
        if (icon.getKind() != Icon.Kind.ITEM) hideDetails(stack);
        if (icon.isGlow()) glow(stack);
        return stack;
    }

    /** Spécification « MODERNE|LEGACY:data » : premier nom qui existe (les noms legacy sont ignorés). */
    public static Item parse(String spec) {
        if (spec != null) {
            for (String alt : spec.trim().split("\\|")) {
                String name = alt.split(":")[0].trim();
                Item item = byName(name);
                if (item != null && item != Items.AIR) return item;
            }
        }
        return Items.STONE;
    }

    private static ItemStack colored(String color, String suffix, Item fallback) {
        Item item = byName((color == null ? "white" : color.toLowerCase(Locale.ROOT)) + suffix);
        return new ItemStack(item == null ? fallback : item);
    }
}
