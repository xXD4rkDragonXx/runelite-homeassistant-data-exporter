package haexporterplugin.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import haexporterplugin.TestUtils;
import haexporterplugin.data.*;
import net.runelite.api.GameState;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static haexporterplugin.TestUtils.assertJsonEquals;
import static org.junit.Assert.*;

public class MessageBuilderTest
{
	private final Gson gson = new Gson();
	private final MessageBuilder messageBuilder = new MessageBuilder();

	@Before
	public void setUp() throws Exception
	{
		TestUtils.setField(messageBuilder, "gson", gson);
	}

	// The message as Home Assistant receives it
	@Test
	public void testBuildPutsEveryCategoryUnderItsOwnKey()
	{
		ItemData coins = new ItemData("Coins", 995, 1, 1, 100);
		coins.setInventorySlot(0);
		ItemData whip = new ItemData("Abyssal whip", 4151, 3_000_000_000L, 72000, 1);
		whip.setEquipmentSlot("WEAPON");

		messageBuilder.setData("NAME", "TestPlayer"); // categories ignore case
		messageBuilder.setData("accounthash", "abc");
		messageBuilder.setData("accounttype", "IRONMAN");
		messageBuilder.setData("world", "302");
		messageBuilder.setData("worldtypes", new String[]{"MEMBERS", "SEASONAL"});
		messageBuilder.setData("health", new HealthData(85, 99));
		messageBuilder.setData("prayer", new PrayerData(52, 70));
		messageBuilder.setData("spellbook", new SpellbookData(1));
		messageBuilder.setData("location", new PlayerLocation(3222, 3218, 1, true));
		messageBuilder.setData("stats", new Stats(Map.of("Attack", new SkillInfo(200000000, 99))));
		messageBuilder.setData("inventory", new Inventory(List.of(coins)));
		messageBuilder.setData("equipment", new Equipment(List.of(whip)));
		messageBuilder.setData("unknown", "ignored");
		messageBuilder.setState(GameState.LOGGED_IN);
		messageBuilder.setTickDelay(100);

		JsonObject built = gson.fromJson(messageBuilder.build(), JsonObject.class);
		built.remove("timestamp");
		assertJsonEquals("{'player':{'name':'TestPlayer','accountHash':'abc','accountType':'IRONMAN','world':'302',"
			+ "'worldTypes':['MEMBERS','SEASONAL'],'location':{'x':3222,'y':3218,'plane':1,'isOnBoat':true},'locationTrail':[],"
			+ "'health':{'current':85,'max':99},'prayerPoints':{'current':52,'max':70},'spellbook':{'id':1,'name':'ancient'},"
			+ "'stats':{'skills':{'Attack':{'xp':200000000,'level':99}}},"
			+ "'inventory':{'items':[{'name':'Coins','id':995,'gePrice':1,'haPrice':1,'quantity':100,'inventorySlot':0}]},"
			+ "'equipment':{'items':[{'name':'Abyssal whip','id':4151,'gePrice':3000000000,'haPrice':72000,'quantity':1,'equipmentSlot':'WEAPON'}]}},"
			+ "'events':[],'state':'LOGGED_IN','tickDelay':100}", built.toString());
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
	public void testResetData()
	{
		messageBuilder.setData("name", "TestPlayer");
		messageBuilder.addEvent("level", "data");
		messageBuilder.resetData();

		assertNull(messageBuilder.getRoot().getPlayer());
		assertTrue(messageBuilder.getRoot().getEvents().isEmpty());
	}

	@Test
	public void testAddEventWrapsTypeDataIdAndTimestamp()
	{
		long before = System.currentTimeMillis();
		messageBuilder.addEvent("achievementDiary", "some-data");
		long after = System.currentTimeMillis();

		JsonObject event = builtEvents().get(0).getAsJsonObject();
		assertEquals("achievementDiary", event.get("type").getAsString());
		assertEquals("some-data", event.get("data").getAsString());

		String eventId = event.get("eventId").getAsString();
		assertEquals(eventId, UUID.fromString(eventId).toString());

		long timestamp = event.get("timestamp").getAsLong();
		assertTrue(timestamp >= before && timestamp <= after);
	}

	@Test
	public void testEventIdsAreUnique()
	{
		int count = 1000;
		for (int i = 0; i < count; i++)
		{
			// identical events are legitimate (e.g. two diary tasks), so ids must still differ
			messageBuilder.addEvent("achievementDiary", "same-data");
		}

		Set<String> eventIds = new HashSet<>();
		for (JsonElement event : builtEvents())
		{
			eventIds.add(event.getAsJsonObject().get("eventId").getAsString());
		}
		assertEquals(count, eventIds.size());
	}

	@Test
	public void testBuildSetsRootTimestamp()
	{
		long before = System.currentTimeMillis();
		JsonObject root = gson.fromJson(messageBuilder.build(), JsonObject.class);
		long after = System.currentTimeMillis();

		long timestamp = root.get("timestamp").getAsLong();
		assertTrue(timestamp >= before && timestamp <= after);
	}

	private JsonArray builtEvents()
	{
		return gson.fromJson(messageBuilder.build(), JsonObject.class).getAsJsonArray("events");
	}
}
