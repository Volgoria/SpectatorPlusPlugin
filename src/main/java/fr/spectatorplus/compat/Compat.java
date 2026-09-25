package fr.spectatorplus.compat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Couche de compatibilité : toutes les API qui diffèrent entre 1.8 et 26.x passent par ici.
 */
public final class Compat {

    private static final Map<String, String> EFFECT_ALIASES = new HashMap<>();
    private static final Map<String, String> ENCHANT_ALIASES = new HashMap<>();

    static {
        String[][] effects = {
                {"INCREASE_DAMAGE", "STRENGTH"}, {"DAMAGE_RESISTANCE", "RESISTANCE"}, {"SLOW", "SLOWNESS"},
                {"FAST_DIGGING", "HASTE"}, {"SLOW_DIGGING", "MINING_FATIGUE"}, {"JUMP", "JUMP_BOOST"},
                {"HEAL", "INSTANT_HEALTH"}, {"HARM", "INSTANT_DAMAGE"}, {"CONFUSION", "NAUSEA"}};
        for (String[] e : effects) {
            EFFECT_ALIASES.put(e[0], e[1]);
            EFFECT_ALIASES.put(e[1], e[0]);
        }
        String[][] enchants = {
                {"DAMAGE_ALL", "SHARPNESS"}, {"DAMAGE_UNDEAD", "SMITE"}, {"DAMAGE_ARTHROPODS", "BANE_OF_ARTHROPODS"},
                {"ARROW_DAMAGE", "POWER"}, {"ARROW_FIRE", "FLAME"}, {"ARROW_INFINITE", "INFINITY"},
                {"ARROW_KNOCKBACK", "PUNCH"}, {"PROTECTION_ENVIRONMENTAL", "PROTECTION"},
                {"PROTECTION_FIRE", "FIRE_PROTECTION"}, {"PROTECTION_FALL", "FEATHER_FALLING"},
                {"PROTECTION_EXPLOSIONS", "BLAST_PROTECTION"}, {"PROTECTION_PROJECTILE", "PROJECTILE_PROTECTION"},
                {"DIG_SPEED", "EFFICIENCY"}, {"LOOT_BONUS_BLOCKS", "FORTUNE"}, {"LOOT_BONUS_MOBS", "LOOTING"},
                {"DURABILITY", "UNBREAKING"}, {"OXYGEN", "RESPIRATION"}, {"WATER_WORKER", "AQUA_AFFINITY"},
                {"LUCK", "LUCK_OF_THE_SEA"}, {"SWEEPING_EDGE", "SWEEPING"}};
        for (String[] e : enchants) {
            ENCHANT_ALIASES.put(e[0], e[1]);
            ENCHANT_ALIASES.put(e[1], e[0]);
        }
    }

    private Compat() {
    }

    // ------------------------------------------------------------------ visibilité

    public static void hidePlayer(Plugin plugin, Player viewer, Player target) {
        if (!Reflect.tryInvoke(viewer, "hidePlayer", plugin, target)) {
            Reflect.tryInvoke(viewer, "hidePlayer", target);
        }
    }

    public static void showPlayer(Plugin plugin, Player viewer, Player target) {
        if (!Reflect.tryInvoke(viewer, "showPlayer", plugin, target)) {
            Reflect.tryInvoke(viewer, "showPlayer", target);
        }
    }

    public static void setCollidable(Player p, boolean collidable) {
        if (!Reflect.tryInvoke(p, "setCollidable", collidable)) {
            Reflect.tryInvoke(Reflect.invoke(p, "spigot"), "setCollidesWithEntities", collidable);
        }
    }

    /** Paper : empêche le joueur de faire apparaître des mobs autour de lui. */
    public static void setAffectsSpawning(Player p, boolean value) {
        Reflect.tryInvoke(p, "setAffectsSpawning", value);
    }

    public static void setInvulnerable(Player p, boolean value) {
        Reflect.tryInvoke(p, "setInvulnerable", value);
    }

    // ------------------------------------------------------------------ messages

