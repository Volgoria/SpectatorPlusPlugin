package fr.spectatorplus.config;

import fr.spectatorplus.core.config.YamlConfig;
import fr.spectatorplus.core.platform.Host;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Chargement des fichiers YAML en UTF-8 sur toutes les plateformes
 * (la 1.8 utilisait l'encodage système pour certains chargements).
 */
public final class ConfigFiles {

    private final Host host;
    private YamlConfig config;
    private YamlConfig events;

    public ConfigFiles(Host host) {
        this.host = host;
    }

    public void load() {
        config = load("config.yml");
        events = load("events.yml");
    }

    public YamlConfig config() {
        return config;
    }

    public YamlConfig events() {
        return events;
    }

    public YamlConfig load(String name) {
        File file = new File(host.dataFolder(), name);
        if (!file.exists()) saveResource(name);
        YamlConfig yaml = new YamlConfig();
        try {
            yaml = YamlConfig.parse(new String(Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException e) {
            host.logger().severe("Impossible de lire " + name + " : " + e.getMessage());
        }
        YamlConfig defaults = loadResource(name);
        if (defaults != null) yaml.setDefaults(defaults);
        return yaml;
    }

    public YamlConfig loadResource(String name) {
        try (InputStream in = host.resource(name)) {
            if (in == null) return null;
            return YamlConfig.read(in);
        } catch (IOException e) {
            host.logger().severe("Ressource invalide " + name + " : " + e.getMessage());
            return null;
        }
    }

    /** Copie une ressource du jar dans le dossier de configuration si elle n'y est pas déjà. */
    public void saveResource(String name) {
        File file = new File(host.dataFolder(), name);
        if (file.exists()) return;
        try (InputStream in = host.resource(name)) {
            if (in == null) return;
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("Cannot create " + parent);
            Files.copy(in, file.toPath());
        } catch (IOException e) {
            host.logger().severe("Impossible de créer " + name + " : " + e.getMessage());
        }
    }

    public static void save(YamlConfig yaml, File file) throws IOException {
        yaml.save(file);
    }

    public static YamlConfig read(File file) {
        return YamlConfig.read(file);
    }
}
