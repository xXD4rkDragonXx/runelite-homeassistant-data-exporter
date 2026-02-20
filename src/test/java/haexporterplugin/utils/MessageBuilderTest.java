package haexporterplugin.utils;

import com.google.gson.Gson;
import haexporterplugin.data.*;
import net.runelite.api.coords.WorldPoint;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class MessageBuilderTest {

    private MessageBuilder messageBuilder;

    @Before
    public void setUp() throws Exception {
        messageBuilder = new MessageBuilder();
        // Inject Gson instance via reflection since @Inject won't be processed in tests
        Field gsonField = MessageBuilder.class.getDeclaredField("gson");
        gsonField.setAccessible(true);
        gsonField.set(messageBuilder, new Gson());
    }

    @Test
    public void testConstructorInitializesRoot() {
        assertNotNull(messageBuilder.getRoot());
    }

    @Test
    public void testGetPlayerCreatesPlayerIfNull() {
        assertNull(messageBuilder.getRoot().getPlayer());
        Player player = messageBuilder.getPlayer();
        assertNotNull(player);
        assertNotNull(messageBuilder.getRoot().getPlayer());
    }

    @Test
    public void testGetPlayerReturnsSameInstance() {
        Player first = messageBuilder.getPlayer();
        Player second = messageBuilder.getPlayer();
        assertSame(first, second);
    }

    @Test
    public void testSetDataName() {
        messageBuilder.setData("name", "Zezima");
        assertEquals("Zezima", messageBuilder.getPlayer().getName());
    }

    @Test
    public void testSetDataAccountType() {
        messageBuilder.setData("accounttype", "IRONMAN");
        assertEquals("IRONMAN", messageBuilder.getPlayer().getAccountType());
    }

    @Test
    public void testSetDataHealth() {
        HealthData health = new HealthData(75, 99);
        messageBuilder.setData("health", health);
        assertNotNull(messageBuilder.getPlayer().getHealth());
    }

    @Test
    public void testSetDataPrayer() {
        PrayerData prayer = new PrayerData(50, 70);
        messageBuilder.setData("prayer", prayer);
        assertNotNull(messageBuilder.getPlayer().getPrayerPoints());
    }

    @Test
    public void testSetDataSpellbook() {
        SpellbookData spellbook = new SpellbookData(1);
        messageBuilder.setData("spellbook", spellbook);
        assertNotNull(messageBuilder.getPlayer().getSpellbook());
    }

    @Test
    public void testSetDataWorld() {
        messageBuilder.setData("world", "302");
        assertEquals("302", messageBuilder.getPlayer().getWorld());
    }

    @Test
    public void testSetDataLocation() {
        WorldPoint location = new WorldPoint(3200, 3200, 0);
        messageBuilder.setData("location", location);
        assertEquals(location, messageBuilder.getPlayer().getLocation());
    }

    @Test
    public void testSetDataStats() {
        Map<String, SkillInfo> skills = new HashMap<>();
        skills.put("Attack", new SkillInfo(100000, 75));
        Stats stats = new Stats(skills);

        messageBuilder.setData("stats", stats);
        assertNotNull(messageBuilder.getPlayer().getStats());
        assertEquals(1, messageBuilder.getPlayer().getStats().getSkills().size());
    }

    @Test
    public void testSetDataInventory() {
        List<ItemData> items = new ArrayList<>();
        items.add(new ItemData("Shark", 385, 800, 0, 5));
        Inventory inventory = new Inventory(items);

        messageBuilder.setData("inventory", inventory);
        assertNotNull(messageBuilder.getPlayer().getInventory());
        assertEquals(1, messageBuilder.getPlayer().getInventory().getItems().size());
    }

    @Test
    public void testSetDataEquipment() {
        List<ItemData> items = new ArrayList<>();
        items.add(new ItemData("Abyssal whip", 4151, 2500000, 120001, 1));
        Equipment equipment = new Equipment(items);

        messageBuilder.setData("equipment", equipment);
        assertNotNull(messageBuilder.getPlayer().getEquipment());
        assertEquals(1, messageBuilder.getPlayer().getEquipment().getItems().size());
    }

    @Test
    public void testSetDataCaseInsensitive() {
        messageBuilder.setData("NAME", "TestPlayer");
        assertEquals("TestPlayer", messageBuilder.getPlayer().getName());

        messageBuilder.setData("Health", new HealthData(50, 99));
        assertNotNull(messageBuilder.getPlayer().getHealth());
    }

    @Test
    public void testAddEvent() {
        messageBuilder.addEvent("testEvent");
        assertEquals(1, messageBuilder.getRoot().getEvents().size());
    }

    @Test
    public void testResetEvents() {
        messageBuilder.addEvent("event1");
        messageBuilder.addEvent("event2");
        messageBuilder.resetEvents();
        assertTrue(messageBuilder.getRoot().getEvents().isEmpty());
    }

    @Test
    public void testResetData() {
        messageBuilder.setData("name", "OldPlayer");
        messageBuilder.addEvent("oldEvent");
        messageBuilder.resetData();

        assertNull(messageBuilder.getRoot().getPlayer());
        assertTrue(messageBuilder.getRoot().getEvents().isEmpty());
    }

    @Test
    public void testBuildProducesValidJson() {
        messageBuilder.setData("name", "TestPlayer");
        messageBuilder.setData("world", "302");

        String json = messageBuilder.build();
        assertNotNull(json);
        assertTrue(json.contains("\"name\":\"TestPlayer\""));
        assertTrue(json.contains("\"world\":\"302\""));
    }

    @Test
    public void testBuildWithEvents() {
        messageBuilder.setData("name", "TestPlayer");
        messageBuilder.addEvent("deathEvent");

        String json = messageBuilder.build();
        assertNotNull(json);
        assertTrue(json.contains("\"events\""));
        assertTrue(json.contains("deathEvent"));
    }

    @Test
    public void testBuildEmptyRoot() {
        String json = messageBuilder.build();
        assertNotNull(json);
        assertTrue(json.contains("\"events\":[]"));
    }

    @Test
    public void testBuildWithFullPlayerData() {
        messageBuilder.setData("name", "FullPlayer");
        messageBuilder.setData("accounttype", "NORMAL");
        messageBuilder.setData("world", "301");
        messageBuilder.setData("health", new HealthData(99, 99));
        messageBuilder.setData("prayer", new PrayerData(70, 70));
        messageBuilder.setData("spellbook", new SpellbookData(0));

        String json = messageBuilder.build();
        assertTrue(json.contains("\"name\":\"FullPlayer\""));
        assertTrue(json.contains("\"accountType\":\"NORMAL\""));
        assertTrue(json.contains("\"world\":\"301\""));
    }
}
