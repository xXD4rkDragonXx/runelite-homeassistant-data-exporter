package haexporterplugin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Providers;
import haexporterplugin.utils.HomeAssistUtils;
import haexporterplugin.utils.RarityUtils;
import haexporterplugin.utils.ThievingUtils;
import haexporterplugin.utils.TickUtils;
import net.runelite.api.Client;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;

// Setup shared by the tests
public final class TestUtils
{
	private TestUtils()
	{
	}

	/**
	 * An injector for plugin classes such as notifiers. What every notifier injects is a mock (the client, config,
	 * HomeAssistUtils, TickUtils, RarityUtils and ThievingUtils), Gson is real, and Guice creates anything else, such
	 * as the MessageBuilder a test reads. The given instances replace the mocks or add bindings.
	 */
	@SuppressWarnings("unchecked")
	public static Injector injector(Map<Class<?>, Object> instances)
	{
		Map<Class<?>, Object> bindings = new HashMap<>();
		for (Class<?> type : List.of(Client.class, HAExporterConfig.class, HomeAssistUtils.class, TickUtils.class, RarityUtils.class, ThievingUtils.class))
		{
			bindings.put(type, mock(type));
		}
		bindings.put(Gson.class, new Gson());
		bindings.putAll(instances);

		// Providers.of avoids Guice member-injecting the mocks' inherited @Inject fields
		return Guice.createInjector(binder -> bindings.forEach((type, instance) ->
			binder.bind((Class<Object>) type).toProvider(Providers.of(type.cast(instance)))));
	}

	// A config with every switch on
	public static HAExporterConfig configWithEverythingOn()
	{
		return mock(HAExporterConfig.class, invocation -> invocation.getMethod().getReturnType() == boolean.class
			? Boolean.TRUE
			: RETURNS_DEFAULTS.answer(invocation));
	}

	// Sets a field that Guice would inject
	public static void setField(Object target, String name, Object value) throws ReflectiveOperationException
	{
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	// Parses leniently, so JSON in a Java string can use single quotes
	public static JsonElement json(String json)
	{
		return new Gson().fromJson(json, JsonElement.class);
	}

	// The key order doesn't matter, but numbers must be written the same: 3000000000 doesn't match 3.0E9
	public static void assertJsonEquals(String expected, String actual)
	{
		assertEquals(sortKeys(json(expected)).toString(), sortKeys(json(actual)).toString());
	}

	private static JsonElement sortKeys(JsonElement element)
	{
		if (element.isJsonArray())
		{
			JsonArray array = new JsonArray();
			element.getAsJsonArray().forEach(item -> array.add(sortKeys(item)));
			return array;
		}
		if (!element.isJsonObject())
		{
			return element;
		}
		JsonObject object = new JsonObject();
		element.getAsJsonObject().entrySet().stream()
			.sorted(Map.Entry.comparingByKey())
			.forEach(entry -> object.add(entry.getKey(), sortKeys(entry.getValue())));
		return object;
	}
}
