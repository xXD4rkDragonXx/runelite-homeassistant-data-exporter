package haexporterplugin.notifiers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Providers;
import haexporterplugin.HAExporterConfig;
import haexporterplugin.utils.HomeAssistUtils;
import haexporterplugin.utils.MessageBuilder;
import haexporterplugin.utils.RarityUtils;
import haexporterplugin.utils.ThievingUtils;
import haexporterplugin.utils.TickUtils;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LocationNotifierTest
{
	private final Gson gson = new Gson();
	private TickUtils tickUtils;
	private HAExporterConfig config;
	private MessageBuilder messageBuilder;
	private LocationNotifier locationNotifier;

	// The position of the most recent tick, see standStill()
	private WorldPoint lastPoint;
	private WorldPoint lastScenePoint;
	private boolean lastOnBoat;

	@Before
	public void setUp()
	{
		config = mock(HAExporterConfig.class);
		when(config.includeLocation()).thenReturn(true);
		tickUtils = mock(TickUtils.class);

		// Providers.of avoids Guice member-injecting the mocks' inherited @Inject fields
		Injector injector = Guice.createInjector(binder ->
		{
			binder.bind(Client.class).toProvider(Providers.of(mock(Client.class)));
			binder.bind(HAExporterConfig.class).toProvider(Providers.of(config));
			binder.bind(TickUtils.class).toProvider(Providers.of(tickUtils));
			binder.bind(HomeAssistUtils.class).toProvider(Providers.of(mock(HomeAssistUtils.class)));
			binder.bind(RarityUtils.class).toProvider(Providers.of(mock(RarityUtils.class)));
			binder.bind(ThievingUtils.class).toProvider(Providers.of(mock(ThievingUtils.class)));
			binder.bind(Gson.class).toInstance(gson);
		});
		messageBuilder = injector.getInstance(MessageBuilder.class);
		locationNotifier = injector.getInstance(LocationNotifier.class);
	}

	/* ============================
	   TRAIL
	   ============================ */

	@Test
	public void testFirstLocationStartsTheTrail()
	{
		long before = System.currentTimeMillis();
		walkTo(3222, 3218, 0);
		long after = System.currentTimeMillis();

		JsonArray trail = trail();
		assertEquals(1, trail.size());
		JsonObject point = trail.get(0).getAsJsonObject();
		assertEquals(3222, point.get("x").getAsInt());
		assertEquals(3218, point.get("y").getAsInt());
		assertEquals(0, point.get("plane").getAsInt());
		assertFalse(point.get("isOnBoat").getAsBoolean());
		assertFalse(point.get("teleport").getAsBoolean());
		long timestamp = point.get("timestamp").getAsLong();
		assertTrue(timestamp >= before && timestamp <= after);
	}

	@Test
	public void testLocationStillHoldsTheLatestTile()
	{
		walkTo(3222, 3218, 0);
		walkTo(3223, 3219, 0);

		JsonObject location = player().getAsJsonObject("location");
		assertEquals(3223, location.get("x").getAsInt());
		assertEquals(3219, location.get("y").getAsInt());
	}

	@Test
	public void testStandingStillAddsNoPoints()
	{
		walkTo(3222, 3218, 0);
		walkTo(3222, 3218, 0);
		walkTo(3222, 3218, 0);

		assertEquals(1, trail().size());
	}

	@Test
	public void testEveryTileChangeIsRecordedOldestFirst()
	{
		walkTo(3222, 3218, 0);
		walkTo(3223, 3218, 0);
		walkTo(3225, 3220, 0);

		JsonArray trail = trail();
		assertEquals(3, trail.size());
		assertPoint(trail, 0, 3222, 3218, 0);
		assertPoint(trail, 1, 3223, 3218, 0);
		assertPoint(trail, 2, 3225, 3220, 0);
	}

	@Test
	public void testPlaneChangeIsRecordedWithoutTeleport()
	{
		walkTo(3205, 3209, 0);
		walkTo(3205, 3209, 1);

		JsonArray trail = trail();
		assertEquals(2, trail.size());
		assertPoint(trail, 1, 3205, 3209, 1);
		assertFalse(isTeleport(trail, 1));
		assertNoTeleportSent();
	}

	@Test
	public void testBoatStateChangeIsRecorded()
	{
		walkTo(3050, 3193, 0);
		sailTo(3050, 3193);

		JsonArray trail = trail();
		assertEquals(2, trail.size());
		assertTrue(trail.get(1).getAsJsonObject().get("isOnBoat").getAsBoolean());
	}

	@Test
	public void testTrailKeepsOnlyTheNewestPoints()
	{
		int max = MessageBuilder.MAX_LOCATION_TRAIL_POINTS;
		for (int i = 0; i <= max; i++)
		{
			walkTo(3000 + i, 3218, 0);
		}

		JsonArray trail = trail();
		assertEquals(max, trail.size());
		assertPoint(trail, 0, 3001, 3218, 0);
		assertPoint(trail, max - 1, 3000 + max, 3218, 0);
	}

	/* ============================
	   TELEPORTS
	   ============================ */

	@Test
	public void testRunningIsNotATeleport()
	{
		walkTo(3222, 3218, 0);
		walkTo(3224, 3220, 0);

		assertFalse(isTeleport(trail(), 1));
		assertNoTeleportSent();
	}

	@Test
	public void testFiveTilesInOneTickIsNotATeleport()
	{
		walkTo(3222, 3218, 0);
		walkTo(3227, 3213, 0);

		assertFalse(isTeleport(trail(), 1));
		assertNoTeleportSent();
	}

	@Test
	public void testSixTilesInOneTickIsATeleport()
	{
		walkTo(3222, 3218, 0);
		walkTo(3222, 3224, 0);
		standStill();

		assertTrue(isTeleport(trail(), 1));
		assertEquals(1, events().size());
	}

	@Test
	public void testTeleportEventWaitsForTheNextTick()
	{
		walkTo(3164, 3487, 0);
		walkTo(1640, 9562, 1);

		assertNoTeleportSent();
	}

	@Test
	public void testTeleportSendsEventWithFromAndTo()
	{
		// Grand Exchange -> a cave
		walkTo(3164, 3487, 0);
		walkTo(1640, 9562, 1);
		standStill();

		JsonArray events = events();
		assertEquals(1, events.size());
		JsonObject event = events.get(0).getAsJsonObject();
		assertEquals("teleport", event.get("type").getAsString());

		JsonObject from = event.getAsJsonObject("data").getAsJsonObject("from");
		assertEquals(3164, from.get("x").getAsInt());
		assertEquals(3487, from.get("y").getAsInt());
		assertEquals(0, from.get("plane").getAsInt());

		JsonObject to = event.getAsJsonObject("data").getAsJsonObject("to");
		assertEquals(1640, to.get("x").getAsInt());
		assertEquals(9562, to.get("y").getAsInt());
		assertEquals(1, to.get("plane").getAsInt());

		verify(tickUtils).sendNow();
	}

	@Test
	public void testTeleportArrivalIsFlaggedInTheTrail()
	{
		walkTo(3164, 3487, 0);
		walkTo(1640, 9562, 0);
		walkTo(1641, 9562, 0);

		JsonArray trail = trail();
		assertFalse(isTeleport(trail, 0));
		assertTrue(isTeleport(trail, 1));
		assertPoint(trail, 1, 1640, 9562, 0);
		assertFalse(isTeleport(trail, 2));
	}

	@Test
	public void testBoardingABoatIsNotATeleport()
	{
		// The boat's position is its centre, which can be several tiles from the dock
		walkTo(3050, 3193, 0);
		sailTo(3058, 3193);

		assertFalse(isTeleport(trail(), 1));
		assertNoTeleportSent();
	}

	@Test
	public void testFastBoatIsNotATeleport()
	{
		sailTo(3058, 3193);
		sailTo(3064, 3193);

		assertFalse(isTeleport(trail(), 1));
		assertNoTeleportSent();
	}

	@Test
	public void testTwentyTilesOnABoatIsNotATeleport()
	{
		sailTo(3058, 3193);
		sailTo(3078, 3193);

		assertFalse(isTeleport(trail(), 1));
		assertNoTeleportSent();
	}

	@Test
	public void testTwentyOneTilesOnABoatIsATeleport()
	{
		sailTo(3058, 3193);
		sailTo(3058, 3214);
		standStill();

		assertTrue(isTeleport(trail(), 1));
		assertEquals(1, events().size());
	}

	@Test
	public void testTeleportingOffABoatIsATeleport()
	{
		sailTo(3058, 3193);
		walkTo(3164, 3487, 0);
		standStill();

		assertTrue(isTeleport(trail(), 1));
		assertEquals(1, events().size());
	}

	@Test
	public void testWalkingBetweenHouseRoomsSendsNoTeleportEvent()
	{
		// Two rooms next to each other in the house, built from templates that are far apart on the map
		walkInInstance(1943, 7053, 6471, 6420);
		walkInInstance(1864, 5092, 6472, 6420);

		assertNoTeleportSent();
	}

	@Test
	public void testWalkingBetweenHouseRoomsStillBreaksTheTrail()
	{
		walkInInstance(1943, 7053, 6471, 6420);
		walkInInstance(1864, 5092, 6472, 6420);

		assertTrue(isTeleport(trail(), 1));
	}

	@Test
	public void testTeleportInsideAnInstanceIsATeleport()
	{
		walkInInstance(1943, 7053, 6471, 6420);
		walkInInstance(1864, 5092, 6440, 6452);
		standStill();

		assertTrue(isTeleport(trail(), 1));
		assertEquals(1, events().size());
	}

	@Test
	public void testHouseTeleportViaThePortalSendsOneEvent()
	{
		// A house teleport puts the player outside the portal for one tick before loading the house
		walkTo(3209, 3428, 0);
		walkTo(2954, 3224, 0);
		walkInInstance(1939, 7053, 6500, 6500);
		standStill();

		JsonArray events = events();
		assertEquals(1, events.size());
		JsonObject data = events.get(0).getAsJsonObject().getAsJsonObject("data");
		assertEquals(3209, data.getAsJsonObject("from").get("x").getAsInt());
		assertEquals(3428, data.getAsJsonObject("from").get("y").getAsInt());
		assertEquals(1939, data.getAsJsonObject("to").get("x").getAsInt());
		assertEquals(7053, data.getAsJsonObject("to").get("y").getAsInt());
		verify(tickUtils, times(1)).sendNow();

		JsonArray trail = trail();
		assertEquals(3, trail.size());
		assertTrue(isTeleport(trail, 1));
		assertTrue(isTeleport(trail, 2));
	}

	@Test
	public void testTeleportEventPointsAtTheArrivalTileWhenWalkingOn()
	{
		walkTo(3164, 3487, 0);
		walkTo(1640, 9562, 0);
		walkTo(1642, 9562, 0);

		JsonArray events = events();
		assertEquals(1, events.size());
		JsonObject to = events.get(0).getAsJsonObject().getAsJsonObject("data").getAsJsonObject("to");
		assertEquals(1640, to.get("x").getAsInt());
		assertEquals(3, trail().size());
	}

	@Test
	public void testTeleportEventIsSentOnlyOnce()
	{
		walkTo(3164, 3487, 0);
		walkTo(1640, 9562, 0);
		standStill();
		standStill();
		walkTo(1641, 9562, 0);

		assertEquals(1, events().size());
		verify(tickUtils, times(1)).sendNow();
	}

	@Test
	public void testResetDropsATeleportThatWasNotSentYet()
	{
		walkTo(3164, 3487, 0);
		walkTo(1640, 9562, 0);
		locationNotifier.reset();
		walkTo(1640, 9562, 0);
		standStill();

		assertNoTeleportSent();
	}

	@Test
	public void testNoTeleportAcrossReset()
	{
		walkTo(3164, 3487, 0);
		locationNotifier.reset();
		walkTo(1640, 9562, 0);

		assertFalse(isTeleport(trail(), 1));
		assertNoTeleportSent();
	}

	/* ============================
	   SHARE LOCATION SWITCH
	   ============================ */

	@Test
	public void testNothingRecordedWhileLocationSharingIsOff()
	{
		when(config.includeLocation()).thenReturn(false);

		walkTo(3164, 3487, 0);
		walkTo(1640, 9562, 0);

		assertEquals(0, trail().size());
		assertNoTeleportSent();
	}

	@Test
	public void testNoTeleportWhenLocationSharingIsTurnedOn()
	{
		when(config.includeLocation()).thenReturn(false);
		walkTo(3164, 3487, 0);

		when(config.includeLocation()).thenReturn(true);
		walkTo(1640, 9562, 0);

		JsonArray trail = trail();
		assertEquals(1, trail.size());
		assertFalse(isTeleport(trail, 0));
		assertNoTeleportSent();
	}

	/* ============================
	   HELPERS
	   ============================ */

	private void walkTo(int x, int y, int plane)
	{
		WorldPoint point = new WorldPoint(x, y, plane);
		tick(point, point, false);
	}

	private void sailTo(int x, int y)
	{
		WorldPoint point = new WorldPoint(x, y, 0);
		tick(point, point, true);
	}

	// Inside an instance the reported tile is the template's, while the player really stands on the scene tile
	private void walkInInstance(int templateX, int templateY, int sceneX, int sceneY)
	{
		tick(new WorldPoint(templateX, templateY, 1), new WorldPoint(sceneX, sceneY, 1), false);
	}

	// One more game tick on the same tile
	private void standStill()
	{
		tick(lastPoint, lastScenePoint, lastOnBoat);
	}

	private void tick(WorldPoint point, WorldPoint scenePoint, boolean isOnBoat)
	{
		lastPoint = point;
		lastScenePoint = scenePoint;
		lastOnBoat = isOnBoat;
		locationNotifier.recordLocation(point, scenePoint, isOnBoat);
	}

	private JsonObject player()
	{
		return gson.fromJson(messageBuilder.build(), JsonObject.class).getAsJsonObject("player");
	}

	private JsonArray trail()
	{
		return player().getAsJsonArray("locationTrail");
	}

	private JsonArray events()
	{
		return gson.fromJson(messageBuilder.build(), JsonObject.class).getAsJsonArray("events");
	}

	private void assertNoTeleportSent()
	{
		assertEquals(0, events().size());
		verify(tickUtils, never()).sendNow();
	}

	private static boolean isTeleport(JsonArray trail, int index)
	{
		return trail.get(index).getAsJsonObject().get("teleport").getAsBoolean();
	}

	private static void assertPoint(JsonArray trail, int index, int x, int y, int plane)
	{
		JsonObject point = trail.get(index).getAsJsonObject();
		assertEquals(x, point.get("x").getAsInt());
		assertEquals(y, point.get("y").getAsInt());
		assertEquals(plane, point.get("plane").getAsInt());
	}
}
