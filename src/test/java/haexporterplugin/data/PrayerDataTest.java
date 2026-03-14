package haexporterplugin.data;

import org.junit.Test;

import static org.junit.Assert.*;

public class PrayerDataTest
{
	@Test
	public void testConstructor()
	{
		PrayerData prayer = new PrayerData(52, 70);
		assertNotNull(prayer);
	}

	@Test
	public void testFullPrayer()
	{
		PrayerData prayer = new PrayerData(99, 99);
		assertNotNull(prayer);
	}

	@Test
	public void testZeroPrayer()
	{
		PrayerData prayer = new PrayerData(0, 70);
		assertNotNull(prayer);
	}
}
