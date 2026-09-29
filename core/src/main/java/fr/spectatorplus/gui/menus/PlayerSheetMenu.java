package fr.spectatorplus.gui.menus;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.core.platform.Click;
import fr.spectatorplus.core.platform.EffectInfo;
import fr.spectatorplus.core.platform.Icon;
import fr.spectatorplus.core.platform.ItemRef;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.Position;
import fr.spectatorplus.filter.Preferences;
import fr.spectatorplus.gui.Menu;
import fr.spectatorplus.spectator.SpectatorSession;
import fr.spectatorplus.util.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Fiche détaillée d'un joueur.
 */
public final class PlayerSheetMenu extends Menu {

    private final PlatformPlayer target;

    public PlayerSheetMenu(SpectatorCore plugin, PlatformPlayer viewer, PlatformPlayer target) {
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
        Position l = target.getLocation();
        String team = plugin.game().getTeam(target);

        set(4, Icon.skull(target).name("&e" + target.getName())
                .lore(lore("gui.sheet.head",
                        "team", team == null ? msg("placeholders.none") : team,
                        "gamemode", target.getGameMode().name(),
                        "alive", plugin.spectators().isSpectator(target) ? msg("gui.common.no") : msg("gui.common.yes")))
                .build());

        set(19, item("APPLE").name(msg("gui.sheet.health", "health", Text.hearts(target.getHealth()),
                "max_health", Text.hearts(target.getMaxHealth()))).lore(msg("gui.sheet.absorption",
                "absorption", Text.hearts(target.getAbsorption()))).build());
        set(20, item("COOKED_BEEF").name(msg("gui.sheet.food", "food", target.getFoodLevel(),
                "saturation", Text.oneDecimal(target.getSaturation()))).build());
        set(21, item("EXPERIENCE_BOTTLE|EXP_BOTTLE").name(msg("gui.sheet.level", "level", target.getLevel())).build());

        List<String> effects = new ArrayList<>();
        for (EffectInfo e : target.getEffects()) {
            effects.add(msg("gui.sheet.effect-line", "effect", Text.pretty(e.getName()),
                    "level", e.getAmplifier() + 1, "duration", Text.duration(e.getDuration() / 20)));
        }
        if (effects.isEmpty()) effects.add(msg("gui.sheet.no-effect"));
        set(22, item("POTION").name(msg("gui.sheet.effects")).lore(effects).build());

        set(23, item("COMPASS").name(msg("gui.sheet.location")).lore(lore("gui.sheet.location-lore",
                "world", target.getWorld().getName(), "x", l.getBlockX(), "y", l.getBlockY(), "z", l.getBlockZ(),
                "biome", Text.pretty(target.getBiome()))).build());
        set(24, item("IRON_SWORD").name(msg("gui.sheet.stats")).lore(lore("gui.sheet.stats-lore",
                "kills", plugin.stats().kills(target.getUniqueId()),
                "deaths", plugin.stats().deaths(target.getUniqueId()),
                "streak", plugin.stats().streak(target.getUniqueId()))).build());

        List<String> armor = new ArrayList<>();
        for (ItemRef it : target.getArmorContents()) {
            if (it != null && !it.isEmpty()) armor.add("&7- &f" + Text.pretty(it.getType()));
        }
        if (armor.isEmpty()) armor.add(msg("placeholders.none"));
        set(25, item("DIAMOND_CHESTPLATE").name(msg("gui.sheet.armor")).lore(armor).build());

        if (s != null) {
            set(29, item("ENDER_PEARL").name(msg("gui.sheet.teleport")).build(), new ClickHandler() {
                @Override
                public void click(Click type) {
                    viewer.closeInventory();
                    plugin.spectators().teleport(s, target);
                }
            });
            set(30, item("ENDER_EYE|EYE_OF_ENDER").name(msg("gui.sheet.follow")).build(), new ClickHandler() {
                @Override
                public void click(Click type) {
                    viewer.closeInventory();
                    plugin.spectators().follow(s, target);
                }
            });
            if (plugin.config().getBoolean("pov.enabled", true) && viewer.hasPermission("spectatorplus.pov")) {
                set(31, item("SPYGLASS|GLASS").name(msg("gui.sheet.pov")).build(), new ClickHandler() {
                    @Override
                    public void click(Click type) {
                        plugin.spectators().startPov(s, target);
                    }
                });
            }
            set(32, item("CHEST").name(msg("gui.sheet.inventory")).build(), new ClickHandler() {
                @Override
                public void click(Click type) {
                    s.openInventory(target, false);
                }
            });
            if (viewer.hasPermission("spectatorplus.inspect.enderchest")) {
                set(33, item("ENDER_CHEST").name(msg("gui.sheet.enderchest")).build(), new ClickHandler() {
                    @Override
                    public void click(Click type) {
                        s.openInventory(target, true);
                    }
                });
            }
        }
        final Preferences prefs = plugin.filters().get(viewer);
        final boolean fav = prefs.favorites.contains(target.getUniqueId());
        set(34, item(fav ? "NETHER_STAR" : "GOLD_NUGGET").name(msg(fav ? "gui.sheet.unfavorite" : "gui.sheet.favorite")).glow(fav).build(), new ClickHandler() {
            @Override
            public void click(Click type) {
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
