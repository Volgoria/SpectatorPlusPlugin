package fr.spectatorplus.spectator;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.api.SpectatorState;
import fr.spectatorplus.compat.Compat;
import fr.spectatorplus.compat.Sounds;
import fr.spectatorplus.gui.menus.FiltersMenu;
import fr.spectatorplus.gui.menus.GameInfoMenu;
import fr.spectatorplus.gui.menus.HistoryMenu;
import fr.spectatorplus.gui.menus.MainMenu;
import fr.spectatorplus.gui.menus.PlayerListMenu;
import fr.spectatorplus.gui.menus.SettingsMenu;
import fr.spectatorplus.util.ItemBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Barre d'inventaire du spectateur (config.yml → hotbar).
 */
public final class HotbarManager {

    private final SpectatorPlus plugin;

    public HotbarManager(SpectatorPlus plugin) {
        this.plugin = plugin;
    }

    public void give(Player p, SpectatorSession s) {
        p.getInventory().clear();
        s.hotbarActions.clear();
        ConfigurationSection items = plugin.getConfig().getConfigurationSection("hotbar.items");
        if (!plugin.getConfig().getBoolean("hotbar.enabled", true) || items == null) {
            p.updateInventory();
            return;
        }
        for (String action : items.getKeys(false)) {
            ConfigurationSection it = items.getConfigurationSection(action);
            if (it == null || !it.getBoolean("enabled", true)) continue;
            String perm = it.getString("permission", "");
            if (!perm.isEmpty() && !p.hasPermission(perm)) continue;
            int slot = it.getInt("slot", 1) - 1;
            if (slot < 0 || slot > 8) continue;
            // nom / description : config.yml s'ils y sont définis, sinon le fichier de langue du joueur
            String key = "hotbar." + action.toLowerCase();
            String name = it.isString("name") ? it.getString("name") : plugin.messages().raw(p, key + ".name");
            java.util.List<String> lore = it.isList("lore") ? it.getStringList("lore") : plugin.messages().list(p, key + ".lore");
            p.getInventory().setItem(slot, ItemBuilder.of(it.getString("material", "PAPER"))
                    .name(name)
                    .lore(lore)
                    .build());
            s.hotbarActions.put(slot, action.toLowerCase());
        }
        p.updateInventory();
    }

    /** Exécute l'action associée à un objet de la barre. */
    public void use(Player p, SpectatorSession s, String action, boolean leftClick, boolean sneaking) {
        if (plugin.filters().get(p).sounds) Compat.playSound(p, Sounds.CLICK, 0.4f, 1.6f);
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
                if (p.hasPermission("spectatorplus.leave")) plugin.spectators().leave(p, fr.spectatorplus.api.LeaveReason.COMMAND, false);
                break;
            default:
                // action personnalisée : commande exécutée par le joueur
                String cmd = plugin.getConfig().getString("hotbar.items." + action + ".command", "");
                if (!cmd.isEmpty()) p.performCommand(cmd.replace("{player}", p.getName()));
                break;
        }
    }
}
