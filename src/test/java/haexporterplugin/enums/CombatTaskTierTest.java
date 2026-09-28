package haexporterplugin.enums;

import org.junit.Test;

import static org.junit.Assert.*;

public class CombatTaskTierTest
{
	@Test
	public void testAllValues()
	{
		assertEquals(6, CombatTaskTier.values().length);
	}

	@Test
	public void testFromString()
	{
		assertEquals(CombatTaskTier.EASY, CombatTaskTier.fromString("easy"));
		assertEquals(CombatTaskTier.EASY, CombatTaskTier.fromString("Easy"));
		assertEquals(CombatTaskTier.GRANDMASTER, CombatTaskTier.fromString(" grandmaster "));
	}

	@Test
	public void testFromStringUnknown()
	{
		assertNull(CombatTaskTier.fromString(null));
		assertNull(CombatTaskTier.fromString("impossible"));
	}

	@Test
	public void testOrdering()
	{
		assertTrue(CombatTaskTier.EASY.compareTo(CombatTaskTier.MEDIUM) < 0);
		assertTrue(CombatTaskTier.GRANDMASTER.compareTo(CombatTaskTier.HARD) > 0);
	}

	@Test
	public void testToString()
	{
		assertEquals("Easy", CombatTaskTier.EASY.toString());
	}
}
