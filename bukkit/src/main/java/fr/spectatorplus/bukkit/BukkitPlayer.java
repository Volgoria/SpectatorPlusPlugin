package fr.spectatorplus.bukkit;

import fr.spectatorplus.compat.Compat;
import fr.spectatorplus.compat.Positions;
import fr.spectatorplus.compat.Reflect;
import fr.spectatorplus.compat.Sounds;
import fr.spectatorplus.core.platform.EffectInfo;
import fr.spectatorplus.core.platform.GameMode;
import fr.spectatorplus.core.platform.Icon;
import fr.spectatorplus.core.platform.ItemRef;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.PlatformWorld;
import fr.spectatorplus.core.platform.Position;
import fr.spectatorplus.core.platform.Vector3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Joueur Bukkit vu par le code commun. Une instance par UUID (voir {@link BukkitPlatform#wrap(Player)}).
 */
public final class BukkitPlayer implements PlatformPlayer {

    private final Plugin plugin;
    private final BukkitPlatform platform;
    private final UUID id;
    private Player last;

    BukkitPlayer(Plugin plugin, BukkitPlatform platform, Player player) {
        this.plugin = plugin;
        this.platform = platform;
        this.id = player.getUniqueId();
        this.last = player;
    }

    /** Joueur Bukkit (dernier objet connu si le joueur est déconnecté). */
    public Player handle() {
        Player live = Bukkit.getPlayer(id);
        if (live != null) last = live;
        return last;
    }

    public static Player unwrap(PlatformPlayer p) {
        return p == null ? null : ((BukkitPlayer) p).handle();
    }

    @Override
    public UUID getUniqueId() {
        return id;
    }

    @Override
    public String getName() {
        return handle().getName();
    }

    @Override
    public boolean hasPermission(String permission) {
        return handle().hasPermission(permission);
    }

    @Override
    public void sendMessage(String message) {
        handle().sendMessage(message);
    }

    @Override
    public boolean isOnline() {
        return Bukkit.getPlayer(id) != null && handle().isOnline();
    }

    @Override
    public String getClientLocale() {
        Player p = handle();
        Object loc = Reflect.invoke(p, "getLocale");
        if (loc == null) loc = Reflect.invoke(Reflect.invoke(p, "spigot"), "getLocale");
        return loc == null ? null : loc.toString();
    }

    // ------------------------------------------------------------------ messages

    @Override
    public void sendClickable(String text, String hover, String command) {
        Compat.clickable(handle(), text, hover, command);
    }

    @Override
    public void actionBar(String text) {
        Compat.actionBar(handle(), text);
    }

    @Override
    public void title(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Compat.title(handle(), title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void playSound(Sounds sound, float volume, float pitch) {
        Compat.playSound(handle(), sound, volume, pitch);
    }

    @Override
    public void performCommand(String command) {
        handle().performCommand(command);
    }

    // ------------------------------------------------------------------ position

    @Override
    public Position getLocation() {
        return Positions.of(handle().getLocation());
    }

    @Override
    public Position getEyeLocation() {
        return Positions.of(handle().getEyeLocation());
    }

    @Override
    public PlatformWorld getWorld() {
        return new BukkitWorld(handle().getWorld());
    }

    @Override
    public boolean teleport(Position destination) {
        Location l = Positions.toLocation(destination);
        return l != null && handle().teleport(l);
    }

    @Override
    public void setVelocity(Vector3 v) {
        handle().setVelocity(new Vector(v.getX(), v.getY(), v.getZ()));
    }

    @Override
    public void leaveVehicle() {
        handle().leaveVehicle();
    }

    @Override
    public boolean isInsideVehicle() {
        return handle().isInsideVehicle();
    }

    // ------------------------------------------------------------------ mode de jeu / vol

    @Override
    public GameMode getGameMode() {
        return GameMode.valueOf(handle().getGameMode().name());
    }

    @Override
    public void setGameMode(GameMode mode) {
        handle().setGameMode(org.bukkit.GameMode.valueOf(mode.name()));
    }

    @Override
    public boolean getAllowFlight() {
        return handle().getAllowFlight();
    }

    @Override
    public void setAllowFlight(boolean allow) {
        handle().setAllowFlight(allow);
    }

    @Override
    public boolean isFlying() {
        return handle().isFlying();
    }

    @Override
    public void setFlying(boolean flying) {
        handle().setFlying(flying);
    }

    @Override
    public float getFlySpeed() {
        return handle().getFlySpeed();
    }

    @Override
    public void setFlySpeed(float speed) {
        handle().setFlySpeed(speed);
    }

    @Override
    public float getWalkSpeed() {
        return handle().getWalkSpeed();
    }

    @Override
    public void setWalkSpeed(float speed) {
        handle().setWalkSpeed(speed);
    }

    // ------------------------------------------------------------------ état

    @Override
    public double getHealth() {
        return handle().getHealth();
    }

    @Override
    public void setHealth(double health) {
        handle().setHealth(health);
    }

    @Override
    public double getMaxHealth() {
        return Compat.maxHealth(handle());
    }

    @Override
    public double getAbsorption() {
        return Compat.absorption(handle());
    }

    @Override
    public int getFoodLevel() {
        return handle().getFoodLevel();
    }

    @Override
    public void setFoodLevel(int food) {
        handle().setFoodLevel(food);
    }

    @Override
    public float getSaturation() {
        return handle().getSaturation();
    }

    @Override
    public void setSaturation(float saturation) {
        handle().setSaturation(saturation);
    }

    @Override
    public int getLevel() {
        return handle().getLevel();
    }

    @Override
    public void setLevel(int level) {
        handle().setLevel(level);
    }

    @Override
    public float getExp() {
        return handle().getExp();
    }

    @Override
    public void setExp(float exp) {
        handle().setExp(exp);
    }

    @Override
    public void setFireTicks(int ticks) {
        handle().setFireTicks(ticks);
    }

    @Override
    public float getFallDistance() {
        return handle().getFallDistance();
    }

    @Override
    public void setFallDistance(float distance) {
        handle().setFallDistance(distance);
    }

    @Override
    public boolean isDead() {
        return handle().isDead();
    }

    @Override
    public boolean isSneaking() {
        return handle().isSneaking();
    }

    @Override
    public boolean isSprinting() {
        return handle().isSprinting();
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOnGround() {
        return handle().isOnGround();
    }

    @Override
    public int getHeldSlot() {
        return handle().getInventory().getHeldItemSlot();
    }

    @Override
    public String getScoreboardTeam() {
        return Compat.scoreboardTeam(handle());
    }

    @Override
    public boolean isFake() {
        Player p = handle();
        return p.hasMetadata("NPC") || Compat.isFake(p);
    }

    // ------------------------------------------------------------------ spectateur

    @Override
    public void setSpectatorTarget(PlatformPlayer target) {
        handle().setSpectatorTarget(target == null ? null : unwrap(target));
    }

    @Override
    public UUID getSpectatorTargetId() {
        Entity e = handle().getSpectatorTarget();
        return e == null ? null : e.getUniqueId();
    }

    @Override
    public void setCanPickupItems(boolean value) {
        handle().setCanPickupItems(value);
    }

    @Override
    public void setSleepingIgnored(boolean value) {
        handle().setSleepingIgnored(value);
    }

    @Override
    public void setCollidable(boolean value) {
        Compat.setCollidable(handle(), value);
    }

    @Override
    public void setAffectsSpawning(boolean value) {
        Compat.setAffectsSpawning(handle(), value);
    }

    @Override
    public void setInvulnerable(boolean value) {
        Compat.setInvulnerable(handle(), value);
    }

    @Override
    public void setHidden(PlatformPlayer target, boolean hidden) {
        if (hidden) Compat.hidePlayer(plugin, handle(), unwrap(target));
        else Compat.showPlayer(plugin, handle(), unwrap(target));
    }

    @Override
    public void setHealthDisplay(boolean enabled, String title) {
        platform.healthDisplay().set(handle(), enabled, title);
    }

    // ------------------------------------------------------------------ effets

    @Override
    public List<EffectInfo> getEffects() {
        List<EffectInfo> res = new ArrayList<>();
        for (PotionEffect e : handle().getActivePotionEffects()) {
            res.add(new EffectInfo(Compat.effectName(e.getType()), e.getAmplifier(), e.getDuration()));
        }
        return res;
    }

    @Override
    public void clearEffects() {
        Player p = handle();
        for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
    }

    @Override
    public void addEffect(String name, int duration, int amplifier, boolean ambient, boolean particles) {
        // NIGHT_VISION et INVISIBILITY portent le même nom sur toutes les versions
        PotionEffectType type = PotionEffectType.getByName(name);
        if (type != null) handle().addPotionEffect(new PotionEffect(type, duration, amplifier, ambient, particles));
    }

    // ------------------------------------------------------------------ inventaire

    @Override
    public void closeInventory() {
        handle().closeInventory();
    }

    @Override
    public void clearInventory() {
        Player p = handle();
        p.getInventory().clear();
        p.getInventory().setArmorContents(new ItemStack[4]);
    }

    @Override
    public void setItem(int slot, Icon icon) {
        handle().getInventory().setItem(slot, BukkitIcons.render(icon));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void updateInventory() {
        handle().updateInventory();
    }

    @Override
    public ItemRef[] getStorageContents() {
        return BukkitItem.of(Compat.storageContents(handle()));
    }

    @Override
    public ItemRef[] getArmorContents() {
        return BukkitItem.of(handle().getInventory().getArmorContents());
    }

    @Override
    public ItemRef getOffHand() {
        if (!Compat.hasOffHand()) return null;
        ItemStack off = Compat.offHand(handle());
        return off == null ? BukkitItem.of(new ItemStack(org.bukkit.Material.AIR)) : BukkitItem.of(off);
    }

    @Override
    public ItemRef getMainHand() {
        return BukkitItem.of(Compat.mainHand(handle()));
    }

    @Override
    public ItemRef[] getEnderChest() {
        return BukkitItem.of(handle().getEnderChest().getContents());
    }

    @Override
    public String getBiome() {
        return Compat.biomeName(handle().getLocation());
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
