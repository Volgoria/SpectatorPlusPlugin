package fr.spectatorplus.mod;

import fr.spectatorplus.core.platform.Position;
import fr.spectatorplus.event.detect.Damage;
import fr.spectatorplus.event.detect.EntityInfo;
import fr.spectatorplus.event.detect.Signals;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Traduction des évènements du jeu (mixins, évènements des loaders) en signaux du code commun.
 * Toute la logique des évènements natifs est dans {@link Signals}.
 */
public final class ModSignals {

    /** Causes de dégâts vanilla (identifiant du type de dégâts) → noms de Bukkit. */
    private static final Map<String, String> CAUSES = new HashMap<>();

    static {
        String[][] c = {
                {"inFire", "FIRE"}, {"campfire", "CAMPFIRE"}, {"lightningBolt", "LIGHTNING"}, {"onFire", "FIRE_TICK"},
                {"lava", "LAVA"}, {"hotFloor", "HOT_FLOOR"}, {"inWall", "SUFFOCATION"}, {"cramming", "CRAMMING"},
                {"drown", "DROWNING"}, {"starve", "STARVATION"}, {"cactus", "CONTACT"}, {"sweetBerryBush", "CONTACT"},
                {"stalagmite", "CONTACT"}, {"fall", "FALL"}, {"enderPearl", "FALL"}, {"flyIntoWall", "FLY_INTO_WALL"},
                {"outOfWorld", "VOID"}, {"generic", "CUSTOM"}, {"magic", "MAGIC"}, {"indirectMagic", "MAGIC"},
                {"wither", "WITHER"}, {"witherSkull", "PROJECTILE"}, {"anvil", "FALLING_BLOCK"},
                {"fallingBlock", "FALLING_BLOCK"}, {"fallingStalactite", "FALLING_BLOCK"}, {"dragonBreath", "DRAGON_BREATH"},
                {"dryout", "DRYOUT"}, {"freeze", "FREEZE"}, {"mob", "ENTITY_ATTACK"}, {"player", "ENTITY_ATTACK"},
                {"sting", "ENTITY_ATTACK"}, {"mace_smash", "ENTITY_ATTACK"}, {"spit", "PROJECTILE"},
                {"arrow", "PROJECTILE"}, {"trident", "PROJECTILE"}, {"fireball", "PROJECTILE"}, {"thrown", "PROJECTILE"},
                {"mobProjectile", "PROJECTILE"}, {"fireworks", "ENTITY_EXPLOSION"}, {"thorns", "THORNS"},
                {"sonic_boom", "SONIC_BOOM"}, {"outsideBorder", "WORLD_BORDER"}, {"genericKill", "KILL"},
                {"badRespawnPoint", "BLOCK_EXPLOSION"}};
        for (String[] e : c) CAUSES.put(e[0], e[1]);
    }

    private static final Set<UUID> summoned = new HashSet<>();

    private ModSignals() {
    }

    private static Signals signals() {
        return SpectatorPlusMod.core() == null ? null : SpectatorPlusMod.core().signals();
    }

    private static ModPlatform platform() {
        return SpectatorPlusMod.platform();
    }

    // ------------------------------------------------------------------ conversions

    /** Type d'entité façon Bukkit (ZOMBIE, ENDER_DRAGON...). */
    public static String entityType(Entity e) {
        return Keys.entityName(e.getType());
    }

    static EntityInfo entity(LivingEntity e) {
        return e == null ? null : new EntityInfo(e.getUUID(), entityType(e), e.getHealth());
    }

    /** Cause de dégâts façon Bukkit. */
    public static String cause(DamageSource source) {
        if (source == null) return "CUSTOM";
        String id = source.getMsgId();
        if (id.startsWith("explosion")) return source.getEntity() != null || source.getDirectEntity() != null ? "ENTITY_EXPLOSION" : "BLOCK_EXPLOSION";
        String c = CAUSES.get(id);
        return c != null ? c : id.toUpperCase(Locale.ROOT);
    }

    private static Position position(ServerLevel level, BlockPos pos) {
        return new Position(Mc.worldName(level), pos.getX(), pos.getY(), pos.getZ());
    }

