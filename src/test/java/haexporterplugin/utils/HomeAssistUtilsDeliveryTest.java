package haexporterplugin.utils;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import haexporterplugin.HAExporterConfig;
import haexporterplugin.data.HAConnection;
import okhttp3.Dispatcher;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HomeAssistUtilsDeliveryTest
{
	private static final long T0 = 1_700_000_000_000L;
	private static final long TICK = 600L;
	private static final long SECOND = 1000L;
	private static final String LEVEL_UP = "{\"type\":\"levelUp\",\"data\":[{\"skill\":\"Attack\",\"level\":99}],\"eventId\":\"e\",\"timestamp\":1}";

	private final Gson gson = new Gson();
	private HAConnection connection;
	private HomeAssistUtils homeAssistUtils;

	// Clock of the connection's backoff state
	private long now = T0;

	// Status code answered to every request, and the bodies of the requests that were sent
	private int responseCode = 200;
	private final List<String> sentBodies = new ArrayList<>();

	// Retries the plugin scheduled for the end of a pause
	private final List<Runnable> scheduledRetries = new ArrayList<>();

	@Before
	public void setUp() throws Exception
	{
		HAExporterConfig config = mock(HAExporterConfig.class);
		when(config.includeInventory()).thenReturn(true);
		when(config.includeEquipment()).thenReturn(true);
		when(config.includeLocation()).thenReturn(true);
		when(config.includeLootEvents()).thenReturn(true);
		when(config.includeDeathEvents()).thenReturn(true);
		when(config.includeLevelUpEvents()).thenReturn(true);
		when(config.includeAchievementDiaryEvents()).thenReturn(true);
		when(config.includeCombatTaskEvents()).thenReturn(true);
		when(config.includeSuperiorEvents()).thenReturn(true);
		when(config.includeCollectionLogEvents()).thenReturn(true);

		connection = new HAConnection("http://ha.local:8123", "secret-token");
		ConfigUtils configUtils = mock(ConfigUtils.class);
		when(configUtils.getStoredConnections()).thenAnswer(invocation -> List.of(connection));

		// The interceptor answers every call itself, and the direct executor runs the callback before
		// sendMessage returns, so no network or waiting is involved
		OkHttpClient okHttpClient = new OkHttpClient.Builder()
			.dispatcher(new Dispatcher(MoreExecutors.newDirectExecutorService()))
			.addInterceptor(chain ->
			{
				Buffer body = new Buffer();
				chain.request().body().writeTo(body);
				sentBodies.add(body.readUtf8());
				return response(chain.request(), responseCode);
			})
			.build();

		ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
		when(executor.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenAnswer(invocation ->
		{
			scheduledRetries.add(invocation.getArgument(0));
			return null;
		});

		homeAssistUtils = new HomeAssistUtils();
		homeAssistUtils.config = config;
		// Inject dependencies via reflection since we're not using Guice in tests
		setField("gson", gson);
		setField("configUtils", configUtils);
		setField("okHttpClient", okHttpClient);
		setField("executor", executor);
		setField("backoff", new ConnectionBackoff(() -> now));
	}

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
				return response(chain.request(), 200);
			})
			.build();
		setField("okHttpClient", unansweredClient);

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
	public void testFailedSnapshotWithTrailIsResentAfterThePause()
	{
		responseCode = 500;
		homeAssistUtils.sendMessage(snapshot(T0, T0 - 2 * TICK, T0 - TICK));

		responseCode = 200;
		endPause();

		assertEquals(2, sentBodies.size());
		assertEquals(List.of(T0 - 2 * TICK, T0 - TICK), trailTimestamps(sentBodies.get(1)));
		assertEquals(0, homeAssistUtils.getQueuedCount(connection));
	}

	@Test
	public void testSnapshotWithTrailBuiltDuringAPauseIsSentAfterIt()
	{
		responseCode = 500;
		homeAssistUtils.sendMessage(snapshot(T0));
		homeAssistUtils.sendMessage(snapshot(T0 + 10 * SECOND, T0 + 9 * SECOND));
		assertEquals(1, sentBodies.size());

		responseCode = 200;
		endPause();

		assertEquals(2, sentBodies.size());
		assertEquals(List.of(T0 + 9 * SECOND), trailTimestamps(sentBodies.get(1)));
	}

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

	@Test
	public void testRejectedSnapshotWithTrailIsNotRetried()
	{
		responseCode = 400;
		homeAssistUtils.sendMessage(snapshot(T0, T0 - TICK));

		assertEquals(0, homeAssistUtils.getQueuedCount(connection));
		assertEquals(0, homeAssistUtils.getPausedUntil(connection));
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
	   HELPERS
	   ============================ */

	private static Response response(Request request, int code)
	{
		return new Response.Builder()
			.request(request)
			.protocol(Protocol.HTTP_1_1)
			.code(code)
			.message("Test")
			.body(ResponseBody.create(MediaType.get("application/json"), "{}"))
			.build();
	}

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

	private void setField(String name, Object value) throws Exception
	{
		Field field = HomeAssistUtils.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(homeAssistUtils, value);
	}
}
