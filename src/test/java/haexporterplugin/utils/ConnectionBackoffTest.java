package haexporterplugin.utils;

import haexporterplugin.utils.ConnectionBackoff.Outcome;
import org.junit.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ConnectionBackoffTest
{
	private static final String KEY = "http://ha.local:8123\ntoken";
	private static final long SECOND = 1000L;
	private static final long MINUTE = 60 * SECOND;

	private long now = 1_700_000_000_000L;
	private final ConnectionBackoff backoff = new ConnectionBackoff(() -> now);

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
	public void testParseRetryAfter()
	{
		assertEquals(Long.valueOf(now + 120 * SECOND), ConnectionBackoff.parseRetryAfter("120", now));
		assertEquals(Long.valueOf(now + 5 * SECOND), ConnectionBackoff.parseRetryAfter(" 5 ", now));
		assertEquals(Long.valueOf(now), ConnectionBackoff.parseRetryAfter("0", now));
		assertEquals(Long.valueOf(Instant.parse("2015-10-21T07:28:00Z").toEpochMilli()),
			ConnectionBackoff.parseRetryAfter("Wed, 21 Oct 2015 07:28:00 GMT", now));

		for (String invalid : new String[]{null, "", "   ", "soon", "-5", "1.5", "99999999999999999999", "2015-10-21T07:28:00Z"})
		{
			assertNull(invalid, ConnectionBackoff.parseRetryAfter(invalid, now));
		}
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
	public void testNewestQueuedPayloadCanBeReplacedByAMergedOne()
	{
		backoff.enqueue(KEY, "a");
		backoff.enqueue(KEY, "b");

		String candidate = backoff.getMergeCandidate(KEY);
		assertEquals("b", candidate);
		assertTrue(backoff.replaceNewest(KEY, candidate, "b+c"));

		assertEquals(2, backoff.getQueuedCount(KEY));
		assertEquals("a", backoff.beginDrain(KEY));
		backoff.completeDrain(KEY, true);
		assertEquals("b+c", backoff.beginDrain(KEY));
	}

	@Test
	public void testPayloadBeingResentIsNoMergeCandidate()
	{
		backoff.enqueue(KEY, "a");
		assertEquals("a", backoff.beginDrain(KEY));

		assertNull(backoff.getMergeCandidate(KEY));
	}

	@Test
	public void testReplaceFailsWhenTheCandidateStartedBeingResent()
	{
		backoff.enqueue(KEY, "a");
		String candidate = backoff.getMergeCandidate(KEY);
		assertEquals("a", backoff.beginDrain(KEY));

		assertFalse(backoff.replaceNewest(KEY, candidate, "a+b"));

		backoff.completeDrain(KEY, true);
		assertEquals(0, backoff.getQueuedCount(KEY));
	}

	@Test
	public void testReplaceFailsWhenAnotherPayloadWasQueuedMeanwhile()
	{
		backoff.enqueue(KEY, "a");
		String candidate = backoff.getMergeCandidate(KEY);
		backoff.enqueue(KEY, "b");

		assertFalse(backoff.replaceNewest(KEY, candidate, "a+c"));

		assertEquals(2, backoff.getQueuedCount(KEY));
		assertEquals("a", backoff.beginDrain(KEY));
	}

	@Test
	public void testPayloadIsOnlyAMergeCandidateForAMinute()
	{
		backoff.enqueue(KEY, "a");

		now += MINUTE - 1;
		assertEquals("a", backoff.getMergeCandidate(KEY));

		now += 1;
		assertNull(backoff.getMergeCandidate(KEY));
	}

	@Test
	public void testMergedPayloadKeepsItsQueueTime()
	{
		backoff.enqueue(KEY, "a");
		now += 30 * SECOND;
		assertTrue(backoff.replaceNewest(KEY, backoff.getMergeCandidate(KEY), "a+b"));

		now += 9 * MINUTE + 30 * SECOND;
		assertEquals(1, backoff.getQueuedCount(KEY));

		now += 1;
		assertEquals(0, backoff.getQueuedCount(KEY));
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

	// A 404 says the endpoint isn't there (yet), not that it refused the payload: Home Assistant answers it
	// until the integration has registered its endpoints
	@Test
	public void testClassify()
	{
		Map<Outcome, List<Integer>> codes = Map.of(
			Outcome.SUCCESS, List.of(200, 204),
			Outcome.UNAUTHORIZED, List.of(401),
			Outcome.GONE, List.of(410),
			Outcome.RETRY_AFTER, List.of(429, 503),
			Outcome.BACKOFF, List.of(404, 500, 502, 504),
			Outcome.REJECTED, List.of(302, 400, 403, 405, 413, 422));
		codes.forEach((outcome, list) -> list.forEach(code -> assertEquals("HTTP " + code, outcome, ConnectionBackoff.classify(code))));
	}
}
