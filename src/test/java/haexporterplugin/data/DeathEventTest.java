package haexporterplugin.data;

import haexporterplugin.enums.Danger;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class DeathEventTest
{
	@Test
	public void testConstructorWithAllFields()
	{
		List<ItemData> keptItems = Arrays.asList(
			new ItemData("Abyssal whip", 4151, 1650000, 72000, 1)
		);
		List<ItemData> lostItems = Arrays.asList(
			new ItemData("Coins", 995, 1, 1, 50000)
		);
		WorldPoint location = new WorldPoint(3222, 3218, 0);

		haexporterplugin.events.DeathEvent event = new haexporterplugin.events.DeathEvent(
			500000, Danger.DANGEROUS, "Jad", 3127, keptItems, lostItems, location
		);

		assertNotNull(event);
	}

	@Test
	public void testConstructorWithNullKiller()
	{
		haexporterplugin.events.DeathEvent event = new haexporterplugin.events.DeathEvent(
			0, Danger.SAFE, null, null,
			Collections.emptyList(), Collections.emptyList(),
			new WorldPoint(3222, 3218, 0)
		);

		assertNotNull(event);
	}

	@Test
	public void testSafeDeath()
	{
		haexporterplugin.events.DeathEvent event = new haexporterplugin.events.DeathEvent(
			0, Danger.SAFE, null, null,
			new ArrayList<>(), new ArrayList<>(),
			new WorldPoint(2440, 5172, 0)
		);

		assertNotNull(event);
	}

	@Test
	public void testExceptionalDeath()
	{
		haexporterplugin.events.DeathEvent event = new haexporterplugin.events.DeathEvent(
			100000, Danger.EXCEPTIONAL, "TzTok-Jad", 3127,
			new ArrayList<>(), new ArrayList<>(),
			new WorldPoint(2440, 5172, 0)
		);

		assertNotNull(event);
	}
}