    private static boolean ready() {
        return signals() != null;
    }

    // ------------------------------------------------------------------ dégâts / morts

    /** Dégâts réellement subis (vie + absorption perdues), appelé après LivingEntity#actuallyHurt. */
    public static void damage(LivingEntity victim, DamageSource source, double healthBefore, double amount) {
        if (!ready() || amount <= 0) return;
        Damage d = new Damage();
        d.amount = amount;
        d.cause = cause(source);
        d.victimHealthBefore = healthBefore;
        if (victim instanceof ServerPlayer) {
            ServerPlayer v = (ServerPlayer) victim;
            d.victim = platform().wrap(v);
            d.victimBlocking = v.isBlocking();
            d.blocked = v.isBlocking();
        } else {
            d.victimMob = new EntityInfo(victim.getUUID(), entityType(victim), healthBefore);
        }
        Entity cause = source.getEntity(), direct = source.getDirectEntity();
        if (direct != null) {
            d.directType = entityType(direct);
            d.projectile = direct instanceof Projectile;
        }
        if (cause instanceof ServerPlayer) {
            ServerPlayer a = (ServerPlayer) cause;
            d.attacker = platform().wrap(a);
            if (direct == a) {
                d.weapon = ModItem.of(a.getMainHandItem());
                d.critical = critical(a);
            }
        } else if (cause instanceof LivingEntity) {
            d.attackerMob = entity((LivingEntity) cause);
        }
        if (d.victim != null) signals().player().damage(d);
        if (d.attacker != null || d.attackerMob != null) signals().combat().entityDamage(d);
    }

    /** Mêmes règles que le coup critique vanilla (et que la détection Bukkit). */
    private static boolean critical(ServerPlayer p) {
        return Mc.fallDistance(p) > 0 && !Mc.onGround(p) && !p.onClimbable() && !p.isInWater() && !p.isPassenger()
                && !p.hasEffect(MobEffects.BLINDNESS) && !p.isSprinting();
    }

    public static void playerDeath(ServerPlayer p, DamageSource source) {
        if (!ready()) return;
        Entity cause = source == null ? null : source.getEntity();
        LivingEntity credit = p.getKillCredit();
        ServerPlayer killer = cause instanceof ServerPlayer ? (ServerPlayer) cause
                : credit instanceof ServerPlayer ? (ServerPlayer) credit : null;
        LivingEntity mob = cause instanceof LivingEntity && !(cause instanceof ServerPlayer) ? (LivingEntity) cause : null;
        signals().combat().playerDeath(platform().wrap(p), cause(source), killer == null ? null : platform().wrap(killer), entity(mob));
    }

    public static void entityDeath(LivingEntity mob, DamageSource source) {
        if (!ready() || mob instanceof ServerPlayer) return;
        Entity cause = source == null ? null : source.getEntity();
        LivingEntity credit = mob.getKillCredit();
        ServerPlayer killer = cause instanceof ServerPlayer ? (ServerPlayer) cause
                : credit instanceof ServerPlayer ? (ServerPlayer) credit : null;
        signals().combat().entityDeath(new EntityInfo(mob.getUUID(), entityType(mob), 0), killer == null ? null : platform().wrap(killer));
        summoned.remove(mob.getUUID());
    }

    public static void respawn(ServerPlayer p) {
        if (ready()) signals().combat().respawn(platform().wrap(p));
    }

    /** Une créature prend un joueur pour cible. */
    public static void target(LivingEntity mob, LivingEntity target) {
        if (ready() && target instanceof ServerPlayer) signals().combat().target(platform().wrap((ServerPlayer) target), entityType(mob));
    }

    /** Entité invoquée par un joueur (wither, golems...) : seul le wither est un évènement. */
    public static void summoned(Entity e) {
        if (!ready() || !entityType(e).equals("WITHER") || !summoned.add(e.getUUID())) return;
        signals().combat().witherSummon(position((ServerLevel) Mc.entityLevel(e), e.blockPosition()));
    }

    public static void totem(ServerPlayer p) {
        if (ready()) signals().player().totem(platform().wrap(p));
    }

    // ------------------------------------------------------------------ monde / blocs

