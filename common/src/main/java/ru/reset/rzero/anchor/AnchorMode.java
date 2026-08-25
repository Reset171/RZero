package ru.reset.rzero.anchor;

import java.util.Locale;

public enum AnchorMode {

    FIXED,

    MULTI,

    EVERYONE,

    ROTATING;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static AnchorMode byId(String id) {
        if (id == null) {
            return FIXED;
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        for (AnchorMode mode : values()) {
            if (mode.id().equals(normalized)) {
                return mode;
            }
        }
        return FIXED;
    }
}
