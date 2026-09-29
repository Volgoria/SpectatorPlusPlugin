package fr.spectatorplus.event;

import fr.spectatorplus.api.event.EventCategory;
import fr.spectatorplus.api.event.Importance;
import fr.spectatorplus.api.event.SpectatorEventType;
import fr.spectatorplus.core.CoreContext;
import fr.spectatorplus.util.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registre des catégories et types d'évènements + réglages de events.yml.
 * La diffusion aux spectateurs est faite par chaque plateforme (sous-classe).
 */
public class EventRegistry {

    protected final CoreContext core;
    private final Map<String, EventCategory> categories = new LinkedHashMap<>();
    private final Map<String, SpectatorEventType> types = new LinkedHashMap<>();
    private final Map<String, EventSettings> settings = new HashMap<>();

    public EventRegistry(CoreContext core) {
        this.core = core;
    }

    public void load() {
        settings.clear();
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
            boolean def = type == null || !type.isNative() || core.files().events().getBoolean("default-enabled", true);
            s = new EventSettings(core.files().events().getConfigurationSection(id), def);
            settings.put(id, s);
        }
        return s;
    }

    public boolean isEnabled(String typeId) {
        return settings(typeId).enabled() && !core.filters().isGloballyDisabled(typeId);
    }

    /** Importance effective d'un type (surcharge API > events.yml > défaut). */
    public Importance importanceOf(String typeId) {
        Importance override = core.filters().getImportanceOverride(typeId);
        if (override != null) return override;
        Importance cfg = settings(typeId).importance();
        if (cfg != null) return cfg;
        SpectatorEventType t = types.get(typeId);
        return t == null ? Importance.NORMAL : t.getDefaultImportance();
    }

    // ------------------------------------------------------------------ registre

    public EventCategory registerCategory(EventCategory category) {
        categories.put(category.getId(), category);
        return category;
    }

    public EventCategory getCategory(String id) {
        return id == null ? null : categories.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<EventCategory> getCategories() {
        return Collections.unmodifiableCollection(categories.values());
    }

    public SpectatorEventType registerType(SpectatorEventType type) {
        if (!categories.containsKey(type.getCategory())) {
            registerCategory(new EventCategory(type.getCategory(), "&f" + Text.pretty(type.getCategory()), "PAPER"));
        }
        types.put(type.getId(), type);
        settings.remove(type.getId());
        return type;
    }

    public void unregisterType(String id) {
        SpectatorEventType t = types.get(id);
        if (t != null && !t.isNative()) types.remove(id);
    }

    public boolean isRegistered(String id) {
        return types.containsKey(id);
    }

    public SpectatorEventType getType(String id) {
        return id == null ? null : types.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<SpectatorEventType> getTypes() {
        return Collections.unmodifiableCollection(types.values());
    }

    public List<SpectatorEventType> getTypes(String categoryId) {
        List<SpectatorEventType> res = new ArrayList<>();
        for (SpectatorEventType t : types.values()) if (t.getCategory().equals(categoryId)) res.add(t);
        return res;
    }
}
