package haexporterplugin.enums;

import org.junit.Test;

import static org.junit.Assert.*;

public class ExceptionalDeathTest
{
	@Test
	public void testAllDisplayNames()
	{
		assertEquals("Chambers of Xeric", ExceptionalDeath.COX.toString());
		assertEquals("Diary Resurrection", ExceptionalDeath.DIARY_RESURRECTION.toString());
		assertEquals("Fight Caves", ExceptionalDeath.FIGHT_CAVE.toString());
		assertEquals("Gauntlet", ExceptionalDeath.GAUNTLET.toString());
		assertEquals("Inferno", ExceptionalDeath.INFERNO.toString());
		assertEquals("Jad challenges", ExceptionalDeath.JAD_CHALLENGES.toString());
		assertEquals("Theatre of Blood", ExceptionalDeath.TOB.toString());
		assertEquals("Tombs of Amascut", ExceptionalDeath.TOA.toString());
	}

	@Test
	public void testAllValuesCount()
	{
		assertEquals(8, ExceptionalDeath.values().length);
	}

	@Test
	public void testValueOf()
	{
		assertEquals(ExceptionalDeath.COX, ExceptionalDeath.valueOf("COX"));
		assertEquals(ExceptionalDeath.INFERNO, ExceptionalDeath.valueOf("INFERNO"));
		assertEquals(ExceptionalDeath.TOA, ExceptionalDeath.valueOf("TOA"));
	}
}
