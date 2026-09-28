package haexporterplugin.utils;

import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.*;

public class ConnectionBackoffTest
{
	private static final String KEY = "http://ha.local:8123\ntoken";
	private static final long SECOND = 1000L;
	private static final long MINUTE = 60 * SECOND;

	private long now = 1_700_000_000_000L;
	private final ConnectionBackoff backoff = new ConnectionBackoff(() -> now);

	@Test
	public void testUnknownKeyIsNotPaused()
	{
		assertFalse(backoff.isPaused(KEY));
		assertEquals(0, backoff.getPausedUntil(KEY));
		assertEquals(0, backoff.getQueuedCount(KEY));
		assertNull(backoff.beginDrain(KEY));
	}

	@Test
	public void testFailureBackoffDoublesUpToTenMinutes()
	{
		long[] expectedSeconds = {30, 60, 120, 240, 480, 600, 600};

		for (long seconds : expectedSeconds)
		{
			backoff.recordFailure(KEY);
			assertTrue(backoff.isPaused(KEY));
			assertEquals(now + seconds * SECOND, backoff.getPausedUntil(KEY));

			now = backoff.getPausedUntil(KEY);
			assertFalse(backoff.isPaused(KEY));
		}
	}

	@Test
	public void testFirstFailureIsReportedOnce()
	{
		assertTrue(backoff.recordFailure(KEY));

		now += 30 * SECOND;
		assertFalse(backoff.recordFailure(KEY));

		backoff.recordSuccess(KEY);
		assertTrue(backoff.recordFailure(KEY));
	}

	@Test
	public void testFailureWhilePausedDoesNotEscalate()
	{
		backoff.recordFailure(KEY);
		long pausedUntil = backoff.getPausedUntil(KEY);

		now += 10 * SECOND;
		assertFalse(backoff.recordFailure(KEY));
		assertEquals(pausedUntil, backoff.getPausedUntil(KEY));

		now = pausedUntil;
		backoff.recordFailure(KEY);
		assertEquals(now + 60 * SECOND, backoff.getPausedUntil(KEY));
	}

	@Test
	public void testSuccessResetsBackoff()
	{
		backoff.recordFailure(KEY);
		now += 30 * SECOND;
		backoff.recordFailure(KEY);
		assertTrue(backoff.isPaused(KEY));

		backoff.recordSuccess(KEY);
		assertFalse(backoff.isPaused(KEY));
		assertEquals(0, backoff.getPausedUntil(KEY));

		backoff.recordFailure(KEY);
		assertEquals(now + 30 * SECOND, backoff.getPausedUntil(KEY));
	}

	@Test
	public void testRetryAfterPausesUntilGivenTimeAndExpires()
	{
		long until = now + 45 * SECOND;
		assertTrue(backoff.recordRetryAfter(KEY, until));
		assertTrue(backoff.isPaused(KEY));
		assertEquals(until, backoff.getPausedUntil(KEY));

		now = until - 1;
		assertTrue(backoff.isPaused(KEY));

		now = until;
		assertFalse(backoff.isPaused(KEY));
		assertEquals(0, backoff.getPausedUntil(KEY));
	}

	@Test
	public void testRetryAfterDoesNotAdvanceExponentialBackoff()
	{
		backoff.recordRetryAfter(KEY, now + 5 * MINUTE);
		now += 5 * MINUTE;

		backoff.recordFailure(KEY);
		assertEquals(now + 30 * SECOND, backoff.getPausedUntil(KEY));
	}

	@Test
	public void testRetryAfterIsKeptWithinBounds()
	{
		backoff.recordRetryAfter(KEY, now + 60 * MINUTE);
		assertEquals(now + 10 * MINUTE, backoff.getPausedUntil(KEY));

		backoff.clear(KEY);
		backoff.recordRetryAfter(KEY, now - MINUTE);
		assertEquals(now + SECOND, backoff.getPausedUntil(KEY));
	}

	@Test
	public void testParseRetryAfterDeltaSeconds()
	{
		assertEquals(Long.valueOf(now + 120 * SECOND), ConnectionBackoff.parseRetryAfter("120", now));
		assertEquals(Long.valueOf(now + 5 * SECOND), ConnectionBackoff.parseRetryAfter(" 5 ", now));
		assertEquals(Long.valueOf(now), ConnectionBackoff.parseRetryAfter("0", now));
	}

	@Test
	public void testParseRetryAfterHttpDate()
	{
		long expected = Instant.parse("2015-10-21T07:28:00Z").toEpochMilli();
		assertEquals(Long.valueOf(expected), ConnectionBackoff.parseRetryAfter("Wed, 21 Oct 2015 07:28:00 GMT", now));
	}

	@Test
	public void testParseRetryAfterInvalidValues()
	{
		assertNull(ConnectionBackoff.parseRetryAfter(null, now));
		assertNull(ConnectionBackoff.parseRetryAfter("", now));
		assertNull(ConnectionBackoff.parseRetryAfter("   ", now));
		assertNull(ConnectionBackoff.parseRetryAfter("soon", now));
		assertNull(ConnectionBackoff.parseRetryAfter("-5", now));
		assertNull(ConnectionBackoff.parseRetryAfter("1.5", now));
		assertNull(ConnectionBackoff.parseRetryAfter("99999999999999999999", now));
		assertNull(ConnectionBackoff.parseRetryAfter("2015-10-21T07:28:00Z", now));
	}

	@Test
	public void testQueueDropsOldestBeyondLimit()
	{
		for (int i = 1; i <= ConnectionBackoff.MAX_QUEUED_PAYLOADS + 1; i++)
		{
			backoff.enqueue(KEY, "payload-" + i);
		}

		assertEquals(ConnectionBackoff.MAX_QUEUED_PAYLOADS, backoff.getQueuedCount(KEY));
		assertEquals("payload-2", backoff.beginDrain(KEY));
	}

