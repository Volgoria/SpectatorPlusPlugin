package fr.spectatorplus.api.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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

    private final SpectatorEventType type;
    private final List<UUID> players;
    private final Map<UUID, String> playerNames;
    private final Map<String, String> data;
    private final Location location;
    private final long timestamp;
    private final Double damage;
    private final String damageType;
    private final boolean pvp;
    private final boolean critical;
    private Importance importance;
    private String message;

    private SpectatorGameEvent(Builder b) {
        this.type = b.type;
        this.players = Collections.unmodifiableList(new ArrayList<>(b.players));
        this.playerNames = Collections.unmodifiableMap(new LinkedHashMap<>(b.names));
        this.data = new LinkedHashMap<>(b.data);
        this.location = b.location;
        this.timestamp = b.timestamp;
        this.damage = b.damage;
        this.damageType = b.damageType;
        this.pvp = b.pvp;
        this.critical = b.critical;
        this.importance = b.importance;
        this.message = b.message;
    }

    public static Builder builder(SpectatorEventType type) {
        return new Builder(type);
    }

    public SpectatorEventType getType() {
        return type;
    }

    public String getTypeId() {
        return type.getId();
    }

    public String getCategory() {
        return type.getCategory();
    }

    /** Joueurs concernés, le premier étant le joueur principal. */
    public List<UUID> getPlayers() {
        return players;
    }

    public UUID getPrimaryPlayer() {
        return players.isEmpty() ? null : players.get(0);
    }

    public String getPlayerName(UUID id) {
        return playerNames.get(id);
    }

    /** Données personnalisées, utilisables comme placeholders : {clé}. */
    public Map<String, String> getData() {
        return data;
    }

    public String get(String key) {
        return data.get(key);
    }

    public Location getLocation() {
        return location == null ? null : location.clone();
    }

    /** Date de déclenchement (ms epoch). */
    public long getTimestamp() {
        return timestamp;
    }

    public boolean hasDamage() {
        return damage != null;
    }

    public double getDamage() {
        return damage == null ? 0 : damage;
    }

    public String getDamageType() {
        return damageType;
    }

    public boolean isPvp() {
        return pvp;
    }

    public boolean isCritical() {
        return critical;
    }

    /** Importance explicite, ou null si elle doit être déterminée par la configuration. */
    public Importance getImportance() {
        return importance;
    }

    public void setImportance(Importance importance) {
        this.importance = importance;
    }

    /** Message spécifique à cette occurrence (remplace celui de la configuration), ou null. */
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static final class Builder {
        private final SpectatorEventType type;
        private final List<UUID> players = new ArrayList<>();
        private final Map<UUID, String> names = new LinkedHashMap<>();
        private final Map<String, String> data = new LinkedHashMap<>();
        private Location location;
        private long timestamp = System.currentTimeMillis();
        private Double damage;
        private String damageType;
        private boolean pvp;
        private boolean critical;
        private Importance importance;
        private String message;

        private Builder(SpectatorEventType type) {
            if (type == null) throw new IllegalArgumentException("type cannot be null");
            this.type = type;
        }

        /** Ajoute un joueur concerné. Le premier ajouté est le joueur principal ({player}). */
        public Builder player(Player player) {
            if (player == null) return this;
            if (!players.contains(player.getUniqueId())) {
                players.add(player.getUniqueId());
                names.put(player.getUniqueId(), player.getName());
            }
            if (players.size() == 1) {
                data.put("player", player.getName());
                if (location == null) location = player.getLocation();
            }
            return this;
        }

        public Builder player(UUID id, String name) {
            if (id == null) return this;
            if (!players.contains(id)) {
                players.add(id);
                names.put(id, name);
            }
            if (players.size() == 1) data.put("player", name);
            return this;
        }

        public Builder data(String key, Object value) {
            if (key != null && value != null) data.put(key, String.valueOf(value));
            return this;
        }

        public Builder data(Map<String, ?> values) {
            if (values != null) {
                for (Map.Entry<String, ?> e : values.entrySet()) data(e.getKey(), e.getValue());
            }
            return this;
        }

        public Builder location(Location location) {
            this.location = location == null ? null : location.clone();
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder damage(double damage, String damageType, boolean pvp, boolean critical) {
            this.damage = damage;
            this.damageType = damageType;
            this.pvp = pvp;
            this.critical = critical;
            return this;
        }

        public Builder importance(Importance importance) {
            this.importance = importance;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public SpectatorGameEvent build() {
            return new SpectatorGameEvent(this);
        }
    }
}
