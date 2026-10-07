package haexporterplugin;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
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

import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;

// Setup shared by the tests
public final class TestUtils
{
	private TestUtils()
	{
	}

	/**
	 * An injector for plugin classes such as notifiers. Everything a notifier injects is a mock, except the
	 * MessageBuilder, which is real so the test can read what was built. The given instances replace the mocks.
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
			binder.bind((Class<Object>) type).toProvider(Providers.of(instance))));
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

	// Parses JSON written with single quotes, which keeps expected JSON readable in Java strings
	public static JsonElement json(String singleQuotedJson)
	{
		return new Gson().fromJson(singleQuotedJson.replace('\'', '"'), JsonElement.class);
	}
}
