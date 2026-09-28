package haexporterplugin.utils;

import net.runelite.api.WorldType;
import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.*;

public class WorldUtilsTest
{
	@Test
	public void testGetWorldTypeNamesEmpty()
	{
		assertEquals(0, WorldUtils.getWorldTypeNames(EnumSet.noneOf(WorldType.class)).length);
	}

	@Test
	public void testGetWorldTypeNamesInEnumOrder()
	{
		assertArrayEquals(new String[]{"MEMBERS", "SEASONAL"},
			WorldUtils.getWorldTypeNames(EnumSet.of(WorldType.SEASONAL, WorldType.MEMBERS)));
	}
}
