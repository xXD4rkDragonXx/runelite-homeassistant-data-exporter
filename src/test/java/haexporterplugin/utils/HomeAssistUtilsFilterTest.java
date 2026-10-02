package haexporterplugin.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import haexporterplugin.HAExporterConfig;
import haexporterplugin.data.HAConnection;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class HomeAssistUtilsFilterTest
{
	private static final String PAYLOAD = "{"
		+ "\"player\":{"
		+ "\"name\":\"PlayerName\","
		+ "\"location\":{\"x\":1640,\"y\":9562,\"plane\":0,\"isOnBoat\":false},"
		+ "\"locationTrail\":[{\"x\":1640,\"y\":9562,\"plane\":0,\"isOnBoat\":false,\"timestamp\":1735689600000}]"
		+ "},"
		+ "\"events\":["
		+ "{\"type\":\"levelUp\",\"data\":[{\"skill\":\"Attack\",\"level\":99}]}"
		+ "]}";

	private final Gson gson = new Gson();
	private HAExporterConfig config;
	private HAConnection connection;
	private HomeAssistUtils homeAssistUtils;

	@Before
	public void setUp() throws Exception
	{
		config = mock(HAExporterConfig.class);
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

		homeAssistUtils = new HomeAssistUtils();
		homeAssistUtils.config = config;
		// Inject Gson via reflection since we're not using Guice in tests
		Field gsonField = HomeAssistUtils.class.getDeclaredField("gson");
		gsonField.setAccessible(true);
		gsonField.set(homeAssistUtils, gson);
	}

	@Test
	public void testTrailSentWhenLocationIsShared()
	{
		JsonObject filtered = filter();

		assertTrue(filtered.getAsJsonObject("player").has("location"));
		assertEquals(1, filtered.getAsJsonObject("player").getAsJsonArray("locationTrail").size());
		assertEquals(List.of("levelUp"), eventTypes(filtered));
	}

	@Test
	public void testConnectionWithoutLocationGetsNoTrail()
	{
		connection.setIncludeLocation(false);

		JsonObject filtered = filter();

		assertFalse(filtered.getAsJsonObject("player").has("location"));
		assertFalse(filtered.getAsJsonObject("player").has("locationTrail"));
		assertEquals(List.of("levelUp"), eventTypes(filtered));
	}

	@Test
	public void testGlobalLocationSwitchRemovesTrail()
	{
		when(config.includeLocation()).thenReturn(false);

		JsonObject filtered = filter();

		assertFalse(filtered.getAsJsonObject("player").has("location"));
		assertFalse(filtered.getAsJsonObject("player").has("locationTrail"));
		assertEquals(List.of("levelUp"), eventTypes(filtered));
	}

	@Test
	public void testTrailKeptWhenOtherDataIsFiltered()
	{
		connection.setIncludeInventory(false);
		connection.setIncludeLevelUpEvents(false);

		JsonObject filtered = filter();

		assertEquals(1, filtered.getAsJsonObject("player").getAsJsonArray("locationTrail").size());
		assertEquals(List.of(), eventTypes(filtered));
	}

	private JsonObject filter()
	{
		return gson.fromJson(homeAssistUtils.applyConnectionFilters(PAYLOAD, connection), JsonObject.class);
	}

	private static List<String> eventTypes(JsonObject payload)
	{
		List<String> types = new ArrayList<>();
		JsonArray events = payload.getAsJsonArray("events");
		for (JsonElement event : events)
		{
			types.add(event.getAsJsonObject().get("type").getAsString());
		}
		return types;
	}
}
