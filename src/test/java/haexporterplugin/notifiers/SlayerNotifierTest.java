package haexporterplugin.notifiers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Injector;
import haexporterplugin.HAExporterConfig;
import haexporterplugin.TestUtils;
import haexporterplugin.data.SlayerTask;
import haexporterplugin.utils.MessageBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SlayerNotifierTest
{
	private MessageBuilder messageBuilder;
	private SlayerNotifier notifier;
	// The task's size at assignment: the remaining amount at its highest
	private int highest;

	@Before
	public void setUp()
	{
		Injector injector = TestUtils.injector(Map.of(HAExporterConfig.class, TestUtils.configWithEverythingOn()));
		messageBuilder = injector.getInstance(MessageBuilder.class);
		notifier = injector.getInstance(SlayerNotifier.class);
	}

	@Test
	public void testSendsAssignedAndCompletedButNotKillsOrCancels()
	{
		// The first ticks after logging in only set the state, even when it changes
		tick(null, 0, 100, 10, 5);
		tick("Abyssal demons", 160, 100, 10, 5);
		for (int i = 0; i < 4; i++) tick("Abyssal demons", 160, 100, 10, 5);
		assertEquals("[]", events().toString());

		tick("Abyssal demons", 159, 100, 10, 5);
		tick(null, 0, 115, 11, 5);
		// Mortimer's tasks raise their own streak, not the normal one
		tick("Bloodveld", 80, 115, 11, 5);
		tick(null, 0, 125, 11, 6);
		// Cancelling costs points, but doesn't raise a streak
		tick("Nechryael", 100, 125, 11, 6);
		tick(null, 0, 95, 11, 6);

		TestUtils.assertJsonEquals("["
			+ "{'type':'slayerTaskComplete','data':{'task':'Abyssal demons','amount':160,'pointsEarned':15,'points':115,'streakType':'normal','streak':11}},"
			+ "{'type':'slayerTaskAssigned','data':{'task':'Bloodveld','amount':80}},"
			+ "{'type':'slayerTaskComplete','data':{'task':'Bloodveld','amount':80,'pointsEarned':10,'points':125,'streakType':'mortimer','streak':6}},"
			+ "{'type':'slayerTaskAssigned','data':{'task':'Nechryael','amount':100}}]", withoutIds(events()));
		TestUtils.assertJsonEquals("{'points':95,'streaks':{'normal':11,'wilderness':0,'mortimer':6}}",
			TestUtils.json(messageBuilder.build()).getAsJsonObject().getAsJsonObject("player").get("slayer").toString());
	}

	private void tick(String task, int remaining, int points, int normalStreak, int mortimerStreak)
	{
		highest = task == null ? 0 : Math.max(highest, remaining);
		Map<String, Integer> streaks = new LinkedHashMap<>();
		streaks.put("normal", normalStreak);
		streaks.put("wilderness", 0);
		streaks.put("mortimer", mortimerStreak);
		notifier.update(task == null
			? new SlayerTask(null, null, null, null, points, streaks)
			: new SlayerTask(task, null, remaining, highest, points, streaks));
	}

	private JsonArray events()
	{
		return TestUtils.json(messageBuilder.build()).getAsJsonObject().getAsJsonArray("events");
	}

	private static String withoutIds(JsonArray events)
	{
		for (int i = 0; i < events.size(); i++)
		{
			JsonObject event = events.get(i).getAsJsonObject();
			event.remove("eventId");
			event.remove("timestamp");
		}
		return events.toString();
	}
}
