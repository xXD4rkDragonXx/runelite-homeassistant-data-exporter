package haexporterplugin.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Injector;
import haexporterplugin.HAExporterConfig;
import haexporterplugin.TestUtils;
import haexporterplugin.data.TrailPoint;
import net.runelite.api.Client;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TickUtilsTest
{
	private static final int SEND_RATE = 3;

	private final Gson gson = new Gson();
	private final Client client = mock(Client.class);
	private final HAExporterConfig config = mock(HAExporterConfig.class);
	private final HomeAssistUtils homeAssistUtils = mock(HomeAssistUtils.class);
	private MessageBuilder messageBuilder;
	private TickUtils tickUtils;

	@Before
	public void setUp()
	{
		when(config.sendRate()).thenReturn(SEND_RATE);
		onWorld(WorldType.MEMBERS);

		Injector injector = TestUtils.injector(Map.of(Client.class, client, HAExporterConfig.class, config, HomeAssistUtils.class, homeAssistUtils));
		messageBuilder = injector.getInstance(MessageBuilder.class);
		tickUtils = new TickUtils();
		injector.injectMembers(tickUtils);
	}

	/* ============================
	   SENDING EACH EVENT ONCE
	   ============================ */

	@Test
	public void testEventNotResentOnNextPeriodicSend()
	{
		messageBuilder.addEvent("combatTask", "combat-task");

		tickUntilPeriodicSend();
		tickUntilPeriodicSend();

		verify(homeAssistUtils, times(2)).sendMessage(anyString());
		assertEquals(1, sentEvents().size());
	}

	@Test
	public void testEventNotResentAfterShutdownSend()
	{
		messageBuilder.addEvent("clientShutdown", "Shutdown");

		tickUtils.sendShutdown();
		tickUtils.sendNow();

		assertEquals(1, sentEvents().size());
	}

	@Test
	public void testEventsAddedAfterSendAreSentNextTime()
	{
		messageBuilder.addEvent("achievementDiary", "first");
		tickUtils.sendNow();
		messageBuilder.addEvent("achievementDiary", "second");
		tickUntilPeriodicSend();

		List<JsonObject> payloads = sentPayloads();
		assertEquals(2, payloads.size());
		assertEquals(List.of("first"), eventData(payloads.get(0)));
		assertEquals(List.of("second"), eventData(payloads.get(1)));
	}

	@Test
	public void testLocationTrailSentOnceAcrossSends()
	{
		addTrailPoint();

		tickUtils.sendNow();
		tickUntilPeriodicSend();

		List<JsonObject> payloads = sentPayloads();
		assertEquals(2, payloads.size());
		assertEquals(1, trail(payloads.get(0)).size());
		assertEquals(0, trail(payloads.get(1)).size());
	}

	/* ============================
	   SPECIAL WORLDS
	   ============================ */

	@Test
	public void testMembersWorldSentByDefault()
	{
		tickUtils.sendNow();
		tickUntilPeriodicSend();
		tickUtils.sendShutdown();

		verify(homeAssistUtils, times(3)).sendMessage(anyString());
	}

	@Test
	public void testNothingSentFromSpecialWorldByDefault()
	{
		onWorld(WorldType.MEMBERS, WorldType.SEASONAL);
		messageBuilder.addEvent("levelUp", "attack-99");

		tickUtils.sendNow();
		tickUntilPeriodicSend();
		tickUtils.sendShutdown();

		verify(homeAssistUtils, never()).sendMessage(anyString());
	}

	@Test
	public void testSpecialWorldEventsAndTrailDoNotLeakIntoLaterMessages()
	{
		onWorld(WorldType.MEMBERS, WorldType.DEADMAN);
		messageBuilder.addEvent("levelUp", "from-sendNow");
		addTrailPoint();
		tickUtils.sendNow();
		messageBuilder.addEvent("achievementDiary", "from-periodic");
		tickUntilPeriodicSend();

		onWorld(WorldType.MEMBERS);
		tickUtils.sendNow();

		JsonObject payload = onlySentPayload();
		assertEquals(List.of(), eventData(payload));
		assertEquals(0, trail(payload).size());
	}

	@Test
	public void testSpecialWorldSentWhenEnabled()
	{
		when(config.sendSpecialWorldData()).thenReturn(true);
		onWorld(WorldType.MEMBERS, WorldType.SEASONAL);
		messageBuilder.addEvent("levelUp", "attack-99");

		tickUtils.sendNow();

		assertEquals(List.of("attack-99"), eventData(onlySentPayload()));
	}

	/* ============================
	   HELPERS
	   ============================ */

	private void onWorld(WorldType first, WorldType... rest)
	{
		when(client.getWorldType()).thenReturn(EnumSet.of(first, rest));
	}

	private void addTrailPoint()
	{
		messageBuilder.addLocationTrailPoint(new TrailPoint(new WorldPoint(3222, 3218, 0), false, 1735689600000L));
	}

	private void tickUntilPeriodicSend()
	{
		for (int i = 0; i < SEND_RATE; i++)
		{
			tickUtils.onTick();
			tickUtils.sendOnSendRate();
		}
	}

	private List<JsonObject> sentPayloads()
	{
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(homeAssistUtils, atLeastOnce()).sendMessage(captor.capture());

		List<JsonObject> payloads = new ArrayList<>();
		captor.getAllValues().forEach(json -> payloads.add(gson.fromJson(json, JsonObject.class)));
		return payloads;
	}

	private JsonObject onlySentPayload()
	{
		List<JsonObject> payloads = sentPayloads();
		assertEquals(1, payloads.size());
		return payloads.get(0);
	}

	// The events of every message sent so far
	private List<JsonObject> sentEvents()
	{
		List<JsonObject> events = new ArrayList<>();
		sentPayloads().forEach(payload -> payload.getAsJsonArray("events").forEach(event -> events.add(event.getAsJsonObject())));
		return events;
	}

	private static List<String> eventData(JsonObject payload)
	{
		List<String> data = new ArrayList<>();
		payload.getAsJsonArray("events").forEach(event -> data.add(event.getAsJsonObject().get("data").getAsString()));
		return data;
	}

	private static JsonArray trail(JsonObject payload)
	{
		return payload.getAsJsonObject("player").getAsJsonArray("locationTrail");
	}
}
