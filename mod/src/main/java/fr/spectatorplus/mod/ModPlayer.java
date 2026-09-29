package fr.spectatorplus.mod;

import fr.spectatorplus.compat.Sounds;
import fr.spectatorplus.core.platform.EffectInfo;
import fr.spectatorplus.core.platform.GameMode;
import fr.spectatorplus.core.platform.Icon;
import fr.spectatorplus.core.platform.ItemRef;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.PlatformWorld;
import fr.spectatorplus.core.platform.Position;
import fr.spectatorplus.core.platform.Vector3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Joueur d'un serveur moddé vu par le code commun. L'entité ServerPlayer est recréée à chaque
 * réapparition : elle est retrouvée par UUID à chaque appel.
 */
public final class ModPlayer implements PlatformPlayer {

    private final ModPlatform platform;
    private final UUID id;
    private ServerPlayer last;

    ModPlayer(ModPlatform platform, ServerPlayer player) {
        this.platform = platform;
        this.id = player.getUUID();
        this.last = player;
    }

    /** Entité actuelle (dernière connue si le joueur est déconnecté). */
    public ServerPlayer handle() {
        ServerPlayer live = platform.server().getPlayerList().getPlayer(id);
        if (live != null) last = live;
        return last;
    }

    public static ServerPlayer unwrap(PlatformPlayer p) {
        return p == null ? null : ((ModPlayer) p).handle();
    }

    @Override
    public UUID getUniqueId() {
        return id;
    }

    @Override
    public String getName() {
        return handle().getScoreboardName();
    }

    @Override
    public boolean hasPermission(String permission) {
        return platform.permissions().has(handle(), permission);
    }

    @Override
    public void sendMessage(String message) {
        Mc.sendMessage(handle(), Texts.of(message));
    }

    @Override
    public boolean isOnline() {
        return platform.server().getPlayerList().getPlayer(id) != null;
    }

    @Override
    public String getClientLocale() {
        return Mc.clientLanguage(handle());
    }

    // ------------------------------------------------------------------ messages

    @Override
    public void sendClickable(String text, String hover, String command) {
        Mc.sendMessage(handle(), Mc.clickable(Texts.of(text), Texts.of(hover), command));
    }

    @Override
    public void actionBar(String text) {
        Mc.actionBar(handle(), Texts.of(text));
    }

    @Override
    public void title(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Mc.title(handle(), Texts.of(title), Texts.of(subtitle), fadeIn, stay, fadeOut);
    }

    @Override
    public void playSound(Sounds sound, float volume, float pitch) {
        if (sound != null) Mc.playSound(handle(), sound.name(true, true), volume, pitch);
    }

    @Override
    public void performCommand(String command) {
        Mc.performCommand(handle(), command);
    }

    // ------------------------------------------------------------------ position

    @Override
    public Position getLocation() {
        ServerPlayer p = handle();
        return new Position(Mc.worldName(Mc.level(p)), p.getX(), p.getY(), p.getZ(), Mc.yaw(p), Mc.pitch(p));
    }

    @Override
    public Position getEyeLocation() {
        ServerPlayer p = handle();
        return new Position(Mc.worldName(Mc.level(p)), p.getX(), p.getEyeY(), p.getZ(), Mc.yaw(p), Mc.pitch(p));
    }

    @Override
    public PlatformWorld getWorld() {
        return new ModWorld(Mc.level(handle()));
    }

    @Override
    public boolean teleport(Position d) {
        ServerLevel level = platform.level(d.getWorld());
        if (level == null) return false;
        return Mc.teleport(handle(), level, d.getX(), d.getY(), d.getZ(), d.getYaw(), d.getPitch());
    }

    @Override
    public void setVelocity(Vector3 v) {
        Mc.setVelocity(handle(), v.getX(), v.getY(), v.getZ());
    }

    @Override
    public void leaveVehicle() {
        handle().stopRiding();
    }

    @Override
    public boolean isInsideVehicle() {
        return handle().isPassenger();
    }

    // ------------------------------------------------------------------ mode de jeu / vol

