package haexporterplugin.data;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.coords.WorldPoint;

@Getter
public class Player {
    @Setter
    private String name;
    @Setter
    private String accountType;
    @Setter
    private String world;
    @Setter
    private WorldPoint location;
    private Stats stats;
    private Inventory inventory;
    private Equipment equipment;

    public Player() {
    }

    public void setStats(Object stats) {
        this.stats = (Stats) stats;
    }

    public void setInventory(Object inventory) {
        this.inventory = (Inventory) inventory;
    }

    public void setEquipment(Object equipment) {
        this.equipment = (Equipment) equipment;
    }
}
