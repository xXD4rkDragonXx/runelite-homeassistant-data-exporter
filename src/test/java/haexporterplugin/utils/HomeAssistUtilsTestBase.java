package haexporterplugin.utils;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import haexporterplugin.HAExporterConfig;
import haexporterplugin.TestUtils;
import haexporterplugin.data.HAConnection;
import okhttp3.Dispatcher;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A real HomeAssistUtils without network or waiting. Every config switch is on and {@link #connection} is the only
 * connection. Every request is answered at once with {@link #responseCode} and {@link #responseBody}, retries wait in
 * {@link #scheduledRetries} and {@link #now} is the clock of the connection's backoff state.
 */
abstract class HomeAssistUtilsTestBase
{
	static final long T0 = 1_700_000_000_000L;
	static final long SECOND = 1000L;

	final Gson gson = new Gson();
	final HAExporterConfig config = TestUtils.configWithEverythingOn();
	final HAConnection connection = new HAConnection("http://ha.local:8123", "secret-token");
	HomeAssistUtils homeAssistUtils;

	long now = T0;
	int responseCode = 200;
	String responseBody = "{}";
	final List<Request> sentRequests = new ArrayList<>();
	final List<String> sentBodies = new ArrayList<>();
	final List<Runnable> scheduledRetries = new ArrayList<>();

	@Before
	public void setUpHomeAssistUtils() throws Exception
	{
		ConfigUtils configUtils = mock(ConfigUtils.class);
		when(configUtils.getStoredConnections()).thenAnswer(invocation -> List.of(connection));

		// The direct executor runs the response callback before the call that sent the request returns
		OkHttpClient okHttpClient = new OkHttpClient.Builder()
			.dispatcher(new Dispatcher(MoreExecutors.newDirectExecutorService()))
			.addInterceptor(chain ->
			{
				Buffer body = new Buffer();
				chain.request().body().writeTo(body);
				sentRequests.add(chain.request());
				sentBodies.add(body.readUtf8());
				return response(chain.request(), responseCode, responseBody);
			})
			.build();

		ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
		when(executor.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenAnswer(invocation ->
		{
			scheduledRetries.add(invocation.getArgument(0));
			return null;
		});

		homeAssistUtils = new HomeAssistUtils();
		homeAssistUtils.config = config;
		TestUtils.setField(homeAssistUtils, "gson", gson);
		TestUtils.setField(homeAssistUtils, "configUtils", configUtils);
		TestUtils.setField(homeAssistUtils, "okHttpClient", okHttpClient);
		TestUtils.setField(homeAssistUtils, "executor", executor);
		TestUtils.setField(homeAssistUtils, "backoff", new ConnectionBackoff(() -> now));
	}

	static Response response(Request request, int code, String body)
	{
		return new Response.Builder()
			.request(request)
			.protocol(Protocol.HTTP_1_1)
			.code(code)
			.message("Test")
			.body(ResponseBody.create(MediaType.get("application/json"), body))
			.build();
	}
}
