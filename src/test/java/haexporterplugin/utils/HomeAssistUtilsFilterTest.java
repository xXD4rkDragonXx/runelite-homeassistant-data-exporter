package haexporterplugin.utils;

import com.google.gson.JsonObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class HomeAssistUtilsFilterTest extends HomeAssistUtilsTestBase
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

	// Either switch being off is enough
	@Test
	public void testLocationSwitchesRemoveLocationAndTrail()
	{
		connection.setIncludeLocation(false);
		assertNoLocation(filter());

		connection.setIncludeLocation(true);
		when(config.includeLocation()).thenReturn(false);
		assertNoLocation(filter());
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

	private static void assertNoLocation(JsonObject filtered)
	{
		assertFalse(filtered.getAsJsonObject("player").has("location"));
		assertFalse(filtered.getAsJsonObject("player").has("locationTrail"));
		assertEquals(List.of("levelUp"), eventTypes(filtered));
	}

	private static List<String> eventTypes(JsonObject payload)
	{
		List<String> types = new ArrayList<>();
		payload.getAsJsonArray("events").forEach(event -> types.add(event.getAsJsonObject().get("type").getAsString()));
		return types;
	}
}
