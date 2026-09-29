package fr.spectatorplus.placeholder;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.core.event.GameEvent;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.PlatformWorld;
import fr.spectatorplus.spectator.SpectatorSession;
import fr.spectatorplus.util.Text;

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
 * et ceux fournis par la plateforme (PlaceholderAPI %...% sur Bukkit).
 */
public final class PlaceholderManager {

    /** Contexte de résolution : lecteur, évènement affiché et joueur principal (chacun peut être null). */
    public static final class Context {
        private final PlatformPlayer viewer;
        private final GameEvent event;
        private final UUID subject;

        public Context(PlatformPlayer viewer, GameEvent event, UUID subject) {
            this.viewer = viewer;
            this.event = event;
            this.subject = subject;
        }

        public PlatformPlayer getViewer() {
            return viewer;
        }

        public GameEvent getEvent() {
            return event;
        }

        public UUID getSubject() {
            return subject;
        }
    }

    /** Placeholder calculé par un plugin / mod externe. */
    public interface Resolver {
        /** @return la valeur, ou null si le placeholder ne s'applique pas dans ce contexte */
        String resolve(Context context);
    }

    private static final Pattern PATTERN = Pattern.compile("\\{([a-zA-Z0-9_.\\-]+)}");

    private final SpectatorCore plugin;
    private final Map<String, Resolver> external = new ConcurrentHashMap<>();

    public PlaceholderManager(SpectatorCore plugin) {
        this.plugin = plugin;
    }

    public void register(String key, Resolver resolver) {
        external.put(strip(key), resolver);
    }

    public void unregister(String key) {
        external.remove(strip(key));
    }

    public boolean isRegistered(String key) {
        return external.containsKey(strip(key));
    }

    public Set<String> getRegisteredKeys() {
        return Collections.unmodifiableSet(external.keySet());
    }

    private static String strip(String key) {
        String k = key.trim();
        if (k.startsWith("{") && k.endsWith("}")) k = k.substring(1, k.length() - 1);
        return k.toLowerCase(Locale.ROOT);
    }

    public String apply(String text, PlatformPlayer viewer, GameEvent event) {
        if (text == null || text.isEmpty()) return "";
        UUID subject = event == null ? null : event.getPrimaryPlayer();
        Context ctx = new Context(viewer, event, subject);
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
        if (result.indexOf('%') >= 0) result = plugin.platform().externalPlaceholders(result, subject, viewer);
        return result;
    }

    private String resolve(String rawKey, Context ctx) {
        String key = rawKey.toLowerCase(Locale.ROOT);
        GameEvent e = ctx.getEvent();
        if (e != null) {
            String v = e.get(key);
            // « @clé » : valeur traduite dans la langue du lecteur (ex : @placeholders.zone-enter)
            if (v != null && v.startsWith("@") && v.length() > 1) return plugin.messages().raw(lang(ctx), v.substring(1));
            if (v != null) return v;
        }
        Resolver ext = external.get(key);
        if (ext != null) {
            try {
                String v = ext.resolve(ctx);
                if (v != null) return v;
            } catch (Throwable t) {
                plugin.host().logger().log(Level.WARNING, "Erreur du placeholder externe {" + key + "}", t);
            }
        }
        return internal(key, ctx);
    }

    private String lang(Context ctx) {
        return ctx.getViewer() == null ? plugin.messages().defaultLang() : plugin.messages().lang(ctx.getViewer());
    }

    private String internal(String key, Context ctx) {
        GameEvent e = ctx.getEvent();
        PlatformPlayer viewer = ctx.getViewer();
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
                SpectatorSession s = viewer == null ? null : plugin.spectators().getSpectator(viewer);
                PlatformPlayer t = s == null ? null : s.getFollowTarget();
                return t == null ? plugin.messages().raw(lang(ctx), "placeholders.none") : t.getName();
            }
            case "spectator_mode": {
                SpectatorSession s = viewer == null ? null : plugin.spectators().getSpectator(viewer);
                return s == null ? plugin.messages().raw(lang(ctx), "placeholders.not-spectator") : s.getState().name();
            }
            // --- serveur / partie
            case "online_players":
                return String.valueOf(plugin.platform().getOnlinePlayers().size());
            case "spectator_count":
                return String.valueOf(plugin.spectators().getSpectators().size());
            case "alive_players":
                return String.valueOf(plugin.game().getAlivePlayers().size());
            case "world_time": {
                PlatformWorld w = viewer != null ? viewer.getWorld() : plugin.platform().getWorlds().get(0);
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
