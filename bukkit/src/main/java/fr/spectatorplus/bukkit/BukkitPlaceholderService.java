package fr.spectatorplus.bukkit;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.api.event.SpectatorGameEvent;
import fr.spectatorplus.api.placeholder.PlaceholderContext;
import fr.spectatorplus.api.placeholder.PlaceholderResolver;
import fr.spectatorplus.api.placeholder.PlaceholderService;
import fr.spectatorplus.placeholder.PlaceholderManager;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * API publique des placeholders (Bukkit) → {@link PlaceholderManager}.
 */
public final class BukkitPlaceholderService implements PlaceholderService {

    private final SpectatorCore core;
    private final BukkitPlatform platform;

    public BukkitPlaceholderService(SpectatorCore core, BukkitPlatform platform) {
        this.core = core;
        this.platform = platform;
    }

    @Override
    public void register(String key, final PlaceholderResolver resolver) {
        core.placeholders().register(key, new PlaceholderManager.Resolver() {
            @Override
            public String resolve(PlaceholderManager.Context c) {
                return resolver.resolve(new PlaceholderContext(BukkitPlayer.unwrap(c.getViewer()),
                        SpectatorGameEvent.wrap(c.getEvent()), c.getSubject()));
            }
        });
    }

    @Override
    public void unregister(String key) {
        core.placeholders().unregister(key);
    }

    @Override
    public boolean isRegistered(String key) {
        return core.placeholders().isRegistered(key);
    }

    @Override
    public Set<String> getRegisteredKeys() {
        return core.placeholders().getRegisteredKeys();
    }

    @Override
    public String apply(String text, Player viewer, SpectatorGameEvent event) {
        return core.placeholders().apply(text, platform.wrap(viewer), event == null ? null : event.unwrap());
    }
}
