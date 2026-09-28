package haexporterplugin.enums;

import org.junit.Test;

import static org.junit.Assert.*;

public class DiaryTierTest
{
	@Test
	public void testAllValues()
	{
		assertEquals(4, DiaryTier.values().length);
	}

	@Test
	public void testFromString()
	{
		assertEquals(DiaryTier.EASY, DiaryTier.fromString("easy"));
		assertEquals(DiaryTier.EASY, DiaryTier.fromString("Easy"));
		assertEquals(DiaryTier.ELITE, DiaryTier.fromString(" elite "));
	}

	@Test
	public void testFromStringUnknown()
	{
		assertNull(DiaryTier.fromString(null));
		assertNull(DiaryTier.fromString("impossible"));
	}

	@Test
	public void testOrdering()
	{
		assertTrue(DiaryTier.EASY.compareTo(DiaryTier.MEDIUM) < 0);
		assertTrue(DiaryTier.ELITE.compareTo(DiaryTier.HARD) > 0);
	}

	@Test
	public void testToString()
	{
		assertEquals("Easy", DiaryTier.EASY.toString());
	}
}
