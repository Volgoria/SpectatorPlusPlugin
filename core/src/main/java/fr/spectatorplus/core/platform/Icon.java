package fr.spectatorplus.core.platform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Description d'un objet affiché dans un menu ou la barre d'inventaire, indépendante de la plateforme
 * (remplace ItemBuilder). La plateforme le transforme en objet natif.
 * <p>
 * Types de base :
 * <ul>
 *     <li>{@link #of(String)} : matériau au format « MODERNE|LEGACY:data » (ex « CLOCK|WATCH ») ;</li>
 *     <li>{@link #pane(String)} / {@link #wool(String)} : vitre / laine d'une couleur (BLACK, LIME...) ;</li>
 *     <li>{@link #head()} / {@link #skull(UUID, String)} : tête de joueur ;</li>
 *     <li>{@link #item(ItemRef)} : copie d'un objet réel.</li>
 * </ul>
 */
public final class Icon {

    public enum Kind {MATERIAL, PANE, WOOL, HEAD, SKULL, ITEM}

    private final Kind kind;
    private final String spec;
    private final ItemRef item;
    private final UUID owner;
    private final String ownerName;
    private final List<String> lore = new ArrayList<>();
    private String name;
    private int amount = 1;
    private boolean glow;

    private Icon(Kind kind, String spec, ItemRef item, UUID owner, String ownerName) {
        this.kind = kind;
        this.spec = spec;
        this.item = item;
        this.owner = owner;
        this.ownerName = ownerName;
    }

    public static Icon of(String spec) {
        return new Icon(Kind.MATERIAL, spec, null, null, null);
    }

    public static Icon pane(String color) {
        return new Icon(Kind.PANE, color, null, null, null);
    }

    public static Icon wool(String color) {
        return new Icon(Kind.WOOL, color, null, null, null);
    }

    public static Icon head() {
        return new Icon(Kind.HEAD, null, null, null, null);
    }

    public static Icon skull(UUID owner, String ownerName) {
        return new Icon(Kind.SKULL, null, null, owner, ownerName);
    }

    public static Icon skull(PlatformPlayer owner) {
        return skull(owner.getUniqueId(), owner.getName());
    }

    /** Objet réel ; null ou vide donne une case vide. */
    public static Icon item(ItemRef item) {
        return new Icon(Kind.ITEM, null, item, null, null);
    }

    public Icon name(String name) {
        this.name = name;
        return this;
    }

    public Icon lore(String... lines) {
        lore.addAll(Arrays.asList(lines));
        return this;
    }

    public Icon lore(List<String> lines) {
        if (lines != null) lore.addAll(lines);
        return this;
    }

    public Icon amount(int amount) {
        this.amount = Math.max(1, Math.min(64, amount));
        return this;
    }

    public Icon glow(boolean glow) {
        this.glow = glow;
        return this;
    }

    /** Pour garder l'écriture des menus : {@code Icon.of("BOOK").name(...).build()}. */
    public Icon build() {
        return this;
    }

    // ------------------------------------------------------------------ lecture (plateformes)

    public Kind getKind() {
        return kind;
    }

    public String getSpec() {
        return spec;
    }

    public ItemRef getItem() {
        return item;
    }

    public UUID getOwner() {
        return owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    /** Nom avec codes « & » (non traduits), ou null. */
    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return Collections.unmodifiableList(lore);
    }

    public int getAmount() {
        return amount;
    }

    public boolean isGlow() {
        return glow;
    }

    /** Case vide (objet réel absent). */
    public boolean isEmpty() {
        return kind == Kind.ITEM && (item == null || item.isEmpty());
    }
}