    public static void actionBar(Player p, String text) {
        if (Reflect.tryInvoke(p, "sendActionBar", text)) return;
        try {
            Class<?> typeClass = Reflect.getClass("net.md_5.bungee.api.ChatMessageType");
            Class<?> textClass = Reflect.getClass("net.md_5.bungee.api.chat.TextComponent");
            Object spigot = Reflect.invoke(p, "spigot");
            if (typeClass != null && textClass != null && spigot != null) {
                Object type = Reflect.getStaticField(typeClass, "ACTION_BAR");
                Object components = Reflect.invokeStatic(textClass, "fromLegacyText", text);
                if (type != null && components != null && Reflect.tryInvoke(spigot, "sendMessage", type, components)) {
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
        legacyActionBar(p, text);
    }

    private static Constructor<?> legacyChatPacket;
    private static Method legacySerializer;
    private static boolean legacyFailed;

    /** 1.8 : aucune API, on passe par un paquet NMS. */
    private static void legacyActionBar(Player p, String text) {
        if (legacyFailed) return;
        try {
            if (legacyChatPacket == null) {
                String pkg = Bukkit.getServer().getClass().getPackage().getName();
                String ver = pkg.substring(pkg.lastIndexOf('.') + 1);
                Class<?> component = Class.forName("net.minecraft.server." + ver + ".IChatBaseComponent");
                Class<?> serializer = Class.forName("net.minecraft.server." + ver + ".IChatBaseComponent$ChatSerializer");
                Class<?> packet = Class.forName("net.minecraft.server." + ver + ".PacketPlayOutChat");
                legacySerializer = serializer.getMethod("a", String.class);
                legacyChatPacket = packet.getConstructor(component, byte.class);
            }
            String json = "{\"text\":\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
            Object comp = legacySerializer.invoke(null, json);
            Object packet = legacyChatPacket.newInstance(comp, (byte) 2);
            Object handle = Reflect.invoke(p, "getHandle");
            Object connection = Reflect.getField(handle, "playerConnection");
            for (Method m : connection.getClass().getMethods()) {
                if (m.getName().equals("sendPacket") && m.getParameterTypes().length == 1) {
                    m.invoke(connection, packet);
                    return;
                }
            }
        } catch (Throwable t) {
            legacyFailed = true;
        }
    }

    public static void title(Player p, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (Reflect.tryInvoke(p, "sendTitle", title, subtitle, fadeIn, stay, fadeOut)) return;
        Reflect.tryInvoke(p, "sendTitle", title, subtitle);
    }

    /** Envoie un message cliquable (avec survol) si le serveur le permet, sinon un message simple. */
    public static void clickable(Player p, String text, String hover, String command) {
        try {
            ChatComponents.send(p, text, hover, command);
        } catch (Throwable t) {
            p.sendMessage(text);
        }
    }

    public static void playSound(Player p, Sounds sound, float volume, float pitch) {
        if (sound == null) return;
        try {
            p.playSound(p.getLocation(), sound.name(Version.atLeast(1, 9), Version.atLeast(1, 13)), volume, pitch);
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------------ inventaire

    @SuppressWarnings("deprecation")
    public static ItemStack mainHand(Player p) {
        Object it = Reflect.invoke(p.getInventory(), "getItemInMainHand");
        if (it instanceof ItemStack) return (ItemStack) it;
        Object legacy = Reflect.invoke(p, "getItemInHand");
        return legacy instanceof ItemStack ? (ItemStack) legacy : null;
    }

    public static ItemStack offHand(Player p) {
        Object it = Reflect.invoke(p.getInventory(), "getItemInOffHand");
        return it instanceof ItemStack ? (ItemStack) it : null;
    }

    public static boolean hasOffHand() {
        return Version.atLeast(1, 9);
    }

    public static void setOffHand(Player p, ItemStack item) {
        Reflect.tryInvoke(p.getInventory(), "setItemInOffHand", item);
    }

    /** Les 36 emplacements principaux (sans armure ni seconde main). */
    public static ItemStack[] storageContents(Player p) {
        Object o = Reflect.invoke(p.getInventory(), "getStorageContents");
        if (o instanceof ItemStack[]) return (ItemStack[]) o;
        ItemStack[] all = p.getInventory().getContents();
        ItemStack[] res = new ItemStack[36];
        System.arraycopy(all, 0, res, 0, Math.min(36, all.length));
        return res;
    }

    public static void setStorageContents(Player p, ItemStack[] items) {
        if (!Reflect.tryInvoke(p.getInventory(), "setStorageContents", (Object) items)) {
            for (int i = 0; i < 36 && i < items.length; i++) p.getInventory().setItem(i, items[i]);
        }
    }

    /** Tête de joueur avec le skin du joueur. */
    public static ItemStack skull(OfflinePlayer owner) {
        ItemStack head = Mat.playerHead();
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof SkullMeta && owner != null) {
            if (!Reflect.tryInvoke(meta, "setOwningPlayer", owner)) {
                Reflect.tryInvoke(meta, "setOwner", owner.getName());
            }
            head.setItemMeta(meta);
        }
        return head;
    }

    // ------------------------------------------------------------------ santé

    public static double maxHealth(Player p) {
        Object o = Reflect.invoke(p, "getMaxHealth");
        if (o instanceof Number) return ((Number) o).doubleValue();
        return 20.0;
    }

    public static double absorption(Player p) {
        Object o = Reflect.invoke(p, "getAbsorptionAmount");
        if (o instanceof Number) return ((Number) o).doubleValue();
        for (PotionEffect e : p.getActivePotionEffects()) {
            if (effectKeys(e.getType()).contains("ABSORPTION")) return (e.getAmplifier() + 1) * 4.0;
        }
        return 0;
    }

    public static void respawn(Player p) {
        Reflect.tryInvoke(Reflect.invoke(p, "spigot"), "respawn");
    }

    // ------------------------------------------------------------------ noms / registres

    /** Nom « constante » d'un enum ou d'un élément de registre (Biome, Sound... devenus interfaces en 1.21). */
    public static String enumName(Object o) {
        if (o == null) return "UNKNOWN";
        if (o instanceof Enum) return ((Enum<?>) o).name();
        Object key = Reflect.invoke(o, "getKey");
        if (key != null) {
            Object k = Reflect.invoke(key, "getKey");
            if (k != null) return k.toString().toUpperCase(Locale.ROOT);
        }
        Object name = Reflect.invoke(o, "name");
        if (name != null) return name.toString();
        return o.toString().toUpperCase(Locale.ROOT);
    }

    /** Tous les noms possibles d'un effet (legacy + moderne), en majuscules. */
    public static Set<String> effectKeys(PotionEffectType type) {
        Set<String> keys = new LinkedHashSet<>();
        if (type == null) return keys;
        Object key = Reflect.invoke(type, "getKey");
        if (key != null) {
            Object k = Reflect.invoke(key, "getKey");
            if (k != null) keys.add(k.toString().toUpperCase(Locale.ROOT));
        }
        Object name = Reflect.invoke(type, "getName");
        if (name != null) keys.add(name.toString().toUpperCase(Locale.ROOT));
        for (String k : new LinkedHashSet<>(keys)) {
            String alias = EFFECT_ALIASES.get(k);
            if (alias != null) keys.add(alias);
        }
        return keys;
    }

    /** Nom moderne d'un effet (STRENGTH plutôt que INCREASE_DAMAGE). */
    public static String effectName(PotionEffectType type) {
        Set<String> keys = effectKeys(type);
        for (String k : keys) {
            if (!isLegacyEffectName(k)) return k;
        }
        return keys.isEmpty() ? "UNKNOWN" : keys.iterator().next();
    }

    private static boolean isLegacyEffectName(String k) {
        return k.equals("INCREASE_DAMAGE") || k.equals("DAMAGE_RESISTANCE") || k.equals("SLOW")
                || k.equals("FAST_DIGGING") || k.equals("SLOW_DIGGING") || k.equals("JUMP")
                || k.equals("HEAL") || k.equals("HARM") || k.equals("CONFUSION");
    }

    public static Set<String> enchantKeys(Enchantment ench) {
        Set<String> keys = new LinkedHashSet<>();
        if (ench == null) return keys;
        Object key = Reflect.invoke(ench, "getKey");
        if (key != null) {
            Object k = Reflect.invoke(key, "getKey");
            if (k != null) keys.add(k.toString().toUpperCase(Locale.ROOT));
        }
        Object name = Reflect.invoke(ench, "getName");
        if (name != null) keys.add(name.toString().toUpperCase(Locale.ROOT));
        for (String k : new LinkedHashSet<>(keys)) {
            String alias = ENCHANT_ALIASES.get(k);
            if (alias != null) keys.add(alias);
        }
        return keys;
    }

    public static String enchantName(Enchantment ench) {
        Set<String> keys = enchantKeys(ench);
        if (keys.isEmpty()) return "UNKNOWN";
        // la clé moderne est ajoutée en premier si elle existe
        String first = keys.iterator().next();
        String alias = ENCHANT_ALIASES.get(first);
        if (alias != null && first.contains("_") && isLegacyEnchantName(first)) return alias;
        return first;
    }

    private static boolean isLegacyEnchantName(String k) {
        return k.startsWith("DAMAGE_") || k.startsWith("ARROW_") || k.startsWith("PROTECTION_")
                || k.startsWith("LOOT_") || k.equals("DIG_SPEED") || k.equals("DURABILITY")
                || k.equals("OXYGEN") || k.equals("WATER_WORKER") || k.equals("LUCK");
    }

    /** Type de base d'une potion (SWIFTNESS, STRENGTH, ...). */
    public static String potionName(ItemStack item) {
        if (item == null) return "UNKNOWN";
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof PotionMeta) {
            Object type = Reflect.invoke(meta, "getBasePotionType");
            if (type != null) return enumName(type);
            Object data = Reflect.invoke(meta, "getBasePotionData");
            if (data != null) {
                Object t = Reflect.invoke(data, "getType");
                if (t != null) return enumName(t);
            }
            PotionMeta pm = (PotionMeta) meta;
            if (pm.hasCustomEffects() && !pm.getCustomEffects().isEmpty()) {
                return effectName(pm.getCustomEffects().get(0).getType());
            }
        }
        // 1.8 : org.bukkit.potion.Potion.fromItemStack
        try {
            Class<?> potion = Reflect.getClass("org.bukkit.potion.Potion");
            Object pot = Reflect.invokeStatic(potion, "fromItemStack", item);
            Object type = Reflect.invoke(pot, "getType");
            if (type != null) return enumName(type);
        } catch (Throwable ignored) {
        }
        return "WATER";
    }

    public static String biomeName(Location loc) {
        try {
            return enumName(loc.getBlock().getBiome());
        } catch (Throwable t) {
            return "UNKNOWN";
        }
    }

    // ------------------------------------------------------------------ équipes

    public static String scoreboardTeam(Player p) {
        Object team = findTeam(p.getScoreboard(), p);
        if (team == null && Bukkit.getScoreboardManager() != null) {
            team = findTeam(Bukkit.getScoreboardManager().getMainScoreboard(), p);
        }
        Object name = Reflect.invoke(team, "getName");
        return name == null ? null : name.toString();
    }

    private static Object findTeam(Object scoreboard, Player p) {
        if (scoreboard == null) return null;
        Object team = Reflect.invoke(scoreboard, "getEntryTeam", p.getName());
        if (team == null) team = Reflect.invoke(scoreboard, "getPlayerTeam", (OfflinePlayer) p);
        return team;
    }

    // ------------------------------------------------------------------ monde

    public static boolean isOutsideBorder(Location loc) {
        try {
            World w = loc.getWorld();
            if (w == null) return false;
            WorldBorder wb = w.getWorldBorder();
            double half = wb.getSize() / 2.0;
            Location c = wb.getCenter();
            return Math.abs(loc.getX() - c.getX()) > half || Math.abs(loc.getZ() - c.getZ()) > half;
        } catch (Throwable t) {
            return false;
        }
    }

    public static int minHeight(World w) {
        Object o = Reflect.invoke(w, "getMinHeight");
        return o instanceof Number ? ((Number) o).intValue() : 0;
    }

    /** Paper : TPS du serveur, ou null si non disponible. */
    public static double[] serverTps() {
        Object o = Reflect.invoke(Bukkit.getServer(), "getTPS");
        return o instanceof double[] ? (double[]) o : null;
    }

    /** true si l'évènement (1.9+) concerne la main principale. */
    public static boolean isMainHand(Object event) {
        Object hand = Reflect.invoke(event, "getHand");
        return hand == null || "HAND".equals(hand.toString());
    }
}
