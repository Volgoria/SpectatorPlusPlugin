package fr.spectatorplus.api.event;

import java.util.Locale;

/**
 * Type d'évènement (ex : {@code pvp.first_kill}, {@code uhc.episode.start}).
 * <pre>
 * SpectatorEventType type = SpectatorEventType.builder("uhc.role.reveal")
 *         .category(EventCategory.CUSTOM)
 *         .displayName("Révélation d'un rôle")
 *         .importance(Importance.IMPORTANT)
 *         .message("&amp;d{player} &amp;7est &amp;d{role}")
 *         .icon("NAME_TAG")
 *         .build();
 * SpectatorPlusProvider.get().getEventService().registerType(type);
 * </pre>
 */
public final class SpectatorEventType {

    private final String id;
    private final String category;
    private final String displayName;
    private final Importance defaultImportance;
    private final String defaultMessage;
    private final String icon;
    private final boolean nativeType;

    private SpectatorEventType(Builder b) {
        this.id = b.id;
        this.category = b.category;
        this.displayName = b.displayName == null ? b.id : b.displayName;
        this.defaultImportance = b.importance;
        this.defaultMessage = b.message;
        this.icon = b.icon;
        this.nativeType = b.nativeType;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Importance getDefaultImportance() {
        return defaultImportance;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public String getIcon() {
        return icon;
    }

    /** true pour les évènements fournis par Spectator Plus lui-même. */
    public boolean isNative() {
        return nativeType;
    }

    public static final class Builder {
        private final String id;
        private String category = EventCategory.CUSTOM;
        private String displayName;
        private Importance importance = Importance.NORMAL;
        private String message = "&7{event_name} &8» &f{player}";
        private String icon = "PAPER";
        private boolean nativeType;

        private Builder(String id) {
            this.id = id.toLowerCase(Locale.ROOT);
        }

        public Builder category(String category) {
            this.category = category.toLowerCase(Locale.ROOT);
            return this;
        }

        public Builder displayName(String name) {
            this.displayName = name;
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

        public Builder icon(String icon) {
            this.icon = icon;
            return this;
        }

        /** Réservé à Spectator Plus. */
        public Builder nativeType(boolean nativeType) {
            this.nativeType = nativeType;
            return this;
        }

        public SpectatorEventType build() {
            return new SpectatorEventType(this);
        }
    }
}
