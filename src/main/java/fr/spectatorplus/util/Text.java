package fr.spectatorplus.util;

import fr.spectatorplus.compat.Version;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Text {

    private Text() {
    }

    public static String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }

    public static List<String> color(List<String> lines) {
        List<String> res = new ArrayList<>(lines.size());
        for (String l : lines) res.add(color(l));
        return res;
    }

    public static String strip(String s) {
        return s == null ? "" : ChatColor.stripColor(s);
    }

    /** DIAMOND_ORE → Diamond Ore */
    public static String pretty(String constant) {
        if (constant == null) return "";
        String[] words = constant.toLowerCase(Locale.ROOT).replace(':', ' ').split("[_ ]");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    /** Titre d'inventaire : limité à 32 caractères avant la 1.9. */
    public static String title(String s) {
        String c = color(s);
        if (!Version.atLeast(1, 9) && c.length() > 32) c = c.substring(0, 32);
        return c;
    }

    public static String hearts(double health) {
        return String.format(Locale.ROOT, "%.1f", health / 2.0);
    }

    public static String oneDecimal(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    public static String duration(long seconds) {
        if (seconds < 0) return "∞";
        long h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60;
        if (h > 0) return String.format(Locale.ROOT, "%dh%02dm%02ds", h, m, s);
        return String.format(Locale.ROOT, "%02d:%02d", m, s);
    }

    public static String bool(boolean b) {
        return b ? "&aON" : "&cOFF";
    }
}
