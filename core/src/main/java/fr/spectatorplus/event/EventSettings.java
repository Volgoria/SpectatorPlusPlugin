package fr.spectatorplus.event;

import fr.spectatorplus.api.event.Importance;
import fr.spectatorplus.core.config.ConfigSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Réglages d'un type d'évènement lus dans events.yml.
 * Le chemin YAML correspond à l'identifiant : « pvp.first_kill » → pvp: first_kill: ...
 */
public final class EventSettings {

    private static final ConfigSection EMPTY = new ConfigSection();

    private final boolean enabled;
    private final Importance importance;
    private final String message;
    private final int cooldown;
    private final ConfigSection section;

    public EventSettings(ConfigSection section, boolean defaultEnabled) {
        this.section = section == null ? EMPTY : section;
        this.enabled = this.section.getBoolean("enabled", defaultEnabled);
        this.importance = this.section.isString("priority") ? Importance.parse(this.section.getString("priority"), null) : null;
        this.message = this.section.getString("message");
        this.cooldown = this.section.getInt("cooldown", 0);
    }

    public boolean enabled() {
        return enabled;
    }

    /** Importance configurée, ou null. */
    public Importance importance() {
        return importance;
    }

    public String message() {
        return message;
    }

    /** Délai minimum (secondes) entre deux déclenchements pour un même joueur. */
    public int cooldown() {
        return cooldown;
    }

    /** Liste de noms (matériaux, entités, effets...) en majuscules. */
    public List<String> names(String key) {
        if (!section.isList(key)) return Collections.emptyList();
        List<String> res = new ArrayList<>();
        for (String s : section.getStringList(key)) res.add(s.trim().toUpperCase(Locale.ROOT));
        return res;
    }

    public List<String> strings(String key) {
        return section.isList(key) ? section.getStringList(key) : Collections.<String>emptyList();
    }

    public List<Integer> ints(String key) {
        if (!section.isList(key)) {
            if (section.isInt(key)) return Collections.singletonList(section.getInt(key));
            return Collections.emptyList();
        }
        return section.getIntegerList(key);
    }

    public List<Double> doubles(String key) {
        if (!section.isList(key)) {
            if (section.contains(key)) return Collections.singletonList(section.getDouble(key));
            return Collections.emptyList();
        }
        List<Double> res = new ArrayList<>();
        for (Object o : section.getList(key)) {
            if (o instanceof Number) res.add(((Number) o).doubleValue());
        }
        return res;
    }

    public int integer(String key, int def) {
        return section.getInt(key, def);
    }

    public double number(String key, double def) {
        return section.getDouble(key, def);
    }

    public boolean bool(String key, boolean def) {
        return section.getBoolean(key, def);
    }

    public ConfigSection section() {
        return section;
    }

    /** Vrai si le nom est dans la liste (liste vide = tout accepter). */
    public boolean accepts(String key, String name) {
        List<String> list = names(key);
        if (list.isEmpty()) return true;
        String n = name.toUpperCase(Locale.ROOT);
        for (String s : list) {
            if (s.equals("*") || s.equals(n)) return true;
            if (s.endsWith("*") && n.startsWith(s.substring(0, s.length() - 1))) return true;
        }
        return false;
    }

    /** Comme accepts mais avec plusieurs alias possibles (ex : noms legacy et modernes d'un effet). */
    public boolean acceptsAny(String key, Iterable<String> names) {
        List<String> list = names(key);
        if (list.isEmpty()) return true;
        for (String n : names) if (accepts(key, n)) return true;
        return false;
    }
}
