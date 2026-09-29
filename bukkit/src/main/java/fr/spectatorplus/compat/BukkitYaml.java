package fr.spectatorplus.compat;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Lecture / écriture UTF-8 de fichiers YAML Bukkit, pour les données qui contiennent des objets
 * sérialisés par Bukkit (ItemStack, PotionEffect...) : état sauvegardé des spectateurs.
 * Les fichiers de configuration passent par {@link fr.spectatorplus.config.ConfigFiles}.
 */
public final class BukkitYaml {

    private BukkitYaml() {
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
