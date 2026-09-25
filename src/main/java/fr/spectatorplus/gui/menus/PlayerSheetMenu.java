package fr.spectatorplus.gui.menus;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.compat.Compat;
import fr.spectatorplus.compat.Mat;
import fr.spectatorplus.filter.Preferences;
import fr.spectatorplus.gui.Menu;
import fr.spectatorplus.spectator.SpectatorSession;
import fr.spectatorplus.util.ItemBuilder;
import fr.spectatorplus.util.Text;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * Fiche détaillée d'un joueur.
 */
public final class PlayerSheetMenu extends Menu {

    private final Player target;

    public PlayerSheetMenu(SpectatorPlus plugin, Player viewer, Player target) {
        super(plugin, viewer);
        this.target = target;
    }

    @Override
    public String id() {
        return "player";
    }

    @Override
    protected String title() {
        return msg("gui.sheet.title", "player", target.getName());
    }

    @Override
    protected int rows() {
        return 5;
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
        Location l = target.getLocation();
        String team = plugin.game().getTeam(target);

        set(4, new ItemBuilder(Compat.skull(target)).name("&e" + target.getName())
                .lore(lore("gui.sheet.head",
                        "team", team == null ? msg("placeholders.none") : team,
                        "gamemode", target.getGameMode().name(),
                        "alive", plugin.spectators().isSpectator(target) ? msg("gui.common.no") : msg("gui.common.yes")))
                .build());

        set(19, item("APPLE").name(msg("gui.sheet.health", "health", Text.hearts(target.getHealth()),
                "max_health", Text.hearts(Compat.maxHealth(target)))).lore(msg("gui.sheet.absorption",
                "absorption", Text.hearts(Compat.absorption(target)))).build());
        set(20, item("COOKED_BEEF").name(msg("gui.sheet.food", "food", target.getFoodLevel(),
                "saturation", Text.oneDecimal(target.getSaturation()))).build());
        set(21, item("EXPERIENCE_BOTTLE|EXP_BOTTLE").name(msg("gui.sheet.level", "level", target.getLevel())).build());

        List<String> effects = new ArrayList<>();
        for (PotionEffect e : target.getActivePotionEffects()) {
            effects.add(msg("gui.sheet.effect-line", "effect", Text.pretty(Compat.effectName(e.getType())),
                    "level", e.getAmplifier() + 1, "duration", Text.duration(e.getDuration() / 20)));
        }
        if (effects.isEmpty()) effects.add(msg("gui.sheet.no-effect"));
        set(22, item("POTION").name(msg("gui.sheet.effects")).lore(effects).build());

        set(23, item("COMPASS").name(msg("gui.sheet.location")).lore(lore("gui.sheet.location-lore",
                "world", l.getWorld().getName(), "x", l.getBlockX(), "y", l.getBlockY(), "z", l.getBlockZ(),
                "biome", Text.pretty(Compat.biomeName(l)))).build());
        set(24, item("IRON_SWORD").name(msg("gui.sheet.stats")).lore(lore("gui.sheet.stats-lore",
                "kills", plugin.stats().kills(target.getUniqueId()),
                "deaths", plugin.stats().deaths(target.getUniqueId()),
                "streak", plugin.stats().streak(target.getUniqueId()))).build());

        List<String> armor = new ArrayList<>();
        for (ItemStack it : target.getInventory().getArmorContents()) {
            if (!Mat.isAir(it)) armor.add("&7- &f" + Text.pretty(it.getType().name()));
        }
        if (armor.isEmpty()) armor.add(msg("placeholders.none"));
        set(25, item("DIAMOND_CHESTPLATE").name(msg("gui.sheet.armor")).lore(armor).build());

        if (s != null) {
            set(29, item("ENDER_PEARL").name(msg("gui.sheet.teleport")).build(), new ClickHandler() {
                @Override
                public void click(ClickType type) {
                    viewer.closeInventory();
                    plugin.spectators().teleport(s, target);
                }
            });
            set(30, item("ENDER_EYE|EYE_OF_ENDER").name(msg("gui.sheet.follow")).build(), new ClickHandler() {
                @Override
                public void click(ClickType type) {
                    viewer.closeInventory();
                    plugin.spectators().follow(s, target);
                }
            });
            if (plugin.getConfig().getBoolean("pov.enabled", true) && viewer.hasPermission("spectatorplus.pov")) {
                set(31, item("SPYGLASS|GLASS").name(msg("gui.sheet.pov")).build(), new ClickHandler() {
                    @Override
                    public void click(ClickType type) {
                        plugin.spectators().startPov(s, target);
                    }
                });
            }
            set(32, item("CHEST").name(msg("gui.sheet.inventory")).build(), new ClickHandler() {
                @Override
                public void click(ClickType type) {
                    s.openInventory(target, false);
                }
            });
            if (viewer.hasPermission("spectatorplus.inspect.enderchest")) {
                set(33, item("ENDER_CHEST").name(msg("gui.sheet.enderchest")).build(), new ClickHandler() {
                    @Override
                    public void click(ClickType type) {
                        s.openInventory(target, true);
                    }
                });
            }
        }
        final Preferences prefs = plugin.filters().get(viewer);
        final boolean fav = prefs.favorites.contains(target.getUniqueId());
        set(34, item(fav ? "NETHER_STAR" : "GOLD_NUGGET").name(msg(fav ? "gui.sheet.unfavorite" : "gui.sheet.favorite")).glow(fav).build(), new ClickHandler() {
            @Override
            public void click(ClickType type) {
                if (fav) prefs.favorites.remove(target.getUniqueId());
                else prefs.favorites.add(target.getUniqueId());
                prefs.knownNames.put(target.getUniqueId(), target.getName());
                plugin.filters().changed(viewer, "player.favorites", fav, !fav);
                refresh();
            }
        });
        back(36, new PlayerListMenu(plugin, viewer, PlayerListMenu.Mode.PLAYERS));
        close(44);
        fill();
    }
}
