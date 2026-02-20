package haexporterplugin.data;

import net.runelite.api.coords.WorldPoint;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class PlayerTest {

    private Player player;

    @Before
    public void setUp() {
        player = new Player();
    }

    @Test
    public void testDefaultConstructorFieldsAreNull() {
        assertNull(player.getName());
        assertNull(player.getAccountType());
        assertNull(player.getWorld());
        assertNull(player.getLocation());
        assertNull(player.getHealth());
        assertNull(player.getPrayerPoints());
        assertNull(player.getSpellbook());
        assertNull(player.getStats());
        assertNull(player.getInventory());
        assertNull(player.getEquipment());
    }

    @Test
    public void testSetAndGetName() {
        player.setName("Zezima");
        assertEquals("Zezima", player.getName());
    }

    @Test
    public void testSetAndGetAccountType() {
        player.setAccountType("IRONMAN");
        assertEquals("IRONMAN", player.getAccountType());
    }

    @Test
    public void testSetAndGetWorld() {
        player.setWorld("302");
        assertEquals("302", player.getWorld());
    }

    @Test
    public void testSetAndGetLocation() {
        WorldPoint location = new WorldPoint(3200, 3200, 0);
        player.setLocation(location);
        assertEquals(location, player.getLocation());
    }

    @Test
    public void testSetAndGetHealth() {
        HealthData health = new HealthData(75, 99);
        player.setHealth(health);
        assertNotNull(player.getHealth());
    }

    @Test
    public void testSetAndGetPrayerPoints() {
        PrayerData prayer = new PrayerData(50, 70);
        player.setPrayerPoints(prayer);
        assertNotNull(player.getPrayerPoints());
    }

    @Test
    public void testSetAndGetSpellbook() {
        SpellbookData spellbook = new SpellbookData(0);
        player.setSpellbook(spellbook);
        assertNotNull(player.getSpellbook());
    }

    @Test
    public void testSetStatsWithObjectCast() {
        Map<String, SkillInfo> skills = new HashMap<>();
        skills.put("Attack", new SkillInfo(100000, 75));
        Stats stats = new Stats(skills);

        player.setStats((Object) stats);
        assertNotNull(player.getStats());
        assertEquals(1, player.getStats().getSkills().size());
    }

    @Test(expected = ClassCastException.class)
    public void testSetStatsWithInvalidObjectThrows() {
        player.setStats((Object) "invalid");
    }

    @Test
    public void testSetInventoryWithObjectCast() {
        List<ItemData> items = new ArrayList<>();
        items.add(new ItemData("Rune scimitar", 1333, 15000, 10000, 1));
        Inventory inventory = new Inventory(items);

        player.setInventory((Object) inventory);
        assertNotNull(player.getInventory());
        assertEquals(1, player.getInventory().getItems().size());
    }

    @Test(expected = ClassCastException.class)
    public void testSetInventoryWithInvalidObjectThrows() {
        player.setInventory((Object) "invalid");
    }

    @Test
    public void testSetEquipmentWithObjectCast() {
        List<ItemData> items = new ArrayList<>();
        items.add(new ItemData("Dragon boots", 11840, 200000, 90000, 1));
        Equipment equipment = new Equipment(items);

        player.setEquipment((Object) equipment);
        assertNotNull(player.getEquipment());
        assertEquals(1, player.getEquipment().getItems().size());
    }

    @Test(expected = ClassCastException.class)
    public void testSetEquipmentWithInvalidObjectThrows() {
        player.setEquipment((Object) "invalid");
    }
}
