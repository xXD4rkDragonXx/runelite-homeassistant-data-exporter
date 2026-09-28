package haexporterplugin.enums;

import javax.annotation.Nullable;

public enum DiaryTier {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard"),
    ELITE("Elite"),
    ;

    private final String name;

    DiaryTier(String name) {
        this.name = name;
    }

    @Nullable
    public static DiaryTier fromString(@Nullable String tier) {
        if (tier == null) return null;
        for (DiaryTier value : values()) {
            if (value.name.equalsIgnoreCase(tier.trim())) return value;
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}
