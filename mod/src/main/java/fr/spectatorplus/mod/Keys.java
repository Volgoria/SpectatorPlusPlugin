package fr.spectatorplus.mod;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.Block;

import java.util.Locale;

/**
 * Identifiants de registre (blocs, effets, enchantements, biomes...) quelle que soit la version.
 */
public final class Keys {

    private Keys() {
    }

    public static String path(ResourceKey<?> key) {
        //? if >=1.21.11 {
        return key.identifier().getPath();
        //?} else
        /*return key.location().getPath();*/
    }

    public static Identifier block(Block block) {
        //? if >=1.19.3 {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        //?} else
        /*return net.minecraft.core.Registry.BLOCK.getKey(block);*/
    }

    /** Nom façon Bukkit d'un bloc (DIAMOND_ORE). */
    public static String blockName(Block block) {
        Identifier id = block(block);
        String s = id.getNamespace().equals("minecraft") ? id.getPath() : id.toString().replace(':', '_');
        return s.toUpperCase(Locale.ROOT);
    }

    /** Nom façon Bukkit d'un type d'entité (ZOMBIE, ENDER_DRAGON...). */
    public static String entityName(net.minecraft.world.entity.EntityType<?> type) {
        //? if >=1.19.3 {
        Identifier id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type);
        //?} else
        /*Identifier id = net.minecraft.core.Registry.ENTITY_TYPE.getKey(type);*/
        String s = id.getNamespace().equals("minecraft") ? id.getPath() : id.toString().replace(':', '_');
        return s.toUpperCase(Locale.ROOT);
    }

    /** Nom façon Bukkit d'un effet (SPEED, NIGHT_VISION...). */
    public static String effectName(MobEffectInstance e) {
        //? if >=1.20.5 {
        return e.getEffect().unwrapKey().map(Keys::path).orElse("unknown").toUpperCase(Locale.ROOT);
        //?} elif >=1.19.3 {
        /*Identifier id = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(e.getEffect());
        return id == null ? "UNKNOWN" : id.getPath().toUpperCase(Locale.ROOT);
        *///?} else {
        /*Identifier id = net.minecraft.core.Registry.MOB_EFFECT.getKey(e.getEffect());
        return id == null ? "UNKNOWN" : id.getPath().toUpperCase(Locale.ROOT);
        *///?}
    }

    //? if <1.20.5 {
    /*public static Identifier enchantment(net.minecraft.world.item.enchantment.Enchantment e) {
        //? if >=1.19.3 {
        return net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.getKey(e);
        //?} else
        /^return net.minecraft.core.Registry.ENCHANTMENT.getKey(e);^/
    }
    *///?}

    /** Nom du biome en majuscules (PLAINS). */
    public static String biome(ServerLevel level, BlockPos pos) {
        //? if >=1.18.2 {
        return level.getBiome(pos).unwrapKey().map(Keys::path).orElse("unknown").toUpperCase(Locale.ROOT);
        //?} else {
        /*Identifier id = level.registryAccess().registryOrThrow(net.minecraft.core.Registry.BIOME_REGISTRY).getKey(level.getBiome(pos));
        return id == null ? "UNKNOWN" : id.getPath().toUpperCase(Locale.ROOT);
        *///?}
    }
}
