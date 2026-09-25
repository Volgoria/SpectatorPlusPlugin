package fr.spectatorplus.config;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.api.event.EventCategory;
import fr.spectatorplus.api.event.SpectatorEventType;
import fr.spectatorplus.compat.Reflect;
import fr.spectatorplus.filter.Preset;
import fr.spectatorplus.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Gestion multilingue : un fichier par langue dans plugins/SpectatorPlus/lang/.
 * <p>
 * Langue d'un joueur : choix personnel (paramètres) → langue du client Minecraft (si activé)
 * → langue par défaut du serveur. Une clé absente est cherchée dans la langue par défaut, puis en anglais.
 * Pour ajouter une langue, il suffit de copier en.yml en xx.yml et de le traduire.
 * <p>
 * Les remplacements sont passés par paires : get(p, "key", "player", name) remplace {player}.
 */
public final class Messages {

    /** Langues fournies dans le jar. */
    public static final String[] BUNDLED = {"fr", "en", "es", "de", "pt"};
    private static final String FALLBACK = "en";

    private final SpectatorPlus plugin;
    private final Map<String, YamlConfiguration> langs = new LinkedHashMap<>();
    private String defaultLang = FALLBACK;
    private boolean perPlayer = true;

    public Messages(SpectatorPlus plugin) {
        this.plugin = plugin;
    }

    public void load() {
        langs.clear();
        File folder = new File(plugin.getDataFolder(), "lang");
        for (String code : BUNDLED) {
            if (!new File(folder, code + ".yml").exists()) plugin.saveResource("lang/" + code + ".yml", false);
        }
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                String name = f.getName();
                if (!name.toLowerCase(Locale.ROOT).endsWith(".yml")) continue;
                String code = name.substring(0, name.length() - 4).toLowerCase(Locale.ROOT);
                YamlConfiguration yaml = ConfigFiles.read(f);
                YamlConfiguration bundled = plugin.files().loadResource("lang/" + code + ".yml");
                if (bundled == null) bundled = plugin.files().loadResource("lang/" + FALLBACK + ".yml");
                if (bundled != null) yaml.setDefaults(bundled);
                langs.put(code, yaml);
            }
        }
        if (!langs.containsKey(FALLBACK)) {
            YamlConfiguration en = plugin.files().loadResource("lang/" + FALLBACK + ".yml");
            if (en != null) langs.put(FALLBACK, en);
        }
        String def = plugin.getConfig().getString("language.default", "en").toLowerCase(Locale.ROOT);
        defaultLang = langs.containsKey(def) ? def : FALLBACK;
        if (!langs.containsKey(def)) plugin.getLogger().warning("Langue '" + def + "' introuvable dans lang/, anglais utilisé.");
        perPlayer = plugin.getConfig().getBoolean("language.per-player", true);
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

    /** Nom affiché d'une langue (clé language.name de son fichier). */
    public String displayName(String code) {
        YamlConfiguration y = langs.get(code);
        String n = y == null ? null : y.getString("language.name");
        return n == null ? code : n;
    }

    /** Langue utilisée pour un destinataire. */
    public String lang(CommandSender sender) {
        if (!(sender instanceof Player)) return defaultLang;
        Player p = (Player) sender;
        String chosen = plugin.filters() == null ? null : plugin.filters().get(p).language;
        if (chosen != null && !chosen.equalsIgnoreCase("auto") && langs.containsKey(chosen)) return chosen;
        if (perPlayer) {
            String client = clientLocale(p);
            if (client != null && langs.containsKey(client)) return client;
        }
        return defaultLang;
    }

    /** « fr_fr » → « fr ». Player#getLocale (1.12+) ou Player.Spigot#getLocale (1.8). */
    private static String clientLocale(Player p) {
        Object loc = Reflect.invoke(p, "getLocale");
        if (loc == null) loc = Reflect.invoke(Reflect.invoke(p, "spigot"), "getLocale");
        if (loc == null) return null;
        String s = loc.toString().toLowerCase(Locale.ROOT);
        int i = s.indexOf('_');
        if (i < 0) i = s.indexOf('-');
        return i > 0 ? s.substring(0, i) : s;
    }

    // ------------------------------------------------------------------ lecture

    private String find(String code, String key) {
        YamlConfiguration y = langs.get(code);
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

    private List<String> findList(String code, String key) {
        for (String c : new String[]{code, defaultLang, FALLBACK}) {
            YamlConfiguration y = langs.get(c);
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

    public String raw(CommandSender sender, String key) {
        return raw(lang(sender), key);
    }

    public String get(String key, Object... replacements) {
        return Text.color(replace(raw(defaultLang, key), replacements));
    }

    public String get(CommandSender sender, String key, Object... replacements) {
        return Text.color(replace(raw(lang(sender), key), replacements));
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

    public List<String> list(CommandSender sender, String key, Object... replacements) {
        return listIn(lang(sender), key, replacements);
    }

    public String prefix(CommandSender sender) {
        return Text.color(raw(lang(sender), "prefix"));
    }

    public void send(CommandSender to, String key, Object... replacements) {
        String msg = raw(lang(to), key);
        if (msg.isEmpty()) return;
        to.sendMessage(prefix(to) + Text.color(replace(msg, replacements)));
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
