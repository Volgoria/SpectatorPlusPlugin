package fr.spectatorplus.compat;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Gestion des matériaux compatibles 1.8 → 26.x.
 * <p>
 * Ne jamais référencer directement une constante {@link Material} qui a été renommée
 * en 1.13 : le plugin déclare {@code api-version: 1.13}, les anciens noms n'existent donc plus
 * à l'exécution sur les versions récentes.
 * <p>
 * Syntaxe utilisée dans la configuration : {@code MODERNE|LEGACY:data}
 * ex : {@code LIME_STAINED_GLASS_PANE|STAINED_GLASS_PANE:5}.
 */
public final class Mat {

    private static final Map<String, Integer> COLOR_DATA = new HashMap<>();

    static {
        String[] colors = {"WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY",
                "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"};
        for (int i = 0; i < colors.length; i++) COLOR_DATA.put(colors[i], i);
    }

    private Mat() {
    }

    /** Premier matériau existant parmi les noms donnés, ou null. */
    public static Material get(String... names) {
        for (String n : names) {
            if (n == null || n.isEmpty()) continue;
            Material m = Material.getMaterial(n.toUpperCase(Locale.ROOT));
            if (m != null) return m;
        }
        return null;
    }

    public static Material getOr(Material def, String... names) {
        Material m = get(names);
        return m == null ? def : m;
    }

    public static Material stone() {
        // STONE porte le même nom sur toutes les versions
        return Material.STONE;
    }

    /** Crée un ItemStack en choisissant le nom moderne ou legacy selon la version. */
    @SuppressWarnings("deprecation")
    public static ItemStack item(String modern, String legacy, int legacyData) {
        if (Version.isLegacy() && legacy != null) {
            Material m = get(legacy, modern);
            if (m != null) return new ItemStack(m, 1, (short) legacyData);
        }
        Material m = get(modern, legacy);
        if (m == null) m = stone();
        return new ItemStack(m, 1);
    }

    /**
     * Analyse une spécification de configuration « MODERNE|LEGACY:data ».
     * Une seule partie est aussi acceptée : « COMPASS » ou « SKULL_ITEM:3 ».
     */
    public static ItemStack parse(String spec) {
        if (spec == null || spec.trim().isEmpty()) return new ItemStack(stone());
        String[] alternatives = spec.trim().split("\\|");
        if (Version.isLegacy()) {
            for (int i = alternatives.length - 1; i >= 0; i--) {
                ItemStack it = parseSingle(alternatives[i]);
                if (it != null) return it;
            }
        } else {
            for (String alt : alternatives) {
                ItemStack it = parseSingle(alt);
                if (it != null) return it;
            }
        }
        return new ItemStack(stone());
    }

    @SuppressWarnings("deprecation")
    private static ItemStack parseSingle(String s) {
        String[] parts = s.trim().split(":");
        Material m = get(parts[0]);
        if (m == null) return null;
        short data = 0;
        if (parts.length > 1 && Version.isLegacy()) {
            try {
                data = Short.parseShort(parts[1]);
            } catch (NumberFormatException ignored) {
            }
        }
        return new ItemStack(m, 1, data);
    }

    /** Vitre teintée. Couleur : WHITE, LIME, RED, YELLOW, GRAY, LIGHT_GRAY, ... */
    public static ItemStack pane(String color) {
        String c = color.toUpperCase(Locale.ROOT);
        Integer data = COLOR_DATA.get(c);
        return item(c + "_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", data == null ? 0 : data);
    }

    /** Laine teintée. */
    public static ItemStack wool(String color) {
        String c = color.toUpperCase(Locale.ROOT);
        Integer data = COLOR_DATA.get(c);
        return item(c + "_WOOL", "WOOL", data == null ? 0 : data);
    }

    public static ItemStack playerHead() {
        return item("PLAYER_HEAD", "SKULL_ITEM", 3);
    }

    public static boolean is(Material m, String... names) {
        if (m == null) return false;
        String n = m.name();
        for (String s : names) if (n.equalsIgnoreCase(s)) return true;
        return false;
    }

    public static boolean isAir(ItemStack it) {
        return it == null || it.getType() == null || it.getType().name().endsWith("AIR");
    }

    /** Teste si un matériau appartient à une liste de noms de configuration (vide = tout). */
    public static boolean matches(Material m, Collection<String> names, boolean emptyMeansAll) {
        if (names == null || names.isEmpty()) return emptyMeansAll;
        if (m == null) return false;
        String n = m.name();
        for (String s : names) {
            if (s.equals("*") || s.equalsIgnoreCase(n)) return true;
            if (s.endsWith("*") && n.startsWith(s.substring(0, s.length() - 1).toUpperCase(Locale.ROOT))) return true;
        }
        return false;
    }
}
