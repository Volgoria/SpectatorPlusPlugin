package fr.spectatorplus.mod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.Position;
import fr.spectatorplus.spectator.SpectatorSession;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Spectator Plus sur serveur moddé, partie commune aux loaders (Fabric, NeoForge, Forge).
 * Le point d'entrée de chaque loader transmet ici le démarrage, les ticks, les connexions et les interactions ;
 * les mixins (package mixin) appellent les méthodes statiques.
 */
public final class SpectatorPlusMod {

    public static final String[] COMMANDS = {"spectatorplus", "spec", "sp", "spectate"};

    private static ModPlatform platform;
    private static SpectatorCore core;

    /** Suivi par joueur pour les vérifications de chaque tick. */
    private static final class Track {
        boolean sneaking;
        String world;
        Position frozenAt;
        long lastInteraction;
    }

    private static final Map<UUID, Track> tracks = new HashMap<>();
    /** Niveau d'expérience de chaque joueur au tick précédent. */
    private static final Map<UUID, Integer> levels = new HashMap<>();
    private static long tick;

    private SpectatorPlusMod() {
    }

    public static ModPlatform platform() {
        return platform;
    }

    public static SpectatorCore core() {
        return core;
    }

    // ------------------------------------------------------------------ cycle de vie

    public static void start(MinecraftServer server, File configDir, Logger logger, String loaderName) {
        platform = new ModPlatform(server, configDir, logger, loaderName);
        core = new SpectatorCore(platform);
        core.load();
        platform.attach(core);
        core.enable();
        logger.info("Spectator Plus activé (" + platform.describe() + ", mode " + core.getMode() + ", stockage "
                + core.storage().name() + ", " + core.events().getTypes().size() + " évènements).");
    }

    public static void stop() {
        if (core != null) core.disable();
        if (platform != null) platform.shutdown();
        core = null;
        platform = null;
        tracks.clear();
        levels.clear();
    }

    public static void tick() {
        if (platform == null) return;
        tick++;
        platform.tick();
        for (ServerPlayer sp : platform.server().getPlayerList().getPlayers()) {
            Integer old = levels.put(sp.getUUID(), sp.experienceLevel);
            if (old != null && old != sp.experienceLevel) ModSignals.levelChange(sp, old, sp.experienceLevel);
        }
        for (SpectatorSession s : core.spectators().getSpectators()) {
            PlatformPlayer p = s.getPlayer();
            if (p == null) continue;
            check(p);
        }
    }

    /** Vérifications de chaque tick pour un spectateur : POV (accroupi), gel, vide, changement de monde. */
    private static void check(PlatformPlayer p) {
        Track t = track(p.getUniqueId());
        ServerPlayer sp = ModPlayer.unwrap(p);
        boolean sneaking = sp.isShiftKeyDown();
        if (sneaking && !t.sneaking) core.interactions().sneak(p);
        t.sneaking = sneaking;

        String world = Mc.worldName(Mc.level(sp));
        if (t.world != null && !t.world.equals(world)) core.interactions().worldChanged(p);
        t.world = world;

        if (core.interactions().isFrozen(p)) {
            Position cur = p.getLocation();
            if (t.frozenAt == null || !t.frozenAt.sameWorld(cur)) t.frozenAt = cur;
            else if (t.frozenAt.distanceSquared(cur) > 1.0E-4) p.teleport(t.frozenAt.withRotation(cur.getYaw(), cur.getPitch()));
        } else {
            t.frozenAt = null;
        }

        if (sp.getY() < Mc.minHeight(Mc.level(sp)) - 5) core.interactions().voidDamage(p);
    }

    private static Track track(UUID id) {
        Track t = tracks.get(id);
        if (t == null) {
            t = new Track();
            tracks.put(id, t);
        }
        return t;
    }

    // ------------------------------------------------------------------ connexion

    public static void join(ServerPlayer player) {
        if (core == null) return;
        PlatformPlayer p = platform.wrap(player);
        core.filters().loadPlayer(p.getUniqueId());
        core.spectators().handleJoin(p);
        ModSignals.join(player);
    }

    public static void quit(ServerPlayer player) {
        if (core == null) return;
        PlatformPlayer p = platform.wrap(player);
        ModSignals.quit(player);
        core.menus().forget(p);
        core.spectators().handleQuit(p);
        core.filters().unload(p.getUniqueId());
        core.events().forget(p.getUniqueId());
        core.interactions().quit(p.getUniqueId());
        platform.forget(p.getUniqueId());
        tracks.remove(p.getUniqueId());
        levels.remove(p.getUniqueId());
    }

    /** Réapparition (nouvelle entité joueur). */
    public static void respawn(ServerPlayer player) {
        if (core == null) return;
        PlatformPlayer p = platform.wrap(player);
        Position override = core.interactions().respawn(p);
        if (override != null) p.teleport(override);
        ModSignals.respawn(player);
    }

