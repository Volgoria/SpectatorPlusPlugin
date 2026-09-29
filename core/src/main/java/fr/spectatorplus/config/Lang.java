package fr.spectatorplus.config;

import fr.spectatorplus.api.event.EventCategory;
import fr.spectatorplus.api.event.SpectatorEventType;
import fr.spectatorplus.core.config.YamlConfig;
import fr.spectatorplus.core.platform.Host;
import fr.spectatorplus.filter.Preset;
import fr.spectatorplus.util.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Fichiers de langue : un fichier par langue dans le dossier lang/ de la configuration.
 * <p>
 * Une clé absente est cherchée dans la langue par défaut, puis en anglais.
 * Pour ajouter une langue, il suffit de copier en.yml en xx.yml et de le traduire.
 * La langue d'un destinataire (choix personnel, langue du client...) est résolue par chaque plateforme
 * à l'aide de {@link #resolve(String, String)}.
 * <p>
 * Les remplacements sont passés par paires : get("key", "player", name) remplace {player}.
 */
public class Lang {

    /** Langues fournies dans le jar. */
    public static final String[] BUNDLED = {"fr", "en", "es", "de", "pt"};
    protected static final String FALLBACK = "en";

    protected final Host host;
    protected final ConfigFiles files;
    private final Map<String, YamlConfig> langs = new LinkedHashMap<>();
    private String defaultLang = FALLBACK;
    private boolean perPlayer = true;

    public Lang(Host host, ConfigFiles files) {
        this.host = host;
        this.files = files;
    }

    public void load() {
        langs.clear();
        File folder = new File(host.dataFolder(), "lang");
        for (String code : BUNDLED) files.saveResource("lang/" + code + ".yml");
        File[] list = folder.listFiles();
        if (list != null) {
            for (File f : list) {
                String name = f.getName();
                if (!name.toLowerCase(Locale.ROOT).endsWith(".yml")) continue;
                String code = name.substring(0, name.length() - 4).toLowerCase(Locale.ROOT);
                YamlConfig yaml = YamlConfig.read(f);
                YamlConfig bundled = files.loadResource("lang/" + code + ".yml");
                if (bundled == null) bundled = files.loadResource("lang/" + FALLBACK + ".yml");
                if (bundled != null) yaml.setDefaults(bundled);
                langs.put(code, yaml);
            }
        }
        if (!langs.containsKey(FALLBACK)) {
            YamlConfig en = files.loadResource("lang/" + FALLBACK + ".yml");
            if (en != null) langs.put(FALLBACK, en);
        }
        String def = files.config().getString("language.default", "en").toLowerCase(Locale.ROOT);
        defaultLang = langs.containsKey(def) ? def : FALLBACK;
        if (!langs.containsKey(def)) host.logger().warning("Langue '" + def + "' introuvable dans lang/, anglais utilisé.");
        perPlayer = files.config().getBoolean("language.per-player", true);
    }

    // ------------------------------------------------------------------ langues

    public Set<String> available() {
        return Collections.unmodifiableSet(langs.keySet());
    }

    public boolean exists(String code) {
        return code != null && langs.containsKey(code.toLowerCase(Locale.ROOT));
    }

    public String defaultLang() {
        return defaultLang;
    }

    /** La langue du client Minecraft est-elle prise en compte (language.per-player) ? */
    public boolean perPlayer() {
        return perPlayer;
    }

    /** Nom affiché d'une langue (clé language.name de son fichier). */
    public String displayName(String code) {
        YamlConfig y = langs.get(code);
        String n = y == null ? null : y.getString("language.name");
        return n == null ? code : n;
    }

    /**
     * Langue d'un joueur : son choix (« auto » ou null si aucun) → langue de son client
     * (« fr_fr », null si inconnue ; ignorée si language.per-player est désactivé) → langue par défaut.
     */
    public String resolve(String chosen, String clientLocale) {
        if (chosen != null && !chosen.equalsIgnoreCase("auto") && langs.containsKey(chosen)) return chosen;
        if (perPlayer && clientLocale != null) {
            String client = shortLocale(clientLocale);
            if (langs.containsKey(client)) return client;
        }
        return defaultLang;
    }

    /** « fr_fr » → « fr ». */
    public static String shortLocale(String locale) {
        String s = locale.toLowerCase(Locale.ROOT);
        int i = s.indexOf('_');
        if (i < 0) i = s.indexOf('-');
        return i > 0 ? s.substring(0, i) : s;
    }

    // ------------------------------------------------------------------ lecture

    protected String find(String code, String key) {
        YamlConfig y = langs.get(code);
        String s = y == null ? null : y.getString(key);
        if (s == null && !code.equals(defaultLang)) {
            y = langs.get(defaultLang);
            s = y == null ? null : y.getString(key);
        }
        if (s == null && !code.equals(FALLBACK)) {
            y = langs.get(FALLBACK);
            s = y == null ? null : y.getString(key);
        }
        return s;
    }

    protected List<String> findList(String code, String key) {
        for (String c : new String[]{code, defaultLang, FALLBACK}) {
            YamlConfig y = langs.get(c);
            if (y == null) continue;
            if (y.isList(key)) return y.getStringList(key);
            if (y.isString(key)) return Collections.singletonList(y.getString(key));
        }
        return null;
    }

    public boolean has(String code, String key) {
        return find(code, key) != null || findList(code, key) != null;
    }

    /** Texte brut (sans couleurs), ou la clé si elle n'existe dans aucune langue. */
    public String raw(String code, String key) {
        String s = find(code, key);
        // « \n » littéral (chaînes entre apostrophes) → vrai retour à la ligne
        return s == null ? key : s.replace("\\n", "\n");
    }

    public String raw(String key) {
        return raw(defaultLang, key);
    }

    public String get(String key, Object... replacements) {
        return Text.color(replace(raw(defaultLang, key), replacements));
    }

    /** Texte coloré dans une langue donnée. */
    public String getIn(String code, String key, Object... replacements) {
        return Text.color(replace(raw(code, key), replacements));
    }

    public List<String> listIn(String code, String key, Object... replacements) {
        List<String> res = new ArrayList<>();
        List<String> lines = findList(code, key);
        if (lines != null) {
            for (String l : lines) res.add(Text.color(replace(l.replace("\\n", "\n"), replacements)));
        }
        return res;
    }

    public List<String> list(String key, Object... replacements) {
        return listIn(defaultLang, key, replacements);
    }

    // ------------------------------------------------------------------ noms traduits

    public String eventName(String code, SpectatorEventType type) {
        String s = find(code, "event-names." + type.getId());
        return s != null ? s : type.getDisplayName();
    }

    /** Message traduit d'un évènement, ou null si aucun fichier de langue ne le définit. */
    public String eventMessage(String code, String typeId) {
        return find(code, "event-messages." + typeId);
    }

    public String categoryName(String code, EventCategory category) {
        String s = find(code, "categories." + category.getId());
        return s != null ? s : category.getDisplayName();
    }

    public String presetName(String code, Preset preset) {
        String s = find(code, "presets." + preset.getId() + ".name");
        return s != null ? s : preset.getDisplayName();
    }

    public List<String> presetDescription(String code, Preset preset) {
        List<String> l = findList(code, "presets." + preset.getId() + ".description");
        return l != null ? l : preset.getDescription();
    }

    public static String replace(String text, Object... replacements) {
        if (text == null) return "";
        String res = text;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            res = res.replace("{" + replacements[i] + "}", String.valueOf(replacements[i + 1]));
        }
        return res;
    }
}
