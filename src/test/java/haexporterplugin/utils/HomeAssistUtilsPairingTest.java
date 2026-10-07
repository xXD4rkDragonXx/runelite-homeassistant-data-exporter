package haexporterplugin.utils;

import haexporterplugin.HAExporterConfig;
import haexporterplugin.HAExporterPlugin;
import haexporterplugin.TestUtils;
import haexporterplugin.data.HAConnection;
import haexporterplugin.data.PairingException;
import haexporterplugin.data.TokenCallback;
import okhttp3.Request;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.*;

public class HomeAssistUtilsPairingTest extends HomeAssistUtilsTestBase
{
	private static final String VERSION_HEADER = "X-Osrs-Exporter-Version";

	// Backing value for the stored connections config item (see createConfigUtils)
	private String storedConnections = "[]";

	/* ============================
	   VERSION HEADER
	   ============================ */

	@Test
	public void testPairRequestSendsVersionHeader()
	{
		pair(200, "{\"token\":\"abc123\"}");

		Request request = sentRequests.get(0);
		assertEquals("POST", request.method());
		assertEquals("http://ha.local:8123/api/osrs-data/pair", request.url().toString());
		assertEquals(HAExporterPlugin.PLUGIN_VERSION, request.header(VERSION_HEADER));
		assertNull(request.header("X-Osrs-Token"));
	}

	@Test
	public void testEventsRequestSendsVersionHeader()
	{
		Request request = homeAssistUtils.buildRequest("http://ha.local:8123/api/osrs-data/events", "{}", "secret-token");

		assertEquals(HAExporterPlugin.PLUGIN_VERSION, request.header(VERSION_HEADER));
		assertEquals("secret-token", request.header("X-Osrs-Token"));
		assertEquals("application/json", request.header("Content-Type"));
	}

	/* ============================
	   PAIR RESPONSES (END TO END)
	   ============================ */

	@Test
	public void testPairPassesTokenAndSanitizedName()
	{
		assertPaired(pair(200, "{\"token\":\"abc123\"}"), null);
		assertPaired(pair(200, "{\"token\":\"abc123\",\"name\":\"My Server\"}"), "My Server");
		assertPaired(pair(200, "{\"token\":\"abc123\",\"name\":\"<html><b>Evil</b></html>\"}"), "htmlbEvil/b/html");
	}

	@Test
	public void testPairFailureCarriesServerError()
	{
		PairResult result = pair(400, "{\"error\":\"Code expired\"}");

		assertNull(result.token);
		assertTrue(result.failure instanceof PairingException);
		assertEquals("Code expired", ((PairingException) result.failure).getServerMessage());
		assertTrue(result.failure.getMessage().startsWith("Unexpected response"));
	}

	/* ============================
	   PARSING AND SANITIZING
	   ============================ */

	@Test
	public void testParsePairName()
	{
		// Only a string with something left after sanitizing is a name
		for (String name : new String[]{"42", "true", "null", "{\"value\":\"My Server\"}", "[\"My Server\"]", "\"\"", "\"\\t\\n\""})
		{
			assertNull(name, HomeAssistUtils.parsePairName(TestUtils.json("{\"name\":" + name + "}").getAsJsonObject()));
		}
		assertEquals("a".repeat(64), HomeAssistUtils.parsePairName(TestUtils.json("{\"name\":\"" + "a".repeat(100) + "\"}").getAsJsonObject()));
	}

	@Test
	public void testExtractPairError()
	{
		// Only a string in the error field of a JSON object is an error
		String[] withoutError = {null, "", "   ", "Forbidden", "<html><body><h1>502 Bad Gateway</h1></body></html>",
			"{\"error\":\"Code exp", "{}", "{\"detail\":\"Code expired\"}", "[\"error\"]", "\"error\"", "null", "{\"error\":42}",
			"{\"error\":\"   \"}"};
		for (String body : withoutError)
		{
			assertNull(body, HomeAssistUtils.extractPairError(gson, body));
		}
		assertEquals("Code expired.\nRequest a new one.",
			HomeAssistUtils.extractPairError(gson, "{\"error\":\"Code expired.\\r\\nRequest a new one.\\u0007\"}"));
		assertEquals("x".repeat(200), HomeAssistUtils.extractPairError(gson, "{\"error\":\"" + "x".repeat(500) + "\"}"));
	}

