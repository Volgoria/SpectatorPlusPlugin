package fr.spectatorplus.api.event;

import fr.spectatorplus.core.event.GameEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Occurrence d'un évènement de partie (un kill, un diamant trouvé, un évènement personnalisé...).
 * <pre>
 * SpectatorPlusAPI api = SpectatorPlusProvider.get();
 * api.getEventService().fire(api.getEventService().builder("uhc.role.reveal")
 *         .player(player)
 *         .data("role", "Loup-Garou")
 *         .importance(Importance.IMPORTANT)
 *         .build());
 * </pre>
 */
public final class SpectatorGameEvent {

    private final GameEvent event;

    private SpectatorGameEvent(GameEvent event) {
        this.event = event;
    }

    /** Usage interne : vue API d'un évènement du code commun. */
    public static SpectatorGameEvent wrap(GameEvent event) {
        return event == null ? null : new SpectatorGameEvent(event);
    }

    /** Usage interne : évènement du code commun. */
    public GameEvent unwrap() {
        return event;
    }

    public static Builder builder(SpectatorEventType type) {
        return new Builder(type);
    }

    public SpectatorEventType getType() {
        return event.getType();
    }

    public String getTypeId() {
        return event.getTypeId();
    }

    public String getCategory() {
        return event.getCategory();
    }

    /** Joueurs concernés, le premier étant le joueur principal. */
    public List<UUID> getPlayers() {
        return event.getPlayers();
    }

    public UUID getPrimaryPlayer() {
        return event.getPrimaryPlayer();
    }

    public String getPlayerName(UUID id) {
        return event.getPlayerName(id);
    }

    /** Données personnalisées, utilisables comme placeholders : {clé}. */
    public Map<String, String> getData() {
        return event.getData();
    }

    public String get(String key) {
        return event.get(key);
    }

    /** Lieu de l'évènement, ou null (ou si son monde n'est pas chargé). */
    public Location getLocation() {
        fr.spectatorplus.core.platform.Position p = event.getLocation();
        if (p == null || p.getWorld() == null) return null;
        World w = Bukkit.getWorld(p.getWorld());
        return w == null ? null : new Location(w, p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
    }

    /** Date de déclenchement (ms epoch). */
    public long getTimestamp() {
        return event.getTimestamp();
    }

    public boolean hasDamage() {
        return event.hasDamage();
    }

    public double getDamage() {
        return event.getDamage();
    }

    public String getDamageType() {
        return event.getDamageType();
    }

    public boolean isPvp() {
        return event.isPvp();
    }

    public boolean isCritical() {
        return event.isCritical();
    }

    /** Importance explicite, ou null si elle doit être déterminée par la configuration. */
    public Importance getImportance() {
        return event.getImportance();
    }

    public void setImportance(Importance importance) {
        event.setImportance(importance);
    }

    /** Message spécifique à cette occurrence (remplace celui de la configuration), ou null. */
    public String getMessage() {
        return event.getMessage();
    }

    public void setMessage(String message) {
        event.setMessage(message);
    }

    public static final class Builder {
        private final GameEvent.Builder builder;
        private boolean hasLocation;
        private boolean hasPlayer;

        private Builder(SpectatorEventType type) {
            this.builder = GameEvent.builder(type);
        }

        /** Ajoute un joueur concerné. Le premier ajouté est le joueur principal ({player}). */
        public Builder player(Player player) {
            if (player == null) return this;
            boolean first = !hasPlayer;
            builder.player(player.getUniqueId(), player.getName());
            hasPlayer = true;
            // le lieu par défaut est celui du joueur principal
            if (first && !hasLocation) location(player.getLocation());
            return this;
        }

        public Builder player(UUID id, String name) {
            if (id != null) hasPlayer = true;
            builder.player(id, name);
            return this;
        }

        public Builder data(String key, Object value) {
            builder.data(key, value);
            return this;
        }

        public Builder data(Map<String, ?> values) {
            builder.data(values);
            return this;
        }

        public Builder location(Location location) {
            hasLocation = location != null;
            builder.location(location == null ? null : new fr.spectatorplus.core.platform.Position(
                    location.getWorld() == null ? null : location.getWorld().getName(),
                    location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch()));
            return this;
        }

        public Builder timestamp(long timestamp) {
            builder.timestamp(timestamp);
            return this;
        }

        public Builder damage(double damage, String damageType, boolean pvp, boolean critical) {
            builder.damage(damage, damageType, pvp, critical);
            return this;
        }

        public Builder importance(Importance importance) {
            builder.importance(importance);
            return this;
        }

        public Builder message(String message) {
            builder.message(message);
            return this;
        }

        public SpectatorGameEvent build() {
            return new SpectatorGameEvent(builder.build());
        }
    }
}
