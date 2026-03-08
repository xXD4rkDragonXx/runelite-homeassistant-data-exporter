package haexporterplugin.events;

import org.junit.Test;

import static org.junit.Assert.*;

public class LevelEventTest
{
	@Test
	public void testConstructor()
	{
		LevelEvent event = new LevelEvent("Attack", 99);
		assertNotNull(event);
	}

	@Test
	public void testLowLevelEvent()
	{
		LevelEvent event = new LevelEvent("Hitpoints", 10);
		assertNotNull(event);
	}

	@Test
	public void testMaxLevelEvent()
	{
		LevelEvent event = new LevelEvent("Runecrafting", 99);
		assertNotNull(event);
	}
}
