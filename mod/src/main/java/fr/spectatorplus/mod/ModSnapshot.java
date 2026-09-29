package fr.spectatorplus.mod;

import fr.spectatorplus.core.config.YamlConfig;
import fr.spectatorplus.core.platform.GameMode;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.PlayerSnapshot;
import fr.spectatorplus.core.platform.Position;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * État du joueur avant son passage en spectateur, restauré à la sortie.
 * Sauvegardé en YAML (objets au format SNBT de Minecraft) pour survivre à une déconnexion ou un redémarrage.
 */
public final class ModSnapshot implements PlayerSnapshot {

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

    GameMode gameMode = GameMode.SURVIVAL;
    ItemStack[] storage = new ItemStack[36];
    ItemStack[] armor = new ItemStack[4];
    ItemStack offHand = ItemStack.EMPTY;
    Position location;
    boolean allowFlight;
    boolean flying;
    float flySpeed = 0.05f;
    float walkSpeed = 0.1f;
    int food = 20;
    float saturation = 5f;
    int level;
    float exp;
    List<MobEffectInstance> effects = new ArrayList<>();

    static ModSnapshot capture(ModPlayer wrapper) {
        ServerPlayer p = wrapper.handle();
        ModSnapshot s = new ModSnapshot();
        s.gameMode = wrapper.getGameMode();
        Inventory inv = Mc.inventory(p);
        for (int i = 0; i < 36; i++) s.storage[i] = inv.getItem(i).copy();
        for (int i = 0; i < 4; i++) s.armor[i] = p.getItemBySlot(ARMOR[i]).copy();
        s.offHand = p.getItemBySlot(EquipmentSlot.OFFHAND).copy();
        s.location = wrapper.getLocation();
        Abilities a = Mc.abilities(p);
        s.allowFlight = a.mayfly;
        s.flying = a.flying;
        s.flySpeed = a.getFlyingSpeed();
        s.walkSpeed = a.getWalkingSpeed();
        s.food = p.getFoodData().getFoodLevel();
        s.saturation = p.getFoodData().getSaturationLevel();
        s.level = p.experienceLevel;
        s.exp = p.experienceProgress;
        for (MobEffectInstance e : p.getActiveEffects()) s.effects.add(new MobEffectInstance(e));
        return s;
    }

    @Override
    public void restore(PlatformPlayer player, boolean teleport, boolean restoreInventory) {
        ServerPlayer p = ModPlayer.unwrap(player);
        p.setGameMode(GameType.valueOf(gameMode.name()));
        if (restoreInventory) {
            Inventory inv = Mc.inventory(p);
            inv.clearContent();
            for (int i = 0; i < 36; i++) inv.setItem(i, storage[i] == null ? ItemStack.EMPTY : storage[i].copy());
            for (int i = 0; i < 4; i++) p.setItemSlot(ARMOR[i], armor[i] == null ? ItemStack.EMPTY : armor[i].copy());
            p.setItemSlot(EquipmentSlot.OFFHAND, offHand == null ? ItemStack.EMPTY : offHand.copy());
        }
        Abilities a = Mc.abilities(p);
        a.mayfly = allowFlight || gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR;
        a.flying = flying && a.mayfly;
        a.setFlyingSpeed(flySpeed);
        a.setWalkingSpeed(walkSpeed);
        p.onUpdateAbilities();
        p.getFoodData().setFoodLevel(food);
        p.getFoodData().setSaturation(saturation);
        p.experienceProgress = exp;
        p.setExperienceLevels(level);
        p.removeAllEffects();
        for (MobEffectInstance e : effects) p.addEffect(new MobEffectInstance(e));
        if (teleport && location != null) player.teleport(location);
        Mc.resyncInventory(p);
    }

    @Override
    public Position getLocation() {
        return location;
    }

    // ------------------------------------------------------------------ persistance

    void write(File file, String reason, MinecraftServer server) throws IOException {
        YamlConfig y = new YamlConfig();
        y.set("reason", reason);
        y.set("state.gamemode", gameMode.name());
        y.set("state.storage", items(server, storage));
        y.set("state.armor", items(server, armor));
        y.set("state.offhand", Mc.saveItem(server, offHand));
        if (location != null) {
            y.set("state.location.world", location.getWorld());
            y.set("state.location.x", location.getX());
            y.set("state.location.y", location.getY());
            y.set("state.location.z", location.getZ());
            y.set("state.location.yaw", (double) location.getYaw());
            y.set("state.location.pitch", (double) location.getPitch());
        }
        y.set("state.allow-flight", allowFlight);
        y.set("state.flying", flying);
        y.set("state.fly-speed", (double) flySpeed);
        y.set("state.walk-speed", (double) walkSpeed);
        y.set("state.food", food);
        y.set("state.saturation", (double) saturation);
        y.set("state.level", level);
        y.set("state.exp", (double) exp);
        List<String> eff = new ArrayList<>();
        for (MobEffectInstance e : effects) {
            eff.add(Keys.effectName(e) + ";" + e.getDuration() + ";" + e.getAmplifier() + ";" + e.isAmbient() + ";" + e.isVisible());
        }
        y.set("state.effects", eff);
        y.save(file);
    }

    static ModSnapshot read(File file, MinecraftServer server) {
        if (!file.exists()) return null;
        YamlConfig y = YamlConfig.read(file);
        ModSnapshot s = new ModSnapshot();
        try {
            s.gameMode = GameMode.valueOf(y.getString("state.gamemode", "SURVIVAL"));
        } catch (IllegalArgumentException ignored) {
        }
        readItems(server, y.getStringList("state.storage"), s.storage);
        readItems(server, y.getStringList("state.armor"), s.armor);
        s.offHand = Mc.loadItem(server, y.getString("state.offhand", ""));
        if (y.isString("state.location.world")) {
            s.location = new Position(y.getString("state.location.world"), y.getDouble("state.location.x"),
                    y.getDouble("state.location.y"), y.getDouble("state.location.z"),
                    (float) y.getDouble("state.location.yaw"), (float) y.getDouble("state.location.pitch"));
        }
        s.allowFlight = y.getBoolean("state.allow-flight");
        s.flying = y.getBoolean("state.flying");
        s.flySpeed = (float) y.getDouble("state.fly-speed", 0.05);
        s.walkSpeed = (float) y.getDouble("state.walk-speed", 0.1);
        s.food = y.getInt("state.food", 20);
        s.saturation = (float) y.getDouble("state.saturation", 5);
        s.level = y.getInt("state.level");
        s.exp = (float) y.getDouble("state.exp");
        for (String line : y.getStringList("state.effects")) {
            String[] p = line.split(";");
            if (p.length < 5) continue;
            try {
                MobEffectInstance e = Mc.effect(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]),
                        Boolean.parseBoolean(p[3]), Boolean.parseBoolean(p[4]));
                if (e != null) s.effects.add(e);
            } catch (NumberFormatException ignored) {
            }
        }
        return s;
    }

    private static List<String> items(MinecraftServer server, ItemStack[] stacks) {
        List<String> res = new ArrayList<>();
        for (ItemStack s : stacks) res.add(Mc.saveItem(server, s));
        return res;
    }

    private static void readItems(MinecraftServer server, List<String> list, ItemStack[] into) {
        for (int i = 0; i < into.length; i++) into[i] = i < list.size() ? Mc.loadItem(server, list.get(i)) : ItemStack.EMPTY;
    }
}
