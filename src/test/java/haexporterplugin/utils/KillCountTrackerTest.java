package haexporterplugin.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class KillCountTrackerTest
{
	private final KillCountTracker tracker = new KillCountTracker();

	@Test
	public void testKillCountArrivingJustAfterItsDrop()
	{
		tracker.onDrop(1000);
		tracker.onKillCount(250, 1001);

		assertEquals(Integer.valueOf(250), tracker.getKillCount(1002));
	}

	@Test
	public void testKillCountIsNotReusedByALaterDrop()
	{
		tracker.onKillCount(250, 1000);
		tracker.onDrop(1001); // the boss loot uses the kill count
		tracker.onDrop(1003); // e.g. a clue casket opened right after

		assertNull(tracker.getKillCount(1004));
	}

	@Test
	public void testDropTooLongAgoHasNoKillCount()
	{
		tracker.onKillCount(250, 1000);
		tracker.onDrop(1001);

		assertNull(tracker.getKillCount(1001 + KillCountTracker.MAX_DROP_AGE + 1));
	}

	@Test
	public void testKillCountTooFarFromDropIsNotAttached()
	{
		tracker.onKillCount(250, 1000);
		tracker.onDrop(1000 + KillCountTracker.MAX_KILL_COUNT_DROP_GAP + 1);

		assertNull(tracker.getKillCount(1010));
	}

	@Test
	public void testWithoutLootFallsBackToARecentKillCount()
	{
		// e.g. a chest whose loot event isn't available
		tracker.onKillCount(250, 1000);

		assertEquals(Integer.valueOf(250), tracker.getKillCount(1001));
		assertNull(tracker.getKillCount(1000 + KillCountTracker.MAX_KILL_COUNT_DROP_GAP + 1));
	}

	@Test
	public void testNegativeTickGapsAreIgnored()
	{
		tracker.onKillCount(250, 5000);
		tracker.onDrop(5001);

		// the tick counter can restart, e.g. after a relog
		assertNull(tracker.getKillCount(100));
	}

	@Test
	public void testReset()
	{
		tracker.onKillCount(250, 1000);
		tracker.onDrop(1001);

		tracker.reset();

		assertNull(tracker.getKillCount(1002));
	}
}
