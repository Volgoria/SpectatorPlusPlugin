package fr.spectatorplus.spectator;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.api.LeaveReason;
import fr.spectatorplus.api.SpectatorState;
import fr.spectatorplus.compat.Sounds;
import fr.spectatorplus.core.config.ConfigSection;
import fr.spectatorplus.core.platform.Icon;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.gui.menus.FiltersMenu;
import fr.spectatorplus.gui.menus.GameInfoMenu;
import fr.spectatorplus.gui.menus.HistoryMenu;
import fr.spectatorplus.gui.menus.MainMenu;
import fr.spectatorplus.gui.menus.PlayerListMenu;
import fr.spectatorplus.gui.menus.SettingsMenu;

import java.util.List;
import java.util.Locale;

/**
 * Barre d'inventaire du spectateur (config.yml → hotbar).
 */
public final class HotbarManager {

    private final SpectatorCore plugin;

    public HotbarManager(SpectatorCore plugin) {
        this.plugin = plugin;
    }

    public void give(PlatformPlayer p, SpectatorSession s) {
        p.clearInventory();
        s.hotbarActions.clear();
        ConfigSection items = plugin.config().getConfigurationSection("hotbar.items");
        if (!plugin.config().getBoolean("hotbar.enabled", true) || items == null) {
            p.updateInventory();
            return;
        }
        for (String action : items.getKeys(false)) {
            ConfigSection it = items.getConfigurationSection(action);
            if (it == null || !it.getBoolean("enabled", true)) continue;
            String perm = it.getString("permission", "");
            if (!perm.isEmpty() && !p.hasPermission(perm)) continue;
            int slot = it.getInt("slot", 1) - 1;
            if (slot < 0 || slot > 8) continue;
            // nom / description : config.yml s'ils y sont définis, sinon le fichier de langue du joueur
            String key = "hotbar." + action.toLowerCase(Locale.ROOT);
            String name = it.isString("name") ? it.getString("name") : plugin.messages().raw(p, key + ".name");
            List<String> lore = it.isList("lore") ? it.getStringList("lore") : plugin.messages().list(p, key + ".lore");
            p.setItem(slot, Icon.of(it.getString("material", "PAPER")).name(name).lore(lore));
            s.hotbarActions.put(slot, action.toLowerCase(Locale.ROOT));
        }
        p.updateInventory();
    }

    /** Exécute l'action associée à un objet de la barre. */
    public void use(PlatformPlayer p, SpectatorSession s, String action, boolean leftClick, boolean sneaking) {
        if (plugin.filters().get(p).sounds) p.playSound(Sounds.CLICK, 0.4f, 1.6f);
        switch (action) {
            case "teleport":
                new PlayerListMenu(plugin, p, PlayerListMenu.Mode.TELEPORT).open();
                break;
            case "players":
                new PlayerListMenu(plugin, p, PlayerListMenu.Mode.PLAYERS).open();
                break;
            case "follow":
                if (s.getMovementState() == SpectatorState.FOLLOWING || s.getMovementState() == SpectatorState.POV) {
                    if (sneaking) plugin.spectators().stopFollowing(s, true);
                    else plugin.spectators().cycleAndFollow(s, leftClick ? -1 : 1);
                } else {
                    new PlayerListMenu(plugin, p, PlayerListMenu.Mode.FOLLOW).open();
                }
                break;
            case "events":
                new HistoryMenu(plugin, p).open();
                break;
            case "menu":
                new MainMenu(plugin, p).open();
                break;
            case "inventories":
                new PlayerListMenu(plugin, p, PlayerListMenu.Mode.INVENTORY).open();
                break;
            case "filters":
                new FiltersMenu(plugin, p).open();
                break;
            case "info":
                new GameInfoMenu(plugin, p).open();
                break;
            case "settings":
                new SettingsMenu(plugin, p).open();
                break;
            case "pov":
                new PlayerListMenu(plugin, p, PlayerListMenu.Mode.POV).open();
                break;
            case "leave":
                if (p.hasPermission("spectatorplus.leave")) plugin.spectators().leave(p, LeaveReason.COMMAND, false);
                break;
            default:
                // action personnalisée : commande exécutée par le joueur
                String cmd = plugin.config().getString("hotbar.items." + action + ".command", "");
                if (!cmd.isEmpty()) p.performCommand(cmd.replace("{player}", p.getName()));
                break;
        }
    }
}
