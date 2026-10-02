package haexporterplugin.data;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Player {
    private String name;
    private String accountHash;
    private String accountType;
    private String world;
    private String[] worldTypes;
    private PlayerLocation location;
    // Tiles visited since the previous message, oldest first
    private List<TrailPoint> locationTrail = new ArrayList<>();
    private HealthData health;
    private PrayerData prayerPoints;
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
