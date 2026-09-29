package fr.spectatorplus.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utilitaires de texte communs à toutes les plateformes.
 * Les couleurs utilisent les codes « § » : compris par Bukkit comme par les composants texte vanilla.
 */
public final class Text {

    /** Caractère de formatage de Minecraft. */
    public static final char SECTION = '§';
    private static final String CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";
    private static final Pattern STRIP = Pattern.compile("(?i)" + SECTION + "[0-9A-FK-ORX]");

    private Text() {
    }

    /** « &c » → « §c » (même règle que ChatColor.translateAlternateColorCodes). */
    public static String color(String s) {
        if (s == null) return "";
        char[] b = s.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == '&' && CODES.indexOf(b[i + 1]) > -1) {
                b[i] = SECTION;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }
        return new String(b);
    }

    public static List<String> color(List<String> lines) {
        List<String> res = new ArrayList<>(lines.size());
        for (String l : lines) res.add(color(l));
        return res;
    }

    public static String strip(String s) {
        return s == null ? "" : STRIP.matcher(s).replaceAll("");
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
