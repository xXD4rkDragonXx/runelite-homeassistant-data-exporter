package haexporterplugin.events;

import com.google.gson.Gson;
import haexporterplugin.data.ItemData;
import haexporterplugin.enums.Danger;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import java.util.List;

import static haexporterplugin.TestUtils.assertJsonEquals;

// The event data as Home Assistant receives it
public class EventJsonTest
{
	private final Gson gson = new Gson();

	@Test
	public void testLevelEvent()
	{
		assertJsonEquals("{'skill':'Attack','level':99}", gson.toJson(new LevelEvent("Attack", 99)));
	}

	@Test
	public void testSuperiorEvent()
	{
		SuperiorEvent event = new SuperiorEvent("Screaming banshee", 7398, new WorldPoint(2700, 3300, 0));

		assertJsonEquals("{'name':'Screaming banshee','npcId':7398,'location':{'x':2700,'y':3300,'plane':0}}", gson.toJson(event));
	}

	// The value lost can be above Integer.MAX_VALUE
	@Test
	public void testDeathEvent()
	{
		DeathEvent event = new DeathEvent(3_000_000_000L, Danger.DANGEROUS, "TzTok-Jad", 3127,
			List.of(new ItemData("Abyssal whip", 4151, 1650000, 72000, 1)), List.of(), new WorldPoint(2440, 5172, 0));

		assertJsonEquals("{'valueLost':3000000000,'danger':'DANGEROUS','killerName':'TzTok-Jad','killerNpcId':3127,"
			+ "'keptItems':[{'name':'Abyssal whip','id':4151,'gePrice':1650000,'haPrice':72000,'quantity':1}],"
			+ "'lostItems':[],'location':{'x':2440,'y':5172,'plane':0}}", gson.toJson(event));
	}
}
