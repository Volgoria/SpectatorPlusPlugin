package fr.spectatorplus.gui.menus;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.filter.Preferences;
import fr.spectatorplus.gui.Menu;
import fr.spectatorplus.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

/**
 * Paramètres personnels du spectateur (comparateur).
 */
public final class SettingsMenu extends Menu {

    private static final int[] RATE_LIMITS = {0, 2, 4, 6, 10, 20};

    public SettingsMenu(SpectatorPlus plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    public String id() {
        return "settings";
    }

    @Override
    protected String title() {
        return msg("gui.settings.title");
    }

    @Override
    protected int rows() {
        return 4;
    }

    private interface Toggle {
        boolean flip(Preferences p);
    }

    private void toggleOption(int slot, String icon, String key, boolean value, final Toggle t) {
        set(slot, item(icon).name(msg("gui.settings." + key + ".name"))
                .lore(lore("gui.settings." + key + ".lore"))
                .lore(msg("gui.settings.value", "value", Text.color(Text.bool(value))))
                .lore(msg("gui.common.click-toggle")).glow(value).build(), new ClickHandler() {
            @Override
            public void click(ClickType type) {
                Preferences p = plugin.filters().get(viewer);
                boolean now = t.flip(p);
                plugin.filters().changed(viewer, "settings", !now, now);
                refresh();
            }
        });
    }

    @Override
    protected void render() {
        final Preferences p = plugin.filters().get(viewer);
        set(10, item("FEATHER").name(msg("gui.settings.fly-speed.name"))
                .lore(lore("gui.settings.fly-speed.lore"))
                .lore(msg("gui.settings.value", "value", p.flySpeed + "/5"))
                .lore(msg("gui.settings.left-right")).build(), new ClickHandler() {
            @Override
            public void click(ClickType type) {
                int old = p.flySpeed;
                p.flySpeed = Math.max(1, Math.min(5, old + (type.isRightClick() ? -1 : 1)));
                plugin.spectators().applyFlySpeed(viewer);
                plugin.filters().changed(viewer, "settings.fly-speed", old, p.flySpeed);
                refresh();
            }
        });
        toggleOption(11, "ENDER_EYE|EYE_OF_ENDER", "see-spectators", p.seeSpectators, new Toggle() {
            @Override
            public boolean flip(Preferences pr) {
                pr.seeSpectators = !pr.seeSpectators;
                plugin.spectators().refreshVisibility(viewer);
                return pr.seeSpectators;
            }
        });
        toggleOption(12, "NOTE_BLOCK", "sounds", p.sounds, new Toggle() {
            @Override
            public boolean flip(Preferences pr) {
                pr.sounds = !pr.sounds;
                return pr.sounds;
            }
        });
        toggleOption(13, "NAME_TAG", "action-bar", p.actionBar, new Toggle() {
            @Override
            public boolean flip(Preferences pr) {
                pr.actionBar = !pr.actionBar;
                return pr.actionBar;
            }
        });
        toggleOption(14, "PAINTING", "titles", p.titles, new Toggle() {
            @Override
            public boolean flip(Preferences pr) {
                pr.titles = !pr.titles;
                return pr.titles;
            }
        });
        toggleOption(15, "PAPER", "chat-events", p.chatEvents, new Toggle() {
            @Override
            public boolean flip(Preferences pr) {
                pr.chatEvents = !pr.chatEvents;
                return pr.chatEvents;
            }
        });
        toggleOption(16, "CLOCK|WATCH", "show-time", p.showTime, new Toggle() {
            @Override
            public boolean flip(Preferences pr) {
                pr.showTime = !pr.showTime;
                return pr.showTime;
            }
        });
        set(20, item("HOPPER").name(msg("gui.settings.rate-limit.name"))
                .lore(lore("gui.settings.rate-limit.lore"))
                .lore(msg("gui.settings.value", "value", p.rateLimit == 0 ? msg("gui.common.unlimited") : p.rateLimit + "/s"))
                .lore(msg("gui.settings.left-right")).build(), new ClickHandler() {
            @Override
            public void click(ClickType type) {
                int idx = 0;
                for (int i = 0; i < RATE_LIMITS.length; i++) if (RATE_LIMITS[i] == p.rateLimit) idx = i;
                idx = (idx + (type.isRightClick() ? RATE_LIMITS.length - 1 : 1)) % RATE_LIMITS.length;
                int old = p.rateLimit;
                p.rateLimit = RATE_LIMITS[idx];
                plugin.filters().changed(viewer, "time.rate-limit", old, p.rateLimit);
                refresh();
            }
        });
        set(22, item("LEAD|LEASH").name(msg("gui.settings.follow-distance.name"))
                .lore(lore("gui.settings.follow-distance.lore"))
                .lore(msg("gui.settings.value", "value", p.followDistance + " " + msg("gui.common.blocks")))
                .lore(msg("gui.settings.left-right")).build(), new ClickHandler() {
            @Override
            public void click(ClickType type) {
                int old = p.followDistance;
                p.followDistance = Math.max(2, Math.min(12, old + (type.isRightClick() ? -1 : 1)));
                plugin.filters().changed(viewer, "settings.follow-distance", old, p.followDistance);
                refresh();
            }
        });
        final java.util.List<String> langs = new java.util.ArrayList<>();
        langs.add("auto");
        langs.addAll(new java.util.TreeSet<>(plugin.messages().available()));
        String current = p.language == null ? "auto" : p.language.toLowerCase(java.util.Locale.ROOT);
        String currentName = current.equals("auto")
                ? msg("gui.settings.language.auto", "language", plugin.messages().displayName(plugin.messages().lang(viewer)))
                : plugin.messages().displayName(current);
        set(23, item("WRITABLE_BOOK|BOOK_AND_QUILL").name(msg("gui.settings.language.name"))
                .lore(lore("gui.settings.language.lore"))
                .lore(msg("gui.settings.value", "value", currentName))
                .lore(msg("gui.settings.left-right")).build(), new ClickHandler() {
            @Override
            public void click(ClickType type) {
                String cur = p.language == null ? "auto" : p.language.toLowerCase(java.util.Locale.ROOT);
                int idx = Math.max(0, langs.indexOf(cur));
                idx = (idx + (type.isRightClick() ? langs.size() - 1 : 1)) % langs.size();
                String old = p.language;
                p.language = langs.get(idx);
                plugin.filters().changed(viewer, "settings.language", old, p.language);
                plugin.spectators().refreshHotbar(viewer);
                open();
            }
        });
        if (plugin.getConfig().getBoolean("spectator.noclip.enabled", true)) {
            toggleOption(24, "GLASS", "noclip", p.noclip, new Toggle() {
                @Override
                public boolean flip(Preferences pr) {
                    pr.noclip = !pr.noclip;
                    return pr.noclip;
                }
            });
        }
        back(31, new MainMenu(plugin, viewer));
        fill();
    }
}
