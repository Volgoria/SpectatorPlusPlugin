package fr.spectatorplus.compat;

/**
 * Sons joués via leur nom (String) : évite l'enum {@code Sound} de Bukkit (devenu une interface en 1.21)
 * et sert aussi aux mods (identifiants vanilla modernes).
 */
public enum Sounds {

    CLICK("random.click", "ui.button.click", "ui.button.click"),
    TELEPORT("mob.endermen.portal", "entity.endermen.teleport", "entity.enderman.teleport"),
    NOTIFY("note.pling", "block.note.pling", "block.note_block.pling"),
    IMPORTANT("random.orb", "entity.experience_orb.pickup", "entity.experience_orb.pickup"),
    CRITICAL("mob.wither.spawn", "entity.wither.spawn", "entity.wither.spawn"),
    ERROR("note.bass", "block.note.bass", "block.note_block.bass");

    private final String legacy;
    private final String v19;
    private final String modern;

    Sounds(String legacy, String v19, String modern) {
        this.legacy = legacy;
        this.v19 = v19;
        this.modern = modern;
    }

    public String name(boolean atLeast19, boolean atLeast113) {
        if (atLeast113) return modern;
        if (atLeast19) return v19;
        return legacy;
    }
}
