package fr.spectatorplus.core.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Section de configuration indépendante de Bukkit.
 * <p>
 * Reproduit le comportement de {@code org.bukkit.configuration.ConfigurationSection} pour que le code
 * commun lise les fichiers de la même manière sur toutes les plateformes :
 * <ul>
 *     <li>chemins séparés par des points (« events.format ») ;</li>
 *     <li>une valeur absente est cherchée dans les valeurs par défaut (fichier du jar),
 *     sauf quand un défaut explicite est passé : {@code getString("x", "y")} renvoie « y » ;</li>
 *     <li>{@link #getKeys(boolean)} ne renvoie que les clés présentes dans le fichier.</li>
 * </ul>
 */
public class ConfigSection {

    private final Map<String, Object> map = new LinkedHashMap<>();
    /** Section équivalente dans les valeurs par défaut, ou null. */
    ConfigSection defaults;

    public ConfigSection() {
    }

    ConfigSection(Map<?, ?> values) {
        if (values != null) {
            for (Map.Entry<?, ?> e : values.entrySet()) {
                map.put(String.valueOf(e.getKey()), wrap(e.getValue()));
            }
        }
    }

    private static Object wrap(Object value) {
        if (value instanceof ConfigSection) return value;
        if (value instanceof Map) return new ConfigSection((Map<?, ?>) value);
        return value;
    }

    // ------------------------------------------------------------------ navigation

    /** Valeur brute du chemin dans cette section uniquement (sans les défauts). */
    private Object own(String path) {
        ConfigSection s = this;
        int start = 0, dot;
        while ((dot = path.indexOf('.', start)) >= 0) {
            Object child = s.map.get(path.substring(start, dot));
            if (!(child instanceof ConfigSection)) return null;
            s = (ConfigSection) child;
            start = dot + 1;
        }
        return s.map.get(path.substring(start));
    }

    /** Valeur par défaut du chemin, ou null. */
    private Object defaultValue(String path) {
        return defaults == null ? null : defaults.get(path);
    }

    /** Valeur du chemin, puis celle des défauts. */
    public Object get(String path) {
        Object v = own(path);
        if (v != null) return v;
        return defaultValue(path);
    }

    /** Valeur du chemin, ou {@code def} si elle est absente (les défauts ne sont pas consultés). */
    public Object get(String path, Object def) {
        Object v = own(path);
        return v != null ? v : def;
    }

    public boolean contains(String path) {
        return get(path) != null;
    }

    public void set(String path, Object value) {
        ConfigSection s = this;
        int start = 0, dot;
        while ((dot = path.indexOf('.', start)) >= 0) {
            String key = path.substring(start, dot);
            Object child = s.map.get(key);
            if (!(child instanceof ConfigSection)) {
                if (value == null) return;
                child = new ConfigSection();
                s.map.put(key, child);
            }
            s = (ConfigSection) child;
            start = dot + 1;
        }
        String key = path.substring(start);
        if (value == null) s.map.remove(key);
        else s.map.put(key, wrap(value));
    }

    public ConfigSection createSection(String path) {
        ConfigSection s = new ConfigSection();
        set(path, s);
        return s;
    }

    /** Clés de cette section (et des sous-sections si {@code deep}), dans l'ordre du fichier. */
    public Set<String> getKeys(boolean deep) {
        Set<String> res = new LinkedHashSet<>();
        collect(res, "", deep);
        return res;
    }

    private void collect(Set<String> into, String prefix, boolean deep) {
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String key = prefix + e.getKey();
            into.add(key);
            if (deep && e.getValue() instanceof ConfigSection) {
                ((ConfigSection) e.getValue()).collect(into, key + ".", true);
            }
        }
    }

    /**
     * Sous-section. Si elle n'existe que dans les défauts, renvoie une section vide
     * qui lit ses valeurs dans les défauts (comme Bukkit).
     */
    public ConfigSection getConfigurationSection(String path) {
        Object v = own(path);
        if (v != null) {
            if (!(v instanceof ConfigSection)) return null;
            ConfigSection s = (ConfigSection) v;
            Object d = defaultValue(path);
            s.defaults = d instanceof ConfigSection ? (ConfigSection) d : null;
            return s;
        }
        Object d = defaultValue(path);
        if (!(d instanceof ConfigSection)) return null;
        ConfigSection view = createSection(path);
        view.defaults = (ConfigSection) d;
        return view;
    }

    public boolean isConfigurationSection(String path) {
        return get(path) instanceof ConfigSection;
    }

    // ------------------------------------------------------------------ valeurs simples

    public String getString(String path) {
        Object d = defaultValue(path);
        return getString(path, d != null ? d.toString() : null);
    }

    public String getString(String path, String def) {
        Object v = get(path, def);
        return v != null ? v.toString() : def;
    }

    public boolean isString(String path) {
        return get(path) instanceof String;
    }

    public boolean getBoolean(String path) {
        Object d = defaultValue(path);
        return getBoolean(path, d instanceof Boolean ? (Boolean) d : false);
    }

    public boolean getBoolean(String path, boolean def) {
        Object v = get(path, def);
        return v instanceof Boolean ? (Boolean) v : def;
    }

    public boolean isBoolean(String path) {
        return get(path) instanceof Boolean;
    }

    public int getInt(String path) {
        Object d = defaultValue(path);
        return getInt(path, d instanceof Number ? ((Number) d).intValue() : 0);
    }

    public int getInt(String path, int def) {
        Object v = get(path, def);
        return v instanceof Number ? ((Number) v).intValue() : def;
    }

    public boolean isInt(String path) {
        return get(path) instanceof Integer;
    }

    public long getLong(String path) {
        Object d = defaultValue(path);
        return getLong(path, d instanceof Number ? ((Number) d).longValue() : 0L);
    }

    public long getLong(String path, long def) {
        Object v = get(path, def);
        return v instanceof Number ? ((Number) v).longValue() : def;
    }

    public double getDouble(String path) {
        Object d = defaultValue(path);
        return getDouble(path, d instanceof Number ? ((Number) d).doubleValue() : 0.0);
    }

    public double getDouble(String path, double def) {
        Object v = get(path, def);
        return v instanceof Number ? ((Number) v).doubleValue() : def;
    }

    public boolean isDouble(String path) {
        return get(path) instanceof Double;
    }

    // ------------------------------------------------------------------ listes

    public List<?> getList(String path) {
        Object d = defaultValue(path);
        return getList(path, d instanceof List ? (List<?>) d : null);
    }

    public List<?> getList(String path, List<?> def) {
        Object v = get(path, def);
        return v instanceof List ? (List<?>) v : def;
    }

    public boolean isList(String path) {
        return get(path) instanceof List;
    }

    public List<String> getStringList(String path) {
        List<?> list = getList(path);
        if (list == null) return new ArrayList<>(0);
        List<String> res = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof String || o instanceof Number || o instanceof Boolean || o instanceof Character) {
                res.add(String.valueOf(o));
            }
        }
        return res;
    }

    public List<Integer> getIntegerList(String path) {
        List<?> list = getList(path);
        if (list == null) return new ArrayList<>(0);
        List<Integer> res = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Number) {
                res.add(((Number) o).intValue());
            } else if (o instanceof String) {
                try {
                    res.add(Integer.valueOf(((String) o).trim()));
                } catch (NumberFormatException ignored) {
                }
            } else if (o instanceof Character) {
                res.add((int) (Character) o);
            }
        }
        return res;
    }

    public List<Map<?, ?>> getMapList(String path) {
        List<?> list = getList(path);
        if (list == null) return new ArrayList<>(0);
        List<Map<?, ?>> res = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Map) res.add((Map<?, ?>) o);
            else if (o instanceof ConfigSection) res.add(((ConfigSection) o).toMap());
        }
        return res;
    }

    // ------------------------------------------------------------------ conversion

    /** Copie en Map imbriquées (pour l'écriture YAML). */
    public Map<String, Object> toMap() {
        Map<String, Object> res = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            Object v = e.getValue();
            res.put(e.getKey(), v instanceof ConfigSection ? ((ConfigSection) v).toMap() : v);
        }
        return res;
    }

    public Map<String, Object> getValues() {
        return Collections.unmodifiableMap(map);
    }
}
