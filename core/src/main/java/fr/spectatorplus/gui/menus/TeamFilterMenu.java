package fr.spectatorplus.gui.menus;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.core.platform.Click;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.filter.Preferences;
import fr.spectatorplus.gui.Menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Équipes suivies (aucune sélection = toutes les équipes).
 */
public final class TeamFilterMenu extends Menu {

    public TeamFilterMenu(SpectatorCore plugin, PlatformPlayer viewer) {
        super(plugin, viewer);
    }

    @Override
    public String id() {
        return "team-filters";
    }

    @Override
    protected String title() {
        return msg("gui.team-filters.title");
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void render() {
        final Preferences prefs = plugin.filters().get(viewer);
        Set<String> teams = new LinkedHashSet<>();
        for (PlatformPlayer p : plugin.spectators().targets()) {
            String t = plugin.game().getTeam(p);
            if (t != null) teams.add(t);
        }
        teams.addAll(prefs.teams);
        List<String> sorted = new ArrayList<>(teams);
        Collections.sort(sorted);
        int slot = 0;
        for (final String team : sorted) {
            if (slot >= 45) break;
            final boolean on = prefs.teams.contains(team);
            set(slot++, toggle(team, on, false, null), new ClickHandler() {
                @Override
                public void click(Click type) {
                    if (on) prefs.teams.remove(team);
                    else prefs.teams.add(team);
                    plugin.filters().changed(viewer, "player.teams", on, !on);
                    refresh();
                }
            });
        }
        if (sorted.isEmpty()) set(22, item("BARRIER").name(msg("gui.team-filters.empty")).build());
        set(47, item("PAPER").name(msg("gui.team-filters.info")).build());
        set(51, item("LAVA_BUCKET").name(msg("gui.team-filters.clear")).build(), new ClickHandler() {
            @Override
            public void click(Click type) {
                prefs.teams.clear();
                plugin.filters().changed(viewer, "player.teams", "", "");
                refresh();
            }
        });
        back(49, new PlayerFilterMenu(plugin, viewer));
        fill(45, 53);
    }
}