    @Override
    public GameMode getGameMode() {
        return GameMode.valueOf(Mc.gameMode(handle()).name());
    }

    @Override
    public void setGameMode(GameMode mode) {
        handle().setGameMode(GameType.valueOf(mode.name()));
    }

    private Abilities abilities() {
        return Mc.abilities(handle());
    }

    @Override
    public boolean getAllowFlight() {
        return abilities().mayfly;
    }

    @Override
    public void setAllowFlight(boolean allow) {
        abilities().mayfly = allow;
        if (!allow) abilities().flying = false;
        handle().onUpdateAbilities();
    }

    @Override
    public boolean isFlying() {
        return abilities().flying;
    }

    @Override
    public void setFlying(boolean flying) {
        abilities().flying = flying && abilities().mayfly;
        handle().onUpdateAbilities();
    }

    // Bukkit : 0.1 = vitesse normale ; vanilla : 0.05 en vol, 0.1 en marche
    @Override
    public float getFlySpeed() {
        return abilities().getFlyingSpeed() * 2f;
    }

    @Override
    public void setFlySpeed(float speed) {
        abilities().setFlyingSpeed(speed / 2f);
        handle().onUpdateAbilities();
    }

    @Override
    public float getWalkSpeed() {
        return abilities().getWalkingSpeed() * 2f;
    }

    @Override
    public void setWalkSpeed(float speed) {
        abilities().setWalkingSpeed(speed / 2f);
        handle().onUpdateAbilities();
    }

    // ------------------------------------------------------------------ état

    @Override
    public double getHealth() {
        return handle().getHealth();
    }

    @Override
    public void setHealth(double health) {
        handle().setHealth((float) health);
    }

    @Override
    public double getMaxHealth() {
        return handle().getMaxHealth();
    }

    @Override
    public double getAbsorption() {
        return handle().getAbsorptionAmount();
    }

    @Override
    public int getFoodLevel() {
        return handle().getFoodData().getFoodLevel();
    }

    @Override
    public void setFoodLevel(int food) {
        handle().getFoodData().setFoodLevel(food);
    }

    @Override
    public float getSaturation() {
        return handle().getFoodData().getSaturationLevel();
    }

    @Override
    public void setSaturation(float saturation) {
        handle().getFoodData().setSaturation(saturation);
    }

    @Override
    public int getLevel() {
        return handle().experienceLevel;
    }

    @Override
    public void setLevel(int level) {
        handle().setExperienceLevels(level);
    }

    @Override
    public float getExp() {
        return handle().experienceProgress;
    }

    @Override
    public void setExp(float exp) {
        ServerPlayer p = handle();
        p.experienceProgress = exp;
        p.setExperienceLevels(p.experienceLevel); // renvoie la barre d'expérience au client
    }

    @Override
    public void setFireTicks(int ticks) {
        handle().setRemainingFireTicks(ticks);
    }

    @Override
    public float getFallDistance() {
        return Mc.fallDistance(handle());
    }

    @Override
    public void setFallDistance(float distance) {
        Mc.setFallDistance(handle(), distance);
    }

    @Override
    public boolean isDead() {
        return !handle().isAlive();
    }

    @Override
    public boolean isSneaking() {
        return handle().isShiftKeyDown();
    }

    @Override
    public boolean isSprinting() {
        return handle().isSprinting();
    }

    @Override
    public boolean isOnGround() {
        return Mc.onGround(handle());
    }

    @Override
    public int getHeldSlot() {
        return Mc.selectedSlot(handle());
    }

    @Override
    public String getScoreboardTeam() {
        Team team = handle().getTeam();
        return team == null ? null : team.getName();
    }

    @Override
    public boolean isFake() {
        return ModPlatform.isFakePlayer(handle());
    }

    // ------------------------------------------------------------------ spectateur

    @Override
    public void setSpectatorTarget(PlatformPlayer target) {
        ServerPlayer p = handle();
        ServerPlayer t = unwrap(target);
        p.setCamera(t == null ? p : t);
    }