	@Test
	public void testSanitizeServerText()
	{
		assertEquals("My Server", HomeAssistUtils.sanitizeServerText(" My\u0000 Serv\u001ber\u007f\t", 64, false));
		assertEquals("ab", HomeAssistUtils.sanitizeServerText("a\nb", 64, false));
		assertEquals("a\nb", HomeAssistUtils.sanitizeServerText("a\r\nb", 64, true));
		assertEquals("htmlscriptx/script", HomeAssistUtils.sanitizeServerText("<html><script>x</script>", 64, true));
		assertEquals("abcde", HomeAssistUtils.sanitizeServerText("abcdefgh", 5, false));
		assertEquals("abc", HomeAssistUtils.sanitizeServerText("abc   def", 5, false));
		assertNull(HomeAssistUtils.sanitizeServerText("  \u0000 <> ", 64, true));
		// Cutting "ab" and an emoji at 3 characters would keep half of the emoji's surrogate pair
		assertEquals("ab", HomeAssistUtils.sanitizeServerText("ab" + new String(Character.toChars(0x1F600)), 3, false));
	}

	/* ============================
	   STORING THE PAIRED CONNECTION
	   ============================ */

	@Test
	public void testAddStoredConnectionUsesName() throws Exception
	{
		ConfigUtils configUtils = createConfigUtils();
		configUtils.addStoredConnection("http://ha.local:8123", "abc123", "My Server");

		HAConnection stored = configUtils.getStoredConnections().get(0);
		assertEquals("abc123", stored.getToken());
		assertEquals("My Server", stored.getFriendlyName());
		assertEquals("My Server", stored.getDisplayName());
	}

	@Test
	public void testAddStoredConnectionWithoutName() throws Exception
	{
		ConfigUtils configUtils = createConfigUtils();
		configUtils.addStoredConnection("http://ha.local:8123", "abc123", null);

		HAConnection stored = configUtils.getStoredConnections().get(0);
		assertNull(stored.getFriendlyName());
		assertEquals("http://ha.local:8123", stored.getDisplayName());
		assertFalse(storedConnections.contains("friendlyName"));
	}

	/* ============================
	   HELPERS
	   ============================ */

	private PairResult pair(int code, String body)
	{
		responseCode = code;
		responseBody = body;

		PairResult result = new PairResult();
		homeAssistUtils.getToken("http://ha.local:8123", "12345", result);
		assertTrue("Pairing callback was not called", result.called);
		return result;
	}

	private static void assertPaired(PairResult result, String name)
	{
		assertNull(result.failure);
		assertEquals("abc123", result.token);
		assertEquals(name, result.name);
	}

	// ConfigUtils only touches the stored connections item, so an in-memory proxy is enough as config
	private ConfigUtils createConfigUtils() throws Exception
	{
		HAExporterConfig storedConfig = (HAExporterConfig) Proxy.newProxyInstance(
			HAExporterConfig.class.getClassLoader(),
			new Class<?>[]{HAExporterConfig.class},
			(proxy, method, args) ->
			{
				switch (method.getName())
				{
					case "homeassistantConnections":
						return storedConnections;
					case "setHomeassistantConnections":
						storedConnections = (String) args[0];
						return null;
					default:
						throw new UnsupportedOperationException(method.getName());
				}
			});

		ConfigUtils configUtils = new ConfigUtils();
		TestUtils.setField(configUtils, "config", storedConfig);
		TestUtils.setField(configUtils, "gson", gson);
		return configUtils;
	}

	private static class PairResult implements TokenCallback
	{
		private boolean called;
		private String token;
		private String name;
		private Exception failure;

		@Override
		public void onSuccess(String token, String name)
		{
			this.called = true;
			this.token = token;
			this.name = name;
		}

		@Override
		public void onFailure(Exception e)
		{
			this.called = true;
			this.failure = e;
		}
	}
}
