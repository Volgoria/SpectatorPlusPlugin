package fr.spectatorplus.core.platform;

/**
 * Type de clic dans un menu.
 */
public enum Click {
    LEFT,
    RIGHT,
    SHIFT_LEFT,
    SHIFT_RIGHT,
    MIDDLE,
    DROP,
    OTHER;

    public boolean isRightClick() {
        return this == RIGHT || this == SHIFT_RIGHT;
    }

    public boolean isLeftClick() {
        return this == LEFT || this == SHIFT_LEFT;
    }

    public boolean isShiftClick() {
        return this == SHIFT_LEFT || this == SHIFT_RIGHT;
    }
}
