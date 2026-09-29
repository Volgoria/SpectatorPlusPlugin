package fr.spectatorplus.filter;

import fr.spectatorplus.api.event.Importance;
import fr.spectatorplus.core.config.ConfigSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Profil de filtres prédéfini (config.yml → presets).
 * <p>
 * Motifs acceptés dans include / exclude :
 * <ul>
 *     <li>{@code *} : tous les évènements</li>
 *     <li>{@code pvp.*} : tous les évènements dont l'id commence par « pvp. »</li>
 *     <li>{@code @custom} : tous les évènements de la catégorie « custom »</li>
 *     <li>{@code pvp.first_kill} : un évènement précis</li>
 * </ul>
 */
public final class Preset {

    private final String id;
    private final String displayName;
    private final String icon;
    private final List<String> description;
    private final Importance minImportance;
    private final List<String> include;
    private final List<String> exclude;

    public Preset(String id, ConfigSection s) {
        this.id = id.toLowerCase(Locale.ROOT);
        this.displayName = s.getString("display-name", id);
        this.icon = s.getString("icon", "PAPER");
        this.description = s.getStringList("description");
        this.minImportance = Importance.parse(s.getString("minimum-priority"), Importance.LOW);
        this.include = lower(s.getStringList("include"));
        this.exclude = lower(s.getStringList("exclude"));
    }

    private static List<String> lower(List<String> in) {
        List<String> res = new ArrayList<>();
        for (String s : in) res.add(s.toLowerCase(Locale.ROOT));
        return res;
    }

    public boolean includes(String eventId, String category) {
        return matchesAny(include, eventId, category) && !matchesAny(exclude, eventId, category);
    }

    private static boolean matchesAny(List<String> patterns, String id, String category) {
        for (String p : patterns) {
            if (p.equals("*") || p.equals(id)) return true;
            if (p.startsWith("@") && p.substring(1).equals(category)) return true;
            if (p.endsWith("*") && id.startsWith(p.substring(0, p.length() - 1))) return true;
        }
        return false;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public List<String> getDescription() {
        return description;
    }

    public Importance getMinImportance() {
        return minImportance;
    }
}
