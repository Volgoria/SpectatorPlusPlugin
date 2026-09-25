package fr.spectatorplus.placeholder;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.api.Spectator;
import fr.spectatorplus.api.event.SpectatorGameEvent;
import fr.spectatorplus.api.placeholder.PlaceholderContext;
import fr.spectatorplus.api.placeholder.PlaceholderResolver;
import fr.spectatorplus.api.placeholder.PlaceholderService;
import fr.spectatorplus.compat.Reflect;
import fr.spectatorplus.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Placeholders {clé} : données de l'évènement, placeholders internes, placeholders externes (API)
 * et, si présent, PlaceholderAPI (%...%).
 */
public final class PlaceholderManager implements PlaceholderService {

    private static final Pattern PATTERN = Pattern.compile("\\{([a-zA-Z0-9_.\\-]+)}");

    private final SpectatorPlus plugin;
    private final Map<String, PlaceholderResolver> external = new ConcurrentHashMap<>();
    private Class<?> papi;

    public PlaceholderManager(SpectatorPlus plugin) {
        this.plugin = plugin;
    }

    public void hookPlaceholderApi() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papi = Reflect.getClass("me.clip.placeholderapi.PlaceholderAPI");
            if (papi != null) plugin.getLogger().info("PlaceholderAPI détecté : placeholders %...% activés.");
        }
    }

    @Override
    public void register(String key, PlaceholderResolver resolver) {
        external.put(strip(key), resolver);
    }

    @Override
    public void unregister(String key) {
        external.remove(strip(key));
    }

    @Override
    public boolean isRegistered(String key) {
        return external.containsKey(strip(key));
    }

    @Override
    public Set<String> getRegisteredKeys() {
        return Collections.unmodifiableSet(external.keySet());
    }

    private static String strip(String key) {
        String k = key.trim();
        if (k.startsWith("{") && k.endsWith("}")) k = k.substring(1, k.length() - 1);
        return k.toLowerCase(Locale.ROOT);
    }

    @Override
    public String apply(String text, Player viewer, SpectatorGameEvent event) {
        if (text == null || text.isEmpty()) return "";
        UUID subject = event == null ? null : event.getPrimaryPlayer();
        PlaceholderContext ctx = new PlaceholderContext(viewer, event, subject);
        String result = text;
        // deux passes : un message d'évènement peut contenir des placeholders
        for (int pass = 0; pass < 2 && result.indexOf('{') >= 0; pass++) {
            Matcher m = PATTERN.matcher(result);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String value = resolve(m.group(1), ctx);
                m.appendReplacement(sb, Matcher.quoteReplacement(value == null ? m.group(0) : value));
            }
            m.appendTail(sb);
            result = sb.toString();
        }
        if (papi != null && result.indexOf('%') >= 0) {
            OfflinePlayer target = subject != null ? Bukkit.getOfflinePlayer(subject) : viewer;
            Object r = Reflect.invokeStatic(papi, "setPlaceholders", target, result);
            if (r instanceof String) result = (String) r;
        }
        return result;
    }

    private String resolve(String rawKey, PlaceholderContext ctx) {
        String key = rawKey.toLowerCase(Locale.ROOT);
        SpectatorGameEvent e = ctx.getEvent();
        if (e != null) {
            String v = e.get(key);
            // « @clé » : valeur traduite dans la langue du lecteur (ex : @placeholders.zone-enter)
            if (v != null && v.startsWith("@") && v.length() > 1) return plugin.messages().raw(lang(ctx), v.substring(1));
            if (v != null) return v;
        }
        PlaceholderResolver ext = external.get(key);
        if (ext != null) {
            try {
                String v = ext.resolve(ctx);
                if (v != null) return v;
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "Erreur du placeholder externe {" + key + "}", t);
            }
        }
        return internal(key, ctx);
    }

    private String lang(PlaceholderContext ctx) {
        return ctx.getViewer() == null ? plugin.messages().defaultLang() : plugin.messages().lang(ctx.getViewer());
    }

    private String internal(String key, PlaceholderContext ctx) {
        SpectatorGameEvent e = ctx.getEvent();
        Player viewer = ctx.getViewer();
        switch (key) {
            // --- évènement
            case "event":
                return e == null ? null : e.getTypeId();
            case "event_name":
                return e == null ? null : Text.color(plugin.messages().eventName(lang(ctx), e.getType()));
            case "event_category": {
                if (e == null) return null;
                fr.spectatorplus.api.event.EventCategory c = plugin.events().getCategory(e.getCategory());
                return c == null ? e.getCategory() : Text.color(plugin.messages().categoryName(lang(ctx), c));
            }
            case "event_priority":
                return e == null || e.getImportance() == null ? null : e.getImportance().name();
            case "event_time":
                return e == null ? null : new SimpleDateFormat("HH:mm:ss").format(new Date(e.getTimestamp()));
            case "event_timestamp":
                return e == null ? null : String.valueOf(e.getTimestamp());
            // --- spectateur
            case "spectator":
                return viewer == null ? null : viewer.getName();
            case "spectator_uuid":
                return viewer == null ? null : viewer.getUniqueId().toString();
            case "spectator_target":
            case "spectator_following": {
                Spectator s = viewer == null ? null : plugin.spectators().getSpectator(viewer);
                Player t = s == null ? null : s.getFollowTarget();
                return t == null ? plugin.messages().raw(lang(ctx), "placeholders.none") : t.getName();
            }
            case "spectator_mode": {
                Spectator s = viewer == null ? null : plugin.spectators().getSpectator(viewer);
                return s == null ? plugin.messages().raw(lang(ctx), "placeholders.not-spectator") : s.getState().name();
            }
            // --- serveur / partie
            case "online_players":
                return String.valueOf(Bukkit.getOnlinePlayers().size());
            case "spectator_count":
                return String.valueOf(plugin.spectators().getSpectators().size());
            case "alive_players":
                return String.valueOf(plugin.game().getAlivePlayers().size());
            case "world_time": {
                World w = viewer != null ? viewer.getWorld() : Bukkit.getWorlds().get(0);
                long t = (w.getTime() + 6000) % 24000;
                return String.format(Locale.ROOT, "%02d:%02d", t / 1000, (t % 1000) * 60 / 1000);
            }
            case "server_tps":
                return Text.oneDecimal(plugin.tps().current());
            case "game_time":
                return Text.duration(plugin.game().getGameDuration());
            case "episode":
                return String.valueOf(plugin.game().getEpisode());
            case "prefix":
                return Text.color(plugin.messages().raw(lang(ctx), "prefix"));
            default:
                return null;
        }
    }
}
