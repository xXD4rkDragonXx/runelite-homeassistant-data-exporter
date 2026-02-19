package haexporterplugin.data;

import lombok.Setter;

public class Player {
    @Setter
    private String name;
    @Setter
    private String accountType;
    @Setter
    private String world;
    @Setter
    private int[] location;
    private Stats stats;
    private Inventory inventory;
    private Equipment equipment;

    public Player() {
    }

    public String getName() {
        return name;
    }

    public String getAccountType() {
        return accountType;
    }

    public String getWorld() {
        return world;
    }

    public int[] getLocation() {
        return location;
    }

    public Stats getStats() {
        return stats;
    }

    public void setStats(Object stats) {
        this.stats = (Stats) stats;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Object inventory) {
        this.inventory = (Inventory) inventory;
    }

    public void setInventoryDirect(Inventory inventory) {
        this.inventory = inventory;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Object equipment) {
        this.equipment = (Equipment) equipment;
    }

    public void setEquipmentDirect(Equipment equipment) {
        this.equipment = equipment;
    }
}
