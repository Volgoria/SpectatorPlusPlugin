package fr.spectatorplus.mod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Conversion des textes du code commun (codes couleur « § ») en composants Minecraft.
 */
public final class Texts {

    private static final char SECTION = '§';

    private Texts() {
    }

    /** Texte coloré pour le chat. */
    public static Component of(String legacy) {
        return parse(legacy, Style.EMPTY);
    }

    /** Nom ou ligne de description d'objet : pas d'italique par défaut (comme sur Bukkit). */
    public static Component item(String legacy) {
        return parse(legacy, Style.EMPTY.withItalic(false));
    }

    private static Component parse(String legacy, Style base) {
        MutableComponent root = Mc.literal("");
        if (legacy == null || legacy.isEmpty()) return root;
        Style style = base;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < legacy.length(); i++) {
            char c = legacy.charAt(i);
            if (c == SECTION && i + 1 < legacy.length()) {
                ChatFormatting f = ChatFormatting.getByCode(Character.toLowerCase(legacy.charAt(i + 1)));
                if (f != null || legacy.charAt(i + 1) == 'x' || legacy.charAt(i + 1) == 'X') {
                    if (buf.length() > 0) {
                        root.append(Mc.literal(buf.toString()).withStyle(style));
                        buf.setLength(0);
                    }
                    if (f == ChatFormatting.RESET) style = base;
                    else if (f != null && f.ordinal() <= ChatFormatting.WHITE.ordinal()) style = base.applyFormat(f); // couleur : remet le style à zéro
                    else if (f != null) style = style.applyFormat(f);
                    i++;
                    continue;
                }
            }
            buf.append(c);
        }
        if (buf.length() > 0) root.append(Mc.literal(buf.toString()).withStyle(style));
        return root;
    }
}
