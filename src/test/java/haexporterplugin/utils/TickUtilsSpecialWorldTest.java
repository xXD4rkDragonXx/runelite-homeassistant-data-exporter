package haexporterplugin.utils;

import com.google.gson.Gson;
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

import java.util.EnumSet;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TickUtilsSpecialWorldTest
{
	private static final int SEND_RATE = 3;

	private final Gson gson = new Gson();
	private Client client;
	private HAExporterConfig config;
	private HomeAssistUtils homeAssistUtils;
	private MessageBuilder messageBuilder;
	private TickUtils tickUtils;

	@Before
	public void setUp()
	{
		client = mock(Client.class);
		config = mock(HAExporterConfig.class);
		homeAssistUtils = mock(HomeAssistUtils.class);
		when(config.sendRate()).thenReturn(SEND_RATE);
		when(config.sendSpecialWorldData()).thenReturn(false);

		// Providers.of avoids Guice member-injecting the mocks' inherited @Inject fields
		Injector injector = Guice.createInjector(binder ->
		{
			binder.bind(Client.class).toProvider(Providers.of(client));
			binder.bind(HAExporterConfig.class).toProvider(Providers.of(config));
			binder.bind(HomeAssistUtils.class).toProvider(Providers.of(homeAssistUtils));
			binder.bind(Gson.class).toInstance(gson);
		});
		messageBuilder = injector.getInstance(MessageBuilder.class);
		tickUtils = injector.getInstance(TickUtils.class);
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
	public void testSpecialWorldEventsDoNotLeakIntoLaterMessages()
	{
		onWorld(WorldType.MEMBERS, WorldType.DEADMAN);
		messageBuilder.addEvent("levelUp", "from-sendNow");
		tickUtils.sendNow();
		messageBuilder.addEvent("achievementDiary", "from-periodic");
		tickUntilPeriodicSend();

		onWorld(WorldType.MEMBERS);
		tickUtils.sendNow();

		assertEquals(0, onlySentPayload().getAsJsonArray("events").size());
	}

	@Test
	public void testSpecialWorldSentWhenEnabled()
	{
		when(config.sendSpecialWorldData()).thenReturn(true);
		onWorld(WorldType.MEMBERS, WorldType.SEASONAL);
		messageBuilder.addEvent("levelUp", "attack-99");

		tickUtils.sendNow();

		assertEquals(1, onlySentPayload().getAsJsonArray("events").size());
	}

	@Test
	public void testMembersWorldSentByDefault()
	{
		onWorld(WorldType.MEMBERS);

		tickUtils.sendNow();
		tickUntilPeriodicSend();
		tickUtils.sendShutdown();

		verify(homeAssistUtils, times(3)).sendMessage(anyString());
	}

	@Test
	public void testFreeToPlayWorldSentByDefault()
	{
		onWorld();

		tickUtils.sendNow();

		verify(homeAssistUtils).sendMessage(anyString());
	}

	private void onWorld(WorldType... types)
	{
		EnumSet<WorldType> worldTypes = EnumSet.noneOf(WorldType.class);
		for (WorldType type : types)
		{
			worldTypes.add(type);
		}
		when(client.getWorldType()).thenReturn(worldTypes);
	}

	private void tickUntilPeriodicSend()
	{
		for (int i = 0; i < SEND_RATE; i++)
		{
			tickUtils.onTick();
			tickUtils.sendOnSendRate();
		}
	}

	private JsonObject onlySentPayload()
	{
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(homeAssistUtils).sendMessage(captor.capture());
		return gson.fromJson(captor.getValue(), JsonObject.class);
	}
}
