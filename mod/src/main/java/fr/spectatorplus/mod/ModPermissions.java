package fr.spectatorplus.mod;

import fr.spectatorplus.core.config.ConfigSection;
import fr.spectatorplus.core.config.YamlConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Permissions sur les serveurs moddés.
 * <p>
 * Les valeurs par défaut viennent du plugin.yml Bukkit (copié dans le jar) : « true » pour tout le monde,
 * « op » pour les opérateurs (niveau 2), « false » pour personne. Si un gestionnaire de permissions
 * compatible fabric-permissions-api (LuckPerms...) est installé, il est consulté en priorité.
 */
public final class ModPermissions {

    private enum Default {TRUE, OP, FALSE}

    private final Map<String, Default> defaults = new HashMap<>();
    private Method check;
    private Method checkSource;

    void load(Logger logger) {
        try (InputStream in = ModPermissions.class.getClassLoader().getResourceAsStream("spectatorplus-permissions.yml")) {
            if (in != null) {
                ConfigSection perms = YamlConfig.read(in).getConfigurationSection("permissions");
                if (perms != null) {
                    for (String node : perms.getKeys(false)) {
                        String d = perms.getString(node + ".default", "op").toLowerCase(Locale.ROOT);
                        defaults.put(node.toLowerCase(Locale.ROOT),
                                d.equals("true") ? Default.TRUE : d.equals("false") ? Default.FALSE : Default.OP);
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Permissions par défaut illisibles : " + e.getMessage());
        }
        try {
            Class<?> api = Class.forName("me.lucko.fabric.api.permissions.v0.Permissions");
            check = api.getMethod("check", net.minecraft.world.entity.Entity.class, String.class, boolean.class);
            checkSource = api.getMethod("check", net.minecraft.commands.SharedSuggestionProvider.class, String.class, boolean.class);
            logger.info("fabric-permissions-api détecté : permissions gérées par le gestionnaire du serveur.");
        } catch (Throwable ignored) {
            check = null;
            checkSource = null;
        }
    }

    private boolean fallback(String node, boolean operator) {
        Default d = defaults.get(node.toLowerCase(Locale.ROOT));
        if (d == null) d = Default.OP;
        return d == Default.TRUE || (d == Default.OP && operator);
    }

    public boolean has(ServerPlayer player, String node) {
        boolean def = fallback(node, Mc.isOperator(player));
        if (check != null) {
            try {
                return (Boolean) check.invoke(null, player, node, def);
            } catch (Throwable ignored) {
            }
        }
        return def;
    }

    public boolean has(CommandSourceStack source, String node) {
        boolean def = fallback(node, Mc.isOperator(source));
        if (checkSource != null) {
            try {
                return (Boolean) checkSource.invoke(null, source, node, def);
            } catch (Throwable ignored) {
            }
        }
        return def;
    }
}
