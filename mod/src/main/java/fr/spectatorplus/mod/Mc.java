package fr.spectatorplus.mod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

import java.util.Collections;

/**
 * Tout ce qui change d'une version de Minecraft à l'autre (mappings Mojang), au même endroit.
 * Les variantes sont choisies à la compilation par Stonecutter (commentaires « //? if »).
 */
public final class Mc {

    private Mc() {
    }

    // ------------------------------------------------------------------ texte

    public static MutableComponent literal(String s) {
        //? if >=1.19 {
        return Component.literal(s);
        //?} else
        /*return new net.minecraft.network.chat.TextComponent(s);*/
    }

    public static void sendMessage(ServerPlayer p, Component c) {
        //? if >=1.19 {
        p.sendSystemMessage(c);
        //?} else
        /*p.sendMessage(c, net.minecraft.Util.NIL_UUID);*/
    }

    public static void actionBar(ServerPlayer p, Component c) {
        //? if >=26.1 {
        p.sendOverlayMessage(c);
        //?} else
        /*p.displayClientMessage(c, true);*/
    }

    public static void title(ServerPlayer p, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        //? if >=1.17 {
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(subtitle));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(title));
        //?} else {
        /*p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesPacket(fadeIn, stay, fadeOut));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesPacket(
                net.minecraft.network.protocol.game.ClientboundSetTitlesPacket.Type.SUBTITLE, subtitle));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesPacket(
                net.minecraft.network.protocol.game.ClientboundSetTitlesPacket.Type.TITLE, title));
        *///?}
    }

    /** Ajoute une commande au clic et une info-bulle. */
    public static MutableComponent clickable(Component text, Component hover, String command) {
        MutableComponent c = literal("").append(text);
        //? if >=1.21.5 {
        return c.withStyle(s -> s.withClickEvent(new net.minecraft.network.chat.ClickEvent.RunCommand(command))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(hover)));
        //?} else {
        /*return c.withStyle(s -> s.withClickEvent(new net.minecraft.network.chat.ClickEvent(
                        net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                        net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, hover)));
        *///?}
    }

    // ------------------------------------------------------------------ scoreboard (paquets)

    /** Affiche l'objectif sous le pseudo des joueurs. */
    public static net.minecraft.network.protocol.Packet<?> displayBelowName(net.minecraft.world.scores.Objective o) {
        //? if >=1.20.2 {
        return new net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket(net.minecraft.world.scores.DisplaySlot.BELOW_NAME, o);
        //?} else
        /*return new net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket(2, o);*/
    }

    /** Score d'un joueur dans un objectif. */
    public static net.minecraft.network.protocol.Packet<?> score(String owner, String objective, int value) {
        //? if >=1.20.5 {
        return new net.minecraft.network.protocol.game.ClientboundSetScorePacket(owner, objective, value, java.util.Optional.empty(), java.util.Optional.empty());
        //?} elif >=1.20.3 {
        /*return new net.minecraft.network.protocol.game.ClientboundSetScorePacket(owner, objective, value, null, null);
        *///?} else
        /*return new net.minecraft.network.protocol.game.ClientboundSetScorePacket(net.minecraft.server.ServerScoreboard.Method.CHANGE, objective, owner, value);*/
    }

    // ------------------------------------------------------------------ joueur

    public static ServerLevel level(ServerPlayer p) {
        //? if >=1.21.11 {
        return p.level();
        //?} elif >=1.20 {
        /*return p.serverLevel();
        *///?} else
        /*return p.getLevel();*/
    }

    /** Monde d'une entité quelconque. */
    public static net.minecraft.world.level.Level entityLevel(Entity e) {
        //? if >=1.20 {
        return e.level();
        //?} else
        /*return e.level;*/
    }

    public static Abilities abilities(Player p) {
        //? if >=1.17 {
        return p.getAbilities();
        //?} else
        /*return p.abilities;*/
    }

    public static Inventory inventory(Player p) {
        //? if >=1.17 {
        return p.getInventory();
        //?} else
        /*return p.inventory;*/
    }

    public static int selectedSlot(Player p) {
        //? if >=1.21.5 {
        return inventory(p).getSelectedSlot();
        //?} else
        /*return inventory(p).selected;*/
    }

    public static GameType gameMode(ServerPlayer p) {
        //? if >=1.21.5 {
        return p.gameMode();
        //?} else
        /*return p.gameMode.getGameModeForPlayer();*/
    }

    public static float yaw(Entity e) {
        //? if >=1.17 {
        return e.getYRot();
        //?} else
        /*return e.yRot;*/
    }

    public static float pitch(Entity e) {
        //? if >=1.17 {
        return e.getXRot();
        //?} else
        /*return e.xRot;*/
    }

    public static float fallDistance(Entity e) {
        return (float) e.fallDistance;
    }

    public static void setFallDistance(Entity e, float distance) {
        e.fallDistance = distance;
    }

    public static boolean onGround(Entity e) {
        //? if >=1.20 {
        return e.onGround();
        //?} else
        /*return e.isOnGround();*/
    }

    /** Vélocité envoyée au client. */
    public static void setVelocity(ServerPlayer p, double x, double y, double z) {
        p.setDeltaMovement(x, y, z);
        //? if >=26.1 {
        p.needsSync = true;
        //?} else
        /*p.hurtMarked = true;*/
    }

    public static boolean teleport(ServerPlayer p, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        //? if >=1.21.2 {
        return p.teleportTo(level, x, y, z, Collections.emptySet(), yaw, pitch, false);
        //?} else {
        /*p.teleportTo(level, x, y, z, yaw, pitch);
        return true;
        *///?}
    }

    /** Langue du client (« fr_fr »), ou null. */
    public static String clientLanguage(ServerPlayer p) {
        //? if >=1.20.2 {
        return p.clientInformation().language();
        //?} else
        /*return SpectatorPlusMod.platform() == null ? null : SpectatorPlusMod.platform().language(p.getUUID());*/
    }

    /** Son joué pour ce joueur uniquement (identifiant vanilla : « ui.button.click »). */
    public static void playSound(ServerPlayer p, String id, float volume, float pitch) {
        net.minecraft.resources.Identifier key = net.minecraft.resources.Identifier.tryParse(id);
        if (key == null) return;
        //? if >=1.19.3 {
        net.minecraft.sounds.SoundEvent event = net.minecraft.sounds.SoundEvent.createVariableRangeEvent(key);
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.Holder.direct(event),
                net.minecraft.sounds.SoundSource.MASTER, p.getX(), p.getY(), p.getZ(), volume, pitch, p.getRandom().nextLong()));
        //?} else {
        /*net.minecraft.sounds.SoundEvent event = net.minecraft.core.Registry.SOUND_EVENT.get(key);
        if (event != null) p.playNotifySound(event, net.minecraft.sounds.SoundSource.MASTER, volume, pitch);
        *///?}
    }

    /** Renvoie tout l'inventaire au client (après une action annulée). */
    public static void resyncInventory(ServerPlayer p) {
        //? if >=1.17 {
        p.containerMenu.sendAllDataToRemote();
        //?} else
        /*p.refreshContainer(p.containerMenu);*/
    }

    public static void performCommand(ServerPlayer p, String command) {
        MinecraftServer server = server(p);
        //? if >=1.19 {
        server.getCommands().performPrefixedCommand(p.createCommandSourceStack(), command);
        //?} else
        /*server.getCommands().performCommand(p.createCommandSourceStack(), "/" + command);*/
    }

    public static MinecraftServer server(ServerPlayer p) {
        return level(p).getServer();
    }

    /** Joueur opérateur (niveau 2, comme la valeur « op » des permissions Bukkit). */
    public static boolean isOperator(ServerPlayer p) {
        //? if >=1.21.11 {
        return p.createCommandSourceStack().permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
        //?} else
        /*return p.hasPermissions(2);*/
    }

    public static boolean isOperator(net.minecraft.commands.CommandSourceStack source) {
        //? if >=1.21.11 {
        return source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
        //?} else
        /*return source.hasPermission(2);*/
    }

    // ------------------------------------------------------------------ sauvegarde

    /** Objet → texte SNBT (vide pour un objet vide). */
    public static String saveItem(MinecraftServer server, net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        //? if >=1.20.5 {
        return net.minecraft.world.item.ItemStack.CODEC.encodeStart(
                        net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, server.registryAccess()), stack)
                .result().map(Object::toString).orElse("");
        //?} else
        /*return stack.save(new net.minecraft.nbt.CompoundTag()).toString();*/
    }

    public static net.minecraft.world.item.ItemStack loadItem(MinecraftServer server, String snbt) {
        if (snbt == null || snbt.isEmpty()) return net.minecraft.world.item.ItemStack.EMPTY;
        try {
            //? if >=1.21.5 {
            net.minecraft.nbt.CompoundTag tag = net.minecraft.nbt.TagParser.parseCompoundFully(snbt);
            //?} else
            /*net.minecraft.nbt.CompoundTag tag = net.minecraft.nbt.TagParser.parseTag(snbt);*/
            //? if >=1.20.5 {
            return net.minecraft.world.item.ItemStack.CODEC.parse(
                            net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, server.registryAccess()), tag)
                    .result().orElse(net.minecraft.world.item.ItemStack.EMPTY);
            //?} else
            /*return net.minecraft.world.item.ItemStack.of(tag);*/
        } catch (Exception e) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
    }

    /** Effet de potion par nom (SPEED, minecraft:speed...), ou null. */
    public static net.minecraft.world.effect.MobEffectInstance effect(String name, int duration, int amplifier, boolean ambient, boolean visible) {
        net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.tryParse(name.toLowerCase(java.util.Locale.ROOT));
        if (id == null) return null;
        //? if >=1.21.2 {
        return net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(id)
                .map(h -> new net.minecraft.world.effect.MobEffectInstance(h, duration, amplifier, ambient, visible)).orElse(null);
        //?} elif >=1.20.5 {
        /*return net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(id)
                .map(h -> new net.minecraft.world.effect.MobEffectInstance(h, duration, amplifier, ambient, visible)).orElse(null);
        *///?} elif >=1.19.3 {
        /*net.minecraft.world.effect.MobEffect effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(id);
        return effect == null ? null : new net.minecraft.world.effect.MobEffectInstance(effect, duration, amplifier, ambient, visible);
        *///?} else {
        /*net.minecraft.world.effect.MobEffect effect = net.minecraft.core.Registry.MOB_EFFECT.get(id);
        return effect == null ? null : new net.minecraft.world.effect.MobEffectInstance(effect, duration, amplifier, ambient, visible);
        *///?}
    }

    // ------------------------------------------------------------------ monde

    public static String worldName(ServerLevel level) {
        //? if >=1.21.11 {
        return level.dimension().identifier().toString();
        //?} else
        /*return level.dimension().location().toString();*/
    }

    public static int minHeight(ServerLevel level) {
        //? if >=1.21.2 {
        return level.getMinY();
        //?} elif >=1.17 {
        /*return level.getMinBuildHeight();
        *///?} else
        /*return 0;*/
    }

    public static BlockPos spawn(ServerLevel level) {
        //? if >=1.21.9 {
        return level.getRespawnData().pos();
        //?} else
        /*return level.getSharedSpawnPos();*/
    }
}
