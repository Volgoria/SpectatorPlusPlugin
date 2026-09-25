package fr.spectatorplus.storage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Un fichier YAML par joueur dans plugins/SpectatorPlus/players/.
 */
public final class YamlStorage implements Storage {

    private final File folder;

    public YamlStorage(File dataFolder) {
        this.folder = new File(dataFolder, "players");
    }

    @Override
    public void init() throws IOException {
        if (!folder.exists() && !folder.mkdirs()) throw new IOException("Cannot create " + folder);
    }

    @Override
    public String load(UUID player) throws IOException {
        File f = file(player);
        if (!f.exists()) return null;
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    @Override
    public void save(UUID player, String data) throws IOException {
        Files.write(file(player).toPath(), data.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void delete(UUID player) throws IOException {
        Files.deleteIfExists(file(player).toPath());
    }

    private File file(UUID player) {
        return new File(folder, player + ".yml");
    }

    @Override
    public void close() {
    }

    @Override
    public String name() {
        return "YAML";
    }
}
