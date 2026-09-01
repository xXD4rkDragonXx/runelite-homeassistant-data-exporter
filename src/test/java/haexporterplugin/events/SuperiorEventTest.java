package haexporterplugin.events;

import com.google.gson.Gson;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.*;

public class SuperiorEventTest
{
	private final Gson gson = new Gson();

	@Test
	public void testConstructor()
	{
		SuperiorEvent event = new SuperiorEvent("Chasm crawler", 7404, new WorldPoint(3200, 3200, 0));
		assertNotNull(event);
	}

	@Test
	public void testSerializesFields()
	{
		SuperiorEvent event = new SuperiorEvent("Screaming banshee", 7398, new WorldPoint(2700, 3300, 0));
		String json = gson.toJson(event);

		assertTrue(json.contains("\"name\":\"Screaming banshee\""));
		assertTrue(json.contains("\"npcId\":7398"));
		assertTrue(json.contains("\"location\""));
	}
}
