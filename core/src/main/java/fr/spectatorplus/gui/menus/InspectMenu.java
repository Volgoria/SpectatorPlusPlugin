package fr.spectatorplus.gui.menus;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.core.platform.Click;
import fr.spectatorplus.core.platform.EffectInfo;
import fr.spectatorplus.core.platform.Icon;
import fr.spectatorplus.core.platform.ItemRef;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.gui.Menu;
import fr.spectatorplus.spectator.SpectatorSession;
import fr.spectatorplus.util.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Inventaire (ou ender chest) d'un joueur, en lecture seule et mis à jour en direct.
 */
public final class InspectMenu extends Menu {

    private final PlatformPlayer target;
    private final boolean enderChest;

    public InspectMenu(SpectatorCore plugin, PlatformPlayer viewer, PlatformPlayer target, boolean enderChest) {
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

    @Override
    protected void render() {
        if (!target.isOnline()) {
            viewer.closeInventory();
            return;
        }
        final SpectatorSession s = plugin.spectators().getSpectator(viewer);
        if (enderChest) {
            ItemRef[] ec = target.getEnderChest();
            for (int i = 0; i < 27 && i < ec.length; i++) set(i, ec[i]);
            set(27, item("ARROW").name(msg("gui.common.back")).build(), new ClickHandler() {
                @Override
                public void click(Click type) {
                    if (s != null) s.openInventory(target, false);
                }
            });
            close(35);
            fill(27, 35);
            return;
        }

        ItemRef[] storage = target.getStorageContents();
        // lignes 1-3 : inventaire principal (emplacements 9 à 35)
        for (int i = 9; i < 36 && i < storage.length; i++) set(i - 9, storage[i]);
        // ligne 4 : barre d'action (0 à 8)
        for (int i = 0; i < 9 && i < storage.length; i++) set(27 + i, storage[i]);
        // ligne 5 : armure + seconde main
        ItemRef[] armor = target.getArmorContents();
        for (int i = 0; i < 4 && i < armor.length; i++) {
            ItemRef a = armor[3 - i]; // casque en premier
            if (a == null || a.isEmpty()) {
                set(36 + i, item("LIGHT_GRAY_STAINED_GLASS_PANE|STAINED_GLASS_PANE:8").name(msg("gui.inspect.armor-slot." + i)).build());
            } else {
                set(36 + i, a);
            }
        }
        ItemRef off = target.getOffHand();
        if (off != null) {
            if (off.isEmpty()) {
                set(40, item("LIGHT_GRAY_STAINED_GLASS_PANE|STAINED_GLASS_PANE:8").name(msg("gui.inspect.offhand")).build());
            } else {
                set(40, off);
            }
        }
        set(42, item("APPLE").name(msg("gui.inspect.status"))
                .lore(lore("gui.inspect.status-lore",
                        "health", Text.hearts(target.getHealth()),
                        "max_health", Text.hearts(target.getMaxHealth()),
                        "food", target.getFoodLevel(),
                        "level", target.getLevel(),
                        "slot", target.getHeldSlot() + 1))
                .build());
        List<String> effects = new ArrayList<>();
        for (EffectInfo e : target.getEffects()) {
            effects.add(msg("gui.sheet.effect-line", "effect", Text.pretty(e.getName()),
                    "level", e.getAmplifier() + 1, "duration", Text.duration(e.getDuration() / 20)));
        }
        if (effects.isEmpty()) effects.add(msg("gui.sheet.no-effect"));
        set(43, item("POTION").name(msg("gui.sheet.effects")).lore(effects).build());
        if (viewer.hasPermission("spectatorplus.inspect.enderchest") && s != null) {
            set(44, item("ENDER_CHEST").name(msg("gui.sheet.enderchest")).build(), new ClickHandler() {
                @Override
                public void click(Click type) {
                    s.openInventory(target, true);
                }
            });
        }
        set(45, item("ARROW").name(msg("gui.common.back")).build(), new ClickHandler() {
            @Override
            public void click(Click type) {
                new PlayerSheetMenu(plugin, viewer, target).open();
            }
        });
        set(49, Icon.skull(target).name("&e" + target.getName()).build());
        close(53);
        fill(36, 53);
    }
}