	@Test
	public void testQueueDropsEntriesOlderThanTenMinutes()
	{
		backoff.enqueue(KEY, "old");
		now += 5 * MINUTE;
		backoff.enqueue(KEY, "new");

		now += 5 * MINUTE;
		assertEquals(2, backoff.getQueuedCount(KEY));

		now += 1;
		assertEquals(1, backoff.getQueuedCount(KEY));
		assertEquals("new", backoff.beginDrain(KEY));
	}

	@Test
	public void testDrainIsFifo()
	{
		backoff.enqueue(KEY, "a");
		backoff.enqueue(KEY, "b");
		backoff.enqueue(KEY, "c");

		for (String expected : new String[]{"a", "b", "c"})
		{
			assertEquals(expected, backoff.beginDrain(KEY));
			backoff.completeDrain(KEY, true);
		}

		assertEquals(0, backoff.getQueuedCount(KEY));
		assertNull(backoff.beginDrain(KEY));
	}

	@Test
	public void testBeginDrainReturnsNullWhilePaused()
	{
		backoff.enqueue(KEY, "a");
		backoff.recordFailure(KEY);

		assertNull(backoff.beginDrain(KEY));

		now = backoff.getPausedUntil(KEY);
		assertEquals("a", backoff.beginDrain(KEY));
	}

	@Test
	public void testBeginDrainReturnsNullWhileInFlight()
	{
		backoff.enqueue(KEY, "a");
		backoff.enqueue(KEY, "b");

		assertEquals("a", backoff.beginDrain(KEY));
		assertNull(backoff.beginDrain(KEY));

		backoff.completeDrain(KEY, true);
		assertEquals("b", backoff.beginDrain(KEY));
	}

	@Test
	public void testFailedDrainKeepsHead()
	{
		backoff.enqueue(KEY, "a");
		backoff.enqueue(KEY, "b");

		assertEquals("a", backoff.beginDrain(KEY));
		backoff.completeDrain(KEY, false);

		assertEquals(2, backoff.getQueuedCount(KEY));
		assertEquals("a", backoff.beginDrain(KEY));
	}

	@Test
	public void testDeliveredDrainDoesNotRemoveNewerEntryWhenHeadWasPushedOut()
	{
		backoff.enqueue(KEY, "payload-0");
		assertEquals("payload-0", backoff.beginDrain(KEY));

		for (int i = 1; i <= ConnectionBackoff.MAX_QUEUED_PAYLOADS; i++)
		{
			backoff.enqueue(KEY, "payload-" + i);
		}
		backoff.completeDrain(KEY, true);

		assertEquals(ConnectionBackoff.MAX_QUEUED_PAYLOADS, backoff.getQueuedCount(KEY));
		assertEquals("payload-1", backoff.beginDrain(KEY));
	}

	@Test
	public void testClearResetsEverything()
	{
		backoff.enqueue(KEY, "a");
		backoff.recordFailure(KEY);

		backoff.clear(KEY);

		assertFalse(backoff.isPaused(KEY));
		assertEquals(0, backoff.getQueuedCount(KEY));
		assertNull(backoff.beginDrain(KEY));

		backoff.recordFailure(KEY);
		assertEquals(now + 30 * SECOND, backoff.getPausedUntil(KEY));
	}

	@Test
	public void testClearAllResetsEveryConnection()
	{
		String other = "http://other.local:8123\ntoken";
		backoff.enqueue(KEY, "a");
		backoff.recordFailure(KEY);
		backoff.enqueue(other, "b");
		backoff.recordFailure(other);

		backoff.clearAll();

		for (String key : new String[]{KEY, other})
		{
			assertFalse(backoff.isPaused(key));
			assertEquals(0, backoff.getQueuedCount(key));
			assertNull(backoff.beginDrain(key));
		}
	}

	@Test
	public void testConnectionsAreIndependent()
	{
		String other = "http://other.local:8123\ntoken";
		backoff.recordFailure(KEY);
		backoff.enqueue(KEY, "a");

		assertFalse(backoff.isPaused(other));
		assertEquals(0, backoff.getQueuedCount(other));
	}

	@Test
	public void testClassify()
	{
		assertEquals(ConnectionBackoff.Outcome.SUCCESS, ConnectionBackoff.classify(200));
		assertEquals(ConnectionBackoff.Outcome.SUCCESS, ConnectionBackoff.classify(204));
		assertEquals(ConnectionBackoff.Outcome.UNAUTHORIZED, ConnectionBackoff.classify(401));
		assertEquals(ConnectionBackoff.Outcome.GONE, ConnectionBackoff.classify(410));
		assertEquals(ConnectionBackoff.Outcome.RETRY_AFTER, ConnectionBackoff.classify(429));
		assertEquals(ConnectionBackoff.Outcome.RETRY_AFTER, ConnectionBackoff.classify(503));
		assertEquals(ConnectionBackoff.Outcome.BACKOFF, ConnectionBackoff.classify(500));
		assertEquals(ConnectionBackoff.Outcome.BACKOFF, ConnectionBackoff.classify(502));
		assertEquals(ConnectionBackoff.Outcome.BACKOFF, ConnectionBackoff.classify(504));
		assertEquals(ConnectionBackoff.Outcome.REJECTED, ConnectionBackoff.classify(400));
		assertEquals(ConnectionBackoff.Outcome.REJECTED, ConnectionBackoff.classify(404));
		assertEquals(ConnectionBackoff.Outcome.REJECTED, ConnectionBackoff.classify(413));
		assertEquals(ConnectionBackoff.Outcome.REJECTED, ConnectionBackoff.classify(302));
	}
}
