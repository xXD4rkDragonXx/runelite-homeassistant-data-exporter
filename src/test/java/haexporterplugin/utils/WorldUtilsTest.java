package haexporterplugin.utils;

import net.runelite.api.WorldType;
import org.junit.Test;

import java.util.Arrays;
import java.util.EnumSet;

import static org.junit.Assert.*;

public class WorldUtilsTest
{
	@Test
	public void testGetWorldTypeNamesEmpty()
	{
		assertTrue(WorldUtils.getWorldTypeNames(EnumSet.noneOf(WorldType.class)).isEmpty());
	}

	@Test
	public void testGetWorldTypeNamesInEnumOrder()
	{
		assertEquals(Arrays.asList("MEMBERS", "SEASONAL"),
			WorldUtils.getWorldTypeNames(EnumSet.of(WorldType.SEASONAL, WorldType.MEMBERS)));
	}
}
