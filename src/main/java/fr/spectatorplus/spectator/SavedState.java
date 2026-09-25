package fr.spectatorplus.spectator;

import fr.spectatorplus.compat.Compat;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * État du joueur avant son passage en spectateur, restauré à la sortie.
 * Sauvegardé sur disque pour survivre à une déconnexion ou un redémarrage.
 */
public final class SavedState {

    GameMode gameMode = GameMode.SURVIVAL;
    ItemStack[] storage = new ItemStack[36];
    ItemStack[] armor = new ItemStack[4];
    ItemStack offHand;
    Location location;
    boolean allowFlight;
    boolean flying;
    float flySpeed = 0.1f;
    float walkSpeed = 0.2f;
    int food = 20;
    float saturation = 5f;
    int level;
    float exp;
    List<PotionEffect> effects = new ArrayList<>();

    public static SavedState capture(Player p) {
        SavedState s = new SavedState();
        s.gameMode = p.getGameMode();
        s.storage = clone(Compat.storageContents(p));
        s.armor = clone(p.getInventory().getArmorContents());
        ItemStack off = Compat.offHand(p);
        s.offHand = off == null ? null : off.clone();
        s.location = p.getLocation().clone();
        s.allowFlight = p.getAllowFlight();
        s.flying = p.isFlying();
        s.flySpeed = p.getFlySpeed();
        s.walkSpeed = p.getWalkSpeed();
        s.food = p.getFoodLevel();
        s.saturation = p.getSaturation();
        s.level = p.getLevel();
        s.exp = p.getExp();
        s.effects = new ArrayList<>(p.getActivePotionEffects());
        return s;
    }

    private static ItemStack[] clone(ItemStack[] in) {
        ItemStack[] res = new ItemStack[in.length];
        for (int i = 0; i < in.length; i++) res[i] = in[i] == null ? null : in[i].clone();
        return res;
    }

    public void restore(Player p, boolean teleport, boolean restoreInventory) {
        p.setGameMode(gameMode);
        if (restoreInventory) {
            p.getInventory().clear();
            Compat.setStorageContents(p, storage);
            p.getInventory().setArmorContents(armor);
            if (offHand != null) Compat.setOffHand(p, offHand);
        }
        p.setAllowFlight(allowFlight || gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR);
        p.setFlying(flying && p.getAllowFlight());
        p.setFlySpeed(clamp(flySpeed));
        p.setWalkSpeed(clamp(walkSpeed));
        p.setFoodLevel(food);
        p.setSaturation(saturation);
        p.setLevel(level);
        p.setExp(exp);
        Collection<PotionEffect> current = p.getActivePotionEffects();
        for (PotionEffect e : current) p.removePotionEffect(e.getType());
        for (PotionEffect e : effects) p.addPotionEffect(e);
        if (teleport && location != null && location.getWorld() != null) p.teleport(location);
        p.updateInventory();
    }

    private static float clamp(float f) {
        return Math.max(-1f, Math.min(1f, f));
    }

    public Location getLocation() {
        return location;
    }

    // ------------------------------------------------------------------ persistance

    public void save(ConfigurationSection y) {
        y.set("gamemode", gameMode.name());
        y.set("storage", toList(storage));
        y.set("armor", toList(armor));
        y.set("offhand", offHand);
        if (location != null && location.getWorld() != null) {
            y.set("location.world", location.getWorld().getName());
            y.set("location.x", location.getX());
            y.set("location.y", location.getY());
            y.set("location.z", location.getZ());
            y.set("location.yaw", (double) location.getYaw());
            y.set("location.pitch", (double) location.getPitch());
        }
        y.set("allow-flight", allowFlight);
        y.set("flying", flying);
        y.set("fly-speed", (double) flySpeed);
        y.set("walk-speed", (double) walkSpeed);
        y.set("food", food);
        y.set("saturation", (double) saturation);
        y.set("level", level);
        y.set("exp", (double) exp);
        y.set("effects", new ArrayList<>(effects));
    }

    public static SavedState load(YamlConfiguration y) {
        SavedState s = new SavedState();
        try {
            s.gameMode = GameMode.valueOf(y.getString("gamemode", "SURVIVAL"));
        } catch (IllegalArgumentException ignored) {
        }
        s.storage = fromList(y.getList("storage"), 36);
        s.armor = fromList(y.getList("armor"), 4);
        s.offHand = y.getItemStack("offhand");
        World w = y.isString("location.world") ? Bukkit.getWorld(y.getString("location.world")) : null;
        if (w != null) {
            s.location = new Location(w, y.getDouble("location.x"), y.getDouble("location.y"), y.getDouble("location.z"),
                    (float) y.getDouble("location.yaw"), (float) y.getDouble("location.pitch"));
        }
        s.allowFlight = y.getBoolean("allow-flight");
        s.flying = y.getBoolean("flying");
        s.flySpeed = (float) y.getDouble("fly-speed", 0.1);
        s.walkSpeed = (float) y.getDouble("walk-speed", 0.2);
        s.food = y.getInt("food", 20);
        s.saturation = (float) y.getDouble("saturation", 5);
        s.level = y.getInt("level");
        s.exp = (float) y.getDouble("exp");
        List<?> eff = y.getList("effects");
        if (eff != null) {
            for (Object o : eff) if (o instanceof PotionEffect) s.effects.add((PotionEffect) o);
        }
        return s;
    }

    private static List<ItemStack> toList(ItemStack[] items) {
        List<ItemStack> res = new ArrayList<>();
        for (ItemStack i : items) res.add(i);
        return res;
    }

    private static ItemStack[] fromList(List<?> list, int size) {
        ItemStack[] res = new ItemStack[size];
        if (list == null) return res;
        for (int i = 0; i < list.size() && i < size; i++) {
            Object o = list.get(i);
            if (o instanceof ItemStack) res[i] = (ItemStack) o;
        }
        return res;
    }
}
