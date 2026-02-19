package haexporterplugin.data;

public class Player {
    private String name;
    private String accountType;
    private String world;
    private int[] location;
    private Stats stats;
    private Inventory inventory;
    private Equipment equipment;

    public Player() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public int[] getLocation() {
        return location;
    }

    public void setLocation(int[] location) {
        this.location = location;
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

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Object equipment) {
        this.equipment = (Equipment) equipment;
    }
}
