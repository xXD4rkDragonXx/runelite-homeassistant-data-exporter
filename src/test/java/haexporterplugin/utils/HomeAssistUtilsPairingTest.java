package haexporterplugin.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import haexporterplugin.HAExporterConfig;
import haexporterplugin.HAExporterPlugin;
import haexporterplugin.data.HAConnection;
import haexporterplugin.data.PairingException;
import haexporterplugin.data.TokenCallback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class HomeAssistUtilsPairingTest
{
	private static final String VERSION_HEADER = "X-Osrs-Exporter-Version";

	private final Gson gson = new Gson();
	private HomeAssistUtils homeAssistUtils;
	private OkHttpClient okHttpClient;

	// Canned response returned for every call, and the last request that was sent
	private int responseCode;
	private String responseBody;
	private volatile Request sentRequest;

	// Backing value for the stored connections config item (see createConfigUtils)
	private String storedConnections = "[]";

	@Before
	public void setUp() throws Exception
	{
		// The interceptor answers every call itself, so no network is involved
		okHttpClient = new OkHttpClient.Builder()
			.addInterceptor(chain ->
			{
				sentRequest = chain.request();
				return new Response.Builder()
					.request(chain.request())
					.protocol(Protocol.HTTP_1_1)
					.code(responseCode)
					.message("Test")
					.body(ResponseBody.create(MediaType.get("application/json"), responseBody))
					.build();
			})
			.build();

		homeAssistUtils = new HomeAssistUtils();
		// Inject dependencies via reflection since we're not using Guice in tests
		setField(homeAssistUtils, "gson", gson);
		setField(homeAssistUtils, "okHttpClient", okHttpClient);
	}

	@After
	public void tearDown()
	{
		okHttpClient.dispatcher().executorService().shutdown();
	}

	/* ============================
	   VERSION HEADER
	   ============================ */

	@Test
	public void testPairRequestSendsVersionHeader() throws Exception
	{
		pair(200, "{\"token\":\"abc123\"}");

		assertEquals("POST", sentRequest.method());
		assertEquals("http://ha.local:8123/api/osrs-data/pair", sentRequest.url().toString());
		assertEquals(HAExporterPlugin.PLUGIN_VERSION, sentRequest.header(VERSION_HEADER));
		assertNull(sentRequest.header("X-Osrs-Token"));
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
	public void testPairWithOnlyTokenHasNoName() throws Exception
	{
		PairResult result = pair(200, "{\"token\":\"abc123\"}");

		assertNull(result.failure);
		assertEquals("abc123", result.token);
		assertNull(result.name);
	}

	@Test
	public void testPairPassesNameFromResponse() throws Exception
	{
		PairResult result = pair(200, "{\"token\":\"abc123\",\"name\":\"My Server\"}");

		assertNull(result.failure);
		assertEquals("abc123", result.token);
		assertEquals("My Server", result.name);
	}

	@Test
	public void testPairSanitizesNameFromResponse() throws Exception
	{
		PairResult result = pair(200, "{\"token\":\"abc123\",\"name\":\"<html><b>Evil</b></html>\"}");

		assertEquals("abc123", result.token);
		assertEquals("htmlbEvil/b/html", result.name);
	}

	@Test
	public void testPairFailureCarriesServerError() throws Exception
	{
		PairResult result = pair(400, "{\"error\":\"Code expired\"}");

		assertNull(result.token);
		assertTrue(result.failure instanceof PairingException);
		assertEquals("Code expired", ((PairingException) result.failure).getServerMessage());
		assertTrue(result.failure.getMessage().startsWith("Unexpected response"));
	}

	@Test
	public void testPairFailureWithHtmlBodyHasNoServerError() throws Exception
	{
		PairResult result = pair(502, "<html><body><h1>502 Bad Gateway</h1></body></html>");

		assertTrue(result.failure instanceof PairingException);
		assertNull(((PairingException) result.failure).getServerMessage());
		assertTrue(result.failure.getMessage().startsWith("Unexpected response"));
	}

	@Test
	public void testPairFailureWithEmptyBodyHasNoServerError() throws Exception
	{
		PairResult result = pair(404, "");

		assertTrue(result.failure instanceof PairingException);
		assertNull(((PairingException) result.failure).getServerMessage());
	}

	/* ============================
	   parsePairName
	   ============================ */

	@Test
	public void testParsePairName()
	{
		assertEquals("My Server", HomeAssistUtils.parsePairName(json("{\"token\":\"t\",\"name\":\"My Server\"}")));
	}

	@Test
	public void testParsePairNameMissing()
	{
		assertNull(HomeAssistUtils.parsePairName(json("{\"token\":\"t\"}")));
	}

	@Test
	public void testParsePairNameNonString()
	{
		assertNull(HomeAssistUtils.parsePairName(json("{\"name\":42}")));
		assertNull(HomeAssistUtils.parsePairName(json("{\"name\":true}")));
		assertNull(HomeAssistUtils.parsePairName(json("{\"name\":null}")));
		assertNull(HomeAssistUtils.parsePairName(json("{\"name\":{\"value\":\"My Server\"}}")));
		assertNull(HomeAssistUtils.parsePairName(json("{\"name\":[\"My Server\"]}")));
	}

	@Test
	public void testParsePairNameBlank()
	{
		assertNull(HomeAssistUtils.parsePairName(json("{\"name\":\"\"}")));
		assertNull(HomeAssistUtils.parsePairName(json("{\"name\":\"   \"}")));
		assertNull(HomeAssistUtils.parsePairName(json("{\"name\":\"\\t\\n\"}")));
	}

	@Test
	public void testParsePairNameStripsHtml()
	{
		String name = HomeAssistUtils.parsePairName(json("{\"name\":\"<html><b>Evil</b></html>\"}"));

		assertEquals("htmlbEvil/b/html", name);
	}

	@Test
	public void testParsePairNameTrimsAndRemovesControlCharacters()
	{
		assertEquals("My Server", HomeAssistUtils.parsePairName(json("{\"name\":\"  My\\u0000 Server\\n  \"}")));
	}

	@Test
	public void testParsePairNameIsCappedAt64Characters()
	{
		String name = HomeAssistUtils.parsePairName(json("{\"name\":\"" + "a".repeat(100) + "\"}"));

		assertEquals("a".repeat(64), name);
	}

	/* ============================
	   extractPairError
	   ============================ */

	@Test
	public void testExtractPairError()
	{
		assertEquals("Code expired", HomeAssistUtils.extractPairError(gson, "{\"error\":\"Code expired\"}"));
	}

	@Test
	public void testExtractPairErrorNonJson()
	{
		assertNull(HomeAssistUtils.extractPairError(gson, "<html><body><h1>502 Bad Gateway</h1></body></html>"));
		assertNull(HomeAssistUtils.extractPairError(gson, "Forbidden"));
		assertNull(HomeAssistUtils.extractPairError(gson, "{\"error\":\"Code exp"));
	}

	@Test
	public void testExtractPairErrorWithoutErrorField()
	{
		assertNull(HomeAssistUtils.extractPairError(gson, "{}"));
		assertNull(HomeAssistUtils.extractPairError(gson, "{\"detail\":\"Code expired\"}"));
		assertNull(HomeAssistUtils.extractPairError(gson, "[\"error\"]"));
		assertNull(HomeAssistUtils.extractPairError(gson, "\"error\""));
		assertNull(HomeAssistUtils.extractPairError(gson, "null"));
	}

	@Test
	public void testExtractPairErrorNonStringError()
	{
		assertNull(HomeAssistUtils.extractPairError(gson, "{\"error\":42}"));
		assertNull(HomeAssistUtils.extractPairError(gson, "{\"error\":null}"));
		assertNull(HomeAssistUtils.extractPairError(gson, "{\"error\":{\"message\":\"Code expired\"}}"));
		assertNull(HomeAssistUtils.extractPairError(gson, "{\"error\":[\"Code expired\"]}"));
	}

	@Test
	public void testExtractPairErrorEmptyBody()
	{
		assertNull(HomeAssistUtils.extractPairError(gson, null));
		assertNull(HomeAssistUtils.extractPairError(gson, ""));
		assertNull(HomeAssistUtils.extractPairError(gson, "   "));
		assertNull(HomeAssistUtils.extractPairError(gson, "{\"error\":\"   \"}"));
	}

	@Test
	public void testExtractPairErrorKeepsNewlinesOnly()
	{
		String error = HomeAssistUtils.extractPairError(gson, "{\"error\":\"Code expired.\\r\\nRequest a new one.\\u0007\"}");

		assertEquals("Code expired.\nRequest a new one.", error);
	}

	@Test
	public void testExtractPairErrorStripsHtml()
	{
		assertEquals("htmlbNope/b", HomeAssistUtils.extractPairError(gson, "{\"error\":\"<html><b>Nope</b>\"}"));
	}

	@Test
	public void testExtractPairErrorIsCappedAt200Characters()
	{
		String error = HomeAssistUtils.extractPairError(gson, "{\"error\":\"" + "x".repeat(500) + "\"}");

		assertEquals("x".repeat(200), error);
	}

	/* ============================
	   sanitizeServerText
	   ============================ */

	@Test
	public void testSanitizeNull()
	{
		assertNull(HomeAssistUtils.sanitizeServerText(null, 64, false));
	}

	@Test
	public void testSanitizeRemovesControlCharacters()
	{
		assertEquals("My Server", HomeAssistUtils.sanitizeServerText("My\u0000 Serv\u001ber\u007f\t", 64, false));
	}

	@Test
	public void testSanitizeKeepsNewlinesOnlyWhenRequested()
	{
		assertEquals("ab", HomeAssistUtils.sanitizeServerText("a\nb", 64, false));
		assertEquals("a\nb", HomeAssistUtils.sanitizeServerText("a\nb", 64, true));
		assertEquals("a\nb", HomeAssistUtils.sanitizeServerText("a\r\nb", 64, true));
	}

	@Test
	public void testSanitizeRemovesAngleBrackets()
	{
		String result = HomeAssistUtils.sanitizeServerText("<html><script>x</script>", 64, true);

		assertEquals("htmlscriptx/script", result);
	}

	@Test
	public void testSanitizeCapsLengthAndTrims()
	{
		assertEquals("abcde", HomeAssistUtils.sanitizeServerText("abcdefgh", 5, false));
		assertEquals("abc", HomeAssistUtils.sanitizeServerText("abc   def", 5, false));
	}

	@Test
	public void testSanitizeDoesNotSplitSurrogatePair()
	{
		// "ab" followed by an emoji (a surrogate pair); cutting at 3 would leave half of it
		String text = "ab" + new String(Character.toChars(0x1F600));
		assertEquals("ab", HomeAssistUtils.sanitizeServerText(text, 3, false));
	}

	@Test
	public void testSanitizeBlankBecomesNull()
	{
		assertNull(HomeAssistUtils.sanitizeServerText("  \u0000 <> ", 64, true));
	}

	/* ============================
	   STORING THE PAIRED CONNECTION
	   ============================ */

	@Test
	public void testAddStoredConnectionUsesName() throws Exception
	{
		ConfigUtils configUtils = createConfigUtils();
		configUtils.addStoredConnection("http://ha.local:8123", "abc123", "My Server");

		HAConnection connection = configUtils.getStoredConnections().get(0);
		assertEquals("abc123", connection.getToken());
		assertEquals("My Server", connection.getFriendlyName());
		assertEquals("My Server", connection.getDisplayName());
	}

	@Test
	public void testAddStoredConnectionWithoutName() throws Exception
	{
		ConfigUtils configUtils = createConfigUtils();
		configUtils.addStoredConnection("http://ha.local:8123", "abc123", null);

		HAConnection connection = configUtils.getStoredConnections().get(0);
		assertNull(connection.getFriendlyName());
		assertEquals("http://ha.local:8123", connection.getDisplayName());
		assertFalse(storedConnections.contains("friendlyName"));
	}

	/* ============================
	   HELPERS
	   ============================ */

	private PairResult pair(int code, String body) throws InterruptedException
	{
		responseCode = code;
		responseBody = body;

		PairResult result = new PairResult();
		homeAssistUtils.getToken("http://ha.local:8123", "12345", result);
		assertTrue("Pairing callback was not called", result.done.await(5, TimeUnit.SECONDS));
		return result;
	}

	private JsonObject json(String json)
	{
		return gson.fromJson(json, JsonObject.class);
	}

	// ConfigUtils only touches the stored connections item, so an in-memory proxy is enough as config
	private ConfigUtils createConfigUtils() throws Exception
	{
		HAExporterConfig config = (HAExporterConfig) Proxy.newProxyInstance(
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
		setField(configUtils, "config", config);
		setField(configUtils, "gson", gson);
		return configUtils;
	}

	private static void setField(Object target, String name, Object value) throws Exception
	{
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static class PairResult implements TokenCallback
	{
		private final CountDownLatch done = new CountDownLatch(1);
		private volatile String token;
		private volatile String name;
		private volatile Exception failure;

		@Override
		public void onSuccess(String token, String name)
		{
			this.token = token;
			this.name = name;
			done.countDown();
		}

		@Override
		public void onFailure(Exception e)
		{
			this.failure = e;
			done.countDown();
		}
	}
}
