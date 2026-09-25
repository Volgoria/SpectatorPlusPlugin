package fr.spectatorplus.event;

import fr.spectatorplus.SpectatorPlus;
import fr.spectatorplus.api.event.EventCategory;
import fr.spectatorplus.api.event.EventService;
import fr.spectatorplus.api.event.Importance;
import fr.spectatorplus.api.event.SpectatorEventType;
import fr.spectatorplus.api.event.SpectatorGameEvent;
import fr.spectatorplus.api.events.SpectatorGameEventTriggerEvent;
import fr.spectatorplus.compat.Compat;
import fr.spectatorplus.compat.Sounds;
import fr.spectatorplus.filter.Preferences;
import fr.spectatorplus.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Registre des types d'évènements + diffusion aux spectateurs.
 */
public final class EventManager implements EventService {

    private final SpectatorPlus plugin;
    private final Map<String, EventCategory> categories = new LinkedHashMap<>();
    private final Map<String, SpectatorEventType> types = new LinkedHashMap<>();
    private final Map<String, EventSettings> settings = new HashMap<>();
    private final Deque<SpectatorGameEvent> history = new ArrayDeque<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<UUID, long[]> rates = new HashMap<>();
    private int historySize = 500;

    public EventManager(SpectatorPlus plugin) {
        this.plugin = plugin;
    }

    public void load() {
        settings.clear();
        cooldowns.clear();
        historySize = plugin.getConfig().getInt("events.history-size", 500);
        if (types.isEmpty()) {
            // les noms affichés des catégories viennent des fichiers de langue
            NativeEvents.register(this, null);
        }
    }

    // ------------------------------------------------------------------ réglages

    public EventSettings settings(String id) {
        EventSettings s = settings.get(id);
        if (s == null) {
            SpectatorEventType type = types.get(id);
            boolean def = type == null || !type.isNative() || plugin.files().events().getBoolean("default-enabled", true);
            s = new EventSettings(plugin.files().events().getConfigurationSection(id), def);
            settings.put(id, s);
        }
        return s;
    }

    @Override
    public boolean isEnabled(String typeId) {
        return settings(typeId).enabled() && !plugin.filters().isGloballyDisabled(typeId);
    }

    /** Importance effective d'un type (surcharge API > events.yml > défaut). */
    public Importance importanceOf(String typeId) {
        Importance override = plugin.filters().getImportanceOverride(typeId);
        if (override != null) return override;
        Importance cfg = settings(typeId).importance();
        if (cfg != null) return cfg;
        SpectatorEventType t = types.get(typeId);
        return t == null ? Importance.NORMAL : t.getDefaultImportance();
    }

    // ------------------------------------------------------------------ registre

    @Override
    public EventCategory registerCategory(EventCategory category) {
        categories.put(category.getId(), category);
        return category;
    }

    @Override
    public EventCategory getCategory(String id) {
        return id == null ? null : categories.get(id.toLowerCase(Locale.ROOT));
    }

    @Override
    public Collection<EventCategory> getCategories() {
        return Collections.unmodifiableCollection(categories.values());
    }

    @Override
    public SpectatorEventType registerType(SpectatorEventType type) {
        if (!categories.containsKey(type.getCategory())) {
            registerCategory(new EventCategory(type.getCategory(), "&f" + Text.pretty(type.getCategory()), "PAPER"));
        }
        types.put(type.getId(), type);
        settings.remove(type.getId());
        return type;
    }

    @Override
    public void unregisterType(String id) {
        SpectatorEventType t = types.get(id);
        if (t != null && !t.isNative()) types.remove(id);
    }

    @Override
    public SpectatorEventType getType(String id) {
        return id == null ? null : types.get(id.toLowerCase(Locale.ROOT));
    }

    @Override
    public Collection<SpectatorEventType> getTypes() {
        return Collections.unmodifiableCollection(types.values());
    }

    @Override
    public List<SpectatorEventType> getTypes(String categoryId) {
        List<SpectatorEventType> res = new ArrayList<>();
        for (SpectatorEventType t : types.values()) if (t.getCategory().equals(categoryId)) res.add(t);
        return res;
    }

    @Override
    public SpectatorGameEvent.Builder builder(String typeId) {
        SpectatorEventType type = getType(typeId);
        if (type == null) throw new IllegalArgumentException("Unknown event type: " + typeId);
        return SpectatorGameEvent.builder(type);
    }

    // ------------------------------------------------------------------ diffusion

    @Override
    public void fire(String typeId, Player player, Map<String, ?> data) {
        fire(builder(typeId).player(player).data(data).build());
    }

