package haexporterplugin.data;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ActivePrayersTest
{
	@Test
	public void testDefaultConstructor()
	{
		ActivePrayers activePrayers = new ActivePrayers();
		assertNull(activePrayers.getPrayers());
	}

	@Test
	public void testParameterizedConstructor()
	{
		List<String> prayers = Arrays.asList("PROTECT_FROM_MELEE", "PIETY");

		ActivePrayers activePrayers = new ActivePrayers(prayers);

		assertNotNull(activePrayers.getPrayers());
		assertEquals(2, activePrayers.getPrayers().size());
		assertEquals("PIETY", activePrayers.getPrayers().get(1));
	}

	@Test
	public void testSetPrayers()
	{
		ActivePrayers activePrayers = new ActivePrayers();
		List<String> prayers = new ArrayList<>();
		prayers.add("RIGOUR");
		activePrayers.setPrayers(prayers);

		assertEquals(1, activePrayers.getPrayers().size());
	}

	@Test
	public void testNoActivePrayers()
	{
		ActivePrayers activePrayers = new ActivePrayers(new ArrayList<>());
		assertNotNull(activePrayers.getPrayers());
		assertTrue(activePrayers.getPrayers().isEmpty());
	}
}