    public static void worldChanged(ServerPlayer p, ServerLevel from, ServerLevel to) {
        if (!ready() || from == null || to == null) return;
        signals().world().worldChanged(platform().wrap(p), new ModWorld(from), new ModWorld(to));
        boolean nether = from.dimension() == net.minecraft.world.level.Level.NETHER || to.dimension() == net.minecraft.world.level.Level.NETHER;
        boolean end = from.dimension() == net.minecraft.world.level.Level.END || to.dimension() == net.minecraft.world.level.Level.END;
        if (nether) signals().world().portal(platform().wrap(p), "NETHER_PORTAL");
        else if (end) signals().world().portal(platform().wrap(p), "END_PORTAL");
    }

    public static void blockBreak(ServerPlayer p, ServerLevel level, BlockPos pos, String type, ItemStack tool) {
        if (ready()) signals().mining().blockBreak(platform().wrap(p), type, position(level, pos), ModItem.of(tool));
    }

    public static void blockPlace(ServerPlayer p, ServerLevel level, BlockPos pos) {
        if (!ready()) return;
        signals().mining().blockPlace(platform().wrap(p), Keys.blockName(level.getBlockState(pos).getBlock()), position(level, pos));
    }

    // ------------------------------------------------------------------ objets

    public static void consume(ServerPlayer p, ItemStack stack) {
        if (ready()) signals().player().consume(platform().wrap(p), ModItem.of(stack), !p.getActiveEffects().isEmpty());
    }

    /** Clic droit avec un objet (hors spectateurs) : utilisation, perle, potion lancée. */
    public static void itemUse(ServerPlayer p, ItemStack stack) {
        if (!ready() || stack.isEmpty()) return;
        ModItem it = ModItem.of(stack);
        signals().player().itemUse(platform().wrap(p), it);
        String type = it.getType();
        if (type.equals("ENDER_PEARL")) signals().player().projectileLaunch(platform().wrap(p), "ENDER_PEARL");
        if (type.equals("SPLASH_POTION") || type.equals("LINGERING_POTION")) signals().items().potionThrow(platform().wrap(p), it);
    }

    public static void enchanted(ServerPlayer p, ItemStack stack, int levels) {
        if (!ready()) return;
        ModItem it = ModItem.of(stack);
        Map<String, Integer> added = it.getType().equals("ENCHANTED_BOOK") ? it.getStoredEnchantments() : it.getEnchantments();
        signals().items().enchant(platform().wrap(p), it, added, levels);
    }

    public static void craft(ServerPlayer p, ItemStack result) {
        if (ready()) signals().items().craft(platform().wrap(p), ModItem.of(result));
    }

    public static void furnace(ServerPlayer p, ItemStack result) {
        if (ready()) signals().items().furnace(platform().wrap(p), ModItem.of(result));
    }

    public static void anvil(ServerPlayer p, ItemStack left, ItemStack right, ItemStack result, int cost) {
        if (ready()) signals().items().anvil(platform().wrap(p), ModItem.of(left), ModItem.of(right), ModItem.of(result), cost);
    }

    public static void pickup(ServerPlayer p, ItemStack picked) {
        if (ready() && !picked.isEmpty()) signals().items().pickup(platform().wrap(p), ModItem.of(picked));
    }

    public static void drop(ServerPlayer p, ItemStack dropped) {
        if (ready() && !dropped.isEmpty()) signals().items().drop(platform().wrap(p), ModItem.of(dropped));
    }

    public static void itemBreak(ServerPlayer p, ItemStack broken) {
        if (ready() && !broken.isEmpty()) signals().items().itemBreak(platform().wrap(p), ModItem.of(broken));
    }

    // ------------------------------------------------------------------ connexion

    public static void join(ServerPlayer p) {
        if (ready()) signals().player().join(platform().wrap(p));
    }

    public static void quit(ServerPlayer p) {
        if (!ready()) return;
        signals().player().quit(platform().wrap(p));
        signals().polling().quit(p.getUUID());
    }

    public static void levelChange(ServerPlayer p, int oldLevel, int newLevel) {
        if (ready()) signals().player().levelChange(platform().wrap(p), oldLevel, newLevel);
    }
}
