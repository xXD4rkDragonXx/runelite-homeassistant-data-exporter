package haexporterplugin.enums;

import org.junit.Test;

import static org.junit.Assert.*;

public class PlayerLookupServiceTest
{
	@Test
	public void testToStringReturnsDisplayName()
	{
		assertEquals("None", PlayerLookupService.NONE.toString());
		assertEquals("OSRS HiScore", PlayerLookupService.OSRS_HISCORE.toString());
		assertEquals("Crystal Math Labs", PlayerLookupService.CRYSTAL_MATH_LABS.toString());
		assertEquals("Temple OSRS", PlayerLookupService.TEMPLE_OSRS.toString());
		assertEquals("Wise Old Man", PlayerLookupService.WISE_OLD_MAN.toString());
		assertEquals("RuneProfile", PlayerLookupService.RUNE_PROFILE.toString());
	}

	@Test
	public void testGetPlayerUrlOsrsHiscore()
	{
		String url = PlayerLookupService.OSRS_HISCORE.getPlayerUrl("TestPlayer");
		assertNotNull(url);
		assertTrue(url.contains("runescape.com"));
		assertTrue(url.contains("TestPlayer"));
	}

	@Test
	public void testGetPlayerUrlWiseOldMan()
	{
		String url = PlayerLookupService.WISE_OLD_MAN.getPlayerUrl("TestPlayer");
		assertNotNull(url);
		assertTrue(url.contains("wiseoldman.net"));
		assertTrue(url.contains("TestPlayer"));
	}

	@Test
	public void testGetPlayerUrlCrystalMathLabs()
	{
		String url = PlayerLookupService.CRYSTAL_MATH_LABS.getPlayerUrl("TestPlayer");
		assertNotNull(url);
		assertTrue(url.contains("crystalmathlabs.com"));
		assertTrue(url.contains("TestPlayer"));
	}

	@Test
	public void testGetPlayerUrlTempleOsrs()
	{
		String url = PlayerLookupService.TEMPLE_OSRS.getPlayerUrl("TestPlayer");
		assertNotNull(url);
		assertTrue(url.contains("templeosrs.com"));
		assertTrue(url.contains("TestPlayer"));
	}

	@Test
	public void testGetPlayerUrlRuneProfile()
	{
		String url = PlayerLookupService.RUNE_PROFILE.getPlayerUrl("TestPlayer");
		assertNotNull(url);
		assertTrue(url.contains("runeprofile.com"));
		assertTrue(url.contains("TestPlayer"));
	}

	@Test
	public void testGetPlayerUrlNoneReturnsNull()
	{
		assertNull(PlayerLookupService.NONE.getPlayerUrl("TestPlayer"));
	}

	@Test
	public void testPlayerNameEscaping()
	{
		String url = PlayerLookupService.WISE_OLD_MAN.getPlayerUrl("Test Player");
		assertNotNull(url);
		assertTrue(url.contains("Test%20Player"));
	}

	@Test
	public void testAllValuesCount()
	{
		assertEquals(6, PlayerLookupService.values().length);
	}
}
