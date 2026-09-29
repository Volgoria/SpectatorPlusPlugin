package fr.spectatorplus.core.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

/**
 * Fichier YAML complet (racine d'une {@link ConfigSection}), lu et écrit en UTF-8.
 * <p>
 * N'utilise que l'API de SnakeYAML commune aux versions 1.15 (Spigot 1.8) à 2.x (Paper récent,
 * copie embarquée dans les mods) : {@code new Yaml(DumperOptions)}, {@code load}, {@code dump}.
 */
public class YamlConfig extends ConfigSection {

    public YamlConfig() {
    }

    private YamlConfig(Map<?, ?> values) {
        super(values);
    }

    private static Yaml yaml() {
        DumperOptions o = new DumperOptions();
        o.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        o.setIndent(2);
        o.setAllowUnicode(true);
        o.setWidth(4096);
        return new Yaml(o);
    }

    /** Remplace le contenu par le texte YAML donné. */
    public static YamlConfig parse(String text) throws IOException {
        Object root;
        try {
            root = yaml().load(text);
        } catch (RuntimeException e) {
            throw new IOException(e.getMessage(), e);
        }
        if (root == null) return new YamlConfig();
        if (!(root instanceof Map)) throw new IOException("Top level is not a map");
        return new YamlConfig((Map<?, ?>) root);
    }

    public String saveToString() {
        Map<String, Object> m = toMap();
        return m.isEmpty() ? "" : yaml().dump(m);
    }

    public void setDefaults(ConfigSection defaults) {
        this.defaults = defaults;
    }

    public ConfigSection getDefaults() {
        return defaults;
    }

    // ------------------------------------------------------------------ fichiers

    /** Lit un fichier ; renvoie une configuration vide s'il n'existe pas ou est invalide. */
    public static YamlConfig read(File file) {
        if (!file.exists()) return new YamlConfig();
        try {
            return parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return new YamlConfig();
        }
    }

    public static YamlConfig read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
        return parse(new String(out.toByteArray(), StandardCharsets.UTF_8));
    }

    public void save(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create " + parent);
        }
        Files.write(file.toPath(), saveToString().getBytes(StandardCharsets.UTF_8));
    }
}
