package fr.spectatorplus.bukkit;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.api.event.EventCategory;
import fr.spectatorplus.api.event.EventService;
import fr.spectatorplus.api.event.SpectatorEventType;
import fr.spectatorplus.api.event.SpectatorGameEvent;
import fr.spectatorplus.core.event.GameEvent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * API publique des évènements (Bukkit) → {@link fr.spectatorplus.event.EventManager}.
 */
public final class BukkitEventService implements EventService {

    private final SpectatorCore core;

    public BukkitEventService(SpectatorCore core) {
        this.core = core;
    }

    @Override
    public EventCategory registerCategory(EventCategory category) {
        return core.events().registerCategory(category);
    }

    @Override
    public EventCategory getCategory(String id) {
        return core.events().getCategory(id);
    }

    @Override
    public Collection<EventCategory> getCategories() {
        return core.events().getCategories();
    }

    @Override
    public SpectatorEventType registerType(SpectatorEventType type) {
        return core.events().registerType(type);
    }

    @Override
    public void unregisterType(String id) {
        core.events().unregisterType(id);
    }

    @Override
    public SpectatorEventType getType(String id) {
        return core.events().getType(id);
    }

    @Override
    public Collection<SpectatorEventType> getTypes() {
        return core.events().getTypes();
    }

    @Override
    public List<SpectatorEventType> getTypes(String categoryId) {
        return core.events().getTypes(categoryId);
    }

    @Override
    public SpectatorGameEvent.Builder builder(String typeId) {
        SpectatorEventType type = getType(typeId);
        if (type == null) throw new IllegalArgumentException("Unknown event type: " + typeId);
        return SpectatorGameEvent.builder(type);
    }

    @Override
    public void fire(SpectatorGameEvent event) {
        core.events().fire(event.unwrap());
    }

    @Override
    public void fire(String typeId, Player player, Map<String, ?> data) {
        fire(builder(typeId).player(player).data(data).build());
    }

    @Override
    public List<SpectatorGameEvent> getHistory() {
        List<SpectatorGameEvent> res = new ArrayList<>();
        for (GameEvent e : core.events().getHistory()) res.add(SpectatorGameEvent.wrap(e));
        return Collections.unmodifiableList(res);
    }

    @Override
    public void clearHistory() {
        core.events().clearHistory();
    }

    @Override
    public boolean isEnabled(String typeId) {
        return core.events().isEnabled(typeId);
    }
}
