package haexporterplugin.data;

import lombok.Getter;

public class HAConnection {
    @Getter
    public String baseUrl;
    @Getter
    public String token;

    // Per-connection toggles (Boolean wrappers for backwards-compatible deserialization:
    // old JSON without these fields -> null -> treated as enabled)
    private Boolean includeInventory;
    private Boolean includeEquipment;
    private Boolean includeLocation;

    public HAConnection(String baseUrl, String token)
    {
        this.baseUrl = baseUrl;
        this.token = token;
        this.includeInventory = true;
        this.includeEquipment = true;
        this.includeLocation = true;
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
}