    /** Mort d'un joueur (mixin sur ServerPlayer#die). */
    public static void death(final ServerPlayer player) {
        if (core == null) return;
        PlatformPlayer p = platform.wrap(player);
        if (core.spectators().isSpectator(p)) {
            // un spectateur ne lâche rien : son vrai inventaire est dans l'état sauvegardé
            Mc.inventory(player).clearContent();
            player.experienceLevel = 0;
            player.experienceProgress = 0;
        }
        if (core.interactions().death(p)) {
            final UUID id = player.getUUID();
            platform.runLater(new Runnable() {
                @Override
                public void run() {
                    ServerPlayer live = platform.server().getPlayerList().getPlayer(id);
                    if (live != null && !live.isAlive()) {
                        live.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                    }
                }
            }, 2L);
        }
    }

    // ------------------------------------------------------------------ protections (mixins et évènements des loaders)

    public static boolean isSpectator(Entity e) {
        return core != null && e instanceof ServerPlayer && core.spectators().isSpectator(e.getUUID());
    }

    public static boolean isHiddenFrom(ServerPlayer viewer, ServerPlayer target) {
        return platform != null && platform.visibility().isHidden(viewer.getUUID(), target.getUUID());
    }

    public static boolean canPickup(Player player) {
        if (platform == null || !(player instanceof ServerPlayer)) return true;
        ModPlatform.Flags f = platform.flagsOrNull(player.getUUID());
        return f == null || f.pickup;
    }

    public static boolean isCollidable(Entity entity) {
        if (platform == null || !(entity instanceof ServerPlayer)) return true;
        ModPlatform.Flags f = platform.flagsOrNull(entity.getUUID());
        return f == null || f.collidable;
    }

    /** Langue du client captée par mixin (versions sans ClientInformation). */
    public static void language(ServerPlayer player, String language) {
        if (platform != null) platform.setLanguage(player.getUUID(), language);
    }

    // ------------------------------------------------------------------ interactions

    /** Clic avec un objet (droit, ou gauche sur un bloc) : true si l'action doit être annulée. */
    public static boolean useItem(ServerPlayer player, boolean leftClick) {
        if (core == null) return false;
        if (!isSpectator(player)) {
            if (!leftClick) ModSignals.itemUse(player, player.getMainHandItem());
            return false;
        }
        track(player.getUUID()).lastInteraction = tick;
        core.interactions().useHotbar(platform.wrap(player), leftClick);
        return true;
    }

    /** Clic sur une entité : true si l'action doit être annulée. */
    public static boolean interactEntity(ServerPlayer player, Entity target, boolean leftClick) {
        if (!isSpectator(player)) return false;
        track(player.getUUID()).lastInteraction = tick;
        if (target instanceof ServerPlayer) {
            PlatformPlayer p = platform.wrap(player), t = platform.wrap((ServerPlayer) target);
            if (leftClick) core.interactions().leftClickPlayer(p, t);
            else core.interactions().rightClickPlayer(p, t);
        }
        return true;
    }

    /** Bras levé sans rien viser (clic gauche dans le vide). */
    public static void swing(ServerPlayer player) {
        if (!isSpectator(player)) return;
        Track t = track(player.getUUID());
        if (t.lastInteraction >= tick - 1) return; // déjà traité (clic sur un bloc ou une entité)
        core.interactions().useHotbar(platform.wrap(player), true);
    }

    /** Clic dans l'inventaire du joueur, jet d'objet, échange de main : true pour annuler. */
    public static boolean blockInventoryAction(ServerPlayer player) {
        if (!isSpectator(player)) return false;
        Mc.resyncInventory(player);
        return true;
    }

    // ------------------------------------------------------------------ commande

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (final String name : COMMANDS) {
            dispatcher.register(Commands.literal(name)
                    .executes(ctx -> run(ctx, name, ""))
                    .then(Commands.argument("args", StringArgumentType.greedyString())
                            .suggests(SpectatorPlusMod::suggest)
                            .executes(ctx -> run(ctx, name, StringArgumentType.getString(ctx, "args")))));
        }
    }

    private static int run(CommandContext<CommandSourceStack> ctx, String label, String args) {
        if (core == null) return 0;
        String[] split = args.trim().isEmpty() ? new String[0] : args.trim().split(" +");
        core.command().execute(platform.sender(ctx.getSource()), label, split);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        if (core == null) return builder.buildFuture();
        String typed = builder.getRemaining();
        String[] args = typed.split(" ", -1);
        List<String> res = core.command().complete(platform.sender(ctx.getSource()), args);
        SuggestionsBuilder last = builder.createOffset(builder.getStart() + typed.lastIndexOf(' ') + 1);
        for (String s : res) last.suggest(s);
        return last.buildFuture();
    }
}
