package haexporterplugin.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Providers;
import haexporterplugin.HAExporterConfig;
import net.runelite.api.Client;
import net.runelite.api.WorldType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TickUtilsTest
{
	private static final int SEND_RATE = 3;

	private final Gson gson = new Gson();
	private HomeAssistUtils homeAssistUtils;
	private MessageBuilder messageBuilder;
	private TickUtils tickUtils;

	@Before
	public void setUp()
	{
		homeAssistUtils = mock(HomeAssistUtils.class);
		HAExporterConfig config = mock(HAExporterConfig.class);
		when(config.sendRate()).thenReturn(SEND_RATE);
		Client client = mock(Client.class);
		when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.MEMBERS));

		// Providers.of avoids Guice member-injecting the mocks' inherited @Inject fields
		Injector injector = Guice.createInjector(binder ->
		{
			binder.bind(Client.class).toProvider(Providers.of(client));
			binder.bind(HomeAssistUtils.class).toProvider(Providers.of(homeAssistUtils));
			binder.bind(HAExporterConfig.class).toProvider(Providers.of(config));
			binder.bind(Gson.class).toInstance(gson);
		});
		messageBuilder = injector.getInstance(MessageBuilder.class);
		tickUtils = injector.getInstance(TickUtils.class);
	}

	@Test
	public void testEventSentOnceAcrossPeriodicSendAndSendNow()
	{
		messageBuilder.addEvent("achievementDiary", "diary-task");

		tickUntilPeriodicSend();
		tickUtils.sendNow();

		List<String> eventIds = sentEventIds();
		assertEquals(1, eventIds.size());
	}

	@Test
	public void testEventNotResentOnNextPeriodicSend()
	{
		messageBuilder.addEvent("combatTask", "combat-task");

		tickUntilPeriodicSend();
		tickUntilPeriodicSend();

		verify(homeAssistUtils, times(2)).sendMessage(anyString());
		assertEquals(1, sentEventIds().size());
	}

	@Test
	public void testEventNotResentAfterSendNow()
	{
		messageBuilder.addEvent("levelUp", "attack-99");

		tickUtils.sendNow();
		tickUntilPeriodicSend();

		assertEquals(1, sentEventIds().size());
	}

	@Test
	public void testEventNotResentAfterShutdownSend()
	{
		messageBuilder.addEvent("clientShutdown", "Shutdown");

		tickUtils.sendShutdown();
		tickUtils.sendNow();

		assertEquals(1, sentEventIds().size());
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
		assertEquals("first", onlyEvent(payloads.get(0)).get("data").getAsString());
		assertEquals("second", onlyEvent(payloads.get(1)).get("data").getAsString());
	}

	@Test
	public void testNoPeriodicSendBeforeSendRate()
	{
		for (int i = 0; i < SEND_RATE - 1; i++)
		{
			tickUtils.onTick();
			tickUtils.sendOnSendRate();
		}

		verify(homeAssistUtils, never()).sendMessage(anyString());
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
		for (String json : captor.getAllValues())
		{
			payloads.add(gson.fromJson(json, JsonObject.class));
		}
		return payloads;
	}

	private List<String> sentEventIds()
	{
		List<String> eventIds = new ArrayList<>();
		for (JsonObject payload : sentPayloads())
		{
			for (JsonElement event : payload.getAsJsonArray("events"))
			{
				eventIds.add(event.getAsJsonObject().get("eventId").getAsString());
			}
		}
		return eventIds;
	}

	private static JsonObject onlyEvent(JsonObject payload)
	{
		assertEquals(1, payload.getAsJsonArray("events").size());
		return payload.getAsJsonArray("events").get(0).getAsJsonObject();
	}
}
