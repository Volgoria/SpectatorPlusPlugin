package fr.spectatorplus.core.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie que {@link ConfigSection} lit les fichiers exactement comme l'API Bukkit
 * (mêmes valeurs, mêmes valeurs par défaut) sur tous les fichiers fournis dans le jar.
 */
class ConfigSectionParityTest {

    private static final String[] FILES = {"config.yml", "events.yml",
            "lang/fr.yml", "lang/en.yml", "lang/es.yml", "lang/de.yml", "lang/pt.yml"};

    private static String resource(String name) throws Exception {
        try (InputStream in = ConfigSectionParityTest.class.getClassLoader().getResourceAsStream(name)) {
            assertNotNull(in, name);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static YamlConfiguration bukkit(String text) throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString(text);
        return y;
    }

    @Test
    void sameValuesOnBundledFiles() throws Exception {
        for (String file : FILES) {
            String text = resource(file);
            YamlConfiguration b = bukkit(text);
            YamlConfig c = YamlConfig.parse(text);
            assertEquals(new ArrayList<>(b.getKeys(true)), new ArrayList<>(c.getKeys(true)), file + " : clés");
            for (String k : b.getKeys(true)) compare(file, k, b, c);
        }
    }

    /** Fichier utilisateur incomplet + valeurs par défaut du jar (cas de ConfigFiles et Lang). */
    @Test
    void sameValuesWithDefaults() throws Exception {
        for (String file : FILES) {
            String text = resource(file);
            YamlConfiguration full = bukkit(text);
            // on retire une clé de premier niveau sur deux pour simuler un fichier ancien
            YamlConfiguration partialB = bukkit(text);
            YamlConfig partialC = YamlConfig.parse(text);
            int i = 0;
            for (String top : full.getKeys(false)) {
                if (i++ % 2 == 0) {
                    partialB.set(top, null);
                    partialC.set(top, null);
                }
            }
            partialB.setDefaults(bukkit(text));
            partialC.setDefaults(YamlConfig.parse(text));
            // sections d'abord : Bukkit crée des sections vides au fil des lectures de chemins
            for (String top : full.getKeys(false)) {
                ConfigurationSection sb = partialB.getConfigurationSection(top);
                ConfigSection sc = partialC.getConfigurationSection(top);
                if (sb == null) {
                    assertNull(sc, file + " section " + top);
                    continue;
                }
                assertNotNull(sc, file + " section " + top);
                assertEquals(new ArrayList<>(sb.getKeys(false)), new ArrayList<>(sc.getKeys(false)), file + " clés de " + top);
                for (String k : full.getConfigurationSection(top).getKeys(true)) {
                    compare(file + " sous-section " + top, k, sb, sc);
                }
            }
            for (String k : full.getKeys(true)) {
                compare(file + " (défauts)", k, partialB, partialC);
                if (!partialB.isConfigurationSection(k)) {
                    assertEquals(partialB.getString(k, "X"), partialC.getString(k, "X"), file + " getString(def) " + k);
                }
                assertEquals(partialB.getInt(k, -7), partialC.getInt(k, -7), file + " getInt(def) " + k);
                assertEquals(partialB.getBoolean(k, true), partialC.getBoolean(k, true), file + " getBoolean(def) " + k);
            }
        }
    }

    @Test
    void roundTrip() throws Exception {
        YamlConfig y = new YamlConfig();
        y.set("a.b", 3);
        y.set("list", java.util.Arrays.asList("x", "y"));
        java.util.Map<String, Boolean> m = new java.util.LinkedHashMap<>();
        m.put("pvp", true);
        m.put("world", false);
        y.set("categories", m);
        y.set("text", "Élimination « test »");
        YamlConfig r = YamlConfig.parse(y.saveToString());
        assertEquals(3, r.getInt("a.b"));
        assertEquals(java.util.Arrays.asList("x", "y"), r.getStringList("list"));
        assertTrue(r.isConfigurationSection("categories"));
        assertEquals(false, r.getConfigurationSection("categories").getBoolean("world", true));
        assertEquals("Élimination « test »", r.getString("text"));
        // relu par Bukkit à l'identique
        YamlConfiguration b = bukkit(y.saveToString());
        assertEquals(3, b.getInt("a.b"));
        assertEquals("Élimination « test »", b.getString("text"));
    }

    private static void compare(String file, String k, ConfigurationSection b, ConfigSection c) {
        String ctx = file + " : " + k;
        assertEquals(b.isConfigurationSection(k), c.isConfigurationSection(k), ctx + " isSection");
        assertEquals(b.contains(k), c.contains(k), ctx + " contains");
        if (!b.isConfigurationSection(k)) assertEquals(b.getString(k), c.getString(k), ctx + " getString");
        assertEquals(b.getBoolean(k), c.getBoolean(k), ctx + " getBoolean");
        assertEquals(b.getInt(k), c.getInt(k), ctx + " getInt");
        assertEquals(b.getDouble(k), c.getDouble(k), 0.0, ctx + " getDouble");
        assertEquals(b.isList(k), c.isList(k), ctx + " isList");
        assertEquals(b.isString(k), c.isString(k), ctx + " isString");
        assertEquals(b.isInt(k), c.isInt(k), ctx + " isInt");
        assertEquals(b.getStringList(k), c.getStringList(k), ctx + " getStringList");
        List<Integer> bi = b.getIntegerList(k);
        assertEquals(bi, c.getIntegerList(k), ctx + " getIntegerList");
    }
}
