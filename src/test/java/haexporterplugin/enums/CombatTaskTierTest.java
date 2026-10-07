package haexporterplugin.enums;

import org.junit.Test;

import static org.junit.Assert.*;

public class CombatTaskTierTest
{
	@Test
	public void testFromStringIgnoresCaseAndSpaces()
	{
		assertEquals(CombatTaskTier.EASY, CombatTaskTier.fromString("easy"));
		assertEquals(CombatTaskTier.EASY, CombatTaskTier.fromString("Easy"));
		assertEquals(CombatTaskTier.GRANDMASTER, CombatTaskTier.fromString(" grandmaster "));
		assertNull(CombatTaskTier.fromString(null));
		assertNull(CombatTaskTier.fromString("impossible"));
	}

	// The minimum tier setting compares tiers, so they must be declared from easiest to hardest
	@Test
	public void testTiersAreOrderedByDifficulty()
	{
		assertArrayEquals(new CombatTaskTier[]{CombatTaskTier.EASY, CombatTaskTier.MEDIUM, CombatTaskTier.HARD,
			CombatTaskTier.ELITE, CombatTaskTier.MASTER, CombatTaskTier.GRANDMASTER}, CombatTaskTier.values());
	}
}
