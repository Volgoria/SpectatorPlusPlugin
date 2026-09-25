package fr.spectatorplus.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Chargement des fichiers YAML en UTF-8 sur toutes les versions
 * (la 1.8 utilisait l'encodage système pour certains chargements).
 */
public final class ConfigFiles {

    private final JavaPlugin plugin;
    private YamlConfiguration config;
    private YamlConfiguration events;

    public ConfigFiles(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        config = load("config.yml");
        events = load("events.yml");
    }

    public YamlConfiguration config() {
        return config;
    }

    public YamlConfiguration events() {
        return events;
    }

    public YamlConfiguration load(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) plugin.saveResource(name, false);
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().severe("Impossible de lire " + name + " : " + e.getMessage());
        }
        YamlConfiguration defaults = loadResource(name);
        if (defaults != null) yaml.setDefaults(defaults);
        return yaml;
    }

    public YamlConfiguration loadResource(String name) {
        try (InputStream in = plugin.getResource(name)) {
            if (in == null) return null;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.loadFromString(new String(out.toByteArray(), StandardCharsets.UTF_8));
            return yaml;
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().severe("Ressource invalide " + name + " : " + e.getMessage());
            return null;
        }
    }

    public static void save(YamlConfiguration yaml, File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create " + parent);
        }
        Files.write(file.toPath(), yaml.saveToString().getBytes(StandardCharsets.UTF_8));
    }

    public static YamlConfiguration read(File file) {
        YamlConfiguration yaml = new YamlConfiguration();
        if (!file.exists()) return yaml;
        try {
            yaml.loadFromString(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        } catch (IOException | InvalidConfigurationException ignored) {
        }
        return yaml;
    }
}
