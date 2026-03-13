package haexporterplugin.data;

import lombok.Getter;
import lombok.Setter;

public class HAConnection {
    @Getter
    public String baseUrl;
    @Getter
    public String token;

    // Optional friendly name shown in the panel instead of the URL.
    @Getter
    @Setter
    private String friendlyName;

    // Whether the connection is enabled. Boolean wrapper for backwards-compatible deserialization:
    // old JSON without this field -> null -> treated as enabled.
    private Boolean enabled;

    // If the connection was auto-disabled (e.g. 401 Unauthorized), this stores the reason.
    @Getter
    @Setter
    private String disabledReason;

    // Per-connection data toggles. Boolean wrappers for backwards-compatible deserialization:
    // old JSON without these fields -> null -> treated as enabled.
    private Boolean includeInventory;
    private Boolean includeEquipment;
    private Boolean includeLocation;

    // Per-connection event toggles. Boolean wrappers for backwards-compatible deserialization:
    // old JSON without these fields -> null -> treated as enabled.
    private Boolean includeLootEvents;
    private Boolean includeDeathEvents;
    private Boolean includeLevelUpEvents;
    private Boolean includeAchievementDiaryEvents;
    private Boolean includeCombatTaskEvents;

    public HAConnection(String baseUrl, String token)
    {
        this.baseUrl = baseUrl;
        this.token = token;
        this.includeInventory = true;
        this.includeEquipment = true;
        this.includeLocation = true;
        this.includeLootEvents = true;
        this.includeDeathEvents = true;
        this.includeLevelUpEvents = true;
        this.includeAchievementDiaryEvents = true;
        this.includeCombatTaskEvents = true;
    }

    public String getDisplayName()
    {
        return friendlyName != null && !friendlyName.trim().isEmpty()
                ? friendlyName
                : baseUrl;
    }

    public boolean isEnabled()
    {
        return enabled == null || enabled;
    }

    public void setEnabled(boolean value)
    {
        this.enabled = value;
    }

    public boolean isIncludeInventory()
    {
        return includeInventory == null || includeInventory;
    }

    public boolean isIncludeEquipment()
    {
        return includeEquipment == null || includeEquipment;
    }

    public boolean isIncludeLocation()
    {
        return includeLocation == null || includeLocation;
    }

    public void setIncludeInventory(boolean value)
    {
        this.includeInventory = value;
    }

    public void setIncludeEquipment(boolean value)
    {
        this.includeEquipment = value;
    }

    public void setIncludeLocation(boolean value)
    {
        this.includeLocation = value;
    }

    public boolean isIncludeLootEvents()
    {
        return includeLootEvents == null || includeLootEvents;
    }

    public boolean isIncludeDeathEvents()
    {
        return includeDeathEvents == null || includeDeathEvents;
    }

    public boolean isIncludeLevelUpEvents()
    {
        return includeLevelUpEvents == null || includeLevelUpEvents;
    }

    public boolean isIncludeAchievementDiaryEvents()
    {
        return includeAchievementDiaryEvents == null || includeAchievementDiaryEvents;
    }

    public boolean isIncludeCombatTaskEvents()
    {
        return includeCombatTaskEvents == null || includeCombatTaskEvents;
    }

    public void setIncludeLootEvents(boolean value)
    {
        this.includeLootEvents = value;
    }

    public void setIncludeDeathEvents(boolean value)
    {
        this.includeDeathEvents = value;
    }

    public void setIncludeLevelUpEvents(boolean value)
    {
        this.includeLevelUpEvents = value;
    }

    public void setIncludeAchievementDiaryEvents(boolean value)
    {
        this.includeAchievementDiaryEvents = value;
    }

    public void setIncludeCombatTaskEvents(boolean value)
    {
        this.includeCombatTaskEvents = value;
    }
}
