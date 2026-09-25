package fr.spectatorplus.gui.menus;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.filter.Preferences;
import fr.spectatorplus.filter.Preset;
import fr.spectatorplus.gui.Menu;
import fr.spectatorplus.util.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

/**
 * Choix d'un preset de filtres.
 */
public final class PresetMenu extends Menu {

    public PresetMenu(SpectatorPlus plugin, Player viewer) {
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
            ItemBuilder b = item(p.getIcon()).name(plugin.messages().presetName(lang(), p)).lore(plugin.messages().presetDescription(lang(), p))
                    .lore(msg("gui.presets.importance", "importance", msg("importance." + p.getMinImportance().name())))
                    .lore(msg(current ? "gui.presets.current" : "gui.presets.click"))
                    .glow(current);
            set(slot++, b.build(), new ClickHandler() {
                @Override
                public void click(ClickType type) {
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
