package fr.spectatorplus.compat;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

/**
 * Enregistre des écouteurs pour des évènements Bukkit qui n'existent pas sur toutes les versions
 * (EntityResurrectEvent 1.11+, EntityPickupItemEvent 1.12+, ...). Les données sont lues par réflexion.
 */
public final class DynamicEvents {

    public interface Handler {
        void handle(Event event);
    }

    private static final Listener DUMMY = new Listener() {
    };

    private DynamicEvents() {
    }

    /** @return true si l'évènement existe et a été enregistré */
    @SuppressWarnings("unchecked")
    public static boolean register(Plugin plugin, String className, EventPriority priority, boolean ignoreCancelled,
                                   final Handler handler) {
        final Class<?> clazz = Reflect.getClass(className);
        if (clazz == null || !Event.class.isAssignableFrom(clazz)) return false;
        EventExecutor executor = new EventExecutor() {
            @Override
            public void execute(Listener listener, Event event) throws EventException {
                if (!clazz.isInstance(event)) return;
                try {
                    handler.handle(event);
                } catch (Throwable t) {
                    throw new EventException(t);
                }
            }
        };
        try {
            Bukkit.getPluginManager().registerEvent((Class<? extends Event>) clazz, DUMMY, priority, executor, plugin, ignoreCancelled);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
