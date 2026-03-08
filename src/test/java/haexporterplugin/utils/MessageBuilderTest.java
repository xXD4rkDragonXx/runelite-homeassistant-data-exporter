package haexporterplugin.utils;

import com.google.gson.Gson;
import haexporterplugin.data.*;
import net.runelite.api.GameState;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.*;

public class MessageBuilderTest
{
	private MessageBuilder messageBuilder;

	@Before
	public void setUp() throws Exception
	{
		messageBuilder = new MessageBuilder();
		// Inject Gson via reflection since we're not using Guice in tests
		Field gsonField = MessageBuilder.class.getDeclaredField("gson");
		gsonField.setAccessible(true);
		gsonField.set(messageBuilder, new Gson());
	}

	@Test
	public void testRootInitialized()
	{
		assertNotNull(messageBuilder.getRoot());
	}

	@Test
	public void testGetPlayerCreatesPlayerIfNull()
	{
		Player player = messageBuilder.getPlayer();
		assertNotNull(player);
	}

	@Test
	public void testGetPlayerReturnsSameInstance()
	{
		Player first = messageBuilder.getPlayer();
		Player second = messageBuilder.getPlayer();
		assertSame(first, second);
	}

	@Test
	public void testSetDataName()
	{
		messageBuilder.setData("name", "TestPlayer");
		assertEquals("TestPlayer", messageBuilder.getPlayer().getName());
	}

	@Test
	public void testSetDataAccountType()
	{
		messageBuilder.setData("accounttype", "IRONMAN");
		assertEquals("IRONMAN", messageBuilder.getPlayer().getAccountType());
	}

	@Test
	public void testSetDataHealth()
	{
		HealthData health = new HealthData(85, 99);
		messageBuilder.setData("health", health);
		assertNotNull(messageBuilder.getPlayer().getHealth());
	}

	@Test
	public void testSetDataPrayer()
	{
		PrayerData prayer = new PrayerData(52, 70);
		messageBuilder.setData("prayer", prayer);
		assertNotNull(messageBuilder.getPlayer().getPrayerPoints());
	}

	@Test
	public void testSetDataSpellbook()
	{
		SpellbookData spellbook = new SpellbookData(0);
		messageBuilder.setData("spellbook", spellbook);
		assertNotNull(messageBuilder.getPlayer().getSpellbook());
	}

	@Test
	public void testSetDataWorld()
	{
		messageBuilder.setData("world", "302");
		assertEquals("302", messageBuilder.getPlayer().getWorld());
	}

	@Test
	public void testSetDataLocation()
	{
		PlayerLocation location = new PlayerLocation(3222, 3218, 0, false);
		messageBuilder.setData("location", location);
		assertNotNull(messageBuilder.getPlayer().getLocation());
	}

	@Test
	public void testSetDataStats()
	{
		Map<String, SkillInfo> skills = new HashMap<>();
		skills.put("Attack", new SkillInfo(200000000, 99));
		Stats stats = new Stats(skills);
		messageBuilder.setData("stats", stats);
		assertNotNull(messageBuilder.getPlayer().getStats());
	}

	@Test
	public void testSetDataInventory()
	{
		Inventory inventory = new Inventory(new ArrayList<>());
		messageBuilder.setData("inventory", inventory);
		assertNotNull(messageBuilder.getPlayer().getInventory());
	}

	@Test
	public void testSetDataEquipment()
	{
		Equipment equipment = new Equipment(new ArrayList<>());
		messageBuilder.setData("equipment", equipment);
		assertNotNull(messageBuilder.getPlayer().getEquipment());
	}

	@Test
	public void testSetDataCaseInsensitive()
	{
		messageBuilder.setData("NAME", "Player1");
		assertEquals("Player1", messageBuilder.getPlayer().getName());

		messageBuilder.setData("World", "500");
		assertEquals("500", messageBuilder.getPlayer().getWorld());
	}

	@Test
	public void testSetDataUnknownCategory()
	{
		// Should not throw, just log a warning
		messageBuilder.setData("nonexistent", "value");
	}

	@Test
	public void testAddEvent()
	{
		messageBuilder.addEvent("level", "some-data");
		assertEquals(1, messageBuilder.getRoot().getEvents().size());
	}

	@Test
	public void testAddMultipleEvents()
	{
		messageBuilder.addEvent("level", "data1");
		messageBuilder.addEvent("death", "data2");
		messageBuilder.addEvent("loot", "data3");
		assertEquals(3, messageBuilder.getRoot().getEvents().size());
	}

	@Test
	public void testResetEvents()
	{
		messageBuilder.addEvent("level", "data1");
		messageBuilder.addEvent("death", "data2");
		messageBuilder.resetEvents();
		assertTrue(messageBuilder.getRoot().getEvents().isEmpty());
	}

	@Test
	public void testSetState()
	{
		messageBuilder.setState(GameState.LOGGED_IN);
		assertEquals(GameState.LOGGED_IN, messageBuilder.getRoot().getState());
	}

	@Test
	public void testBuildReturnsJson()
	{
		messageBuilder.setData("name", "TestPlayer");
		messageBuilder.setState(GameState.LOGGED_IN);

		String json = messageBuilder.build();
		assertNotNull(json);
		assertTrue(json.contains("TestPlayer"));
		assertTrue(json.contains("LOGGED_IN"));
	}

	@Test
	public void testBuildIncludesEvents()
	{
		messageBuilder.addEvent("level", "attack-99");
		String json = messageBuilder.build();
		assertTrue(json.contains("level"));
	}

	@Test
	public void testResetData()
	{
		messageBuilder.setData("name", "TestPlayer");
		messageBuilder.addEvent("level", "data");
		messageBuilder.resetData();

		assertNull(messageBuilder.getRoot().getPlayer());
		assertTrue(messageBuilder.getRoot().getEvents().isEmpty());
	}

	@Test
	public void testSetTickDelay()
	{
		messageBuilder.setTickDelay(100);
		// No exception should be thrown
	}
}
