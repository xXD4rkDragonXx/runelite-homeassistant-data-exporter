package haexporterplugin.utils;

import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.gameval.VarbitID;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PrayerUtilsTest
{
	private Client client;

	@Before
	public void setUp()
	{
		// Unstubbed varbits return 0: no prayers active, nothing unlocked
		client = mock(Client.class);
	}

	@Test
	public void testNoActivePrayers()
	{
		assertTrue(PrayerUtils.getActivePrayers(client).isEmpty());
	}

	@Test
	public void testActivePrayersInPrayerBookOrder()
	{
		activate(Prayer.PIETY);
		activate(Prayer.PROTECT_FROM_MELEE);

		assertEquals(List.of("PROTECT_FROM_MELEE", "PIETY"), PrayerUtils.getActivePrayers(client));
	}

	@Test
	public void testRuinousPowers()
	{
		activate(Prayer.RP_TRINITAS);

		assertEquals(List.of("RP_TRINITAS"), PrayerUtils.getActivePrayers(client));
	}

	@Test
	public void testEagleEyeWithoutDeadeyeUnlocked()
	{
		activate(Prayer.EAGLE_EYE);
		activate(Prayer.DEADEYE);

		assertEquals(List.of("EAGLE_EYE"), PrayerUtils.getActivePrayers(client));
	}

	@Test
	public void testDeadeyeReplacesEagleEyeWhenUnlocked()
	{
		when(client.getVarbitValue(VarbitID.PRAYER_DEADEYE_UNLOCKED)).thenReturn(1);
		activate(Prayer.EAGLE_EYE);
		activate(Prayer.DEADEYE);

		assertEquals(List.of("DEADEYE"), PrayerUtils.getActivePrayers(client));
	}

	@Test
	public void testMysticVigourReplacesMysticMightWhenUnlocked()
	{
		when(client.getVarbitValue(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED)).thenReturn(1);
		activate(Prayer.MYSTIC_MIGHT);
		activate(Prayer.MYSTIC_VIGOUR);

		assertEquals(List.of("MYSTIC_VIGOUR"), PrayerUtils.getActivePrayers(client));
	}

	@Test
	public void testUnlockedPrayersIgnoredInLastManStanding()
	{
		when(client.getVarbitValue(VarbitID.BR_INGAME)).thenReturn(1);
		when(client.getVarbitValue(VarbitID.PRAYER_DEADEYE_UNLOCKED)).thenReturn(1);
		when(client.getVarbitValue(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED)).thenReturn(1);
		activate(Prayer.EAGLE_EYE);
		activate(Prayer.DEADEYE);
		activate(Prayer.MYSTIC_MIGHT);
		activate(Prayer.MYSTIC_VIGOUR);

		assertEquals(List.of("EAGLE_EYE", "MYSTIC_MIGHT"), PrayerUtils.getActivePrayers(client));
	}

	@Test
	public void testPietyWithoutZealUnlocked()
	{
		activate(Prayer.PIETY);
		when(client.getVarbitValue(VarbitID.PRAYER_ZEAL)).thenReturn(1);

		assertEquals(List.of("PIETY"), PrayerUtils.getActivePrayers(client));
	}

	@Test
	public void testZealReplacesPietyWhenUnlocked()
	{
		when(client.getVarbitValue(VarbitID.PRAYER_ZEAL_UNLOCKED)).thenReturn(1);
		activate(Prayer.PIETY);
		when(client.getVarbitValue(VarbitID.PRAYER_ZEAL)).thenReturn(1);

		assertEquals(List.of("ZEAL"), PrayerUtils.getActivePrayers(client));
	}

	@Test
	public void testZealUsedInLastManStandingWithoutUnlock()
	{
		when(client.getVarbitValue(VarbitID.BR_INGAME)).thenReturn(1);
		activate(Prayer.PIETY);
		when(client.getVarbitValue(VarbitID.PRAYER_ZEAL)).thenReturn(1);

		assertEquals(List.of("ZEAL"), PrayerUtils.getActivePrayers(client));
	}

	private void activate(Prayer prayer)
	{
		when(client.getVarbitValue(prayer.getVarbit())).thenReturn(1);
	}
}
