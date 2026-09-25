package fr.spectatorplus.gui.menus;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.compat.Compat;
import fr.spectatorplus.gui.Menu;
import fr.spectatorplus.spectator.SpectatorSession;
import fr.spectatorplus.util.ItemBuilder;
import fr.spectatorplus.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * Inventaire (ou ender chest) d'un joueur, en lecture seule et mis à jour en direct.
 */
public final class InspectMenu extends Menu {

    private final Player target;
    private final boolean enderChest;

    public InspectMenu(SpectatorPlus plugin, Player viewer, Player target, boolean enderChest) {
        super(plugin, viewer);
        this.target = target;
        this.enderChest = enderChest;
    }

    @Override
    public String id() {
        return enderChest ? "enderchest" : "inspect";
    }

    @Override
    protected String title() {
        return msg(enderChest ? "gui.inspect.title-enderchest" : "gui.inspect.title", "player", target.getName());
    }

    @Override
    protected int rows() {
        return enderChest ? 4 : 6;
    }

    @Override
    public boolean autoRefresh() {
        return true;
    }

    private static ItemStack copy(ItemStack it) {
        return it == null ? null : it.clone();
    }

    @Override
    protected void render() {
        if (!target.isOnline()) {
            viewer.closeInventory();
            return;
        }
        final SpectatorSession s = plugin.spectators().getSpectator(viewer);
        if (enderChest) {
            ItemStack[] ec = target.getEnderChest().getContents();
            for (int i = 0; i < 27 && i < ec.length; i++) set(i, copy(ec[i]));
            set(27, item("ARROW").name(msg("gui.common.back")).build(), new ClickHandler() {
                @Override
                public void click(ClickType type) {
                    if (s != null) s.openInventory(target, false);
                }
            });
            close(35);
            fill(27, 35);
            return;
        }

        ItemStack[] storage = Compat.storageContents(target);
        // lignes 1-3 : inventaire principal (emplacements 9 à 35)
        for (int i = 9; i < 36 && i < storage.length; i++) set(i - 9, copy(storage[i]));
        // ligne 4 : barre d'action (0 à 8)
        for (int i = 0; i < 9 && i < storage.length; i++) set(27 + i, copy(storage[i]));
        // ligne 5 : armure + seconde main
        ItemStack[] armor = target.getInventory().getArmorContents();
        for (int i = 0; i < 4 && i < armor.length; i++) {
            ItemStack a = armor[3 - i]; // casque en premier
            set(36 + i, a == null || a.getType().name().endsWith("AIR")
                    ? item("LIGHT_GRAY_STAINED_GLASS_PANE|STAINED_GLASS_PANE:8").name(msg("gui.inspect.armor-slot." + i)).build()
                    : copy(a));
        }
        if (Compat.hasOffHand()) {
            ItemStack off = Compat.offHand(target);
            set(40, off == null || off.getType().name().endsWith("AIR")
                    ? item("LIGHT_GRAY_STAINED_GLASS_PANE|STAINED_GLASS_PANE:8").name(msg("gui.inspect.offhand")).build()
                    : copy(off));
        }
        set(42, item("APPLE").name(msg("gui.inspect.status"))
                .lore(lore("gui.inspect.status-lore",
                        "health", Text.hearts(target.getHealth()),
                        "max_health", Text.hearts(Compat.maxHealth(target)),
                        "food", target.getFoodLevel(),
                        "level", target.getLevel(),
                        "slot", target.getInventory().getHeldItemSlot() + 1))
                .build());
        List<String> effects = new ArrayList<>();
        for (PotionEffect e : target.getActivePotionEffects()) {
            effects.add(msg("gui.sheet.effect-line", "effect", Text.pretty(Compat.effectName(e.getType())),
                    "level", e.getAmplifier() + 1, "duration", Text.duration(e.getDuration() / 20)));
        }
        if (effects.isEmpty()) effects.add(msg("gui.sheet.no-effect"));
        set(43, item("POTION").name(msg("gui.sheet.effects")).lore(effects).build());
        if (viewer.hasPermission("spectatorplus.inspect.enderchest") && s != null) {
            set(44, item("ENDER_CHEST").name(msg("gui.sheet.enderchest")).build(), new ClickHandler() {
                @Override
                public void click(ClickType type) {
                    s.openInventory(target, true);
                }
            });
        }
        set(45, item("ARROW").name(msg("gui.common.back")).build(), new ClickHandler() {
            @Override
            public void click(ClickType type) {
                new PlayerSheetMenu(plugin, viewer, target).open();
            }
        });
        set(49, new ItemBuilder(Compat.skull(target)).name("&e" + target.getName()).build());
        close(53);
        fill(36, 53);
    }
}
