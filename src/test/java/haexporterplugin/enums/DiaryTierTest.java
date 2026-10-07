package haexporterplugin.enums;

import org.junit.Test;

import static org.junit.Assert.*;

public class DiaryTierTest
{
	@Test
	public void testFromStringIgnoresCaseAndSpaces()
	{
		assertEquals(DiaryTier.EASY, DiaryTier.fromString("easy"));
		assertEquals(DiaryTier.EASY, DiaryTier.fromString("Easy"));
		assertEquals(DiaryTier.ELITE, DiaryTier.fromString(" elite "));
		assertNull(DiaryTier.fromString(null));
		assertNull(DiaryTier.fromString("impossible"));
	}

	// The minimum tier setting compares tiers, so they must be declared from easiest to hardest
	@Test
	public void testTiersAreOrderedByDifficulty()
	{
		assertArrayEquals(new DiaryTier[]{DiaryTier.EASY, DiaryTier.MEDIUM, DiaryTier.HARD, DiaryTier.ELITE}, DiaryTier.values());
	}
}
