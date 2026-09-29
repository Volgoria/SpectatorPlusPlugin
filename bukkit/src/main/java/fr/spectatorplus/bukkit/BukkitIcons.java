package fr.spectatorplus.bukkit;

import fr.spectatorplus.compat.Compat;
import fr.spectatorplus.compat.Mat;
import fr.spectatorplus.core.platform.Icon;
import fr.spectatorplus.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

/**
 * Transforme les {@link Icon} du code commun en ItemStack Bukkit.
 */
public final class BukkitIcons {

    private BukkitIcons() {
    }

    public static ItemStack render(Icon icon) {
        if (icon == null || icon.isEmpty()) return null;
        ItemStack base;
        switch (icon.getKind()) {
            case PANE:
                base = Mat.pane(icon.getSpec());
                break;
            case WOOL:
                base = Mat.wool(icon.getSpec());
                break;
            case HEAD:
                base = Mat.playerHead();
                break;
            case SKULL:
                base = Compat.skull(owner(icon));
                break;
            case ITEM:
                base = (ItemStack) icon.getItem().handle();
                break;
            default:
                base = Mat.parse(icon.getSpec());
                break;
        }
        // objet réel sans décoration : copie exacte (nom, lore, enchantements d'origine)
        if (icon.getKind() == Icon.Kind.ITEM && icon.getName() == null && icon.getLore().isEmpty() && !icon.isGlow()) {
            return base.clone();
        }
        ItemBuilder b = new ItemBuilder(base).lore(icon.getLore()).glow(icon.isGlow());
        if (icon.getName() != null) b.name(icon.getName());
        if (icon.getAmount() > 1) b.amount(icon.getAmount());
        return b.build();
    }

    @SuppressWarnings("deprecation")
    private static OfflinePlayer owner(Icon icon) {
        if (icon.getOwner() != null) {
            OfflinePlayer online = Bukkit.getPlayer(icon.getOwner());
            return online != null ? online : Bukkit.getOfflinePlayer(icon.getOwner());
        }
        return icon.getOwnerName() == null ? null : Bukkit.getOfflinePlayer(icon.getOwnerName());
    }
}
