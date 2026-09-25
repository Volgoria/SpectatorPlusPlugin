package fr.spectatorplus.api.event;

import java.util.Locale;

/**
 * Catégorie d'évènements (Monde, Minage, PvP, ...). Les plugins externes peuvent en ajouter.
 */
public final class EventCategory {

    public static final String WORLD = "world";
    public static final String MINING = "mining";
    public static final String PLAYER = "player";
    public static final String PVP = "pvp";
    public static final String DEATH = "death";
    public static final String CRAFT = "craft";
    public static final String ENCHANT = "enchant";
    public static final String PVE = "pve";
    public static final String POTION = "potion";
    public static final String GAME = "game";
    public static final String CUSTOM = "custom";

    private final String id;
    private final String displayName;
    private final String icon;

    /**
     * @param id          identifiant unique (minuscules)
     * @param displayName nom affiché (codes couleur &amp; acceptés)
     * @param icon        matériau de l'icône, syntaxe « MODERNE|LEGACY:data »
     */
    public EventCategory(String id, String displayName, String icon) {
        this.id = id.toLowerCase(Locale.ROOT);
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EventCategory && ((EventCategory) o).id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
