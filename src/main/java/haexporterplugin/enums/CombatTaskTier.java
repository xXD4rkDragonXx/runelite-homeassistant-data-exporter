package haexporterplugin.enums;

import javax.annotation.Nullable;

public enum CombatTaskTier {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard"),
    ELITE("Elite"),
    MASTER("Master"),
    GRANDMASTER("Grandmaster"),
    ;

    private final String name;

    CombatTaskTier(String name) {
        this.name = name;
    }

    @Nullable
    public static CombatTaskTier fromString(@Nullable String tier) {
        if (tier == null) return null;
        for (CombatTaskTier value : values()) {
            if (value.name.equalsIgnoreCase(tier.trim())) return value;
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}
