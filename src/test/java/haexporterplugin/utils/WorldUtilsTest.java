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

	@Test
	public void testSpecialWorlds()
	{
		WorldType[] special = {
			WorldType.SEASONAL, WorldType.DEADMAN, WorldType.TOURNAMENT_WORLD, WorldType.BETA_WORLD,
			WorldType.QUEST_SPEEDRUNNING, WorldType.NOSAVE_MODE, WorldType.PVP_ARENA
		};
		for (WorldType type : special)
		{
			assertTrue(type.name(), WorldUtils.isSpecialWorld(EnumSet.of(WorldType.MEMBERS, type)));
		}
	}

	@Test
	public void testNormalWorldsAreNotSpecial()
	{
		assertFalse(WorldUtils.isSpecialWorld(EnumSet.noneOf(WorldType.class)));
		assertFalse(WorldUtils.isSpecialWorld(EnumSet.of(WorldType.MEMBERS)));
		assertFalse(WorldUtils.isSpecialWorld(EnumSet.of(WorldType.MEMBERS, WorldType.PVP)));
		assertFalse(WorldUtils.isSpecialWorld(EnumSet.of(WorldType.MEMBERS, WorldType.SKILL_TOTAL)));
	}
}
