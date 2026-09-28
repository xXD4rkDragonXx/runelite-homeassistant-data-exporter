package haexporterplugin.data;

import com.google.gson.Gson;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.*;

public class PlayerTest
{
	private static final String ACCOUNT_HASH = "de731bc0f710567a6a0e852bbe79eb5fa8daf37d4140440a774591f0";

	@Test
	public void testDefaultConstructor()
	{
		Player player = new Player();
		assertNull(player.getName());
		assertNull(player.getAccountHash());
		assertNull(player.getAccountType());
		assertNull(player.getWorld());
		assertNull(player.getWorldTypes());
		assertNull(player.getLocation());
		assertNull(player.getHealth());
		assertNull(player.getPrayerPoints());
		assertNull(player.getSpellbook());
		assertNull(player.getStats());
		assertNull(player.getInventory());
		assertNull(player.getEquipment());
	}

	@Test
	public void testSetAndGetName()
	{
		Player player = new Player();
		player.setName("TestPlayer");
		assertEquals("TestPlayer", player.getName());
	}

	@Test
	public void testSetAndGetAccountType()
	{
		Player player = new Player();
		player.setAccountType("IRONMAN");
		assertEquals("IRONMAN", player.getAccountType());
	}

	@Test
	public void testSetAndGetWorld()
	{
		Player player = new Player();
		player.setWorld("302");
		assertEquals("302", player.getWorld());
	}

	@Test
	public void testSetHealth()
	{
		Player player = new Player();
		HealthData health = new HealthData(85, 99);
		player.setHealth(health);
		assertNotNull(player.getHealth());
	}

	@Test
	public void testSetPrayerPoints()
	{
		Player player = new Player();
		PrayerData prayer = new PrayerData(52, 70);
		player.setPrayerPoints(prayer);
		assertNotNull(player.getPrayerPoints());
	}

	@Test
	public void testSetSpellbook()
	{
		Player player = new Player();
		SpellbookData spellbook = new SpellbookData(0);
		player.setSpellbook(spellbook);
		assertNotNull(player.getSpellbook());
	}

	@Test
	public void testSetStatsWithObjectCast()
	{
		Player player = new Player();
		Stats stats = new Stats(new HashMap<>());
		player.setStats((Object) stats);
		assertNotNull(player.getStats());
	}

	@Test
	public void testSetInventoryWithObjectCast()
	{
		Player player = new Player();
		Inventory inventory = new Inventory(new ArrayList<>());
		player.setInventory((Object) inventory);
		assertNotNull(player.getInventory());
	}

	@Test
	public void testSetEquipmentWithObjectCast()
	{
		Player player = new Player();
		Equipment equipment = new Equipment(new ArrayList<>());
		player.setEquipment((Object) equipment);
		assertNotNull(player.getEquipment());
	}

	@Test
	public void testSetLocation()
	{
		Player player = new Player();
		PlayerLocation location = new PlayerLocation(3222, 3218, 0, false);
		player.setLocation(location);
		assertNotNull(player.getLocation());
	}

	@Test
	public void testSerializeAccountHashAndWorldTypes()
	{
		Player player = new Player();
		player.setAccountHash(ACCOUNT_HASH);
		player.setWorldTypes(Collections.singletonList("SEASONAL"));
		String json = new Gson().toJson(player);
		assertTrue(json.contains("\"accountHash\":\"" + ACCOUNT_HASH + "\""));
		assertTrue(json.contains("\"worldTypes\":[\"SEASONAL\"]"));
	}

	@Test
	public void testSerializeEmptyWorldTypes()
	{
		Player player = new Player();
		player.setWorldTypes(Collections.emptyList());
		String json = new Gson().toJson(player);
		assertTrue(json.contains("\"worldTypes\":[]"));
	}

	@Test
	public void testSerializeOmitsNullAccountHash()
	{
		Player player = new Player();
		player.setName("TestPlayer");
		player.setAccountHash(null);
		String json = new Gson().toJson(player);
		assertTrue(json.contains("\"name\":\"TestPlayer\""));
		assertFalse(json.contains("accountHash"));
	}
}
