package fr.spectatorplus.event;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.api.event.Importance;
import fr.spectatorplus.api.event.SpectatorEventType;
import fr.spectatorplus.compat.Sounds;
import fr.spectatorplus.core.event.GameEvent;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.Position;
import fr.spectatorplus.filter.Preferences;
import fr.spectatorplus.util.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Diffusion des évènements aux spectateurs (le registre des types est dans {@link EventRegistry}).
 */
public final class EventManager extends EventRegistry {

    private final SpectatorCore plugin;
    private final Deque<GameEvent> history = new ArrayDeque<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<UUID, long[]> rates = new HashMap<>();
    private int historySize = 500;

    public EventManager(SpectatorCore plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public void load() {
        super.load();
        cooldowns.clear();
        historySize = plugin.config().getInt("events.history-size", 500);
    }

    public GameEvent.Builder builder(String typeId) {
        SpectatorEventType type = getType(typeId);
        if (type == null) throw new IllegalArgumentException("Unknown event type: " + typeId);
        return GameEvent.builder(type);
    }

    // ------------------------------------------------------------------ diffusion

    public void fire(String typeId, PlatformPlayer player, Map<String, ?> data) {
        fire(builder(typeId).player(player).data(data).build());
    }

    public void fire(final GameEvent event) {
        if (!plugin.host().isPrimaryThread()) {
            plugin.host().runSync(new Runnable() {
                @Override
                public void run() {
                    fire(event);
                }
            });
            return;
        }
        String id = event.getTypeId();
        if (!isRegistered(id)) registerType(event.getType());
        if (!isEnabled(id)) return;

        Position loc = event.getLocation();
        if (loc != null && loc.getWorld() != null
                && plugin.config().getStringList("events.disabled-worlds").contains(loc.getWorld())) {
            return;
        }

        EventSettings s = settings(id);
        if (s.cooldown() > 0) {
            String key = id + "|" + event.getPrimaryPlayer();
            long now = System.currentTimeMillis();
            Long last = cooldowns.get(key);
            if (last != null && now - last < s.cooldown() * 1000L) return;
            cooldowns.put(key, now);
        }
        if (event.getImportance() == null) event.setImportance(importanceOf(id));

        if (!plugin.platform().api().gameEvent(event)) return;

        history.addLast(event);
        while (history.size() > historySize) history.removeFirst();

        for (PlatformPlayer viewer : recipients()) deliver(viewer, event);

        if (plugin.config().getBoolean("events.log-to-console", false)) {
            plugin.host().logger().info(Text.strip(format(null, event, false)));
        }
    }

    private Collection<PlatformPlayer> recipients() {
        Set<PlatformPlayer> res = new LinkedHashSet<>();
        for (PlatformPlayer p : plugin.platform().getOnlinePlayers()) {
            if (p.isFake()) continue;
            if (plugin.spectators().isSpectator(p)) {
                res.add(p);
            } else if (plugin.config().getBoolean("events.staff-receive", false)
                    && p.hasPermission("spectatorplus.events.receive")) {
                res.add(p);
            }
        }
        return res;
    }

    private void deliver(PlatformPlayer viewer, GameEvent event) {
        Preferences prefs = plugin.filters().get(viewer);
        boolean forced = plugin.filters().isForced(event.getTypeId());
        if (!forced && !prefs.chatEvents) return;
        if (!plugin.filters().canSee(viewer, event)) return;
        Importance importance = event.getImportance();

        if (!forced && importance.ordinal() < Importance.IMPORTANT.ordinal() && prefs.rateLimit > 0) {
            long now = System.currentTimeMillis();
            long[] r = rates.get(viewer.getUniqueId());
            if (r == null || now - r[0] > 1000) {
                r = new long[]{now, 0};
                rates.put(viewer.getUniqueId(), r);
            }
            if (++r[1] > prefs.rateLimit) return;
        }

        String message = format(viewer, event, prefs.showTime);
        UUID subject = event.getPrimaryPlayer();
        PlatformPlayer subjectPlayer = subject == null ? null : plugin.platform().getPlayer(subject);
        if (subjectPlayer != null && !subjectPlayer.equals(viewer) && plugin.config().getBoolean("events.clickable", true)) {
            String lang = plugin.messages().lang(viewer);
            String hover = plugin.messages().get(viewer, "events.hover", "player", subjectPlayer.getName(),
                    "event", Text.strip(Text.color(plugin.messages().eventName(lang, event.getType()))),
                    "priority", Text.strip(plugin.messages().get(viewer, "importance." + importance.name())));
            viewer.sendClickable(message, hover, "/spectatorplus tp " + subjectPlayer.getName());
        } else {
            viewer.sendMessage(message);
        }

        if (importance == Importance.CRITICAL && prefs.titles && plugin.config().getBoolean("events.critical-title", true)) {
            String lang = plugin.messages().lang(viewer);
            String title = plugin.placeholders().apply(plugin.messages().raw(lang, "events.critical-title"), viewer, event);
            String sub = plugin.placeholders().apply(rawMessage(event, lang), viewer, event);
            viewer.title(Text.color(title), Text.color(sub), 5, 40, 10);
        }
        if (prefs.sounds) {
            if (importance == Importance.CRITICAL) viewer.playSound(Sounds.IMPORTANT, 0.8f, 1.2f);
            else if (importance == Importance.IMPORTANT) viewer.playSound(Sounds.NOTIFY, 0.6f, 1.4f);
        }
    }

    /**
     * Message brut d'un évènement dans une langue :
     * message API &gt; message forcé dans events.yml &gt; fichier de langue &gt; message par défaut du type.
     */
    public String rawMessage(GameEvent event, String lang) {
        if (event.getMessage() != null) return event.getMessage();
        String m = settings(event.getTypeId()).message();
        if (m != null) return m;
        m = plugin.messages().eventMessage(lang, event.getTypeId());
        return m != null ? m : event.getType().getDefaultMessage();
    }

    /** Message final (couleurs + placeholders) tel que vu par un spectateur. */
    public String format(PlatformPlayer viewer, GameEvent event, boolean showTime) {
        Importance importance = event.getImportance() == null ? importanceOf(event.getTypeId()) : event.getImportance();
        String template = plugin.config().getString("events.format", "{prefix}{message}");
        String prefix = plugin.config().getString("events.priority-prefix." + importance.name(), "");
        String time = showTime ? plugin.config().getString("events.time-format", "&8[{time}] ")
                .replace("{time}", new SimpleDateFormat("HH:mm:ss").format(new Date(event.getTimestamp()))) : "";
        String text = template.replace("{prefix}", prefix).replace("{time}", time)
                .replace("{message}", rawMessage(event, viewer == null
                        ? plugin.messages().defaultLang() : plugin.messages().lang(viewer)));
        return Text.color(plugin.placeholders().apply(text, viewer, event));
    }

    public List<GameEvent> getHistory() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    public void clearHistory() {
        history.clear();
    }

    public void forget(UUID player) {
        rates.remove(player);
    }
}
