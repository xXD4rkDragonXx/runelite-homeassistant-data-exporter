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
		+ "\"locationTrail\":[{\"x\":1640,\"y\":9562,\"plane\":0,\"isOnBoat\":false,\"timestamp\":1735689600000}],"
		+ "\"bank\":{\"value\":1500000000,\"readAt\":1735689600000}"
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
		assertTrue(filtered.getAsJsonObject("player").has("bank"));
		assertEquals(List.of(), eventTypes(filtered));
	}

	// Either switch being off drops the bank value and nothing else
	@Test
	public void testBankValueSwitches()
	{
		connection.setIncludeBankValue(false);
		assertNoBank(filter());

		connection.setIncludeBankValue(true);
		JsonObject kept = filter();
		assertEquals(1_500_000_000L, kept.getAsJsonObject("player").getAsJsonObject("bank").get("value").getAsLong());

		when(config.includeBankValue()).thenReturn(false);
		assertNoBank(filter());
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

	private static void assertNoBank(JsonObject filtered)
	{
		JsonObject player = filtered.getAsJsonObject("player");
		assertFalse(player.has("bank"));
		assertTrue(player.has("location"));
		assertEquals(List.of("levelUp"), eventTypes(filtered));
	}

	private static List<String> eventTypes(JsonObject payload)
	{
		List<String> types = new ArrayList<>();
		payload.getAsJsonArray("events").forEach(event -> types.add(event.getAsJsonObject().get("type").getAsString()));
		return types;
	}
}
