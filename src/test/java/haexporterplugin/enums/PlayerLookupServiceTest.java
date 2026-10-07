package haexporterplugin.enums;

import org.junit.Test;

import static org.junit.Assert.*;

public class PlayerLookupServiceTest
{
	@Test
	public void testPlayerUrls()
	{
		// The name is escaped as a path segment
		assertEquals("https://wiseoldman.net/players/Test%20Player", PlayerLookupService.WISE_OLD_MAN.getPlayerUrl("Test Player"));
		assertEquals("https://secure.runescape.com/m=hiscore_oldschool/hiscorepersonal?user1=Test", PlayerLookupService.OSRS_HISCORE.getPlayerUrl("Test"));
		assertEquals("https://crystalmathlabs.com/track.php?player=Test", PlayerLookupService.CRYSTAL_MATH_LABS.getPlayerUrl("Test"));
		assertEquals("https://templeosrs.com/player/overview.php?player=Test", PlayerLookupService.TEMPLE_OSRS.getPlayerUrl("Test"));
		assertEquals("https://runeprofile.com/Test", PlayerLookupService.RUNE_PROFILE.getPlayerUrl("Test"));
		assertNull(PlayerLookupService.NONE.getPlayerUrl("Test"));
	}
}
