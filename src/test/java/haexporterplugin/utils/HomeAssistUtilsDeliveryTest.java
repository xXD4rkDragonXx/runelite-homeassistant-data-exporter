package haexporterplugin.utils;

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import haexporterplugin.TestUtils;
import okhttp3.OkHttpClient;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class HomeAssistUtilsDeliveryTest extends HomeAssistUtilsTestBase
{
	private static final long TICK = 600L;
	private static final String LEVEL_UP = "{\"type\":\"levelUp\",\"data\":[{\"skill\":\"Attack\",\"level\":99}],\"eventId\":\"e\",\"timestamp\":1}";

	/* ============================
	   CALLING THREAD
	   ============================ */

	// sendMessage runs on the game thread, which must never wait for an endpoint
	@Test(timeout = 10_000)
	public void testSendMessageDoesNotWaitForTheEndpoint() throws Exception
	{
		CountDownLatch requestStarted = new CountDownLatch(1);
		CountDownLatch answer = new CountDownLatch(1);
		OkHttpClient unansweredClient = new OkHttpClient.Builder()
			.addInterceptor(chain ->
			{
				requestStarted.countDown();
				Uninterruptibles.awaitUninterruptibly(answer);
				return response(chain.request(), 200, "{}");
			})
			.build();
		TestUtils.setField(homeAssistUtils, "okHttpClient", unansweredClient);

		try
		{
			homeAssistUtils.sendMessage(snapshot(T0, T0 - TICK));
			assertTrue(requestStarted.await(5, TimeUnit.SECONDS));

			// The first request is still unanswered
			homeAssistUtils.sendMessage(snapshot(T0 + 10 * SECOND, T0 + 9 * SECOND));
		}
		finally
		{
			answer.countDown();
			unansweredClient.dispatcher().executorService().shutdown();
		}
	}

	/* ============================
	   LOCATION TRAIL
	   ============================ */

	@Test
	public void testSnapshotWithTrailLinesUpBehindPayloadsStillWaiting()
	{
		responseCode = 500;
		homeAssistUtils.sendMessage(payload(T0, LEVEL_UP, T0 - TICK));

		// The pause is over, but the queued payload has not been resent yet
		responseCode = 200;
		now = homeAssistUtils.getPausedUntil(connection);
		homeAssistUtils.sendMessage(snapshot(T0 + 30 * SECOND, T0 + 29 * SECOND));

		assertEquals(3, sentBodies.size());
		assertEquals(List.of(T0 - TICK), trailTimestamps(sentBodies.get(1)));
		assertEquals(List.of(T0 + 29 * SECOND), trailTimestamps(sentBodies.get(2)));
	}

	@Test
	public void testSnapshotWithoutTrailOrEventsIsNotRetried()
	{
		responseCode = 500;
		homeAssistUtils.sendMessage(snapshot(T0));
		homeAssistUtils.sendMessage(snapshot(T0 + 10 * SECOND));
		assertEquals(0, homeAssistUtils.getQueuedCount(connection));

		responseCode = 200;
		endPause();

		assertEquals(1, sentBodies.size());
	}

	@Test
	public void testTrailIsNotKeptForConnectionThatDoesNotReceiveLocation()
	{
		connection.setIncludeLocation(false);

		responseCode = 500;
		homeAssistUtils.sendMessage(snapshot(T0, T0 - TICK));
		homeAssistUtils.sendMessage(snapshot(T0 + 10 * SECOND, T0 + 9 * SECOND));

		assertEquals(0, homeAssistUtils.getQueuedCount(connection));
	}

	/* ============================
	   COMBINING QUEUED SNAPSHOTS
	   ============================ */

	@Test
	public void testTrailSnapshotsQueuedCloseTogetherAreCombined()
	{
		responseCode = 500;
		homeAssistUtils.sendMessage(snapshot(T0, T0 - TICK));
		now += 10 * SECOND;
		homeAssistUtils.sendMessage(snapshot(T0 + 10 * SECOND, T0 + 9 * SECOND));
		assertEquals(1, homeAssistUtils.getQueuedCount(connection));

		responseCode = 200;
		endPause();

		assertEquals(2, sentBodies.size());
		assertEquals(List.of(T0 - TICK, T0 + 9 * SECOND), trailTimestamps(sentBodies.get(1)));
		assertEquals(T0 + 10 * SECOND, timestamp(sentBodies.get(1)));
	}

	@Test
	public void testTrailSnapshotIsNotCombinedWithPayloadWithEvents()
	{
		responseCode = 500;
		homeAssistUtils.sendMessage(payload(T0, LEVEL_UP, T0 - TICK));
		homeAssistUtils.sendMessage(snapshot(T0 + 10 * SECOND, T0 + 9 * SECOND));
		homeAssistUtils.sendMessage(payload(T0 + 20 * SECOND, LEVEL_UP, T0 + 19 * SECOND));
		assertEquals(3, homeAssistUtils.getQueuedCount(connection));

		responseCode = 200;
		endPause();

		assertEquals(4, sentBodies.size());
		assertEquals(List.of(T0 - TICK), trailTimestamps(sentBodies.get(1)));
		assertEquals(List.of(T0 + 9 * SECOND), trailTimestamps(sentBodies.get(2)));
		assertEquals(List.of(T0 + 19 * SECOND), trailTimestamps(sentBodies.get(3)));
	}

	@Test
	public void testCombinedSnapshotStaysWithinTheTrailPointLimit()
	{
		int limit = MessageBuilder.MAX_LOCATION_TRAIL_POINTS;

		responseCode = 500;
		homeAssistUtils.sendMessage(snapshot(T0, ticksBefore(T0, limit - 100)));
		homeAssistUtils.sendMessage(snapshot(T0 + 10 * SECOND, ticksBefore(T0 + 10 * SECOND, 100)));
		assertEquals(1, homeAssistUtils.getQueuedCount(connection));

		homeAssistUtils.sendMessage(snapshot(T0 + 20 * SECOND, T0 + 19 * SECOND));
		assertEquals(2, homeAssistUtils.getQueuedCount(connection));

		responseCode = 200;
		endPause();

		assertEquals(limit, trailTimestamps(sentBodies.get(1)).size());
		assertEquals(List.of(T0 + 19 * SECOND), trailTimestamps(sentBodies.get(2)));
	}

	@Test
	public void testSnapshotsFromDifferentWorldsAreNotCombined()
	{
		responseCode = 500;
		homeAssistUtils.sendMessage(snapshot(T0, T0 - TICK));
		homeAssistUtils.sendMessage(snapshot(T0 + 10 * SECOND, T0 + 9 * SECOND).replace("\"world\":\"302\"", "\"world\":\"303\""));

		assertEquals(2, homeAssistUtils.getQueuedCount(connection));
	}

	@Test
	public void testCombiningKeepsTheNewerSnapshotWhenTheOlderOneComesLast()
	{
		String combined = homeAssistUtils.combineTrailSnapshots(
			snapshot(T0 + 10 * SECOND, T0 + 9 * SECOND),
			snapshot(T0, T0 - TICK));

		assertEquals(List.of(T0 - TICK, T0 + 9 * SECOND), trailTimestamps(combined));
		assertEquals(T0 + 10 * SECOND, timestamp(combined));
	}

	/* ============================
	   404 AND OTHER 4XX
	   ============================ */

	// Home Assistant answers 404 from the moment its web server is up until the integration has registered its endpoints
	@Test
	public void testPayloadAnsweredNotFoundIsResentAfterThePause()
	{
		responseCode = 404;
		homeAssistUtils.sendMessage(payload(T0, LEVEL_UP));

		assertEquals(1, homeAssistUtils.getQueuedCount(connection));
		assertEquals(now + 30 * SECOND, homeAssistUtils.getPausedUntil(connection));

		responseCode = 200;
		endPause();

		assertEquals(2, sentBodies.size());
		assertEquals(sentBodies.get(0), sentBodies.get(1));
		assertEquals(0, homeAssistUtils.getQueuedCount(connection));
	}

	@Test
	public void testQueueIsNotDrainedWhileTheEndpointAnswersNotFound()
	{
		// Deliveries fail while Home Assistant restarts: three payloads with an event wait for it
		responseCode = 500;
		homeAssistUtils.sendMessage(payload(T0, LEVEL_UP));
		homeAssistUtils.sendMessage(payload(T0 + 10 * SECOND, LEVEL_UP));
		homeAssistUtils.sendMessage(payload(T0 + 20 * SECOND, LEVEL_UP));
		assertEquals(3, homeAssistUtils.getQueuedCount(connection));

		// Its web server answers again, but the integration has not registered its endpoints yet
		responseCode = 404;
		endPause();

		assertEquals(2, sentBodies.size());
		assertEquals(3, homeAssistUtils.getQueuedCount(connection));
		assertEquals(now + 60 * SECOND, homeAssistUtils.getPausedUntil(connection));

		responseCode = 200;
		endPause();

		assertEquals(5, sentBodies.size());
		assertEquals(T0, timestamp(sentBodies.get(2)));
		assertEquals(T0 + 10 * SECOND, timestamp(sentBodies.get(3)));
		assertEquals(T0 + 20 * SECOND, timestamp(sentBodies.get(4)));
		assertEquals(0, homeAssistUtils.getQueuedCount(connection));
	}

	// A connection whose address is wrong is answered 404 for as long as the client runs
	@Test
	public void testEndpointThatKeepsAnsweringNotFoundIsRetriedOncePerPause()
	{
		responseCode = 404;
		homeAssistUtils.sendMessage(payload(now, LEVEL_UP));

		long[] expectedPauseSeconds = {30, 60, 120, 240, 480, 600, 600, 600};
		for (long seconds : expectedPauseSeconds)
		{
			assertEquals(now + seconds * SECOND, homeAssistUtils.getPausedUntil(connection));

			// Halfway through the pause, more payloads have been built than the queue holds
			now += seconds * SECOND / 2;
			for (int i = 1; i <= ConnectionBackoff.MAX_QUEUED_PAYLOADS + 10; i++)
			{
				homeAssistUtils.sendMessage(payload(now + i, LEVEL_UP));
			}
			assertEquals(ConnectionBackoff.MAX_QUEUED_PAYLOADS, homeAssistUtils.getQueuedCount(connection));

			endPause();
		}

		// The first attempt and one retry per pause, however many payloads were built meanwhile
		assertEquals(1 + expectedPauseSeconds.length, sentBodies.size());
		assertEquals(1, scheduledRetries.size());

		// The payloads that are still waiting are given up once they are 10 minutes old
		now += ConnectionBackoff.MAX_QUEUE_AGE_MS + 1;
		assertEquals(0, homeAssistUtils.getQueuedCount(connection));
	}

	// A 400 means the endpoint refused that exact payload, so sending it again would only be refused again
	@Test
	public void testPayloadAnsweredBadRequestIsDroppedWithoutARetry()
	{
		responseCode = 400;
		homeAssistUtils.sendMessage(payload(T0, LEVEL_UP));

		assertEquals(1, sentBodies.size());
		assertEquals(0, homeAssistUtils.getQueuedCount(connection));
		assertEquals(0, homeAssistUtils.getPausedUntil(connection));
		assertTrue(scheduledRetries.isEmpty());
	}

	@Test
	public void testQueuedPayloadAnsweredBadRequestIsDroppedAndTheNextOneIsSentAtOnce()
	{
		responseCode = 500;
		homeAssistUtils.sendMessage(payload(T0, LEVEL_UP));
		homeAssistUtils.sendMessage(payload(T0 + 10 * SECOND, LEVEL_UP));
		assertEquals(2, homeAssistUtils.getQueuedCount(connection));

		responseCode = 400;
		endPause();

		assertEquals(3, sentBodies.size());
		assertEquals(T0, timestamp(sentBodies.get(1)));
		assertEquals(T0 + 10 * SECOND, timestamp(sentBodies.get(2)));
		assertEquals(0, homeAssistUtils.getQueuedCount(connection));
		assertEquals(0, homeAssistUtils.getPausedUntil(connection));
	}

	/* ============================
	   HELPERS
	   ============================ */

	// Moves the clock to the end of the current pause and runs the retries that were scheduled for it
	private void endPause()
	{
		now = Math.max(now, homeAssistUtils.getPausedUntil(connection));

		List<Runnable> due = new ArrayList<>(scheduledRetries);
		scheduledRetries.clear();
		due.forEach(Runnable::run);
	}

	// A periodic message without events, with one trail point per given timestamp
	private static String snapshot(long timestamp, long... trailTimestamps)
	{
		return payload(timestamp, "", trailTimestamps);
	}

	private static String payload(long timestamp, String events, long... trailTimestamps)
	{
		StringBuilder trail = new StringBuilder();
		for (long trailTimestamp : trailTimestamps)
		{
			if (trail.length() > 0)
			{
				trail.append(',');
			}
			trail.append("{\"x\":3222,\"y\":3218,\"plane\":0,\"isOnBoat\":false,\"timestamp\":").append(trailTimestamp).append('}');
		}

		return "{"
			+ "\"player\":{"
			+ "\"name\":\"PlayerName\","
			+ "\"world\":\"302\","
			+ "\"location\":{\"x\":3222,\"y\":3218,\"plane\":0,\"isOnBoat\":false},"
			+ "\"locationTrail\":[" + trail + "]"
			+ "},"
			+ "\"events\":[" + events + "],"
			+ "\"timestamp\":" + timestamp
			+ "}";
	}

	// One timestamp per game tick, the last one a tick before the given time
	private static long[] ticksBefore(long timestamp, int count)
	{
		long[] timestamps = new long[count];
		for (int i = 0; i < count; i++)
		{
			timestamps[i] = timestamp - (count - i) * TICK;
		}
		return timestamps;
	}

	private long timestamp(String body)
	{
		return gson.fromJson(body, JsonObject.class).get("timestamp").getAsLong();
	}

	private List<Long> trailTimestamps(String body)
	{
		List<Long> timestamps = new ArrayList<>();
		JsonObject player = gson.fromJson(body, JsonObject.class).getAsJsonObject("player");
		for (JsonElement point : player.getAsJsonArray("locationTrail"))
		{
			timestamps.add(point.getAsJsonObject().get("timestamp").getAsLong());
		}
		return timestamps;
	}
}