    @Override
    public void fire(final SpectatorGameEvent event) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    fire(event);
                }
            });
            return;
        }
        String id = event.getTypeId();
        if (!types.containsKey(id)) registerType(event.getType());
        if (!isEnabled(id)) return;

        Location loc = event.getLocation();
        if (loc != null && loc.getWorld() != null
                && plugin.getConfig().getStringList("events.disabled-worlds").contains(loc.getWorld().getName())) {
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

        SpectatorGameEventTriggerEvent trigger = new SpectatorGameEventTriggerEvent(event, false);
        Bukkit.getPluginManager().callEvent(trigger);
        if (trigger.isCancelled()) return;

        history.addLast(event);
        while (history.size() > historySize) history.removeFirst();

        for (Player viewer : recipients()) deliver(viewer, event);

        if (plugin.getConfig().getBoolean("events.log-to-console", false)) {
            plugin.getLogger().info(Text.strip(format(null, event, false)));
        }
    }

    private Collection<Player> recipients() {
        Set<Player> res = new LinkedHashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.spectators().isSpectator(p)) {
                res.add(p);
            } else if (plugin.getConfig().getBoolean("events.staff-receive", true)
                    && p.hasPermission("spectatorplus.events.receive")) {
                res.add(p);
            }
        }
        return res;
    }

    private void deliver(Player viewer, SpectatorGameEvent event) {
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
        Player subjectPlayer = subject == null ? null : Bukkit.getPlayer(subject);
        if (subjectPlayer != null && subjectPlayer != viewer && plugin.getConfig().getBoolean("events.clickable", true)) {
            String lang = plugin.messages().lang(viewer);
            String hover = plugin.messages().get(viewer, "events.hover", "player", subjectPlayer.getName(),
                    "event", Text.strip(Text.color(plugin.messages().eventName(lang, event.getType()))),
                    "priority", Text.strip(plugin.messages().get(viewer, "importance." + importance.name())));
            Compat.clickable(viewer, message, hover, "/spectatorplus tp " + subjectPlayer.getName());
        } else {
            viewer.sendMessage(message);
        }

        if (importance == Importance.CRITICAL && prefs.titles && plugin.getConfig().getBoolean("events.critical-title", true)) {
            String lang = plugin.messages().lang(viewer);
            String title = plugin.placeholders().apply(plugin.messages().raw(lang, "events.critical-title"), viewer, event);
            String sub = plugin.placeholders().apply(rawMessage(event, lang), viewer, event);
            Compat.title(viewer, Text.color(title), Text.color(sub), 5, 40, 10);
        }
        if (prefs.sounds) {
            if (importance == Importance.CRITICAL) Compat.playSound(viewer, Sounds.IMPORTANT, 0.8f, 1.2f);
            else if (importance == Importance.IMPORTANT) Compat.playSound(viewer, Sounds.NOTIFY, 0.6f, 1.4f);
        }
    }

    /**
     * Message brut d'un évènement dans une langue :
     * message API &gt; message forcé dans events.yml &gt; fichier de langue &gt; message par défaut du type.
     */
    public String rawMessage(SpectatorGameEvent event, String lang) {
        if (event.getMessage() != null) return event.getMessage();
        String m = settings(event.getTypeId()).message();
        if (m != null) return m;
        m = plugin.messages().eventMessage(lang, event.getTypeId());
        return m != null ? m : event.getType().getDefaultMessage();
    }

    /** Message final (couleurs + placeholders) tel que vu par un spectateur. */
    public String format(Player viewer, SpectatorGameEvent event, boolean showTime) {
        Importance importance = event.getImportance() == null ? importanceOf(event.getTypeId()) : event.getImportance();
        String template = plugin.getConfig().getString("events.format", "{prefix}{message}");
        String prefix = plugin.getConfig().getString("events.priority-prefix." + importance.name(), "");
        String time = showTime ? plugin.getConfig().getString("events.time-format", "&8[{time}] ")
                .replace("{time}", new SimpleDateFormat("HH:mm:ss").format(new Date(event.getTimestamp()))) : "";
        String text = template.replace("{prefix}", prefix).replace("{time}", time)
                .replace("{message}", rawMessage(event, viewer == null
                        ? plugin.messages().defaultLang() : plugin.messages().lang(viewer)));
        return Text.color(plugin.placeholders().apply(text, viewer, event));
    }

    @Override
    public List<SpectatorGameEvent> getHistory() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    @Override
    public void clearHistory() {
        history.clear();
    }

    public void forget(UUID player) {
        rates.remove(player);
    }
}
