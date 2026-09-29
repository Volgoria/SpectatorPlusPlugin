package fr.spectatorplus.gui.menus;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.core.platform.Click;
import fr.spectatorplus.core.platform.Icon;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.filter.Preferences;
import fr.spectatorplus.filter.Preset;
import fr.spectatorplus.gui.Menu;

/**
 * Choix d'un preset de filtres.
 */
public final class PresetMenu extends Menu {

    public PresetMenu(SpectatorCore plugin, PlatformPlayer viewer) {
        super(plugin, viewer);
    }

    @Override
    public String id() {
        return "presets";
    }

    @Override
    protected String title() {
        return msg("gui.presets.title");
    }

    @Override
    protected int rows() {
        return 3;
    }

    @Override
    protected void render() {
        Preferences prefs = plugin.filters().get(viewer);
        int slot = 10;
        for (final Preset p : plugin.filters().presets().values()) {
            if (slot > 16) break;
            boolean current = p.getId().equals(prefs.preset) && !prefs.customized;
            Icon b = item(p.getIcon()).name(plugin.messages().presetName(lang(), p)).lore(plugin.messages().presetDescription(lang(), p))
                    .lore(msg("gui.presets.importance", "importance", msg("importance." + p.getMinImportance().name())))
                    .lore(msg(current ? "gui.presets.current" : "gui.presets.click"))
                    .glow(current);
            set(slot++, b.build(), new ClickHandler() {
                @Override
                public void click(Click type) {
                    plugin.filters().setPreset(viewer, p.getId());
                    plugin.messages().send(viewer, "filters.preset-applied", "preset", plugin.messages().presetName(lang(), p));
                    new FiltersMenu(plugin, viewer).open();
                }
            });
        }
        back(22, new FiltersMenu(plugin, viewer));
        fill();
    }
}