    @Override
    public UUID getSpectatorTargetId() {
        ServerPlayer p = handle();
        Entity camera = p.getCamera();
        return camera == null || camera == p ? null : camera.getUUID();
    }

    @Override
    public void setCanPickupItems(boolean value) {
        platform.flags(id).pickup = value;
    }

    @Override
    public void setSleepingIgnored(boolean value) {
        platform.flags(id).sleepIgnored = value;
    }

    @Override
    public void setCollidable(boolean value) {
        platform.flags(id).collidable = value;
    }

    @Override
    public void setAffectsSpawning(boolean value) {
        platform.flags(id).affectsSpawning = value;
    }

    @Override
    public void setInvulnerable(boolean value) {
        abilities().invulnerable = value;
        handle().onUpdateAbilities();
    }

    @Override
    public void setHidden(PlatformPlayer target, boolean hidden) {
        platform.visibility().setHidden(handle(), unwrap(target), hidden);
    }

    @Override
    public void setHealthDisplay(boolean enabled, String title) {
        platform.healthDisplay().set(handle(), enabled, title);
    }

    // ------------------------------------------------------------------ effets

    @Override
    public List<EffectInfo> getEffects() {
        List<EffectInfo> res = new ArrayList<>();
        for (MobEffectInstance e : handle().getActiveEffects()) {
            res.add(new EffectInfo(Keys.effectName(e), e.getAmplifier(), e.getDuration()));
        }
        return res;
    }

    @Override
    public void clearEffects() {
        handle().removeAllEffects();
    }

    @Override
    public void addEffect(String name, int duration, int amplifier, boolean ambient, boolean particles) {
        if (name.equals("NIGHT_VISION")) {
            handle().addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, amplifier, ambient, particles));
        } else if (name.equals("INVISIBILITY")) {
            handle().addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, amplifier, ambient, particles));
        }
    }

    // ------------------------------------------------------------------ inventaire

    @Override
    public void closeInventory() {
        handle().closeContainer();
    }

    @Override
    public void clearInventory() {
        Mc.inventory(handle()).clearContent();
    }

    @Override
    public void setItem(int slot, Icon icon) {
        Mc.inventory(handle()).setItem(slot, ModItems.render(icon, platform));
    }

    @Override
    public void updateInventory() {
        Mc.resyncInventory(handle());
    }

    @Override
    public ItemRef[] getStorageContents() {
        Inventory inv = Mc.inventory(handle());
        ModItem[] res = new ModItem[36];
        for (int i = 0; i < 36; i++) res[i] = ModItem.of(inv.getItem(i));
        return res;
    }

    @Override
    public ItemRef[] getArmorContents() {
        ServerPlayer p = handle();
        return new ItemRef[]{ModItem.of(p.getItemBySlot(EquipmentSlot.FEET)), ModItem.of(p.getItemBySlot(EquipmentSlot.LEGS)),
                ModItem.of(p.getItemBySlot(EquipmentSlot.CHEST)), ModItem.of(p.getItemBySlot(EquipmentSlot.HEAD))};
    }

    @Override
    public ItemRef getOffHand() {
        return ModItem.of(handle().getItemBySlot(EquipmentSlot.OFFHAND));
    }

    @Override
    public ItemRef getMainHand() {
        return ModItem.of(handle().getMainHandItem());
    }

    @Override
    public ItemRef[] getEnderChest() {
        net.minecraft.world.inventory.PlayerEnderChestContainer ec = handle().getEnderChestInventory();
        ItemStack[] items = new ItemStack[ec.getContainerSize()];
        for (int i = 0; i < items.length; i++) items[i] = ec.getItem(i);
        return ModItem.of(items);
    }

    @Override
    public String getBiome() {
        ServerPlayer p = handle();
        return Keys.biome(Mc.level(p), p.blockPosition());
    }

    // ------------------------------------------------------------------ identité

    @Override
    public boolean equals(Object o) {
        return o instanceof PlatformPlayer && ((PlatformPlayer) o).getUniqueId().equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return getName();
    }
}
