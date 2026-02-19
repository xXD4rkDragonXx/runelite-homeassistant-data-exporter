package haexporterplugin.data;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.coords.WorldPoint;

@Getter
@Setter
public class Player {
    private String name;
    private String accountType;
    private String world;
    private WorldPoint location;
    private HealthData health;
    private PrayerData prayer;
    private SpellbookData spellbook;
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
