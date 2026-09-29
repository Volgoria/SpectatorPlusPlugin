package fr.spectatorplus.mod;

import fr.spectatorplus.core.platform.Click;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.gui.Menu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Affichage des menus du code commun dans des coffres côté serveur (clients vanilla) :
 * tous les clics sont annulés et transmis au code commun.
 */
public final class ModMenus {

    private final ModPlatform platform;
    private final Map<UUID, SpMenu> open = new HashMap<>();

    ModMenus(ModPlatform platform) {
        this.platform = platform;
    }

    void open(PlatformPlayer viewer, final Menu menu) {
        ServerPlayer p = ModPlayer.unwrap(viewer);
        final int rows = Math.max(1, Math.min(6, menu.getSize() / 9));
        final SimpleContainer container = new SimpleContainer(rows * 9);
        fill(container, menu);
        p.openMenu(new SimpleMenuProvider(new MenuConstructor() {
            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                SpMenu m = new SpMenu(type(rows), id, inventory, container, rows, menu);
                open.put(player.getUUID(), m);
                return m;
            }
        }, Texts.of(menu.getTitle())));
    }

    void refresh(PlatformPlayer viewer, Menu menu) {
        SpMenu m = open.get(viewer.getUniqueId());
        if (m == null || m.menu != menu) return;
        fill(m.container, menu);
        m.broadcastChanges();
    }

    void forget(UUID id) {
        open.remove(id);
    }

    private void fill(SimpleContainer container, Menu menu) {
        for (int i = 0; i < container.getContainerSize(); i++) container.setItem(i, ModItems.render(menu.getItem(i), platform));
    }

    private static MenuType<?> type(int rows) {
        switch (rows) {
            case 1:
                return MenuType.GENERIC_9x1;
            case 2:
                return MenuType.GENERIC_9x2;
            case 3:
                return MenuType.GENERIC_9x3;
            case 4:
                return MenuType.GENERIC_9x4;
            case 5:
                return MenuType.GENERIC_9x5;
            default:
                return MenuType.GENERIC_9x6;
        }
    }

    private static Click click(int button, ContainerInput type) {
        switch (type) {
            case PICKUP:
                return button == 1 ? Click.RIGHT : Click.LEFT;
            case QUICK_MOVE:
                return button == 1 ? Click.SHIFT_RIGHT : Click.SHIFT_LEFT;
            case CLONE:
                return Click.MIDDLE;
            case THROW:
                return Click.DROP;
            default:
                return Click.OTHER;
        }
    }

    /** Coffre d'un menu : aucun objet ne peut être pris ni déposé. */
    final class SpMenu extends ChestMenu {
        final Menu menu;
        final SimpleContainer container;

        SpMenu(MenuType<?> type, int id, Inventory inventory, SimpleContainer container, int rows, Menu menu) {
            super(type, id, inventory, container, rows);
            this.menu = menu;
            this.container = container;
        }

        //? if >=1.17 {
        @Override
        public void clicked(int slot, int button, ContainerInput type, Player player) {
            handle(slot, button, type);
        }
        //?} else {
        /*@Override
        public ItemStack clicked(int slot, int button, ContainerInput type, Player player) {
            handle(slot, button, type);
            return ItemStack.EMPTY;
        }
        *///?}

        private void handle(int slot, int button, ContainerInput type) {
            if (slot >= 0 && slot < container.getContainerSize()) platform.core().menus().click(menu, slot, click(button, type));
            // le client a déplacé l'objet de son côté : on lui renvoie l'état réel
            //? if >=1.17 {
            setCarried(ItemStack.EMPTY);
            sendAllDataToRemote();
            //?}
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void removed(Player player) {
            super.removed(player);
            if (open.get(player.getUUID()) == this) open.remove(player.getUUID());
            if (player instanceof ServerPlayer) platform.core().menus().closed(platform.wrap((ServerPlayer) player), menu);
        }
    }
}
